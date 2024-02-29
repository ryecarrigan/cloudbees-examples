/**
 * This script is to help users work around BEE-18090, a bug present in CloudBees SSH Build Agents Plugin >= 2.16 and < 2.19
 *
 * You will receive compilation errors if both SSH plugins are not present!
 *
 * https://docs.cloudbees.com/docs/cloudbees-ci-kb/latest/client-and-managed-masters/why-is-my-ssh-agent-connection-terminated-with-error-server-host-key-rejected
 */

import com.cloudbees.jenkins.plugins.sshslaves.SSHConnectionDetails
import com.cloudbees.jenkins.plugins.sshslaves.SSHLauncher as CBLauncher
import hudson.plugins.sshslaves.SSHLauncher as OSSLauncher
import jenkins.model.Jenkins

// Script options.
boolean dryRun = true

/*
 * Advanced launcher settings not available in OSS SSH launcher.
  */
def displayEnvironment = false


/* Helper method to convert the key strategy from the OSS plugin to the CloudBees implementation. */
com.cloudbees.jenkins.plugins.sshslaves.verification.ServerKeyVerificationStrategy convertStrategy(hudson.plugins.sshslaves.verifiers.SshHostKeyVerificationStrategy strategy) {
  switch (strategy) {
    case hudson.plugins.sshslaves.verifiers.NonVerifyingKeyVerificationStrategy:
      return new com.cloudbees.jenkins.plugins.sshslaves.verification.BlindTrustConnectionVerificationStrategy()
    case hudson.plugins.sshslaves.verifiers.KnownHostsFileKeyVerificationStrategy:
      return new com.cloudbees.jenkins.plugins.sshslaves.verification.KnownHostsConnectionVerificationStrategy()
    case hudson.plugins.sshslaves.verifiers.ManuallyProvidedKeyVerificationStrategy:
      return new com.cloudbees.jenkins.plugins.sshslaves.verification.ManuallyConnectionVerificationStrategy(strategy.key)
    case hudson.plugins.sshslaves.verifiers.ManuallyTrustedKeyVerificationStrategy:
      return new com.cloudbees.jenkins.plugins.sshslaves.verification.TrustInitialConnectionVerificationStrategy(strategy.requireInitialManualTrust)
    default:
      // There are only the four possible cases so this should be unreachable.
      return null
  }
}

// Iterate over all shared agents.
for (node in Jenkins.get().getAllItems(com.cloudbees.opscenter.server.model.SharedSlave)) {
  def launcher = node.getLauncher()
  if (launcher instanceof OSSLauncher) {
    // If launcher is OSS, prepare to convert to CloudBees.
    if (node.getOfflineCause() == null) {
      println "Agent '${node.name}' is using OSS SSH launcher but cannot be modified while it is online."
    } else {
      println "Agent '${node.name}' is using OSS SSH launcher and is offline. Converting to OSS SSH launcher."
      if (!dryRun) {
        // Create new launcher from the OSS launcher attributes.
        def details = new SSHConnectionDetails(launcher.credentialsId, launcher.port, launcher.javaPath, launcher.jvmOptions,
            launcher.prefixStartSlaveCmd, launcher.suffixStartSlaveCmd, displayEnvironment, convertStrategy(launcher.sshHostKeyVerificationStrategy))
        def updated = new CBLauncher(launcher.host, details)

        // Update the agent with the new launcher.
        node.launcher = updated
      }
    }
  } else if (launcher instanceof CBLauncher) {
    // If launcher is already OSS, print status.
    println "Agent '${node.name}' is already using the CloudBeess SSH launcher. No action necessary."
  } else {
    // If launcher is using anything else, print status.
    println "Agent '${node.name}' is not using a SSH launcher. No action necessary."
  }
}

return
