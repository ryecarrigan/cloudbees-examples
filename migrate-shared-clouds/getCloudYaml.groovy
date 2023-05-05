/* Script to convert a shared Kubernetes configuration to CasC YAML */

import com.cloudbees.opscenter.clouds.kubernetes.KubernetesConfiguration
import hudson.ExtensionList
import hudson.slaves.Cloud
import io.jenkins.plugins.casc.ConfigurationAsCode
import io.jenkins.plugins.casc.ConfigurationContext
import io.jenkins.plugins.casc.ConfiguratorRegistry
import io.jenkins.plugins.casc.impl.attributes.DescribableListAttribute
import io.jenkins.plugins.casc.model.Mapping
import jenkins.model.Jenkins
import org.kohsuke.stapler.RequestImpl
import org.kohsuke.stapler.Stapler

// Configure the full name of the shared configuration item
def request   = Stapler.getCurrentRequest() as RequestImpl
def fullName  = request.getParameter("name")
def cloudItem = Jenkins.get().getItemByFullName(fullName, KubernetesConfiguration)
if (!cloudItem) {
    println "error: Unable to find Kubernetes configuration named '${fullName}'"
    return
}

class CloudList { Jenkins.CloudList clouds }
def attribute = new DescribableListAttribute<>('clouds', Cloud)
def cloudList = new CloudList(clouds: cloudItem.snippets.collect { it.value } as Jenkins.CloudList)
def context   = new ConfigurationContext(ExtensionList.lookupSingleton(ConfiguratorRegistry))
def cloudYaml = attribute.describe(cloudList, context)
cloudYaml.each { cloud ->
    println "# ${cloud.kubernetes.name}"
    cloud.kubernetes.remove('containerCapStr')
    cloud.kubernetes.templates.each { template -> template.remove('slaveConnectTimeoutStr') }
}

def yamlCNode = new Mapping(jenkins: new Mapping(clouds: cloudYaml))
def yamlNode  = ConfigurationAsCode.get().toYaml(yamlCNode)
def strBuffer = new StringWriter()
ConfigurationAsCode.serializeYamlNode(yamlNode, strBuffer)

println strBuffer
return
