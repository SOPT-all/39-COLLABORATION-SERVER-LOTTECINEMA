# LOTTECINEMA 작업 지침

이 저장소는 Java 21, Spring Boot, Spring Data JPA, MySQL을 사용하는 롯데시네마 백엔드입니다. 작업 전에 [README](README.md)의 Git 컨벤션과 프로젝트 구조, 변경할 도메인의 기존 구현을 확인합니다.

## 이슈, 브랜치, PR

- 이슈에 연결된 작업은 README의 브랜치와 커밋 규칙을 따릅니다. PR을 만들기 전에 대상 브랜치를 확인합니다. 일반 작업의 대상은 `dev`입니다.
- 이슈에는 [기능 템플릿](.github/ISSUE_TEMPLATE/feature.md)이나 [버그 템플릿](.github/ISSUE_TEMPLATE/bug.md)을 사용합니다. PR은 [PR 템플릿](.github/PULL_REQUEST_TEMPLATE.md)을 채우고 작성자를 담당자로 지정한 뒤 작업 라벨을 붙입니다.
- 리뷰 봇과 팀원의 코멘트를 검토해 필요한 수정 사항을 반영합니다. 리뷰 봇 코멘트에는 ⭕ 또는 ❌로 반영 여부를 표시합니다. PR은 작성자를 제외한 팀원 전원의 승인 후 병합합니다.

## Java 구조와 이름

- `org.sopt.lottecinema.<도메인>` 아래에서 `controller`, `service`, `repository`, `entity`, `dto`를 역할에 따라 사용합니다. 패키지는 해당 역할의 코드가 생길 때 추가합니다. 공통 응답, 설정, 예외 처리는 `global`에 둡니다.
- 패키지는 소문자, 클래스·인터페이스·enum 타입은 `UpperCamelCase`, 메서드·변수·필드·파라미터는 `lowerCamelCase`, 상수와 enum 값은 `UPPER_SNAKE_CASE`로 작성합니다.
- 메서드 이름은 동사로 시작합니다. 단건 조회는 `get`, 조건에 맞는 목록 조회는 `find`, 존재 확인은 `exists`, 생성·수정·삭제·유효성 검사는 `create`, `update`, `delete`, `validate`를 권장합니다.
- 역할이 드러나는 이름을 사용합니다. 예시는 `MovieController`, `MovieService`, `MovieRepository`, `Movie`, `CreateMovieRequest`, `MovieDetailResponse`, `MovieNotFoundException`, `WebConfig`입니다. 테스트 클래스는 대상 클래스명에 `Test`를 붙입니다.
- 변경하는 파일의 기존 포맷을 따르고, 작업 범위에 포함된 파일만 정리합니다.

## API, DB, 설정

- 새 API의 Swagger 문서용 어노테이션은 해당 도메인의 `controller` 패키지에 있는 별도 인터페이스에 두고, 컨트롤러가 그 인터페이스를 구현하게 합니다. 성공 응답은 `SuccessResponse`, 오류 응답은 `ErrorResponse`, 비즈니스 예외는 `BusinessException`, `ErrorCode`, `GlobalExceptionHandler`를 활용합니다.
- API 경로는 소문자 명사를 사용합니다. 컬렉션은 복수형으로 쓰고 여러 단어는 하이픈으로 연결합니다. 동작은 HTTP 메서드로, 필터는 쿼리 파라미터로 표현합니다. JSON 필드와 쿼리 파라미터는 `lowerCamelCase`로 작성합니다.
- 새 테이블은 소문자 단수형 `snake_case`로 명명합니다. 기본 키는 `id`, 외래 키 컬럼은 `<참조 대상>_id`, 일반 컬럼은 소문자 `snake_case`를 사용합니다. Boolean 컬럼은 `is_active`처럼 의미가 드러나게 작성합니다.
- 공통 설정은 `application.yml`에 두고, 환경별 설정은 필요한 프로필 파일에 분리합니다. 비밀번호와 API 키는 환경변수로 주입하고 저장소에 기록하지 않습니다.

## 검증

- Java 변경 후 Gradle Wrapper로 빌드와 테스트를 실행합니다. macOS·Linux에서는 `./gradlew build`, Windows PowerShell에서는 `./gradlew.bat build`를 사용합니다. 동작 변경은 관련 테스트에 반영합니다.
- Spring 컨텍스트 테스트에는 로컬 MySQL과 `DB_PASSWORD`가 필요합니다. 기본 사용자와 다른 계정을 쓰면 `DB_USERNAME`도 설정합니다.
- 완료 보고에 실행한 명령과 결과를 적습니다. 환경 제약으로 실행하지 못한 검사는 사유와 함께 구분합니다.
