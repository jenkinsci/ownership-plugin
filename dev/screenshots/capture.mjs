#!/usr/bin/env node
/**
 * Capture ownership-plugin documentation screenshots against a live Jenkins.
 *
 * Env:
 *   JENKINS_URL      default http://localhost:8080/jenkins
 *   JENKINS_USER     default admin
 *   JENKINS_PASSWORD default admin
 *   OUTPUT_DIR       default ../../doc/images (relative to this file)
 *   SHOT_FILTER      optional substring match on tags or file (ui|config|security|mailto)
 *   HEADLESS         default true (set 0/false for headed)
 */
import { chromium } from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import yaml from 'js-yaml';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const JENKINS_URL = (process.env.JENKINS_URL || 'http://localhost:8080/jenkins').replace(/\/$/, '');
const USER = process.env.JENKINS_USER || 'admin';
const PASSWORD = process.env.JENKINS_PASSWORD || 'admin';
const OUTPUT_DIR = path.resolve(
  process.env.OUTPUT_DIR || path.join(__dirname, '../../doc/images'),
);
const SHOT_FILTER = (process.env.SHOT_FILTER || '').trim().toLowerCase();
const HEADLESS = !['0', 'false', 'no'].includes((process.env.HEADLESS || 'true').toLowerCase());

const manifest = yaml.load(fs.readFileSync(path.join(__dirname, 'shots.yaml'), 'utf8'));
const defaultViewport = manifest.viewport || { width: 1440, height: 900 };

function matchesFilter(shot) {
  if (!SHOT_FILTER) {
    return true;
  }
  const tags = (shot.tags || []).map((t) => String(t).toLowerCase());
  if (tags.includes(SHOT_FILTER)) {
    return true;
  }
  const file = String(shot.file).toLowerCase();
  // Path-prefix match only (e.g. configuration/, ownershipBasedSecurity/)
  if (file.startsWith(`${SHOT_FILTER}/`)) {
    return true;
  }
  return false;
}

function joinUrl(base, p) {
  if (!p) {
    return base;
  }
  if (p.startsWith('http')) {
    return p;
  }
  return `${base}${p.startsWith('/') ? p : `/${p}`}`;
}

async function login(page) {
  await page.goto(`${JENKINS_URL}/login`, { waitUntil: 'domcontentloaded' });
  const user = page.locator('#j_username, input[name="j_username"]');
  if (await user.count()) {
    await user.first().fill(USER);
    await page.locator('input[name="j_password"]').fill(PASSWORD);
    await Promise.all([
      page.waitForNavigation({ waitUntil: 'domcontentloaded' }).catch(() => {}),
      page.locator('button[name="Submit"], button[type="submit"], input[type="submit"]').first().click(),
    ]);
  }
  // Confirm we are past login
  await page.goto(`${JENKINS_URL}/`, { waitUntil: 'domcontentloaded' });
  if (page.url().includes('/login')) {
    throw new Error(`Login failed for user=${USER} at ${JENKINS_URL}`);
  }
}

async function tryClick(page, selectorList) {
  const selectors = String(selectorList)
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean);
  for (const sel of selectors) {
    try {
      if (sel.startsWith('text=')) {
        const text = sel.slice('text='.length).trim();
        // Prefer Jenkins advanced buttons when the label matches one.
        const advanced = page
          .locator('button.advanced-button, button.advancedButton')
          .filter({ hasText: text })
          .first();
        if (await advanced.count()) {
          await advanced.scrollIntoViewIfNeeded();
          await advanced.click({ timeout: 5000 });
          return true;
        }
        const loc = page.getByText(text, { exact: false }).first();
        if (await loc.count()) {
          await loc.click({ timeout: 3000 });
          return true;
        }
      } else {
        const loc = page.locator(sel).first();
        if (await loc.count()) {
          await loc.click({ timeout: 3000 });
          return true;
        }
      }
    } catch {
      // try next
    }
  }
  return false;
}

async function tryScroll(page, selectorList) {
  const selectors = String(selectorList)
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean);
  for (const sel of selectors) {
    try {
      if (sel.startsWith('text=')) {
        const text = sel.slice('text='.length);
        const loc = page.getByText(text, { exact: false }).first();
        if (await loc.count()) {
          await loc.scrollIntoViewIfNeeded();
          return true;
        }
      } else {
        const loc = page.locator(sel).first();
        if (await loc.count()) {
          await loc.scrollIntoViewIfNeeded();
          return true;
        }
      }
    } catch {
      // try next
    }
  }
  return false;
}

async function runActions(page, actions = []) {
  for (const action of actions) {
    if (action.wait != null) {
      await page.waitForTimeout(Number(action.wait));
    } else if (action.click) {
      const ok = await tryClick(page, action.click);
      if (!ok) {
        console.warn(`    warn: click missed: ${action.click}`);
      }
    } else if (action.scroll) {
      const ok = await tryScroll(page, action.scroll);
      if (!ok) {
        console.warn(`    warn: scroll missed: ${action.scroll}`);
      }
    }
  }
}

async function waitReady(page, ready) {
  if (!ready) {
    return;
  }
  const selectors = String(ready)
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean);
  for (const sel of selectors) {
    try {
      await page.waitForSelector(sel, { timeout: 8000 });
      return;
    } catch {
      // try next
    }
  }
}

/**
 * Resolve a clip target. Prefer tight regions over full page.
 *
 * Spec forms (try in order if clip is a list / comma-separated):
 *   advanced-body=Advanced e-mail options
 *       → Jenkins f:advanced expanded body next to that button
 *   card=General settings
 *       → .jenkins-card whose title matches
 *   section=Ownership
 *       → .jenkins-section with matching title text
 *   text=Some label
 *       → element containing text (then nearest sensible container)
 *   CSS selector (e.g. [data-testid="ownership-global-config"])
 */
async function resolveClipLocator(page, spec) {
  const sel = String(spec).trim();
  if (!sel) {
    return null;
  }

  if (sel.startsWith('advanced-body=')) {
    const label = sel.slice('advanced-body='.length).trim();
    const btn = page
      .locator('button.advanced-button, button.advancedButton')
      .filter({ hasText: label })
      .first();
    if (!(await btn.count())) {
      return null;
    }
    // Jenkins advanced.js moves .advancedBody children to be the next sibling
    // of the button's .jenkins-form-item, then toggles that sibling's display.
    const expandedBlock = btn.locator(
      'xpath=ancestor::div[contains(@class,"jenkins-form-item")][1]/following-sibling::*[1]',
    );
    if (await expandedBlock.count()) {
      const visible = await expandedBlock.isVisible().catch(() => false);
      if (visible) {
        return expandedBlock;
      }
    }
    // Fallback: still-closed .advancedBody sibling of .advancedLink
    const nestedBody = btn.locator(
      'xpath=ancestor::div[contains(@class,"advancedLink")][1]/following-sibling::div[contains(@class,"advancedBody")][1]',
    );
    if (await nestedBody.count()) {
      return nestedBody;
    }
    return btn.locator('xpath=ancestor::div[contains(@class,"jenkins-form-item")][1]');
  }

  if (sel.startsWith('card=')) {
    const title = sel.slice('card='.length).trim();
    return page
      .locator('.jenkins-card')
      .filter({ hasText: title })
      .first();
  }

  if (sel.startsWith('section=')) {
    const title = sel.slice('section='.length).trim();
    return page
      .locator('section.jenkins-section, .jenkins-section')
      .filter({ hasText: title })
      .first();
  }

  if (sel.startsWith('text=')) {
    const text = sel.slice('text='.length).trim();
    const hit = page.getByText(text, { exact: false }).first();
    if (!(await hit.count())) {
      return null;
    }
    // Climb to a useful form/section container so we don't clip a tiny text node.
    const container = hit.locator(
      'xpath=ancestor::*[contains(@class,"jenkins-form-item") or contains(@class,"optional-block-container") or contains(@class,"jenkins-card") or contains(@class,"jenkins-section") or self::section][1]',
    );
    if (await container.count()) {
      return container;
    }
    return hit;
  }

  // Plain CSS / Playwright selector
  return page.locator(sel).first();
}

function clipCandidates(clipSpec) {
  if (clipSpec == null) {
    return [];
  }
  if (Array.isArray(clipSpec)) {
    return clipSpec.map(String);
  }
  // Do not split on commas inside :has-text("...") — use YAML lists for alternatives.
  // Keep comma-split only for simple CSS lists without parentheses.
  const raw = String(clipSpec);
  if (raw.includes('(') || raw.includes('advanced-body=') || raw.includes('card=') || raw.includes('section=')) {
    return [raw];
  }
  return raw.split(',').map((s) => s.trim()).filter(Boolean);
}

async function screenshotClip(page, clipSpec, outPath) {
  // Hide sticky Save/Apply bar so it does not overlay clipped regions.
  await page.addStyleTag({
    content: '#bottom-sticker, .bottom-sticker { display: none !important; }',
  }).catch(() => {});

  if (!clipSpec) {
    await page.screenshot({ path: outPath, fullPage: false });
    return;
  }
  const candidates = clipCandidates(clipSpec);
  for (const sel of candidates) {
    try {
      const loc = await resolveClipLocator(page, sel);
      if (!loc || !(await loc.count())) {
        continue;
      }
      await loc.scrollIntoViewIfNeeded().catch(() => {});
      await loc.screenshot({ path: outPath });
      return;
    } catch (err) {
      console.warn(`    warn: clip failed for ${sel}: ${err.message || err}`);
    }
  }
  console.warn(`    warn: clip missed (${JSON.stringify(clipSpec)}), using viewport`);
  await page.screenshot({ path: outPath, fullPage: false });
}

function mailtoHtml({ to, cc, subject, body, jobUrl }) {
  const resolvedBody = String(body || '').replaceAll('{{JOB_URL}}', jobUrl);
  const esc = (s) =>
    String(s)
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;');
  return `<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8"/>
<title>Ownership mailto template</title>
<style>
  body { margin: 0; background: #e8e8e8; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif; }
  .window { width: 720px; margin: 24px auto; background: #fff; border-radius: 10px; box-shadow: 0 8px 24px rgba(0,0,0,.18); overflow: hidden; }
  .titlebar { background: #f2f2f2; padding: 10px 14px; font-size: 13px; color: #333; border-bottom: 1px solid #ddd; }
  .row { display: grid; grid-template-columns: 72px 1fr; gap: 8px; padding: 8px 14px; border-bottom: 1px solid #eee; font-size: 13px; }
  .row label { color: #666; }
  .body { padding: 16px 14px 28px; white-space: pre-wrap; font-size: 14px; line-height: 1.45; color: #222; min-height: 220px; }
</style>
</head>
<body>
  <div class="window" id="mail">
    <div class="titlebar">New Message — Ownership contact owners (surrogate)</div>
    <div class="row"><label>To:</label><div>${esc(to)}</div></div>
    <div class="row"><label>Cc:</label><div>${esc(cc || '')}</div></div>
    <div class="row"><label>Subject:</label><div>${esc(subject)}</div></div>
    <div class="body">${esc(resolvedBody)}</div>
  </div>
</body>
</html>`;
}

async function captureMailto(page, shot, outPath) {
  const jobUrl = joinUrl(JENKINS_URL, shot.jobPath || '/job/demo-folder/job/owned-job/');
  const html = mailtoHtml({
    to: shot.to,
    cc: shot.cc,
    subject: shot.subject,
    body: shot.body,
    jobUrl,
  });
  await page.setContent(html, { waitUntil: 'domcontentloaded' });
  const loc = page.locator('#mail');
  await loc.screenshot({ path: outPath });
}

async function captureJenkinsShot(page, shot, outPath) {
  const url = joinUrl(JENKINS_URL, shot.path);
  await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 60000 });
  await waitReady(page, shot.ready);
  await runActions(page, shot.actions || []);
  // Allow Jenkins advanced.js to finish moving/showing rows
  await page.waitForTimeout(400);
  await screenshotClip(page, shot.clip, outPath);
}

async function main() {
  const shots = (manifest.shots || []).filter(matchesFilter);
  if (!shots.length) {
    console.error(`No shots matched SHOT_FILTER=${SHOT_FILTER || '(none)'}`);
    process.exit(1);
  }

  fs.mkdirSync(OUTPUT_DIR, { recursive: true });
  console.log(`Jenkins: ${JENKINS_URL}`);
  console.log(`Output:  ${OUTPUT_DIR}`);
  console.log(`Filter:  ${SHOT_FILTER || '(all)'} — ${shots.length} shot(s)`);
  console.log(`Headless: ${HEADLESS}`);

  const browser = await chromium.launch({ headless: HEADLESS });
  const context = await browser.newContext({
    viewport: defaultViewport,
    ignoreHTTPSErrors: true,
  });
  const page = await context.newPage();

  let ok = 0;
  let failed = 0;

  try {
    // Login once for Jenkins shots; mailto surrogate does not need auth
    const needsLogin = shots.some((s) => s.type !== 'mailto-surrogate');
    if (needsLogin) {
      console.log(`Logging in as ${USER}...`);
      await login(page);
    }

    for (const shot of shots) {
      const outPath = path.join(OUTPUT_DIR, shot.file);
      fs.mkdirSync(path.dirname(outPath), { recursive: true });
      process.stdout.write(`→ ${shot.file} ... `);
      try {
        if (shot.type === 'mailto-surrogate') {
          await captureMailto(page, shot, outPath);
        } else {
          await captureJenkinsShot(page, shot, outPath);
        }
        console.log('ok');
        ok += 1;
      } catch (err) {
        console.log('FAIL');
        console.error(`   ${err.message || err}`);
        failed += 1;
      }
    }
  } finally {
    await browser.close();
  }

  console.log(`Done. ok=${ok} failed=${failed}`);
  if (failed > 0) {
    process.exit(2);
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
