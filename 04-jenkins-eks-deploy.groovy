pipeline {
    agent any
    environment {
        SONARQUBE_ENV = 'sonar'
        JFROG_ACCESS_TOKEN = credentials('jfrog_id')
        DOCKER_HUB_ACCESS_TOKEN = credentials('dockerhub_id')
        AWS_ACCESS_KEY_ID = credentials('AWS_ACCESS_KEY_ID')
        AWS_SECRET_ACCESS_KEY = credentials('AWS_SECRET_ACCESS_KEY')
        AWS_DEFAULT_REGION = 'us-west-2'
        DOCKER_IMAGE = 'matt96/ss_project'
        EKS_CLUSTER_NAME  = "sscluster"
        AWS_CREDS         = "aws-creds"
    }
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
        stage('Sonar Analysis') {
            steps {
                withSonarQubeEnv("${SONARQUBE_ENV}") {
                    sh 'mvn sonar:sonar'
                }
            }
        }
        stage('Upload jar to JFrog Artifactory') {
            steps {
                sh 'jf rt upload --url http://192.166.1.165:8082/artifactory/ --access-token ${JFROG_ACCESS_TOKEN} target/database_service_project-0.0.3-SNAPSHOT.jar java_spring_app'
            }
        }
        stage('docker build image build push deploy') {
            steps {
                withCredentials([string(credentialsId: 'dockerhub_id', variable: 'DOCKER_HUB_ACCESS_TOKEN')]) {
                    script {
                        def imageTag = "${DOCKER_IMAGE}:${BUILD_NUMBER}"
                           sh "docker login -u matt96 -p $DOCKER_HUB_ACCESS_TOKEN"
                           sh "docker build -t ${imageTag} ."
                           sh "docker push ${imageTag}"
                    }
                }
            }
        }
        stage('Initializing Terraform') {
            steps {
                script{
                    dir('EKS'){
                        sh 'terraform init'
                    }
                }
            }
        }
        stage('Validating Terraform'){
            steps {
                script{
                    dir('EKS'){
                        sh 'terraform validate'
                    }
                }
            }
        }
        stage('Terraform Plan'){
            steps{
                script{
                    dir('EKS'){
                        sh 'terraform plan'
                    }
                }
            }
        }
        stage('Create eks cluster'){
            steps{
                script{
                    dir('EKS'){
                        sh 'terraform apply --auto-approve'
                    }
                }
            }
        }
        stage('Configure kubectl for EKS') {
            steps{
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: AWS_CREDS]]) {
                    sh """
                    aws eks update-kubeconfig \
                       --region ${AWS_DEFAULT_REGION} \
                       --name ${EKS_CLUSTER_NAME}
                    """
                }
            }
        }
        stage('Deploy to EKS') {
            steps{
                script {
                    dir('EKS/configuration-files'){
                        sh """
                        sed -i 's|DOCKER_IMAGE|${DOCKER_IMAGE}|g' k8s-deployment.yaml
                        sed -i 's|IMAGE_TAG|${BUILD_NUMBER}|g' k8s-deployment.yaml
                        kubectl apply -f k8s-deployment.yaml
                        """
                    }
                }
            }
        }
    }
}