Changelog
-----

| WARNING: Changelogs have been moved to [GitHub Releases](https://github.com/jenkinsci/ownership/releases) |
| --- |

### New releases

See [GitHub Releases](https://github.com/jenkinsci/ownership/releases).

### 0.12.1

Release date: Apr 30, 2018

* [JENKINS-49744](https://issues.jenkins-ci.org/browse/JENKINS-49744) -
Users with `Manage Ownership` permissions were unable to change Foler ownership from CLI/REST API without `Jenkins/Administer` persiossion.
* [JENKINS-50807](https://issues.jenkins-ci.org/browse/JENKINS-50807) -
Add missing implementations for [OwnershipHelperLocator](https://jenkins.io/doc/developer/extensions/ownership/#ownershiphelperlocator)
extension point.
Now the API can be used to retrieve ownership info for any object.

### 0.12.0

Release date: Feb 26, 2018

* [SECURITY-498](https://jenkins.io/security/advisory/2018-02-26/#SECURITY-498) -
Users with _Item/Configure_ and _Computer/Configure_ permissions were able to change ownership from CLI or REST API without the _Manage Ownership_ permissions.

Compatibility notes:

* External configuration management logic may fail if the client user has no _Overall/Administer_ or _Manage Ownership_ permission.
* Folder ownership modifications from REST/CLI will fail for users who have no _Overall/Administer_ perission.
Re-enabling functionality for users with _Manage Ownership_  permission is tracked as [JENKINS-49744](https://issues.jenkins-ci.org/browse/JENKINS-49744).

### 0.11.0

Release date: Jan 14, 2018

* [JENKINS-20832](https://issues.jenkins-ci.org/browse/JENKINS-20832) -
Use case sensitivity strategies from security realms when comparing user IDs.
* [JENKINS-48707](https://issues.jenkins-ci.org/browse/JENKINS-48707) -
Speedup user fetching operations in the plugin.
* [PR #65](https://github.com/jenkinsci/ownership-plugin/pull/65) - 
Add Russian localization to UI components.

### 0.10.0

Release date: May 07, 2017

Improvements:

* [PR #56](https://github.com/jenkinsci/ownership-plugin/pull/56) -
Add new Preserve Ownership Item Policy.
It allows retaining ownership in plugins like JobDSL.
* [PR #58](https://github.com/jenkinsci/ownership-plugin/pull/58) - 
Add permissions report for items being owned by the specified user. Powered by the [Security Inspector Plugin](https://plugins.jenkins.io/security-inspector).
* [PR #57](https://github.com/jenkinsci/ownership-plugin/pull/57),
[PR #60](https://github.com/jenkinsci/ownership-plugin/pull/60) - 
Extend plugin documentation and move it to GitHub.
* Update Jenkins core minimal version requirement to `1.651.3`.


Fixed issues:

* [JENKINS-42908](https://issues.jenkins-ci.org/browse/JENKINS-42908) -
When contributing ownership environment variables, check jobs by the generic `BuildableItemWithBuildWrappers` interface.


### 0.9.1

Release date: Jan 08, 2017

Fixed issues:

* [JENKINS-38353](https://issues.jenkins-ci.org/browse/JENKINS-38353) -
Fix handling of Multi-branch Pipeline and other computed folders in the ownership management logic.
* [JENKINS-38513](https://issues.jenkins-ci.org/browse/JENKINS-38513) -
Prevent `ClassNotFoundException` in `FolderItemListener` when Folders plugin is not installed or disabled.

### 0.9.0

Release date: Sep 17, 2016

Improvements:

* [JENKINS-28881](https://issues.jenkins-ci.org/browse/JENKINS-28881) -
Integration with Folders Plugin.
  * Ownership info can be defined for folders
  * Ownership info can be inherited by jobs and folders from upper folder levels
  * All Ownership-Based security features got integration with folders
* [JENKINS-32353](https://issues.jenkins-ci.org/browse/JENKINS-32353) -
Integration with Pipeline Plugin (see [this page](doc/PipelineIntegration.md) for more info).
  * Ownership can be now configured for Jenkins Pipeline
  * `ownership` global variable, which provides information about job and node ownership within Pipeline runs
* [JENKINS-28258](https://issues.jenkins-ci.org/browse/JENKINS-28258) -
Add support of non-AbstractProject job types in the [Authorize Project](https://plugins.jenkins.io/authorize-project) extension.
* [JENKINS-36946](https://issues.jenkins-ci.org/browse/JENKINS-36946) -
Ownership summary boxes: Add hint for disabling empty ownership summaries.
* [PR #46](https://github.com/jenkinsci/ownership-plugin/pull/46) -
Update to the new plugin parent POM 2.x.
* [PR #47](https://github.com/jenkinsci/ownership-plugin/pull/47) -
No more "Slave Ownership" in the UI, "Node ownership" is a correct term.

Fixed issues:

* [PR #46](https://github.com/jenkinsci/ownership-plugin/pull/46) -
Cleanup of issues reported by FindBugs.
* [JENKINS-37405](https://issues.jenkins-ci.org/browse/JENKINS-37405) -
Ownership job filter now supports all item types with ownership info.
* [JENKINS-38236](https://issues.jenkins-ci.org/browse/JENKINS-38236) -
Prevent `NullPointerException` in Item Listeners if the policy form submission provides `null` policy.

### 0.8

Release date: Oct 27, 2015

Improvements:

* [JENKINS-28714](https://issues.jenkins-ci.org/browse/JENKINS-28714) -
Add option to hide ownership summary boxes for Runs.
* [JENKINS-28712](https://issues.jenkins-ci.org/browse/JENKINS-28712) -
Add option to hide ownership summary boxes if owners are not assigned.
* [JENKINS-30254](https://issues.jenkins-ci.org/browse/JENKINS-30254) -
Allow hiding user e-mails in summary boxes.
* Better help text in ownership summary boxes for items with unassigned Ownership

Fixed issues:

* [JENKINS-30818](https://issues.jenkins-ci.org/browse/JENKINS-30818) -
Prevent `NullPointerException` in `JobOwnerJobProperty::toString()` if ownership is not configured.

### 0.7

Release date: Jun 03, 2015

Improvements: 

* [JENKINS-26768](https://issues.jenkins-ci.org/browse/JENKINS-26768) -
Contact Owners and Contact Admins links can be disabled.
* [PR #35](https://github.com/jenkinsci/ownership-plugin/pull/35) -
Remove unnecessary `//` from the `mailto:` hyperlink.

Fixed issues:

* [JENKINS-28713](https://issues.jenkins-ci.org/browse/JENKINS-28713) -
Improper permission checks in several API methods.
* [JENKINS-27715](https://issues.jenkins-ci.org/browse/JENKINS-27715) -
Properly inject owner user IDs into `JOB_COOWNERS` and `NODE_COOWNERS` environment variables.

### 0.6

Release date: Jan 18, 2015

* [JENKINS-28713](https://issues.jenkins-ci.org/browse/JENKINS-26283) -
 Add "Contact item owners" and "Contact service owners" links to ownership summary boxes.
  * Links automatically generate a e-mail stub with configurable subject and body templates
  * Owners and co-owners are being automatically added to recipients
* [JENKINS-23947](https://issues.jenkins-ci.org/browse/JENKINS-23947) -
Jenkins admins can enable the global injection of ownership variables.
* [JENKINS-26320](https://issues.jenkins-ci.org/browse/JENKINS-26320) -
Display ownership info for builds.
* [PR #28](https://github.com/jenkinsci/ownership-plugin/pull/28) -
Avoid confusing node property shown on config screen.

Fixed issues:

* [JENKINS-23926](https://issues.jenkins-ci.org/browse/JENKINS-23926) -
Inject ownership variables even if the build fails before build steps execution.
* [JENKINS-19433](https://issues.jenkins-ci.org/browse/JENKINS-19433) -
Update Mailer plugin dependency to mailer-0.9 to support user properties.

### 0.5.1

Release date: Sep 30, 2014

Fixed issues:

* [JENKINS-24475](https://issues.jenkins-ci.org/browse/JENKINS-24475) -
Managing ownership fails in Jenkins 1.565.1+.
* [JENKINS-24921](https://issues.jenkins-ci.org/browse/JENKINS-24921) -
Remove the obsolete `Assign ownership on create` configuration entry.
* [JENKINS-23657](https://issues.jenkins-ci.org/browse/JENKINS-23657) -
Use transparent PNG icons instead of GIFs.

### 0.5 

Release date: Sep 09, 2014

Improvements:

* [JENKINS-21904](https://issues.jenkins-ci.org/browse/JENKINS-21904) -
Add new Ownership policy extension point: different ownership assignment behaviors can be specified.
* [JENKINS-21838](https://issues.jenkins-ci.org/browse/JENKINS-21838) -
Add Ownership authorization strategy for the Authorize Project plugin.
* Improve Ownership summary box layouts.

Fixed issues:

* [JENKINS-24370](https://issues.jenkins-ci.org/browse/JENKINS-24370) -
Fixed the issue with redirects to absolute URLs after the ownership modification.


### 0.4

Release date: Jan 18, 2014

Improvements:

* [JENKINS-18977](https://issues.jenkins-ci.org/browse/JENKINS-18977) -
Display node ownership info in the Computer list.
* [JENKINS-21358](https://issues.jenkins-ci.org/browse/JENKINS-21358) -
Show Job ownership actions in the side panel instead of the Jenkins default panel.
* [JENKINS-20908](https://issues.jenkins-ci.org/browse/JENKINS-20908) -
Remove custom fast mail resolution handlers. Now the plugin uses native handlers from Mailer Plugin 1.6+.

Fixed issues:

* [JENKINS-20488](https://issues.jenkins-ci.org/browse/JENKINS-20488) -
Prevent possible issue during the concurrent nodes renaming and modification.

### 0.3.1

Release date: Oct 25, 2013

Fixed issues:

* [JENKINS-28713](https://issues.jenkins-ci.org/browse/JENKINS-28713),
[JENKINS-20213](https://issues.jenkins-ci.org/browse/JENKINS-20213),
[JENKINS-20181](https://issues.jenkins-ci.org/browse/JENKINS-20181) -
Fix handling of Multi-configuration (Matrix) jobs.
* [JENKINS-19993](https://issues.jenkins-ci.org/browse/JENKINS-19993) -
Fix propagation of default item-specific permissions to jobs after the reconfiguration.

### 0.3

Release date: Sep 08, 2013

Improvements:

* Allow restricting execution of jobs on agents according to ownership configuration (Extension for Job Restrictions Plugin)
* Support of item-specific access rights for jobs (Extension for Role Strategy Plugin)
  * `@ItemSpecific` macro - Check the SID against entries from the item-specific security
  * `@ItemSpecificWithUserId` macro - if SID is "authenticated", checks current user's ID
* Support of Token Macros (`${OWNERSHIP,var=varname}`)

### 0.2.1

Release date: Aug 08, 2013

Fixed issues:

* Hotfix: Added support of the fast e-mail resolver to fix the UI performance issue.
(copy of default resolver from Mailer 1.5)

### 0.2 

Release date: Aug 02, 2013

Enhancements:

* Integration with Role Strategy Plugin via macros.
* Added a build wrapper, which exposes ownership variables to environment.
* Added support of co-owners.
* Replaced built-in e-mail suffix resolver by configurable selector of `MailAddressResolver` extension.
* Ownership management has been migrated to a separate `Manage Ownership` page.

Known compatibility issues:

:exclamation: 
Update from `0.1` to `0.2` corrupts plugin's data structure (issue #19078). You will need to manually cleanup data in the ownership.xml after the update.

### 0.1 

Release date: Jul 12, 2013

* Initial release: ownership management and visualization
