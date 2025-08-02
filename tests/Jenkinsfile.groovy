pipeline {
    agent any

    environment {
        STAGING = "ubuntu@<staging-ec2-ip>"
        PRODUCTION = "ubuntu@<production-ec2-ip>"
    }

    stages {
        stage('Clone') {
            steps {
                git credentialsId: 'your-credential-id', url: 'https://github.com/youruser/flask-app.git'
            }
        }

        stage('Install') {
            steps {
                sh 'pip install -r requirements.txt'
            }
        }

        stage('Test') {
            steps {
                sh 'pytest tests/'
            }
        }

        stage('Build') {
            steps {
                sh 'zip -r app.zip .'
            }
        }

        stage('Deploy to Staging') {
            steps {
                sshagent(['staging-ssh']) {
                    sh '''
                        scp -o StrictHostKeyChecking=no app.zip $STAGING:/home/ubuntu/
                        ssh -o StrictHostKeyChecking=no $STAGING '
                            unzip -o app.zip -d /home/ubuntu/app &&
                            sudo systemctl restart flask
                        '
                    '''
                }
            }
        }

        stage('Approval for Production') {
            steps {
                input message: 'Approve Production Deployment?', ok: 'Deploy'
            }
        }

        stage('Deploy to Production') {
            steps {
                sshagent(['staging-ssh']) {
                    sh '''
                        scp -o StrictHostKeyChecking=no app.zip $PRODUCTION:/home/ubuntu/
                        ssh -o StrictHostKeyChecking=no $PRODUCTION '
                            unzip -o app.zip -d /home/ubuntu/app &&
                            sudo systemctl restart flask
                        '
                    '''
                }
            }
        }
    }
}
