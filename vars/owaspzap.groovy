#!/usr/bin/env groovy

/**
 * This pipeline will execute a simple baseline owasp-zap scan, using a Persistent Volume Claim to store the reports
 *
 * A PersistentVolumeClaim needs to be created ahead of time with the definition in jenkins/pv.yml
 *
 * NOTE that typically writable volumes can only be attached to one Pod at a time, so you can't execute
 * two concurrent jobs with this pipeline. Or change readOnly: true after the first run
 */

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
                    echo "Engagement Id          : ${config.engagement_id}"
                    echo "Target URL             : ${config.target_url}"
                    
                    // Run ZAP scan without DefectDojo integration
                    sh "zap-baseline.py -t ${config.target_url} -r zap_report.html"
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
