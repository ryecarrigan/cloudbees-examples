import hudson.plugins.sonar.SonarGlobalConfiguration
import hudson.plugins.sonar.SonarInstallation

def currName = 'Enterprise_sonar_1'
def nextName = 'Enterprise_sonar_1_BACK'
if (currName == nextName) {
  println "Updated name is the same as the current name. No action will be taken."
  return
}

def sonar = hudson.ExtensionList.lookupSingleton(SonarGlobalConfiguration)
def installations = sonar.installations.toList()
def curr = installations.find { it.name == currName }
if (!curr) {
  println "No SonarQube installation found with the name '${currName}'"
  return
} else {
  println "Found SonarQube installation with the name '${currName}'"
}

// Create a SonarInstallation with the new name and copy the current configuration.
def copy = new SonarInstallation(nextName, curr.serverUrl, curr.serverAuthenticationToken, curr.mojoVersion, curr.additionalProperties, curr.triggers, curr.additionalAnalysisProperties)
println "Created new Sonar Installation with the name '${nextName}'"

// Replace the current installation with the copy.
installations.remove(curr)
installations.add(copy)

// Set the modified installations in global config.
sonar.installations = installations

return
