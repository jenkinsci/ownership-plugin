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