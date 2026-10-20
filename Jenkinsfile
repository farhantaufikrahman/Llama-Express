pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()                         // build berikutnya di branch yang sama menunggu, tidak berebut workspace
    buildDiscarder(logRotator(numToKeepStr: '20'))    // simpan 20 build terakhir saja
    timeout(time: 45, unit: 'MINUTES')                // build macet tidak memakan executor selamanya
  }

  environment {
    JAVA_HOME    = '/opt/jdk-25'                     
    BACKEND_DIR  = 'Backend/backend'
    FRONTEND_DIR = 'Frontend/frontend'
    TESTCONTAINERS_HOST_OVERRIDE = 'host.docker.internal'
    CI = 'true'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
        script {
          env.GIT_SHORT = sh(script: 'git rev-parse --short=12 HEAD', returnStdout: true).trim()
          currentBuild.displayName = "#${env.BUILD_NUMBER} · ${env.GIT_SHORT}"
        }
      }
    }

    stage('Test') {
      parallel {
        stage('Backend') {
          steps {
            dir(env.BACKEND_DIR) {
              sh 'sh ./mvnw -B -ntp verify'
            }
          }
          post {
            always {
              junit allowEmptyResults: true,
                    testResults: "${env.BACKEND_DIR}/target/surefire-reports/*.xml, ${env.BACKEND_DIR}/target/failsafe-reports/*.xml"
            }
          }
        }

        stage('Frontend') {
          steps {
            dir(env.FRONTEND_DIR) {
              sh '''
                npm ci --no-audit --no-fund
                npx tsc -b
                npm run test:ci
                npm run lint --if-present
              '''
            }
          }
          post {
            always {
              junit allowEmptyResults: true, testResults: "${env.FRONTEND_DIR}/reports/jest-junit.xml"
            }
          }
        }
      }
    }

    stage('Coverage') {
      steps {
        recordCoverage(
          id: 'coverage',
          name: 'Coverage',
          tools: [
            [parser: 'JACOCO',    pattern: "${env.BACKEND_DIR}/target/site/jacoco/jacoco.xml"],
            [parser: 'COBERTURA', pattern: "${env.FRONTEND_DIR}/coverage/cobertura-coverage.xml"]
          ],
          sourceCodeRetention: 'LAST_BUILD'
        )
      }
    }

        stage('SonarQube') {
      steps {
        withSonarQubeEnv('sonarqube') {
          dir(env.BACKEND_DIR) {
            sh '''
              sh ./mvnw -B -ntp sonar:sonar \
                -Dsonar.token="$SONAR_AUTH_TOKEN" \
                -Dsonar.qualitygate.wait=true \
                -Dsonar.qualitygate.timeout=300
            '''
          }
          dir(env.FRONTEND_DIR) {
            sh '''
              npx --yes @sonar/scan \
                -Dsonar.host.url="$SONAR_HOST_URL" \
                -Dsonar.token="$SONAR_AUTH_TOKEN" \
                -Dsonar.qualitygate.wait=true \
                -Dsonar.qualitygate.timeout=300
            '''
          }
        }
      }
    }
  }

  post {
    success { echo "CI hijau untuk ${env.GIT_SHORT}" }
    failure { echo "CI gagal — buka tab Tests / Console Output untuk detailnya" }
    always  { cleanWs(deleteDirs: true, notFailBuild: true) }
  }
}