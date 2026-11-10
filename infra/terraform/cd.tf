# AWS에 등록한 GitHub OIDC ID 제공업체의 ARN
variable "github_oidc_provider_arn" {
  type = string
}

# 배포 역할을 요청할 수 있는 GitHub 저장소와 브랜치
variable "github_oidc_subject" {
  type = string
}

# GitHub Actions가 AWS 배포 작업을 할 때 사용할 역할
resource "aws_iam_role" "github_deploy" {
  name = "lottecinema-lab-github-deploy"

  # 누가 이 역할을 맡을 수 있는지 정하는 신뢰 정책
  assume_role_policy = jsonencode({
    Version = "2012-10-17"

    Statement = [{
      Effect = "Allow"

      # OIDC 토큰을 이용해 IAM 역할을 요청하는 작업
      Action = "sts:AssumeRoleWithWebIdentity"

      # 토큰을 확인할 ID 제공업체
      Principal = {
        Federated = var.github_oidc_provider_arn
      }

      # 토큰의 대상과 주체가 둘 다 일치해야 한다.
      Condition = {
        StringEquals = {
          "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
          "token.actions.githubusercontent.com:sub" = var.github_oidc_subject
        }
      }
    }]
  })
}

# EC2에 배포 명령을 전달할 SSM 문서
resource "aws_ssm_document" "deploy" {
  name            = "LottecinemaLabDeploy"
  document_type   = "Command" # EC2에 명령을 실행할 때 사용하는 문서라는 뜻
  document_format = "YAML"

  # Terraform의 값을 SSM 문서가 읽을 YAML 형식으로 변환한다.
  content = yamlencode({
    schemaVersion = "2.2"
    description   = "Install one LOTTECINEMA release"

    parameters = {
      # GitHub Actions에서 배포할 커밋 SHA를 전달한다.
      Release = { # GitHub Actions가 어느 커밋을 배포할지 전달하는 입력값들
        type = "String"

        # 소문자 16진수로 된 40자리 값만 허용한다.
        allowedPattern = "^[0-9a-f]{40}$"

        # 입력값을 SSM_Release 환경변수로 전달한다.
        interpolationType = "ENV_VAR"
      }
    }

    mainSteps = [{ # EC2에서 실제로 수행할 단계를 정의
      # EC2에서 셸 스크립트를 실행하는 SSM 기능
      action = "aws:runShellScript"
      name   = "deploy"

      inputs = {
        # 명령의 최대 실행 시간은 240초
        timeoutSeconds = "240"

        # EC2에 설치할 배포 스크립트에 커밋 SHA를 전달한다.
        runCommand = [
          "exec /usr/local/sbin/deploy-lottecinema \"$SSM_Release\""
        ]
      }
    }]
  })
}

# GitHub Actions가 배포에 사용할 권한
resource "aws_iam_role_policy" "github_deploy" {
  name = "deploy-dev-to-ec2"

  # 앞에서 만든 GitHub Actions 역할에 정책을 연결한다.
  role = aws_iam_role.github_deploy.name

  policy = jsonencode({
    Version = "2012-10-17"

    Statement = [
      {
        # 배포용 S3 경로에 JAR를 업로드할 수 있다.
        Effect = "Allow"
        Action = "s3:PutObject"

        Resource = "${aws_s3_bucket.artifacts.arn}/releases/*"
      },
      {
        # 지정한 SSM 문서를 지정한 EC2에서 실행할 수 있다.
        Effect = "Allow"
        Action = "ssm:SendCommand"

        Resource = [
          aws_ssm_document.deploy.arn,
          aws_instance.app.arn
        ]
      },
      {
        # SSM에 요청한 배포 명령의 실행 결과를 확인할 수 있다.
        Effect   = "Allow"
        Action   = "ssm:GetCommandInvocation"
        Resource = "*"
      }
    ]
  })
}

# GitHub Actions가 맡을 IAM 역할의 ARN
output "github_deploy_role_arn" {
  value = aws_iam_role.github_deploy.arn
}

# GitHub Actions가 실행할 SSM 문서 이름
output "deploy_document_name" {
  value = aws_ssm_document.deploy.name
}