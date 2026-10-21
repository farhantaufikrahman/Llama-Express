// Pipeline CI/CD Llama Express — monorepo (Backend/backend, Frontend/frontend, Infra)
pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '20'))
    timeout(time: 60, unit: 'MINUTES')
  }

  environment {
    JAVA_HOME    = '/opt/jdk-25'                      // mvnw memilih JDK dari JAVA_HOME → build memakai JDK 25
    BACKEND_DIR  = 'Backend/backend'
    FRONTEND_DIR = 'Frontend/frontend'
    // Testcontainers berjalan di Docker host (lewat socket) → port container dijangkau via host.docker.internal
    TESTCONTAINERS_HOST_OVERRIDE = 'host.docker.internal'
    CI = 'true'

    DOCKERHUB_NAMESPACE = 'farhanrahman27'
    BACKEND_IMAGE  = "${DOCKERHUB_NAMESPACE}/llama-express-backend"
    FRONTEND_IMAGE = "${DOCKERHUB_NAMESPACE}/llama-express-frontend"
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
              // Konfigurasi test berasal dari profil "test" + Testcontainers → tidak butuh rahasia apa pun
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
        // sonar.qualitygate.wait=true → scanner menunggu hasil quality gate dan GAGAL bila gate tidak lulus
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

    // ───────────────────────── J3 ─────────────────────────
    stage('Build images') {
      // Di SEMUA branch: membuktikan Dockerfile tetap bisa di-build sebelum di-merge
      environment {
        MIDTRANS_CLIENT_KEY = credentials('midtrans-client-key')   // client key = publik, tapi tetap dari credential agar tidak di-hardcode
      }
      steps {
        sh '''
          docker build --pull \
            --label org.opencontainers.image.revision="$GIT_COMMIT" \
            --label org.opencontainers.image.source="$GIT_URL" \
            -t "$BACKEND_IMAGE:$GIT_SHORT" \
            "$BACKEND_DIR"

          docker build --pull \
            --build-arg VITE_MIDTRANS_CLIENT_KEY="$MIDTRANS_CLIENT_KEY" \
            --label org.opencontainers.image.revision="$GIT_COMMIT" \
            --label org.opencontainers.image.source="$GIT_URL" \
            -t "$FRONTEND_IMAGE:$GIT_SHORT" \
            "$FRONTEND_DIR"
        '''
      }
    }

    stage('Push images') {
      // HANYA dari main: registry hanya berisi kode yang sudah lulus review/merge
      when { branch 'main' }
      environment {
        DOCKERHUB = credentials('dockerhub')   // → DOCKERHUB_USR & DOCKERHUB_PSW (disamarkan di log)
      }
      steps {
        sh '''
          echo "$DOCKERHUB_PSW" | docker login -u "$DOCKERHUB_USR" --password-stdin
          for img in "$BACKEND_IMAGE" "$FRONTEND_IMAGE"; do
            docker push "$img:$GIT_SHORT"           # tag permanen (dipakai deploy di J4)
            docker tag  "$img:$GIT_SHORT" "$img:main"
            docker push "$img:main"                 # tag bergerak (kemudahan saja)
          done
        '''
      }
      post {
        always { sh 'docker logout || true' }   // token tidak tertinggal di ~/.docker/config.json Jenkins
      }
    }
  }

  post {
    success { echo "Pipeline hijau untuk ${env.GIT_SHORT}" }
    failure { echo "Pipeline gagal — buka tab Tests / Console Output untuk detailnya" }
    always  { cleanWs(deleteDirs: true, notFailBuild: true) }
  }
}