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


## 🌿 Git 컨벤션

Git-Flow 기반으로 상시 유지 브랜치와 기능별 보조 브랜치를 운영합니다.

| 분류 | 브랜치 | 설명 | 머지 대상 |
| --- | --- | --- | --- |
| 상시 유지 | `main` | 실제 배포 가능한 프로덕션 브랜치 | - |
| 상시 유지 | `dev` | 다음 출시 버전을 개발하고 통합하는 브랜치 | `main` |
| 작업 | `type/#이슈번호` | 기능, 버그 수정, 문서, 설정 등 일반 작업 | `dev` |
| 긴급 수정 | `hotfix/#이슈번호` | 운영 환경의 긴급 버그 수정 | `main`, `dev` |

### 브랜치와 커밋

이슈를 먼저 생성하고 최신 `dev`에서 일반 작업 브랜치를 만듭니다. 긴급 수정은 `main`에서 분기해 수정 사항을 `main`과 `dev`에 모두 반영합니다.

```bash
git switch dev
git pull --ff-only origin dev
git switch -c 'feat/#2'
```

커밋 제목은 `type: content` 형식으로 작성합니다. 일반 작업 브랜치와 커밋에는 다음 유형을 사용합니다. 긴급 수정 브랜치의 커밋은 `fix`를 사용합니다.

| 유형 | 용도 |
| --- | --- |
| `feat` | 기능 추가 또는 변경 |
| `fix` | 버그 수정 |
| `docs` | 문서 변경 |
| `chore` | 빌드, 설정, 의존성 등 유지보수 |

```text
feat: 상영 시간 조회 추가
```

### 이슈와 PR

- 이슈와 PR 제목은 `[Type] 제목` 형식으로 작성합니다. 유형 표기는 `[Feat]`, `[Fix]`, `[Docs]`, `[Chore]`를 사용합니다. 예시는 `[Feat] 상영 시간 조회 추가`입니다.
- 이슈를 만들 때 [기능 템플릿](.github/ISSUE_TEMPLATE/feature.md) 또는 [버그 템플릿](.github/ISSUE_TEMPLATE/bug.md)을 사용합니다.
- 일반 작업은 로컬에서 빌드와 테스트를 확인한 뒤 `dev` 대상으로 PR을 생성합니다. [PR 템플릿](.github/PULL_REQUEST_TEMPLATE.md)을 작성하고 본인을 담당자로 지정한 뒤 작업에 맞는 라벨을 추가합니다.
- 기본 브랜치인 `dev`에 PR을 병합하면서 이슈를 완료하려면 본문에 `Closes #2`를 씁니다. 이슈를 열어 두거나 `main` 대상 PR에서 참조만 할 때는 `Refs #2`를 씁니다.
- `dev`를 `main`에 병합하는 릴리스 PR의 제목은 `[Release] 제목`으로 작성합니다. 긴급 수정 PR의 제목은 `[Fix] 제목`으로 작성합니다.
- 스쿼시 머지를 할 때도 최종 커밋 제목은 `type: content` 형식으로 작성합니다.
- `main`에 직접 푸시하지 않습니다. 모든 PR은 작성자를 제외한 팀원 전원의 승인 후 병합합니다.

## 📂 프로젝트 구조

도메인을 먼저 나누고, 각 도메인 안에서 `controller`, `service`, `repository`, `entity`, `dto`로 역할을 구분합니다. 각 패키지는 해당 역할의 코드가 생길 때 추가합니다.

```text
src
└── main
    ├── java/org/sopt/lottecinema
    │   ├── global
    │   │   ├── common       # 공통 응답 (예: SuccessResponse, ErrorResponse)
    │   │   ├── config       # 공통 설정 (예: WebConfig, SwaggerConfig)
    │   │   └── exception    # 예외 처리 (예: GlobalExceptionHandler, ErrorCode)
    │   ├── movie
    │   │   └── entity       # Movie, CurationType
    │   └── [domain]
    │       └── entity       # 해당 도메인의 JPA 엔티티와 관련 타입
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
