stage 'Print Job Ownership Info';
if (ownership.job.ownershipEnabled) {
  println "Owner ID: ${ownership.job.primaryOwnerId}"
  println "Owner e-mail: ${ownership.job.primaryOwnerEmail}"
  println "Co-owner IDs: ${ownership.job.secondaryOwnerIds}"
  println "Co-owner e-mails: ${ownership.job.secondaryOwnerEmails}"
} else {
  println "Ownership is disabled";
}
