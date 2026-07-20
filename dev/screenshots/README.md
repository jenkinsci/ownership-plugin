# Documentation screenshot capture

Playwright-based capture of ownership-plugin PNGs under `doc/images/`.

## Prerequisites

1. Jenkins running with Ownership demo data (see [`../README.md`](../README.md)).
2. For `security` shots, also run `02-ownership-security-testdata.groovy`.

## Usage

```bash
export JENKINS_URL=http://localhost:8080/jenkins
export JENKINS_USER=admin
export JENKINS_PASSWORD=admin

npm ci
npx playwright install chromium
npm run capture
```

Filters (exact tag or path prefix):

| Command | Shots |
|---------|--------|
| `npm run capture` | all |
| `npm run capture:ui` | README / job UI / abstractProject / mailto |
| `npm run capture:config` | `configuration/*` |
| `npm run capture:security` | `ownershipBasedSecurity/*` (needs script 02) |

Manifest: [`shots.yaml`](shots.yaml).

### Tight clips

Prefer element screenshots over the full viewport. In `shots.yaml`, `clip` can be:

| Spec | Meaning |
|------|---------|
| `advanced-body=Advanced e-mail options` | Expanded Jenkins `f:advanced` block |
| `card=General settings` | `.jenkins-card` with that title |
| `section=Access Control for Builds` | `.jenkins-section` with that title |
| `text=…` | Nearest form/card/section ancestor of that text |
| CSS / testid | e.g. `[data-testid="ownership-global-config"]` |

Use a YAML list for fallbacks. Avoid `#main-panel` except for full action pages (hub / manage-owners).
