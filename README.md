# 39-COLLABORATION-SERVER-LOTTECINEMA


# 🎬 롯데시네마 모바일 웹 개선 프로젝트 (Backend)

> **롯데시네마 모바일 웹 서비스의 사용자 경험 개선 백엔드 API 서버입니다.**

---

## 🛠 Tech Stack

### Backend Core
![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Lombok](https://img.shields.io/badge/Lombok-BC262C?style=for-the-badge&logo=lombok&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)

### Database & Storage
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![AWS S3](https://img.shields.io/badge/Amazon_S3-569A31?style=for-the-badge&logo=amazons3&logoColor=white)

### Infrastructure & DevOps
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Docker Compose](https://img.shields.io/badge/Docker_Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![AWS EC2](https://img.shields.io/badge/Amazon_EC2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white)
![AWS RDS](https://img.shields.io/badge/Amazon_RDS-527FFF?style=for-the-badge&logo=amazonrds&logoColor=white)

---

## 🏛 Server Architecture

- **Local Development:** `docker-compose.yml`을 통해 로컬 MySQL 및 인프라 환경을 컨테이너로 표준화하여 팀원 간 격리된 개발 환경 보장 (호스트 충돌 방지: `3307` 포트 매핑)
- **Production Environment:**
  - **Option A:** `AWS EC2` + `AWS RDS (MySQL)` + `AWS S3` (고가용성 완전관리형 데이터베이스 아키텍처)
  - **Option B:** `AWS EC2 (Spring Boot + MySQL on Docker)` + `AWS S3` (컨테이너 기반 인프라 운영 및 비용 최적화)
- **정적 미디어 분리 스토리지:** 영화 포스터, 스틸컷, 배너 등 대용량 정적 미디어 리소스를 AWS S3에 독립 적재하여 API 서버의 네트워크 및 디스크 I/O 부하 최소화

```text
[ Client (Mobile Web) ]
          │
          ▼
   [ AWS EC2 Server ] ────────► [ AWS S3 Bucket ]
          │                 (영화 포스터, 스틸컷 등 미디어)
          ▼
 [ MySQL Database / RDS ]
 (영화, 상영관, 상영시간표, 좌석 및 예매 데이터)
```

---

## 🌿 Git Branch Strategy & Rules

Git-Flow 기반의 브랜치 전략을 준수하며, 상시 유지 브랜치와 보조 브랜치를 분리하여 코드 무결성을 관리합니다.

| 분류 | 브랜치명 | 설명 | 머지 대상 |
| :--- | :--- | :--- | :--- |
| **상시 유지** | `main` | 실제 배포 가능한 상태를 유지하는 프로덕션 브랜치 | - |
| **상시 유지** | `dev` | 다음 출시 버전을 개발하고 통합하는 메인 개발 브랜치 | `main` |
| **보조 브랜치** | `feat/#이슈번호` | 개별 기능 개발을 수행하는 단위 브랜치 | `dev` |
| **보조 브랜치** | `hotfix/#이슈번호` | 운영 배포 환경에서 발생한 긴급 버그 픽스 브랜치 | `main`, `dev` |

### 📌 Team Branch Rules
1. `feat` 브랜치는 항상 최신 `dev` 브랜치로부터 분기합니다.
   ```bash
   git checkout dev
   git pull origin dev
   git checkout -b feat/#이슈번호
   ```
2. 기능 구현 완료 후 커밋 시 커밋 메시지에 이슈 번호 또는 명확한 접두사를 표기합니다 (`feat: #이슈번호 기능 설명`).
3. **PR(Pull Request)은 오직 `dev` 브랜치 방향으로만 생성**합니다 (`feat` -> `dev`).
4. `main` 브랜치는 오직 **`dev` 또는 `hotfix` 브랜치**를 통해서만 머지(Merge)가 가능하며, 직접 push는 금지됩니다.
5. 모든 PR은 팀원 간 코드 리뷰 및 승인(Approved)을 거친 후 병합합니다.

---

## 📂 Project Structure (도메인형 구조)

도메인별로 연관된 엔티티, 컨트롤러, 서비스, DTO를 응집도 높게 묶어 비즈니스 로직 확장성과 유지보수성을 극대화한 패키지 구조입니다.

```text
src
├── main
│   ├── java
│   │   └── org
│   │       └── sopt
│   │           └── lottecinema
│   │               └── global                    # 전역 공통 관심사
│   │                   ├── common                # ApiResponse
│   │                   ├── config                # WebConfig (CORS), SwaggerConfig
│   │                   └── exception             # 예외 처리 (GlobalExceptionHandler, ErrorCode, BusinessException)
│   │                 
│   │                   
│   │
│   └── resources
│       └── application.yml                       # 환경 변수 바인딩 기반 설정 파일
│
├── docker-compose.yml                            # 로컬 개발용 컨테이너 인프라 정의 파일
└── README.md
```

---

## Getting Started (팀원 로컬 개발 세팅 가이드)

로컬 머신에 별도의 MySQL 설치 없이, Docker를 통해 완전히 격리된 DB 환경을 구동합니다.

### 1. Repository Clone
```bash
git clone https://github.com/SOPT-all/39-COLLABORATION-SERVER-LOTTECINEMA.git
cd 39-COLLABORATION-SERVER-LOTTECINEMA
```

### 2. 환경 변수 파일(`.env`) 생성
프로젝트 루트 디렉터리에 제공된 `.env.example` 파일을 복사하여 `.env` 파일을 생성합니다.

```bash
cp .env.example .env
```


> **주의사항:** `.env` 파일은 실제 계정 비밀번호가 담길 수 있으므로 절대 Git에 커밋하지 않습니다 (`.gitignore` 등록 완료).

### 3. Docker Compose로 MySQL 컨테이너 구동
백그라운드 모드로 DB 컨테이너를 실행합니다.
```bash
docker compose up -d
```

실행 상태 확인:
```bash
docker compose ps
```
상태가 `Up` 또는 `running`으로 표시되면 정상 구동된 것입니다.

### 4. Spring Boot 애플리케이션 실행
- **IntelliJ 실행 시:** `LottecinemaApplication.java` 실행
- **터미널 실행 시:**
  ```bash
  ./gradlew clean build
  ./gradlew bootRun
  ```

### 5. API 명세서 (Swagger) 접속 확인
서버 구동 후 웹 브라우저에서 아래 주소로 접속하여 API 문서를 확인합니다:
- **Swagger UI:** `http://localhost:8080/swagger.html`

---
