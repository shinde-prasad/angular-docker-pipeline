pipeline {
    agent any
    environment {
		// environment variables and credential retrieval can be interspersed
		def dirpath = "D:\\workspace\\Angularapp"
		def tomcatPath = "C:\\apache-tomcat-9.0.45\\"
		def projectName = "hello-world"
		def autoCancelled = false
		def container = 'angularapp-pipeline-demo'
	}
    stages {
        // try {
            stage('Intialisation') {
                steps {
                    echo "=================================Check path variables==============================="
    				echo "PATH = ${PATH}"
    				echo "tomcatPath  = ${tomcatPath}"
    				echo "dirpath = ${dirpath}"
                    bat('docker -v')

                    //check if docker service daemon running or not
                    // if ('docker -v') {
                    //     // exit %ERRORLEVEL%
                    //     autoCancelled = true
                    //     error('Aborting the build to prevent a loop.')
                    // }
                }
            }
            // stage('BUILD') {
            //     steps {
            //         echo "build path ="
            //         dir(dirpath+'\\'+projectName) {
            //             echo "inside build path"
            //             bat("npm install && npm run-script build -- --optimization=true")
            //         }
            //     }
            //     post {
            //         success {
            //             echo "Build Successful"
            //         }
            //         failure {
            //             echo "Build Failed"
            //         }
            //     }
            // }
            stage('code quality check') {
                environment {
                    sonarScannerHome = tool 'sonar-scanner'
                }
    			steps {
    				echo "TEST Phase Started :: Via sonarqube scripts"
    				dir(dirpath+'\\'+projectName) {
    				    withSonarQubeEnv('SonarQube') {
    				        // bat('sonar-scanner -v')
                            // bat("sonar-scanner")
                            bat "${sonarScannerHome}/bin/sonar-scanner.bat -X"
                        }
    				}
    			}
    		}
    		stage("Quality gate") {
                steps {
                    script {
                        // def qg = waitForQualityGate abortPipeline: true
                        echo "============================= Check Quality Gate ========================================="
                        def qg = waitForQualityGate()
                        if (qg.status != 'OK') {
                            echo "Quality gate status: ${qg.status}"
                            error "Pipeline aborted due to quality gate failure: ${qg.status}"
                        }else {
                            echo "============================= Quality gate status: ${qg.status}============================= "
                        }
                    }
                }
            }
    		stage('POST BUILD PROVISIONING DEPLOYMENT') {
    			steps {
    			    dir(tomcatPath+'\\bin') {
    					bat('shutdown.sh')
    				}
    				dir(dirpath+'\\'+projectName) {
    				    bat("docker build -t ${container} .")
    				    echo "Image Built............"
    				    echo "removing old container..............."
    				    bat("docker rm -f ${container}")
                //       script {
                //           if("docker container inspect --format='{{json .State}}' angularapp-pipeline-demo 2> /dev/null") {
                //               //container doesnt exists
    				        // echo "container does not exist."
                //           }else {
                //               echo "removing old container."
    				        //     bat('docker rm -f ${container}')
                //           }
                //       }

                        bat("docker run -d -p 8084:80 --name ${container} ${container}")
    				    echo "Now you can open link in your browser: 'http://localhost:8084'"
    				}
    			}
    		}
        // }catch(e) {
        //     if(autoCancelled) {
        //         currentBuild.result = 'ABORTED'
        //         echo('Skipping mail notification')
        //         // return here instead of throwing error to keep the build "green"
        //         return
        //     }
        //     // normal error handling try catch block
        //     throw e
        // }
    }
}
