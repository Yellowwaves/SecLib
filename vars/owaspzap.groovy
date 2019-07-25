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
    node("jnlpslave-zap-${config.project_name}"){
        try {
            stage('Checkout') {
                checkout scm
            }
            stage('ZAP Analysis') {
                container('zap'){
                    withCredentials([
                        usernamePassword(
                            usernameVariable: 'DOJO_API_KEY',
                            urlVariable: 'DOJO_URL'
                            credentialsId: config.secret_id ?: 'dojo'
                            )
                        ]) {
			echo "Target URL             : ${config.taget_url}"
			echo "Engagement Id          : ${config.engagement_id}"
			echo "DefectDojo URL         : ${dojo_url}"
                        echo "DefectDojo API Key     : ${dojo_api_key}"

			sh "zap-baseline.py -t ${config.taget_url} -g gen.conf -r testreport.html -U ${dojo_url} -A ${dojo_api_key} -I ${config.engagement_id}"

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

