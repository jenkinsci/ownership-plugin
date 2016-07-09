stage 'Print Node Ownership Info'
node('requiredLabel') {
  echo "Current NODE_NAME = ${env.NODE_NAME}";
  if (ownership.node.ownershipEnabled) {
    println "Owner ID: ${ownership.node.ownerId}"
    println "Owner e-mail: ${ownership.node.ownerEmail}"
    println "Co-owner IDs: ${ownership.node.coOwnerIds}"
    println "Co-owner e-mails: ${ownership.node.coOwnerEmails}"
  } else {
    println "Ownership of ${env.NODE_NAME} is disabled";
  }
}