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
