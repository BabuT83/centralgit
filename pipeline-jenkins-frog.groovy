pipeline {
    agent any

    environment {
        SONARQUBE_ENV = 'sonar'
        JFROG_ACCESS_TOKEN = credentials('jfrog_id')

    }

    tools {
        maven 'Maven 3.8.6'
    }

    stages {

        stage('Git Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/ssinu398/ss_gameapp.git'
            }
        }

        stage('Build with Maven') {
            steps {
                sh 'java -version'
                sh 'mvn clean package'
            }
        }

        stage('Sonar Analysis') {
            steps {
                withSonarQubeEnv("${SONARQUBE_ENV}") {
                    sh 'mvn sonar:sonar'
                }
            }
        }

        stage('Upload jar to JFrog Artifactory') {
            steps {
                sh 'jf rt upload --url http://192.168.10.162:8082/artifactory/ --access-token ${JFROG_ACCESS_TOKEN} target/database_service_project-0.0.3-SNAPSHOT.jar java_spring_app'
            }
        }

        
    }
}