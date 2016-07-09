stage 'Print Job Ownership Info';
if (ownership.job.ownershipEnabled) {
  println "Owner ID: ${ownership.job.ownerId}"
  println "Owner e-mail: ${ownership.job.ownerEmail}"
  println "Co-owner IDs: ${ownership.job.coOwnerIds}"
  println "Co-owner e-mails: ${ownership.job.coOwnerEmails}"
} else {
  println "Ownership is disabled";
}
