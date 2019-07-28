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
            image: 'abdessamadtmr/zap',
            ttyEnabled: true,
            privileged: true,
            alwaysPullImage: true
        )
    ],
  volumes: [
    hostPathVolume(mountPath: "/var/run/docker.sock", hostPath: "/var/run/docker.sock"),
    persistentVolumeClaim(mountPath: '/zap/reports', claimName: 'reports-data', readOnly: false)
  ]
) {
    node("jnlpslave-zap-${config.engagement_id}"){
        try {
            stage('Checkout') {
                checkout scm
            }
            stage('ZAP Analysis') {
                container('zap'){
                    withCredentials([string(credentialsId: 'dojo_url', variable: 'DOJO_URL'), string(credentialsId: 'dojo_api_key', variable: 'DOJO_API_KEY')]) {
                        echo "Engagement Id          : ${config.engagement_id}"
                        echo "Target URL             : ${config.target_url}"
                        echo "DefectDojo URL         : $DOJO_URL"
                        echo "DefectDojo API KEY     : $DOJO_API_KEY"
			
            sh "whoami"
            sh "ls -ald > /zap/reports/"
            sh "zap-baseline.py -t ${config.target_url}; exit 0"
                    }
                }
            }
        }
        catch(Exception e) {
            println "error message : ${e}"
        }
        finally {
            Notification{
                notification_to = config.notification_to ?: false
            }
        }
    }
}
}

