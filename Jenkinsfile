pipeline {
  agent any

  environment {
    AWS_REGION     = 'us-east-1'
    AWS_ACCOUNT_ID = '602167897668'
    ECR_URI        = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/microffull/bff-client"
    CLUSTER        = 'microffull-cluster'
    SERVICE        = 'bff-client-service'
    TASK_FAMILY    = 'bff-client'
    IMAGE_TAG      = "${env.GIT_COMMIT.take(7)}"
  }

  stages {
    stage('Test') {
      steps {
        sh 'mvn -B test'
      }
      post {
        always {
          junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
        }
      }
    }

    stage('Package') {
      steps {
        sh 'mvn -B package -DskipTests'
      }
    }

    stage('Docker Build & Push') {
      steps {
        withCredentials([string(credentialsId: 'github-packages-token', variable: 'GITHUB_TOKEN')]) {
          sh """
            DOCKER_BUILDKIT=1 docker build --secret id=github_token,env=GITHUB_TOKEN -t ${ECR_URI}:${IMAGE_TAG} .
            aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com
            docker push ${ECR_URI}:${IMAGE_TAG}
          """
        }
      }
    }

    stage('Deploy to ECS') {
      steps {
        sh """
          aws ecs describe-task-definition --task-definition ${TASK_FAMILY} --region ${AWS_REGION} \
            --query 'taskDefinition' > current-task-def.json

          jq --arg IMAGE "${ECR_URI}:${IMAGE_TAG}" \
            '.containerDefinitions[0].image = \$IMAGE
             | del(.taskDefinitionArn, .revision, .status, .requiresAttributes, .compatibilities, .registeredAt, .registeredBy)' \
            current-task-def.json > new-task-def.json

          NEW_TASK_DEF_ARN=\$(aws ecs register-task-definition \
            --cli-input-json file://new-task-def.json \
            --region ${AWS_REGION} \
            --query 'taskDefinition.taskDefinitionArn' --output text)

          aws ecs update-service --cluster ${CLUSTER} --service ${SERVICE} \
            --task-definition \$NEW_TASK_DEF_ARN --region ${AWS_REGION}
        """
      }
    }
  }

  post {
    always {
      sh "docker rmi ${ECR_URI}:${IMAGE_TAG} || true"
      sh "docker image prune -af --filter 'until=24h' || true"
    }
  }
}
