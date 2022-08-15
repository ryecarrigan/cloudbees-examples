// Retrieve the Beekeeper update site.
def updateSite = Jenkins.get().updateCenter.sites.find { it instanceof com.cloudbees.jenkins.plugins.assurance.site.BeekeeperUpdateSite }
if (!updateSite) {
  println "No Beekeeper update site is configured."
  return null
}

// Pull the list of plugins from the CloudBees online update site data.
def plugins = updateSite.online.data.plugins

// Pull the list of plugins configured in plugin catalogs.
def catalogPlugins = com.cloudbees.jenkins.plugins.assurance.CloudBeesAssurance.get().pluginCatalogAction.viewData.includedPlugins
if (!catalogPlugins) {
  println "Instance does not have any plugins configured with plugin catalogs."
  return null
}

println "Catalog plugins with updates on this version (${Jenkins.VERSION})"
for (plugin in catalogPlugins) {
  def name = plugin.name
  def curr = plugin.version

  // Check that the catalog plugin is available online.
  if (!plugins.containsKey(name)) {
    println "\t[${name}] '${curr}' configured, but not available in Update Center"
    continue
  }

  // If the update center shows a different version than the catalog has, print a message.
  def next = plugins[name].version
  if (curr != next) {
    println "\t[${name}] '${curr}' configured, '${next}' available"
  }
}

return null
