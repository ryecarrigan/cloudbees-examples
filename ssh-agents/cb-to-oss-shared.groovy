/**
 * This script is to help users work around BEE-18090, a bug present in CloudBees SSH Build Agents Plugin >= 2.16 and < 2.19
 *
 * You will receive compilation errors if both SSH plugins are not present!
 *
 * https://docs.cloudbees.com/docs/cloudbees-ci-kb/latest/client-and-managed-masters/why-is-my-ssh-agent-connection-terminated-with-error-server-host-key-rejected
 */

import com.cloudbees.jenkins.plugins.sshslaves.SSHLauncher as CBLauncher
import hudson.plugins.sshslaves.SSHLauncher as OSSLauncher
import jenkins.model.Jenkins

// Script options.
boolean dryRun = true

/*
 * Advanced launcher settings not available in CloudBees SSH launcher.
 * https://github.com/jenkinsci/ssh-slaves-plugin/blob/main/doc/CONFIGURE.md#advanced-settings
 */
def launchTimeoutSeconds = OSSLauncher.DEFAULT_LAUNCH_TIMEOUT_SECONDS
def maxNumRetries = OSSLauncher.DEFAULT_MAX_NUM_RETRIES
def retryWaitTime = OSSLauncher.DEFAULT_RETRY_WAIT_TIME
def tcpNoDelay = true
def workDir = null


/* Helper method to convert the key strategy from CloudBees implementation to the OSS plugin. */
hudson.plugins.sshslaves.verifiers.SshHostKeyVerificationStrategy convertStrategy(com.cloudbees.jenkins.plugins.sshslaves.verification.ServerKeyVerificationStrategy strategy) {
  switch (strategy) {
    case com.cloudbees.jenkins.plugins.sshslaves.verification.BlindTrustConnectionVerificationStrategy:
      return new hudson.plugins.sshslaves.verifiers.NonVerifyingKeyVerificationStrategy()
    case com.cloudbees.jenkins.plugins.sshslaves.verification.KnownHostsConnectionVerificationStrategy:
      return new hudson.plugins.sshslaves.verifiers.KnownHostsFileKeyVerificationStrategy()
    case com.cloudbees.jenkins.plugins.sshslaves.verification.ManuallyConnectionVerificationStrategy:
      return new hudson.plugins.sshslaves.verifiers.ManuallyProvidedKeyVerificationStrategy(strategy.key)
    case com.cloudbees.jenkins.plugins.sshslaves.verification.TrustInitialConnectionVerificationStrategy:
      return new hudson.plugins.sshslaves.verifiers.ManuallyTrustedKeyVerificationStrategy(strategy.manualVerification)
    default:
      // There are only the four possible cases so this should be unreachable.
      return null
  }
}

// Iterate over all static agents.
for (node in Jenkins.get().getAllItems(com.cloudbees.opscenter.server.model.SharedSlave)) {
  def launcher = node.getLauncher()
  if (launcher instanceof CBLauncher) {
    // If launcher is CloudBees, prepare to convert to OSS.
    if (node.getOfflineCause() == null) {
      println "Agent '${node.name}' is using CloudBees SSH launcher but cannot be modified while it is online."
    } else {
      println "Agent '${node.name}' is using CloudBees SSH launcher and is offline. Converting to OSS SSH launcher."
      if (!dryRun) {
        // Create new launcher from the CloudBees launcher attributes.
        def details = launcher.connectionDetails
        def updated = new OSSLauncher(launcher.host, details.port, details.credentialsId,
            details.jvmOptions, details.javaPath, details.prefixStartSlaveCmd, details.suffixStartSlaveCmd,
            launchTimeoutSeconds, maxNumRetries, retryWaitTime, convertStrategy(details.keyVerificationStrategy)
        )

        // Set the advanced fields in the OSS launcher.
        updated.tcpNoDelay = tcpNoDelay
        updated.workDir = workDir

        // Update the agent with the new launcher.
        node.launcher = updated
      }
    }
  } else if (launcher instanceof OSSLauncher) {
    // If launcher is already OSS, print status.
    println "Agent '${node.name}' is already using the OSS SSH launcher. No action necessary."
  } else {
    // If launcher is using anything else, print status.
    println "Agent '${node.name}' is not using a SSH launcher. No action necessary."
  }
}

return
