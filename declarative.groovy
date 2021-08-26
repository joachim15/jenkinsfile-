pipeline {
  agent any
  tools {
    maven 'sbir_dev_maven'
    jdk 'JDK8'
  }
  parameters {
    string(name: "Version", defaultValue: "1.0.0-2", description: "Build version to download artifact from Nexus")
  }
  options {
    office365ConnectorWebhooks([
      [name: "Office 365", message: "started ${env.JOB_NAME} ${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)", url: "<webhookUrl>", notifyFailure: true, notifyBuildStart: true, notifySuccess: true, notifyUnstable: true]
    ])
  }

  stages {

    stage('Start') {
      steps {
        // send build started notifications
        office365ConnectorSend(message: "STARTED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})", webhookUrl: '<webhookUrl>')
      }
    }
	stage('Compilation and Build') {
      when {
        expression {
          return (BRANCH_NAME == 'develop' || BRANCH_NAME == 'qa' || BRANCH_NAME == 'uat')
        }
      }
      steps {
        sh 'mvn -version'
        sh 'java -version'
        sh "mvn clean -f pom.xml"
        sh 'mvn -f ${WORKSPACE}/pom.xml -DskipTests=true clean install'
      }
    }
    stage('UnitTest') {
      when {
        expression {
          return (BRANCH_NAME == 'develop')
        }
      }
      steps {
        sh "mvn test -f pom.xml -DTARGET_ENVIRONMENT=dev -Dmaven.test.failure.ignore=true"
        step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
      }
    }
    stage("SonarQube Analysis") {
      agent any
      when {
        expression {
          return (BRANCH_NAME == 'develop')
        }
      }
      steps {
        withSonarQubeEnv('SBIR_SonarQube') {
          sh 'mvn clean package -DTARGET_ENVIRONMENT=dev -Dsonar.login=<login> sonar:sonar'
        }
      }
    }
    stage("Quality Gate") {
            steps {
              timeout(time: 1, unit: 'HOURS') {
                waitForQualityGate abortPipeline: true
              }
            }
          }
        
    stage('NexusUpload') {
      when {
        expression {
          return (BRANCH_NAME == 'uat')
        }
      }
      steps {
        nexusArtifactUploader artifacts: [
            [
              artifactId: 'sbir-app',
              classifier: '',
              file: 'target/sbir-app.war',
              type: 'war'
            ]

          ],
          credentialsId: '<Credentials>',
          groupId: 'nasa.sbir',
          nexusUrl: 'nasa-sbir-nexus.amer.reisystems.com',
          nexusVersion: 'nexus3',
          protocol: 'https',
          repository: 'sbir_app_api',
          version: '1.0.0-${BUILD_NUMBER}'

      }
    }

    stage("Pull artifact from Nexus") {
      when {
        expression {
          return (BRANCH_NAME == 'uat' || BRANCH_NAME == 'master')
        }
      }
      steps {
        sh 'wget --user=${user} --password=${PWD} <https: path to artifact>'
      }
    }

    /*stage("Deploy"){
	when {
                expression {
                    return (BRANCH_NAME == 'develop')
                }
            }
	 
	steps{
        sh'''
        scp /var/lib/jenkins/workspace/<jobname>/target/sbir-app.war oracle@10.98.44.28:/vol0/jenkins/modernization;
        ssh oracle@10.98.44.28 'sh /vol0/jenkins/modernization/redeploy_sbirapp.sh' ;
        '''
    }    
 
  
  when {
                expression {
                    return (BRANCH_NAME == 'qa')
                }
            } 
	steps{
        sh'''
        scp /var/lib/jenkins/workspace/<jobname>/target/sbir-app.war oracle@10.98.44.46:/vol0/jenkins/modernization ;
        ssh oracle@10.98.44.46 'sh /vol0/jenkins/modernization/redeploy_sbir-app.sh' ;
        '''
    }    
 
  when {
                expression {
                    return (BRANCH_NAME == 'uat')
                }
            }
	// Update the below shell with commands for uat env 
	steps{
        sh'''
        scp /var/lib/jenkins/workspace/<Job_Name>/user-preferences.war oracle@10.98.44.46:/vol0/jenkins/modernization ;
        ssh oracle@10.98.44.46 'sh /vol0/jenkins/modernization/redeploy_user-preferences.sh' ;
        '''
    }    
 when {
                expression {
                    return (BRANCH_NAME == 'staging')
                }
            }
	// Update the below shell with commands for staging env 
	steps{
        sh'''
        scp /var/lib/jenkins/workspace/<Job_Name>/user-preferences.war oracle@10.98.44.46:/vol0/jenkins/modernization ;
        ssh oracle@10.98.44.46 'sh /vol0/jenkins/modernization/redeploy_user-preferences.sh' ;
        '''
    }
when {
                expression {
                    return (BRANCH_NAME == 'prod')
                }
            }
	// Update the below shell with commands for prod env 
	steps{
        sh'''
        scp /var/lib/jenkins/workspace/<Job_Name>/user-preferences.war oracle@10.98.44.46:/vol0/jenkins/modernization ;
        ssh oracle@10.98.44.46 'sh /vol0/jenkins/modernization/redeploy_user-preferences.sh' ;
        '''
    }   	
  

  }*/
  }
}
