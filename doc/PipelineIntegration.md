Pipeline Integration
====

Ownership plugin is integrated with [Jenkins Pipeline](https://jenkins.io/doc/book/pipeline/).

The following features are available:

* Ownership Management in both Pipeline and Multi-branch Pipeline jobs.
* Inheritance of ownership info from folders by Pipeline and Multi-branch Pipeline jobs.
* Access to the ownership information via the `ownership` global variable.

## Managing Ownership in Pipeline jobs

Ownership management in Pipeline is similar to other job types.
All operations can be done in the `Manage Ownership` action if the user has appropriate permissions.

In Multi-Branch Pipeline it is possible to manage ownership only on the top level.
Fine-grain ownership management for branches is not supported.

## Ownership Global variable

If the Plugin is install, the `ownership` global variable will be available in all jobs.
The `ownership` variable contains `job` and `node` fields, which represent the required info.
Both fields are **read-only**, it is not possible to modify the Ownership info from Pipeline.

The `node` variable is available only within the `node()` block scope.
If it is called outside this block, the behavior is undefined.

### Examples

<!-- Keep examples aligned with the built-in docs src/main/resources -->

The example below demonstrates usage of the `ownership`global variable

```groovy
stage 'Print Job Ownership Info';
def primaryOwnerEmail = ownership.job.primaryOwnerEmail
if (ownership.job.ownershipEnabled) {
  println "Primary owner ID: ${ownership.job.primaryOwnerId}"
  println "Primary owner e-mail: ${primaryOwnerEmail}"
  println "Secondary owner IDs: ${ownership.job.secondaryOwnerIds}"
  println "Secondary owner e-mails: ${ownership.job.secondaryOwnerEmails}"
} else {
  println "Ownership is disabled";
}

stage 'Send e-mail to the job owner'
mail to: primaryOwnerEmail,
  subject: "Job '${env.JOB_NAME}' (${env.BUILD_NUMBER}) is waiting for input",
  body: "Please go to ${env.BUILD_URL} and verify the build"

```

This example demonstrates usage of the Node ownership info:

```groovy
stage 'Print Node Ownership Info'
node('requiredLabel') {
  echo "Current NODE_NAME = ${env.NODE_NAME}";
  if (ownership.node.ownershipEnabled) {
    println "Owner ID: ${ownership.node.primaryOwnerId}"
    println "Owner e-mail: ${ownership.node.primaryOwnerEmail}"
    println "Co-owner IDs: ${ownership.node.secondaryOwnerIds}"
    println "Co-owner e-mails: ${ownership.node.secondaryOwnerEmails}"
  } else {
    println "Ownership of ${env.NODE_NAME} is disabled";
  }
}
```