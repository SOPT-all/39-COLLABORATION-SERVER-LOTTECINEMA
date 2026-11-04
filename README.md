# 39-COLLABORATION-SERVER-LOTTECINEMA


롯데시네마 모바일 웹 서비스의 사용자 경험 개선을 위한 백엔드 API 서버입니다.

## 👥 팀원 소개

| 파트 | 리드 | 팀원 |
| --- | --- | --- |
| 서버 | 이정현 | 정교은, 박이수 |

## 🛠 기술 스택

| 구분 | 기술 |
| --- | --- |
| Backend | Java, Spring Boot, Spring Data JPA, Lombok |
| Build & API Docs | Gradle, Swagger |
| Database & Storage | MySQL, AWS S3 |
| Infrastructure | AWS EC2, AWS RDS |

## 🏛 서버 아키텍처

### 로컬 개발 환경

DB 주소와 계정은 `src/main/resources/application.yml`에 각자 로컬 환경에 맞게 설정합니다.


### 운영 환경 구성안

 | 아키텍처 | 특징 |
 | --- | --- |
 | AWS EC2 + AWS RDS(MySQL) + AWS S3 | 관리형 데이터베이스를 활용하는 고가용성 구성 |


## 🌿 브랜치 전략

Git-Flow 기반으로 상시 유지 브랜치와 기능별 보조 브랜치를 운영합니다.

| 분류 | 브랜치 | 설명 | 머지 대상 |
| --- | --- | --- | --- |
| 상시 유지 | `main` | 실제 배포 가능한 프로덕션 브랜치 | - |
| 상시 유지 | `dev` | 다음 출시 버전을 개발하고 통합하는 브랜치 | `main` |
| 보조 | `feat/#이슈번호` | 개별 기능 개발 브랜치 | `dev` |
| 보조 | `hotfix/#이슈번호` | 운영 환경의 긴급 버그 수정 브랜치 | `main`, `dev` |

### 팀 브랜치 규칙

1. 기능 브랜치는 최신 `dev`에서 분기합니다.

   ```bash
   git checkout dev
   git pull origin dev
   git checkout -b feat/#이슈번호
   ```

2. 커밋 메시지에는 이슈 번호 또는 명확한 접두사를 포함합니다.

   ```text
   feat: #이슈번호 기능 설명
   ```

3. 기능 브랜치의 PR은 `dev`를 대상으로 생성합니다 (`feat` → `dev`).
4. `main`에 직접 push하지 않습니다. `main`에는 `dev` 또는 `hotfix`를 통해서만 머지합니다.
5. 모든 PR은 팀원 리뷰와 승인 후 병합합니다.

## 📂 프로젝트 구조

도메인별로 연관된 구성 요소를 모아 비즈니스 로직의 응집도와 유지보수성을 높입니다.

```text
src
└── main
    ├── java/org/sopt/lottecinema
    │   ├── global
    │   │   ├── common       # 공통 응답 (예: ApiResponse)
    │   │   ├── config       # 공통 설정 (예: WebConfig, SwaggerConfig)
    │   │   └── exception    # 예외 처리 (예: GlobalExceptionHandler, ErrorCode)
    │   └── [domain]         # 도메인별 Entity, Controller, Service, DTO
    └── resources
        └── application.yml  # 로컬/배포 환경의 DB 및 애플리케이션 설정

README.md
```

## 🚀 시작하기

아래 절차를 따라 로컬 MySQL과 Spring Boot 애플리케이션을 실행합니다. MySQL은 개발자 컴퓨터에 설치되어 실행 중이어야 합니다.

### 1. 저장소 복제

```bash
git clone https://github.com/SOPT-all/39-COLLABORATION-SERVER-LOTTECINEMA.git
cd 39-COLLABORATION-SERVER-LOTTECINEMA
```

### 2. 로컬 MySQL 준비

MySQL 서버를 실행하고 프로젝트에서 사용할 데이터베이스를 생성합니다.

```bash
mysql -u root -p
```

MySQL 프롬프트에서 다음을 실행합니다.

```sql
CREATE DATABASE lottecinema CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. DB 접속 정보 설정

`src/main/resources/application.yml`의 다음 값을 로컬 MySQL 설정에 맞게 수정합니다.

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/lottecinema
    username: # 로컬 MySQL 사용자명
    password: # 로컬 MySQL 비밀번호
```

MySQL이 다른 주소나 포트에서 실행 중이면 URL의 `localhost:3306`을 해당 값으로 바꾸세요. 
`username`과 `password`에는 위에서 생성한 데이터베이스에 접근할 MySQL 계정 정보를 입력합니다. 
이 파일에 실제 비밀번호를 넣을 경우 커밋하지 않도록 주의하세요. 
운영 배포에서는 운영 DB 접속 정보를 별도로 관리해야 합니다.

### 4. Spring Boot 애플리케이션 실행

IntelliJ에서 `LottecinemaApplication.java`를 실행하거나 터미널에서 다음 명령을 실행합니다.

```bash
./gradlew clean build
./gradlew bootRun
```

### 5. Swagger API 문서 확인

서버 실행 후 브라우저에서 Swagger UI를 엽니다.

<http://localhost:8080/swagger-ui/index.html>
