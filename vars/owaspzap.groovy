#!/usr/bin/env groovy
def call(Closure configBlock) {
    def config= [:]
    configBlock.resolveStrategy = Closure.DELEGATE_FIRST
    configBlock.delegate = config
    configBlock()
    def uniqId = UUID.randomUUID().toString()

podTemplate(
    label: "jnlpslave-zap-${config.engagement_id}",
    containers: [
        containerTemplate(
            name: 'jnlp',
            image: 'jenkinsci/jnlp-slave',
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
            image: 'gcr.io/cybercops/zap',
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
  volumes: [
    hostPathVolume(mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock'),
  ]
) {
    node("jnlpslave-zap-${config.engagement_id}"){
        try {
            stage('Checkout') {
                checkout scm
            }
            stage('ZAP Analysis') {
                container('zap'){
			withCredentials([
				[string(credentialsId: 'dojo', variable: 'DOJO_API_KEY')],
				[string(credentialsId: 'dojo', variable: 'DOJO_URL')]
			]) {
				echo "Target URL             : ${config.taget_url}"
				echo "Engagement Id          : ${config.engagement_id}"
				echo "DefectDojo URL         : ${dojo_url}"
				echo "DefectDojo API Key     : ${dojo_api_key}"
			}
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

