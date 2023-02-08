import com.cloudbees.jenkins.plugins.sshslaves.SSHConnectionDetails
import com.cloudbees.jenkins.plugins.sshslaves.SSHLauncher
import hudson.model.JDK
import hudson.slaves.DumbSlave
import hudson.tools.ToolLocationNodeProperty
import jenkins.model.Jenkins

/**
 * Script to update the Java binary and JDK tool used by all shared agents to use a new Java home.
 *
 * Set variables for the new Java home, the JDK tool name to update, and the prefix to match agents.
 *
 * javaHome - new $JAVA_HOME to set the tool; the SSH launcher's JavaPath will be $JAVA_HOME/bin/java.
 * toolName - name of JDK tool to update (default "JDK")
 * prefix   - prefix of agents to modify; will match all if left as empty String
 */
def javaHome = ''
def toolName = 'JDK'
def prefix   = ''

// Get the JDK tool descriptor
def jdk = Jenkins.get().getDescriptorByType(JDK.DescriptorImpl)
def javaPath = "${javaHome}/bin/java"

// Create a new tool location property for the configured tool name and home
def toolLocation = new ToolLocationNodeProperty.ToolLocation(jdk, toolName, javaHome)
def toolProperty = new ToolLocationNodeProperty([toolLocation])

// Iterate over all of the nodes connected to Jenkins starting with the configured prefix
def nodes = Jenkins.get().nodes.findAll { it instanceof DumbSlave && it.nodeName.startsWith(prefix) } as List<DumbSlave>
for (node in nodes) {
  // Update JavaPath on SSH Launcher
  if (node.launcher instanceof SSHLauncher) {
    def launcher = node.launcher as SSHLauncher
    if (launcher.connectionDetails?.javaPath == javaPath) {
      println "No action needed on node '${node.name}' launcher's JavaPath because it is already configured as expected"
    } else {
      println "Node '${node.name}' launcher's JavaPath doesn't match and will be updated"
      def sshDetails = launcher.connectionDetails
      def newDetails = new SSHConnectionDetails(sshDetails.credentialsId, sshDetails.port, javaPath, sshDetails.jvmOptions, sshDetails.prefixStartSlaveCmd, sshDetails.suffixStartSlaveCmd, sshDetails.displayEnvironment, sshDetails.keyVerificationStrategy)
      node.launcher = new SSHLauncher(launcher.host, newDetails)
    }
  }

  // Check if node properties contains ToolLocationNodeProperty
  def toolLocations = node.getNodeProperty(ToolLocationNodeProperty)
  if (toolLocation) {
    def locations = toolLocations.locations.toList()
    def tool = toolLocations.locations.find { it.type == jdk && it.name == toolName }
    if (tool) {
      if (tool.home == javaHome) {
        println "No action needed on node '${node.name}' tools because it is already configured as expected"
        continue
      } else {
        // If home doesn't match, remove the location for replacement
        println "Tool '${toolName}' home on node '${node.name}' doesn't match and will be replaced"
        locations.remove(tool)
      }

      toolProperty = new ToolLocationNodeProperty(locations + toolLocation)
    }

    println "Removing existing tool locations from node '${node.name}'"
    node.nodeProperties.remove(toolLocations)
  }

  println "Adding tool locations to node '${node.name}'"
  node.nodeProperties.add(toolProperty)

  println "Updating node in memory"
  Jenkins.get().getNodesObject().replaceNode(node, node)
}
