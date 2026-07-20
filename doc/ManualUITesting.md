Manual UI Testing Checklist
=====

This document provides a checklist for manual verification of Ownership plugin UI surfaces.
Use it after UI changes (for example Design Library migrations) or before a release.

Related documentation:

* [Configuration](Configuration.md)
* [Ownership-Based Security](OwnershipBasedSecurity.md)
* Local test data bootstrap: [dev/README.md](../dev/README.md)
* Scripted screenshot capture: [dev/screenshots/](../dev/screenshots/) (`npm run capture`)

## Prerequisites

Before running the checklist:

* Jenkins with the Ownership plugin loaded (for example via `mvn hpi:run`)
* Integration plugins available as needed: Folders, Matrix Authorization, Role Strategy,
  Job Restrictions, Authorize Project, Security Inspector, Mailer
* At least two users (primary and secondary owners). For a ready-made setup, see [dev/README.md](../dev/README.md)
  (`admin`, `alice`, `bob`, `charlie`, demo folder/job)

Recommended demo items (created by `dev/init.groovy.d/01-ownership-testdata.groovy`):

* `demo-folder` with ownership configured
* `demo-folder/owned-job` with primary and secondary owners
* `demo-folder/specific-rights-job` with Configure specific access rights matrix
* `demo-folder/env-job` with Ownership Build Wrapper
* `bare-job` without ownership
* List View `All (with Ownership)` with Primary Owner column
* Access to a computer/agent page (built-in node is enough)

## Status pages (summary cards)

Ownership summary cards appear on item status pages.
When ownership is configured, the card should be collapsed by default with a one-line preview;
expanding it shows full details and contact actions.

### Job status page

Location: job status page (`/job/<name>/`), sidebar link `Manage Ownership`.

- [ ] `Job Owners` summary card is visible when ownership is enabled
- [ ] Collapsed preview shows the primary owner and co-owner count (for example `Alice · +2 co-owners`)
- [ ] Expanding the card shows Primary / Secondary owners
- [ ] Owner names link to user pages
- [ ] `E-mail to Job owners` action is present when mail options allow it (mail icon)
- [ ] `E-mail to Service owners` action is present when configured (mail + settings icon)
- [ ] Sidebar contains `Manage Ownership`
- [ ] Empty-state card appears when ownership is not configured (unless hidden by display options)

### Folder status page

Location: folder status page (`/job/<folder>/`).

- [ ] `Folder Owners` summary card behaves as on the job page (collapse, preview, expand, mailto)
- [ ] Sidebar contains `Manage Ownership`

### Node / computer status page

Location: agent page (`/computer/<name>/` or built-in node).

- [ ] `Node Owners` (or equivalent) summary card is visible when node ownership is enabled
- [ ] Collapse / preview / expand and mailto actions work as on job pages
- [ ] Sidebar contains `Manage Ownership`

### Run / build status page

Location: build page (`/job/<name>/<number>/`).

- [ ] Run ownership summary card is shown by default when ownership data is available
- [ ] Card can be hidden via _`Manage Jenkins` / `Configure System` / Ownership / Ownership Display Options / Hide ownership summaries on Run pages`_
- [ ] Empty ownership on runs can be hidden via `Hide ownership summaries for missing ownership info`

## Ownership action pages

### Job ownership hub

Location: `/job/<name>/ownership/`.

- [ ] Page title / app bar for ownership management
- [ ] Feature cards (or equivalent links) for `Manage Owners` and `Configure specific access rights`
- [ ] Navigation to child pages works

### Job — Manage Owners

Location: `/job/<name>/ownership/manage-owners`.

- [ ] Warning alert about possible loss of access when changing ownership
- [ ] `Primary Owner` dropdown lists candidate users
- [ ] `Secondary owners` repeatable list: add / remove rows
- [ ] Secondary owner field offers suggestions from known users (combobox / datalist)
- [ ] Secondary owner accepts and saves an unregistered user ID (warning from validation is OK)
- [ ] Save persists ownership; job status summary card updates accordingly
- [ ] Inherited ownership warning appears when ownership comes from a parent folder (if applicable)

### Job — Configure item-specific security

Location: `/job/<name>/ownership/configure-project-specifics`.

- [ ] Optional block for item-specific security is available
- [ ] Permissions matrix (Matrix Auth) can be edited when enabled
- [ ] Save and `Restore default settings...` actions work

### Folder ownership hub and Manage Owners

Location: `/job/<folder>/ownership/` and `.../manage-owners`.

- [ ] Hub shows navigation to manage owners
- [ ] Manage Owners form matches job behavior (primary select, secondary combobox, warning, save)

### Node ownership hub and Manage Owners

Location: `/computer/<name>/ownership/` and `.../manage-owners`.

- [ ] Hub and Manage Owners pages render and save node ownership
- [ ] `Configure node-specific access rights` page shows the unsupported / stub message when opened

## Global configuration

Location: _`Manage Jenkins` / `Configure System`_ — Ownership section.
See [Configuration](Configuration.md) for option meanings.

- [ ] Ownership section renders (cards / sections for general settings)
- [ ] `Require Configure rights` checkbox
- [ ] Service owners e-mail and advanced e-mail options
- [ ] Ownership management policy (hetero-radio / policy selection)
- [ ] Globally inject ownership variables (if shown)
- [ ] Specific e-mail address resolver optional block and dropdown
- [ ] Default permissions for item-specific security optional block
- [ ] Ownership Display Options (advanced)
- [ ] Ownership Inheritance Options (advanced)
- [ ] Save on Configure System persists settings

## List views

### Primary Owner column

Location: Dashboard or List View with the Ownership column enabled.
Column is configured under view configuration → Columns.

- [ ] Column header `Primary Owner` is visible
- [ ] Owner ID / link is shown for jobs the viewer can read
- [ ] Missing ownership displays the expected empty / placeholder value

### Ownership Job Filter

Location: List View configuration → Job Filters → Ownership filter.

- [ ] User selector / owner field is available
- [ ] `Accept items, where the user is a secondary owner` checkbox works
- [ ] Filter restricts the view to matching jobs

## Node monitor

Location: _`Manage Jenkins` / `Nodes`_ (`/computer/`) when the Ownership node monitor is enabled.

- [ ] Ownership column shows primary owner or `N/A`
- [ ] Monitor cause / error presentation uses a clear alert style when applicable

## Job configuration fragments

### Ownership Build Wrapper

Location: Job _`Configure`_ → Build Environment.

- [ ] Ownership-related build wrapper / environment options are configurable
- [ ] Options for injecting job / node ownership variables are available as designed

## Security integrations

These UIs depend on optional plugins. Skip items if the plugin is not installed.
See [Ownership-Based Security](OwnershipBasedSecurity.md).

### Authorize Project

Location: Job _`Configure`_ → Authorization (Authorize Project plugin).

- [ ] Ownership-based authorize project strategy can be selected
- [ ] Strategy configuration page renders without errors

### Job Restrictions

Location: Node or job restriction configuration (Job Restrictions plugin).

- [ ] Owners list restriction is available
- [ ] Accept secondary owners checkbox works
- [ ] User list repeatable / UserSelector fields accept user IDs

### Security Inspector

Location: _`Manage Jenkins`_ → Security Inspector (Security Inspector plugin).

- [ ] Report configuration allows selecting an owner and optional folder / regex filters
- [ ] Generated report body shows permissions for the selected owner

### Role Strategy macros

Location: _`Manage Jenkins` / `Configure Global Security`_ → Authorization → Role-Based Strategy.

- [ ] Ownership-related role macros (for example current user is owner / primary owner) can be used in item or node roles
- [ ] Assignment behaves as documented in [Ownership-Based Security](OwnershipBasedSecurity.md)

## Smoke pass after UI refactor

Minimum pass for Design Library / summary card changes:

- [ ] Job status summary card (collapse + mailto)
- [ ] Job Manage Owners (save primary + secondary)
- [ ] Folder status summary card
- [ ] Build page summary card
- [ ] Global Ownership section on Configure System
