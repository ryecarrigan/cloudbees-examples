import hudson.plugins.sonar.SonarGlobalConfiguration
import hudson.plugins.sonar.SonarInstallation
import hudson.plugins.sonar.model.TriggersConfig

/* Parameters for the new SonarQube installation */
def name          = ''
def serverUrl     = ''
def credentialsId = ''

/* Advanced configuration */
def webhookSecretId              = ''
def mojoVersion                  = ''
def additionalProperties         = ''
def additionalAnalysisProperties = ''

/* Trigger Exclusions */
def skipScmCause      = false
def skipUpstreamCause = false
def envVar            = ''


def sonar = hudson.ExtensionList.lookupSingleton(SonarGlobalConfiguration)
def installations = sonar.installations.toList()
def curr = installations.find { it.name == name }
if (!curr) {
  println "No SonarQube installation found with the name '${name}'"
} else {
  println "Found SonarQube installation with the name '${name}'"
  return
}

// Create a SonarInstallation with the new name and copy the current configuration.
def copy = new SonarInstallation(name, serverUrl, credentialsId, null, webhookSecretId, mojoVersion, additionalProperties, additionalAnalysisProperties, new TriggersConfig(skipScmCause, skipUpstreamCause, envVar))
println "Created new Sonar Installation with the name '${name}'"

// Append the new installation to the existing list.
installations.add(copy)

// Set the modified installations in global config.
sonar.installations = installations

return
