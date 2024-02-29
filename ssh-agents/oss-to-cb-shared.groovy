import com.cloudbees.jenkins.plugins.sshslaves.SSHConnectionDetails
import com.cloudbees.jenkins.plugins.sshslaves.SSHLauncher as CBLauncher
import com.cloudbees.jenkins.plugins.sshslaves.verification.*
import com.cloudbees.opscenter.server.model.SharedSlave
import hudson.plugins.sshslaves.SSHLauncher as OSSLauncher
import hudson.plugins.sshslaves.verifiers.*
import jenkins.model.Jenkins

// Script options.
boolean dryRun = true

/*
 * Advanced launcher settings not available in OSS SSH launcher.
  */
def displayEnvironment = false


/* Helper method to convert the key strategy from the OSS plugin to CloudBees implementation. */
static ServerKeyVerificationStrategy convertStrategy(SshHostKeyVerificationStrategy strategy) {
  switch (strategy) {
    case NonVerifyingKeyVerificationStrategy:
      return new BlindTrustConnectionVerificationStrategy()
    case KnownHostsFileKeyVerificationStrategy:
      return new KnownHostsConnectionVerificationStrategy()
    case ManuallyProvidedKeyVerificationStrategy:
      return new ManuallyConnectionVerificationStrategy(strategy.key)
    case ManuallyTrustedKeyVerificationStrategy:
      return new TrustInitialConnectionVerificationStrategy(strategy.requireInitialManualTrust)
    default:
      // There are only the four possible cases so this should be unreachable.
      return null
  }
}

// Iterate over all static agents.
for (node in Jenkins.get().getAllItems(SharedSlave)) {
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
