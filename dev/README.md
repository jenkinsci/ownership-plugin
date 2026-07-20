# Local form-testing and screenshot environment

Use `mvn hpi:run` — it already loads ownership **and** its integration plugins from `pom.xml`:

| Plugin | Purpose |
|--------|---------|
| cloudbees-folder | Folder ownership UI |
| matrix-auth / matrix-project | Item-specific security matrix |
| role-strategy | Ownership-based role macros |
| job-restrictions | Owners list restriction |
| authorize-project | Ownership authorize strategy |
| security-inspector | Permissions-for-owner report |
| mailer / token-macro / workflow-cps | Mail links, macros, Pipeline |

## One-time bootstrap (users + demo data)

Jenkins home for `hpi:run` is `./work` (gitignored).

```bash
mkdir -p work/init.groovy.d
cp dev/init.groovy.d/01-ownership-testdata.groovy work/init.groovy.d/
# Optional — Role Strategy / Job Restrictions / Authorize Project screenshots:
cp dev/init.groovy.d/02-ownership-security-testdata.groovy work/init.groovy.d/
```

Then restart `mvn hpi:run`, **or** paste/run the scripts in **Manage Jenkins → Script Console** while Jenkins is already up.

Script `02` replaces Global Matrix auth from `01` with Role Strategy. Run `01` first, then `02` when you need security screenshots.

### Users (password = username)

| User | Role in tests |
|------|----------------|
| `admin` / `admin` | Full administer |
| `alice` / `alice` | Primary owner, can manage ownership |
| `bob` / `bob` | Secondary owner / configure |
| `charlie` / `charlie` | Read-only |

### Demo items created by script `01`

| Item | Purpose |
|------|---------|
| `bare-job` | Freestyle **without** ownership (empty summary card) |
| `demo-folder` | Folder with owners alice + bob |
| `demo-folder/owned-job` | Job with owners alice + bob + charlie (hub / manage / summary) |
| `demo-folder/specific-rights-job` | Owners + **Configure specific access rights** matrix (charlie READ+BUILD) |
| `demo-folder/env-job` | Ownership Build Wrapper + shell / token-macro example |
| View `All (with Ownership)` | List View with Primary Owner column |
| Built-in node | Ownership admin / alice |

`design-lib-demo` is **not** created by the bootstrap (legacy manual leftover under `work/jobs/`; safe to delete).

Script `02` additionally creates node `demo-agent` and configures Role Strategy, Job Restrictions, and Authorize Project.

Open: http://localhost:8080/jenkins/

## Scripted documentation screenshots

Capture (or refresh) all PNGs under [`doc/images/`](../doc/images/) against a **running** Jenkins that has the bootstrap applied:

```bash
export JENKINS_URL=http://localhost:8080/jenkins
export JENKINS_USER=admin
export JENKINS_PASSWORD=admin

cd dev/screenshots
npm ci
npx playwright install chromium
npm run capture
```

Optional filters:

```bash
SHOT_FILTER=ui npm run capture          # README / job UI / abstractProject / mailto
SHOT_FILTER=config npm run capture      # Configure System / Ownership
SHOT_FILTER=security npm run capture    # requires script 02
```

Other env vars: `OUTPUT_DIR` (default `../../doc/images`), `HEADLESS=false` for headed browser.

Manifest: [`screenshots/shots.yaml`](screenshots/shots.yaml). Runner: [`screenshots/capture.mjs`](screenshots/capture.mjs).

`generatedEmailTemplate.png` is a **scripted HTML surrogate** of the mailto template (not a Mail.app screenshot).

### Shot → demo URL map (base `/jenkins`)

| PNG | Path / notes |
|-----|----------------|
| `summaryBox.png` | `/job/demo-folder/job/owned-job/` |
| `ownershipHub.png` | `.../owned-job/ownership/` |
| `manageOwners.png` | `.../owned-job/ownership/manage-owners` |
| `configureSpecifics.png` | `.../specific-rights-job/ownership/configure-project-specifics` |
| `ownerColumn.png` | `/view/All%20(with%20Ownership)/` |
| `configuration/*.png` | `/manage/configure` (Ownership section) |
| `abstractProject/*.png` | `/job/demo-folder/job/env-job/configure` |
| `generatedEmailTemplate.png` | mailto surrogate |
| `ownershipBasedSecurity/*` | Role Strategy / nodes / Authorize Project on `/manage/configureSecurity/` (after `02`) |

See also [Manual UI Testing Checklist](../doc/ManualUITesting.md).
