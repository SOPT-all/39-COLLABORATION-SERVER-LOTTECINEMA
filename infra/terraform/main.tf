# 이 프로제트에서 사용할 테라폼과 Provider 버전을 정의함
# Provider는 Terraform이 AWS API와 통신할 수 있도록 해 주는 프로그램
terraform {
  required_version = ">= 1.5, < 2.0"

  required_providers {
    aws = {
      # AWS 리소스를 관리하는 공식 Provider
      source = "hashicorp/aws"

      # AWS Provider의 6.x 버전을 사용한다.
      version = "~> 6.0"
    }
  }
}
# AWS 리소스를 만들 리전을 지정
provider "aws" {
  region = "ap-northeast-2"
}

# EC2 운영체제 이미지 ID를 외부에서 받는다.
# variable은 Terraform 설정에 전달할 값을 선언
# 앞 단계에서 만든 terraform.tfvars의 값이 여기에 들어감
variable "ami_id" {
  type = string
}

# 배포용 JAR를 저장할 S3 버킷 이름을 외부에서 받는다.
variable "artifact_bucket_name" {
  type = string
}

# 서울 리전에서 현재 사용할 수 있는 AZ를 조회
# data는 AWS에 이미 존재하는 정보를 읽어 옴
data "aws_availability_zones" "available" {
  state = "available"

  filter {
    # 기본적으로 사용 가능한 AZ 선택
    name   = "opt-in-status"
    values = ["opt-in-not-required"]
  }
}

# 프로젝트에서 사용할 VPC
# AWS VPC를 하나 만들고 Terraform 안에서 lab이라는 이름으로 부른다
resource "aws_vpc" "lab" {
  cidr_block           = "10.20.0.0/16"
  enable_dns_support   = true
  enable_dns_hostnames = true
  tags                 = { Name = "lottecinema-lab" }
}

# EC2가 위치할 Public Subnet
resource "aws_subnet" "public" {
  vpc_id            = aws_vpc.lab.id # 앞서 선언한 VPC의 ID를 참조
  cidr_block        = "10.20.1.0/24"
  availability_zone = data.aws_availability_zones.available.names[0] # 서브넷을 만들 가용 영역을 선택
}

# RDS가 사용할 Private Subnet
resource "aws_subnet" "db" {
  count  = 2 # 같은 형태의 서브넷을 두 개 만든다.
  vpc_id = aws_vpc.lab.id
  # cidrsubnet(기존_범위, 추가할_비트_수, 서브넷_번호) 큰 IP 범위를 작은 서브넷 범위로 나누는 Terraform 함수
  cidr_block = cidrsubnet(aws_vpc.lab.cidr_block, 8, count.index + 10)
  # count.index르 통해 첫 번째 서브넷은 0번째 가용 영역에 위치시키고, 두 번째 서브넷은 1번째 가용 영역에 배치
  availability_zone = data.aws_availability_zones.available.names[count.index]
}

# VPC를 인터넷과 연결할 게이트웨이
resource "aws_internet_gateway" "lab" {
  vpc_id = aws_vpc.lab.id
}

# 공개 서브넷에서 인터넷으로 향하는 경로
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.lab.id

  # route는 라우팅 테이블에 추가할 경로 하나를 정의
  route {
    cidr_block = "0.0.0.0/0"                 # 다른 경로에 해당하지 않는 IPv4 목적지에 적용한다.
    gateway_id = aws_internet_gateway.lab.id # 해당 트래픽을 VPC의 인터넷 게이트웨이로 보낸다.
  }
}

# 공개 서브넷에 위 라우팅 테이블을 연결
resource "aws_route_table_association" "public" {
  subnet_id      = aws_subnet.public.id
  route_table_id = aws_route_table.public.id
}

# EC2에 연결할 보안 그룹
resource "aws_security_group" "app" {
  name   = "lottecinema-lab-app" # 보안 그룹의 이름
  vpc_id = aws_vpc.lab.id        # 이 보안 그룹을 어떤 VPC 안에 만들지 지정
}

# EC2에서 외부로 나가는 통신을 허용한다.
resource "aws_vpc_security_group_egress_rule" "app" {
  security_group_id = aws_security_group.app.id # 이 규칙을 앞에서 만든 EC2용 app 보안 그룹에 추가
  cidr_ipv4         = "0.0.0.0/0"               # 이 규칙의 목적지 IPv4 주소 범위
  ip_protocol       = "-1"                      # -1은 모든 IP 프로토콜(TCP, UDP 포함)
}

# RDS에 연결할 보안 그룹
resource "aws_security_group" "db" {
  name   = "lottecinema-lab-db"
  vpc_id = aws_vpc.lab.id
}

# EC2 보안 그룹에서 오는 MySQL 연결을 허용한다.
resource "aws_vpc_security_group_ingress_rule" "mysql" {
  security_group_id            = aws_security_group.db.id  # 이 규칙을 RDS용 db 보안 그룹에 추가
  referenced_security_group_id = aws_security_group.app.id # 연결의 출발지를 IP 주소로 적는 대신, app 보안 그룹을 참조
  ip_protocol                  = "tcp"
  from_port                    = 3306
  to_port                      = 3306 #tcp - from - to 로 TCP의 3306포트 하나만 허용
}

# 수동 배포와 CI/CD에서 JAR를 저장할 버킷
resource "aws_s3_bucket" "artifacts" {
  bucket = var.artifact_bucket_name # terraform.tfvars에 작성한 버킷 이름을 주입받음
}

# 버킷의 공개 접근을 차단한다.
# EC2는 IAM 역할에 부여된 권한으로 이 비공개 버킷에 접근
resource "aws_s3_bucket_public_access_block" "artifacts" {
  bucket                  = aws_s3_bucket.artifacts.id # 앞에서 만든 버킷을 참조
  block_public_acls       = true                       # 새로운 공개 ACL 설정을 차단, ACL은 버킷이나 파일에 대한 접근 권한을 설정하는 방식
  block_public_policy     = true                       # 공개 버킷 정책을 새로 적용하는 요청을 차단
  ignore_public_acls      = true                       # 기존 공개 ACL을 통한 공개 접근을 무시
  restrict_public_buckets = true                       # 버킷에 공개로 판단되는 정책이 있을 때, 그 정책을 통한 공개 접근과 다른 계정의 접근을 제한
}

# EC2에 부여할 IAM 역할
resource "aws_iam_role" "app" {
  name = "lottecinema-lab-ec2" # AWS에 생성할 IAM 역할의 이름을 지정

  # EC2 서비스가 이 역할을 사용할 수 있게 한다.
  assume_role_policy = jsonencode({ # AWS IAM 정책은 JSON 형식을 사용
    Version = "2012-10-17"
    Statement = [{                                  # Statement는 정책의 규칙 목록
      Effect    = "Allow"                           # 이 규칙에 해당하는 요청을 허용
      Action    = "sts:AssumeRole"                  # 허용할 작업입니다. sts:AssumeRole은 역할을 맡아 사용할 때 필요한 작업
      Principal = { Service = "ec2.amazonaws.com" } # 누가 역할을 사용할 수 있는지를 지정
    }]
  })
}

# IAM 역할을 EC2에 연결하는 인스턴스 프로필
resource "aws_iam_instance_profile" "app" {
  name = "lottecinema-lab-ec2"
  role = aws_iam_role.app.name
}

# EC2가 Systems Manager에 등록될 권한을 부여한다.
# aws_iam_role_policy_attachment는 기존 정책을 IAM 역할에 붙이는 Terraform 리소스
resource "aws_iam_role_policy_attachment" "ssm" {
  role = aws_iam_role.app.name
  # AWS가 제공하는 AmazonSSMManagedInstanceCore 관리형 정책을 지정
  # 이 정책을 EC2 역할에 붙이면 EC2가 Systems Manager의 관리 대상이 되는 데 필요한 기본 권한을 갖게 됨
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

# EC2가 버킷의 파일을 읽을 수 있게 한다.
resource "aws_iam_role_policy" "read_artifacts" {
  name = "read-release-artifacts"
  role = aws_iam_role.app.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = "s3:GetObject" # S3에 저장된 객체를 읽는 작업
      Resource = "${aws_s3_bucket.artifacts.arn}/*"
    }]
  })
}

# 애플리케이션을 실행할 가상 서버
resource "aws_instance" "app" {
  ami                         = var.ami_id # ami는 사용할 운영체제 이미지
  instance_type               = "t3.micro"
  subnet_id                   = aws_subnet.public.id
  associate_public_ip_address = true # EC2에 공인 IPv4 주소를 자동 할당하도록 설정
  vpc_security_group_ids      = [aws_security_group.app.id]
  iam_instance_profile        = aws_iam_instance_profile.app.name
  tags                        = { Name = "lottecinema-lab-app" }

  # CPU 크레딧 사용 방식을 지정한다.
  credit_specification {
    cpu_credits = "standard"
  }

  # EC2 메타데이터 접근에 IMDSv2를 요구한다.
  metadata_options {
    http_tokens = "required"
  }

  # EC2의 기본 디스크를 암호화한다.
  root_block_device {
    encrypted = true
  }
}

# RDS가 배치될 수 있는 서브넷들을 묶는다.
resource "aws_db_subnet_group" "lab" {
  name       = "lottecinema-lab"
  subnet_ids = aws_subnet.db[*].id
}

# MySQL 데이터베이스
resource "aws_db_instance" "mysql" {
  identifier                  = "lottecinema-lab-mysql"
  engine                      = "mysql"
  engine_version              = "8.4"
  instance_class              = "db.t4g.micro"
  allocated_storage           = 20
  storage_type                = "gp3"
  storage_encrypted           = true
  db_name                     = "lottecinema"
  username                    = "dbadmin"
  manage_master_user_password = true # RDS가 관리자 비밀번호를 생성하고 Secrets Manager에서 관리하게 합니다.
  db_subnet_group_name        = aws_db_subnet_group.lab.name
  vpc_security_group_ids      = [aws_security_group.db.id]
  publicly_accessible         = false # RDS를 비공개로 생성합니다.
  multi_az                    = false
  backup_retention_period     = 1
  deletion_protection         = false
  skip_final_snapshot         = true # 삭제할 때 최종 스냅샷을 자동으로 만들지 않습니다
}

# output은 생성된 리소스의 값을 확인하기 위해 출력
# Session Manager 접속에 사용할 EC2 인스턴스 ID
output "ec2_instance_id" {
  value = aws_instance.app.id
}

# 배포 JAR를 올릴 S3 버킷 이름
output "artifact_bucket" {
  value = aws_s3_bucket.artifacts.bucket
}

# Spring Boot가 연결할 RDS 주소
output "rds_host" {
  value = aws_db_instance.mysql.address
}

# RDS 관리자 비밀번호가 저장된 시크릿의 식별자
output "db_secret_arn" {
  value = aws_db_instance.mysql.master_user_secret[0].secret_arn
}