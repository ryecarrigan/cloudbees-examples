import com.cloudbees.opscenter.clouds.kubernetes.KubernetesConfiguration

jenkins.model.Jenkins.get().getAllItems(KubernetesConfiguration).each {
    println it.fullName
}

return
