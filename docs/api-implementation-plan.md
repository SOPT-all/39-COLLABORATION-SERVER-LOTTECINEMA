# 좌석 조회·예매 생성 API 구현 계획

좌석 조회 GET은 구현되었으며, 예매 생성 POST는 후속 구현 계획이다.

기존 명세 초안의 두 엔드포인트를 대상으로 한다. V2-02 회차 목록에서 받은 `showtimeId`를 V2-03과 V3-01에 전달하는 흐름을 전제로 한다. V2-02에는 별도 엔드포인트 계약이 없어 이번 계획에서는 연동에 필요한 필드만 다룬다. V3-01의 결제 버튼은 이 범위에서 예매 확정을 요청하며 외부 결제 승인, 할인, 쿠폰은 별도 작업이다. [기존 API 명세 초안](showtime-seat-reservation-api.md)

| 순서 | API | 구현 요점 | 상세 SPEC |
| --- | --- | --- | --- |
| 1 | `GET /api/v1/showtimes/{showtimeId}/seats` | 회차와 상영관 좌석을 읽고, 해당 회차에 확정된 좌석을 표시한다. 같은 응답에서 예매에 사용할 관람 유형별 단가를 제공한다. | [회차별 좌석 조회](specs/get-showtime-seats.md) |
| 2 | `POST /api/v1/showtimes/{showtimeId}/reservations` | 요청을 검증하고 서버에서 총액을 다시 계산한다. 예매와 확정 좌석을 원자적으로 저장하고 UUID 재시도를 처리한다. | [회차별 예매 생성](specs/create-showtime-reservation.md) |

## 1. 두 API에 필요한 데이터 준비

| 대상 | 현재 상태 | 작업 |
| --- | --- | --- |
| 상영관 좌석 | `seat`에 `screen_id`, `seat_code`가 있으며 상영관 안에서 좌석 코드는 유일하다. | `row_index`, `column_index`를 0 이상의 필수 정수로 추가한다. `(screen_id, row_index, column_index)`를 유일하게 유지하고 통로는 좌표를 건너뛰어 표현한다. |
| 공통 가격 | 모든 회차에 동일한 유형별 단가를 적용한다. | `application.yml`의 `ticket.prices`를 `TicketPriceProperties`로 바인딩한다. 조회와 예매가 같은 설정을 사용한다. |
| 예매와 확정 좌석 | `reservation`에 요청 UUID, 회차, 관람 유형별 인원, 총액, 생성 시각이 있다. `reservation_seat`은 예매·회차·좌석을 연결한다. | `uk_reservation_request`와 `uk_reservation_seat_showtime`을 재사용한다. 배포 대상 MySQL에서 두 제약이 실제로 적용됐는지 확인한다. |

유형별 단가는 저장된 총액의 `INT` 범위와 최대 8인 규칙을 고려해 `0..268435455`로 제한한다. 서버는 합계를 `long`으로 계산하고 범위를 확인한 뒤 기존 `Reservation.totalAmount`에 저장한다. 단가 누락이나 잘못된 값은 `TicketPriceProperties` 검증으로 애플리케이션 시작 시 감지한다. 실제 단가 0원은 유효한 설정값이다.

기존 DB에는 좌표 컬럼을 먼저 nullable로 추가하고, 상영관 배치 자료로 기존 좌석을 채운다. 누락, 음수, 중복 좌표를 검사한 뒤 두 좌표 컬럼에 `NOT NULL`과 제약을 적용한다. 현재 `ddl-auto: update`는 기존 행의 좌표를 채울 수 없으므로 적용 순서가 있는 SQL과 시드 데이터를 함께 검토한다. 좌표가 미완성인 상영관은 조회 시 공통 500으로 처리한다.

실제 좌석 배치를 확인하고 공통 단가는 application.yml에서 관리한다. 예매 마감은 `Asia/Seoul` 기준으로 상영 시작 시각에 도달하면 마감하는 잠정안이다. 두 API는 기존 `DATETIME` 값을 KST로 해석해 응답에 `+09:00`을 포함한다. 예매 시각은 DB 정밀도에 맞춰 초 단위로 저장한다.

## 2. 회차별 좌석 조회 구현

1. `showtime.controller`에 `ShowtimeApi`와 `ShowtimeController`를 추가한다. 경로 ID를 양의 `Long`으로 검증하고 `ShowtimeSeatService`에 전달한다. 성공 응답은 HTTP 200과 `SuccessResponse.success(...)`를 사용한다.
2. 읽기 전용 트랜잭션에서 회차, 영화, 상영관, 영화관을 함께 조회한다. 회차가 없으면 `NOT_FOUND`를 반환한다. `SeatRepository`는 해당 상영관의 좌석을 `(rowIndex, columnIndex)` 순으로 가져오면서 요청 회차의 `ReservationSeat` 존재 여부를 조회한다. 회차 조건을 조인의 `ON` 절에 두어 미예매 좌석도 포함한다.
3. 같은 읽기 스냅샷에서 회차와 좌석을 읽고, 좌석 목록으로 총 좌석 수와 잔여 좌석 수를 계산한다. 단가는 공통 설정에서 읽는다. 읽기 전용 트랜잭션에 `REPEATABLE READ`를 지정하고 일반 `SELECT`로 두 번 조회한다. DTO 변환도 트랜잭션 안에서 완료한다. [MySQL 8.4 트랜잭션 격리 수준](https://dev.mysql.com/doc/refman/8.4/en/innodb-transaction-isolation-levels.html)
4. 포스터 URL 미등록은 `null`로 직렬화한다. 상영 포맷은 `2D`, `3D`, `4DX`로 노출한다. 매진이거나 상영이 시작된 경우에도 좌석 현황을 200으로 반환한다.

## 3. 회차별 예매 생성 구현

1. `reservation.controller`에 `ReservationApi`와 `ReservationController`를 추가한다. 요청 DTO에서 JSON 타입, 필수 필드, 인원 범위, 좌석 중복, 총인원과 좌석 수의 일치를 검증한다. 경로와 본문 검증 모두 공통 오류 형식을 사용하고, 본문 상세 오류에는 중첩 필드 경로를 보존한다. [Spring MVC 검증](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html)
2. `ReservationService`는 검증된 입력으로 전역 `requestId`를 먼저 조회한다. 저장된 회차, 네 유형의 인원, 좌석 집합, 총액이 일치하면 기존 예매를 200으로 반환한다. 값이 다르면 `RESERVATION_REQUEST_CONFLICT`를 반환한다. 같은 요청은 마감 이후나 요금 변경 이후에도 최초 결과를 돌려받는다.
3. 신규 요청은 회차 조회, 상영관 좌석 코드 검증, 마감 검사, 공통 단가와 총액 검사의 순서로 처리한다. 좌석은 DB에서 후보를 가져온 뒤 실제 `seatCode`와 정확히 대조한다. 요금이 다르면 `RESERVATION_AMOUNT_CHANGED`를 반환한다.
4. 하나의 쓰기 트랜잭션에서 `Reservation`을 먼저 저장하고 `ReservationSeat`를 좌석 ID 순으로 저장한다. 좌석 점유의 최종 판단은 회차·좌석 유일 제약에 맡긴다. 트랜잭션이 커밋된 뒤 HTTP 201을 반환한다. 오류가 발생하면 전체 롤백 뒤 제약을 구분한다. UUID 유일 제약 위반은 새 읽기 트랜잭션에서 기존 예매를 확인해 200 또는 UUID 충돌 409로, 회차·좌석 유일 제약 위반은 `SEAT_ALREADY_RESERVED`로 변환한다. 인식되지 않은 DB 오류는 공통 500으로 처리한다. [MySQL 8.4 InnoDB 오류 처리](https://dev.mysql.com/doc/refman/8.4/en/innodb-error-handling.html)

Swagger 어노테이션은 도메인별 `*Api` 인터페이스에 두고 컨트롤러가 구현한다. 오류는 `ErrorCode`, `BusinessException`, `GlobalExceptionHandler`, `ErrorResponse`를 재사용한다. 예매용 409 코드 네 가지를 추가하고, 서비스 검증의 `errors`가 응답까지 전달되도록 공통 예외 매핑을 보완한다. [공통 응답 형식](API%20공통응답%20형식.md)

## 4. 검증 및 연동 순서

1. MySQL에서 상영관 한 곳, 같은 물리 좌석을 사용하는 회차 두 개와 공통 단가 설정을 준비한다. 좌석 코드·배치 좌표·예매 관련 제약과 기존 행 백필 결과를 확인한다.
2. 좌석 조회는 회차 격리, 정렬, 통로 간격, 매진, 좌석 0개, nullable/생략 직렬화, KST 오프셋과 HTTP 오류 응답을 확인한다. 좌석 수에 따라 SQL 실행 횟수가 증가하지 않는지도 확인한다.
3. 예매 생성은 JSON 타입 및 유효성, 총액 불일치, 마감 경계, 재시도, 좌석 충돌을 확인한다. 동시성 검증은 독립된 MySQL 트랜잭션에서 수행한다. 서로 다른 요청이 겹치는 좌석을 요청했을 때 패자의 예매와 좌석이 남지 않는지, 같은 UUID의 동시 요청이 예매 한 건으로 수렴하는지 DB 행 수로 확인한다.
4. Swagger와 프론트 연동에서 회차 선택 → 좌석 조회 → 예매 생성 → 재조회 및 동일 UUID 재시도 흐름을 확인한다. 신규 예매에서 선택한 좌석 수만큼 잔여 좌석이 감소하고, 재시도에서 잔여 수가 다시 줄지 않아야 한다.
5. Java 변경 후 로컬 MySQL과 `DB_PASSWORD`를 준비해 Windows에서는 `./gradlew.bat build`, macOS·Linux에서는 `./gradlew build`를 실행한다. 로컬 DB 계정이 기본값과 다르면 `DB_USERNAME`을 설정한다.
