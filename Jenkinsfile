pipeline {
    agent any

    tools {
        jdk 'JDK17'
        maven 'Maven3'
    }

    options {
        timestamps()
        timeout(time: 45, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    parameters {
        choice(name: 'ENVIRONMENT', choices: ['qa', 'dev', 'prod'], description: 'Configuration environment')
    }

    environment {
        GOREST_ACCESS_TOKEN = credentials('gorest-access-token')
    }

    stages {
        stage('Compile') {
            steps {
                sh 'mvn --batch-mode clean -DskipTests test-compile'
            }
        }
        stage('Smoke') {
            steps {
                script {
                    catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                        sh 'TEST_ENVIRONMENT="$ENVIRONMENT" bash scripts/run-ci-suite.sh smoke'
                    }
                }
            }
        }
        stage('Regression') {
            steps {
                script {
                    catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                        sh 'TEST_ENVIRONMENT="$ENVIRONMENT" bash scripts/run-ci-suite.sh regression'
                    }
                }
            }
        }
        stage('End-to-End') {
            steps {
                script {
                    catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                        sh 'TEST_ENVIRONMENT="$ENVIRONMENT" bash scripts/run-ci-suite.sh e2e'
                    }
                }
            }
        }
        stage('Mock') {
            steps {
                script {
                    catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                        sh 'TEST_ENVIRONMENT="$ENVIRONMENT" bash scripts/run-ci-suite.sh mocking'
                    }
                }
            }
        }
    }

    post {
        always {
            junit testResults: 'target/ci-surefire-reports/**/*.xml', allowEmptyResults: true
            archiveArtifacts artifacts: 'target/allure-results/**,target/site/allure-maven-plugin/**,target/ci-surefire-reports/**,target/surefire-reports/**',
                    allowEmptyArchive: true
            script {
                if (fileExists('target/allure-results')) {
                    allure includeProperties: false,
                            results: [[path: 'target/allure-results']],
                            reportBuildPolicy: 'ALWAYS',
                            resultPolicy: 'LEAVE_AS_IS'
                }
            }
        }
    }
}
