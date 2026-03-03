pipeline {
   agent any
  tools {
        maven 'Maven 3.8.6'
    }
    stages {
        stage('Git Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/Devssmat/cicd-project-1.git'
            }
        }
      stage('Build with Maven') {
            steps {
                sh 'java -version'
                sh 'mvn clean package'
            }
        }
    }
}