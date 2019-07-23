#!/usr/bin/env groovy
def call(Closure configBlock) {
    def config= [:]
    configBlock.resolveStrategy = Closure.DELEGATE_FIRST
    configBlock.delegate = config
    configBlock()
    def uniqId = UUID.randomUUID().toString()

JobProperties{
    days_to_keep            = "30"
    num_to_keep             = "30"
    upstream_project        = null
    allow_concurrent_builds = false
    optimize_performance    = true
}
podTemplate(
    label: "jnlpslave-checkmarx-${config.project_name}",
    containers: [
        containerTemplate(
            name: 'jnlp',
            image: 'jenkinsci/jnlp-slave:2.62',
            ttyEnabled: true,
            privileged: true,
            envVars: [
            containerEnvVar(
                key: 'HOME',
                value: '/home/jenkins/'
                ),
            ]
        ),
        containerTemplate(
            name: 'zap',
            image: 'owasp/zap2docker-stable',
            ttyEnabled: true,
            alwaysPullImage: true,
            envVars: [
                containerEnvVar(
                    key: 'HOME',
                    value: '/root/'
                )
            ]
        )
    ],
) {
    node("jnlpslave-zap-${config.project_name}"){
        try {
            stage('Checkout') {
                checkout scm
            }
            stage('ZAP Analysis') {
                container('checkmarx'){
                echo "Project Name        : ${config.project_name}"
                }
            }
        }
        catch(Exception e) {
            println "error message : ${e}"
            currentBuild.result = 'FAILURE'
            throw e
        }
        finally {
            Notification{
                notification_to = config.notification_to ?: false
            }
        }
    }
}
}

