# 상영 회차 예매 생성 SPEC

| 항목 | 내용 |
| --- | --- |
| 상태 | 구현 예정 |
| 화면 | V3-01 인원·좌석 선택 완료 |
| 엔드포인트 | `POST /api/v1/showtimes/{showtimeId}/reservations` |
| 선행 호출 | [상영 회차 좌석 조회](get-showtime-seats.md) |
| 계약 출처 | [좌석 조회 및 예매 API 설계](../showtime-seat-reservation-api.md), [공통 응답 형식](../API%20공통응답%20형식.md) |
| 작업 순서 | [좌석 조회·예매 생성 API 구현 계획](../api-implementation-plan.md) |

결제 버튼을 누르면 해당 회차에서 선택한 좌석을 확정한다. 외부 결제 승인, 할인, 쿠폰은 별도 범위다.

## 요청

`Content-Type: application/json`을 사용한다. 쿼리 파라미터는 없다.

| 위치 | 필드 | 타입 | 필수 조건 |
| --- | --- | --- | --- |
| 경로 | `showtimeId` | 정수 (`int64`) | 좌석 조회에 사용한 회차 ID, 1 이상 |
| 본문 | `requestId` | UUID 문자열 | 새 제출에서 `crypto.randomUUID()`로 생성, 같은 제출의 재시도에서 재사용 |
| 본문 | `ticketCounts` | 객체 | 아래 네 유형을 모두 명시, 합계 1~8 |
| 본문 | `ticketCounts.adult` | 정수 | 성인 인원, 0~8 |
| 본문 | `ticketCounts.teen` | 정수 | 청소년 인원, 0~8 |
| 본문 | `ticketCounts.senior` | 정수 | 경로 인원, 0~8 |
| 본문 | `ticketCounts.disabled` | 정수 | 장애인 인원, 0~8 |
| 본문 | `seatCodes` | 문자열 배열 | 1~8개, 중복 불가, 각 값 1~10자, 총인원과 배열 길이 일치 |
| 본문 | `expectedTotalAmount` | 정수 | 화면에 표시된 총액, 원(KRW), 0 이상 |

모든 본문 필드는 `null` 없이 제출한다. `seatCodes`는 좌석 조회에서 받은 값 그대로 보내며 상영관 범위 안에서 대소문자까지 일치해야 한다. 좌석 배열의 순서를 바꿔도 같은 좌석 집합으로 본다.

```http
POST /api/v1/showtimes/1203/reservations
Content-Type: application/json
Accept: application/json
```

```json
{
  "requestId": "f7527fb5-60db-4814-b1f2-67f7630869e9",
  "ticketCounts": {"adult": 2, "teen": 0, "senior": 0, "disabled": 0},
  "seatCodes": ["E4", "E5"],
  "expectedTotalAmount": 30000
}
```

## 응답

| 필드 | 타입 | 내용 |
| --- | --- | --- |
| `status` | 정수 | 신규 생성 `201`, 기존 예매 재응답 `200` |
| `message` | 문자열 | `201`은 `Created`, `200`은 `OK` |
| `data` | 객체 | 확정된 예매 |
| `data.reservationId` | 정수 (`int64`) | 예매 ID |
| `data.showtimeId` | 정수 (`int64`) | 예매한 회차 ID |
| `data.seatCodes` | 문자열 배열 | 확정 좌석 코드, 문자열 오름차순 |
| `data.ticketCounts` | 객체 | 확정된 유형별 관람 인원 |
| `data.ticketCounts.adult` | 정수 | 성인 인원 |
| `data.ticketCounts.teen` | 정수 | 청소년 인원 |
| `data.ticketCounts.senior` | 정수 | 경로 인원 |
| `data.ticketCounts.disabled` | 정수 | 장애인 인원 |
| `data.totalAmount` | 정수 | 서버가 계산하여 확정한 총액, 원(KRW) |
| `data.createdAt` | 문자열 | 최초 생성 일시, ISO 8601 및 KST 오프셋 `+09:00` |

**HTTP 201 Created**

```json
{
  "status": 201,
  "message": "Created",
  "data": {
    "reservationId": 9021,
    "showtimeId": 1203,
    "seatCodes": ["E4", "E5"],
    "ticketCounts": {"adult": 2, "teen": 0, "senior": 0, "disabled": 0},
    "totalAmount": 30000,
    "createdAt": "2026-11-09T16:30:15+09:00"
  }
}
```

동일 요청의 HTTP 200 재응답은 `status`와 `message`를 각각 `200`, `OK`로 바꾸며 최초 응답과 동일한 `data`를 반환한다.

| HTTP 상태 | 오류 코드 | 조건 및 클라이언트 처리 |
| --- | --- | --- |
| 400 | `INVALID_PARAMETER_TYPE` | `showtimeId`를 경로 정수로 변환할 수 없다. |
| 400 | `MALFORMED_REQUEST_BODY` | 본문을 읽을 수 없거나 JSON 문법·필드 타입·UUID 형식이 잘못됐다. |
| 400 | `VALIDATION_FAILED` | 경로 범위, 필수 필드, 인원과 좌석 개수, 좌석 중복, 회차 상영관의 좌석인지 등을 검사하고 상세 항목을 `errors`에 담는다. |
| 404 | `NOT_FOUND` | 해당 상영 회차가 없다. |
| 406 | 본문 없음 | 요청의 `Accept`에 맞는 응답 형식을 제공할 수 없다. |
| 409 | `SEAT_ALREADY_RESERVED` | 선택한 좌석 중 하나 이상이 이미 확정됐다. 좌석 현황을 다시 조회하고 선택을 갱신한다. |
| 409 | `RESERVATION_AMOUNT_CHANGED` | `expectedTotalAmount`가 현재 단가로 계산한 총액과 다르다. 좌석 조회로 가격을 갱신하고 사용자 확인을 받는다. |
| 409 | `RESERVATION_REQUEST_CONFLICT` | 같은 `requestId`로 회차·인원·좌석 집합·예상 금액 중 하나라도 다른 예매를 제출했다. 변경 내용을 확인하고 새 제출에는 새 UUID를 사용한다. |
| 409 | `SHOWTIME_NOT_BOOKABLE` | 회차의 예매 가능 시간이 지났다. 다른 회차를 선택할 수 있게 안내한다. |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | 지원하지 않는 `Content-Type`이다. |
| 500 | `INTERNAL_SERVER_ERROR` | DB 장애 등 서버 내부 오류다. |

오류 본문에는 `status`, `code`, `message`가 포함된다. `VALIDATION_FAILED`의 `errors`에는 `field`와 `message`를 넣으며, 객체 전체에 대한 검증은 `field`를 생략할 수 있다. 예매 결과를 받지 못했다면 처음과 같은 `requestId` 및 요청 내용으로 재시도한다. 인원·좌석·금액을 바꿔 새로 제출할 때는 새 UUID를 발급한다.

**HTTP 409 Conflict**

```json
{
  "status": 409,
  "code": "SEAT_ALREADY_RESERVED",
  "message": "선택한 좌석 중 이미 예매된 좌석이 있습니다."
}
```

## 서버 설계

| 구성 | 책임 |
| --- | --- |
| `reservation.controller.ReservationApi`, `ReservationController` | Swagger 어노테이션은 인터페이스에 둔다. 컨트롤러는 JSON 요청을 바인딩하고 `ResponseEntity<SuccessResponse<ReservationResponse>>`로 201 또는 200을 반환한다. |
| `reservation.dto.CreateReservationRequest`, `ReservationResponse` | 요청 검증과 응답 스키마를 정의한다. 엔티티는 서비스 트랜잭션 안에서 응답 DTO로 매핑한다. |
| `reservation.service.ReservationService` | 입력에서 예매 명령을 만들고 신규 생성 트랜잭션을 실행한다. DB 충돌 시 롤백이 끝난 후 결과를 분류한다. |
| `reservation.repository.ReservationRepository`, `ReservationSeatRepository` | 전역 UUID로 예매를 조회하고 확정 좌석을 가져온다. 신규 예매와 확정 좌석을 저장한다. |
| `ShowtimeRepository`, `SeatRepository`, `TicketPriceProperties` | 회차, 상영관 좌석, GET과 같은 공통 단가를 제공한다. |

### 입력 검증

1. `showtimeId`는 `Long`으로 변환한 뒤 양수를 검사한다. 본문의 필수값은 누락과 명시적 `null`을 모두 검증한다. `ticketCounts`에는 중첩 검증을 적용하고 좌석 배열에는 요소 단위 문자열 길이 검증을 적용한다.
2. 이 요청 DTO에 한해 JSON 타입을 엄격히 읽는다. 정수 필드의 `"2"`, `2.0`, `true`, 문자열 필드의 숫자, 배열 대신 단일 값을 변환하면 계약과 달라진다. 이런 경우와 유효하지 않은 UUID 문자열은 공통 핸들러에서 `MALFORMED_REQUEST_BODY`로 처리할 수 있게 역직렬화 오류를 전달한다. 정수 값은 `BigInteger`로 받아 범위를 검사하고 유형별 인원은 검증 뒤 엔티티의 `byte`로 변환한다. `expectedTotalAmount`에는 기존 계약의 0 이상 조건을 적용한다.
3. DTO의 교차 필드 검증에서 총인원, 중복 좌석, 총인원과 좌석 수의 일치를 확인한다. 회차의 상영관 소속 여부는 서비스에서 확인한다. MySQL 좌석 코드 비교에 대소문자 차이가 무시되는 collation이 적용될 수 있으므로, `screen_id`와 요청 코드 목록으로 후보를 한 번에 가져온 뒤 Java 문자열이 정확히 일치하는지 검사한다.
4. `VALIDATION_FAILED`의 세부 정보가 `errors`에 남도록 서비스의 검증 예외에도 `ValidationError` 목록을 전달하는 경로를 추가한다. 현재 `BusinessException` 처리에는 상세 목록 전달이 없다. 경로 제약과 `@Valid`를 함께 선언한 경우 `HandlerMethodValidationException` 경로에서도 `ticketCounts.adult`, `seatCodes[0]` 같은 본문 필드가 전달되는지 확인하고 필요한 경우 해당 처리만 보완한다. 404와 도메인 409에는 기존 `BusinessException`과 `GlobalExceptionHandler`를 재사용한다.

### 처리 순서와 데이터

1. 형식과 입력 내 검증을 통과한 요청의 `requestId`를 회차 조건 없이 조회한다. 커밋된 예매가 있으면 저장된 회차 ID, 유형별 인원, 좌석 코드 집합, 확정 총액을 제출값과 비교한다. 신규 생성에서 검증을 통과한 `expectedTotalAmount`와 저장된 `totalAmount`가 같으므로 비교에 저장 총액을 재사용한다. 모두 일치하면 200, 다르면 `RESERVATION_REQUEST_CONFLICT`를 반환한다. 200에는 저장된 값만 사용하므로 상영이 시작되거나 현재 단가가 바뀌어도 최초 결과를 돌려준다.
2. 신규 요청은 회차가 있는지와 요청 좌석 전부가 회차 상영관에 속하는지 순서대로 확인한다. 이후 `Clock`의 현재 시각을 `Asia/Seoul`로 변환해 `Showtime.startAt`과 비교한다. 잠정 기준은 `now < startAt`일 때 예매 허용이다. 현재 시각이 시작 시각과 같거나 이후면 `SHOWTIME_NOT_BOOKABLE`이다.
3. `TicketPriceProperties`에서 공통 네 단가를 읽고 `adult × adultPrice + teen × teenPrice + senior × seniorPrice + disabled × disabledPrice`를 계산한다. 단가는 0~268,435,455원이며 총인원은 최대 8명이라 확정 총액은 기존 `reservation.total_amount`의 `INT`에 들어간다. 합계는 `long`으로 계산하고 `BigInteger` 요청값과 정확히 비교한 뒤 저장할 `int`로 변환한다. 단가 설정은 시작 시 검증하며, 금액이 다르면 `RESERVATION_AMOUNT_CHANGED`다.
4. `reservation`에 회차·UUID·인원·확정 금액·생성 시각을 저장한 뒤 `reservation_seat`에 확정 좌석을 저장한다. 초기 생성과 재응답에는 좌석 코드를 Java 문자열 오름차순으로 정렬한다. 같은 응답을 재구성할 수 있도록 기존 예매가 참조하는 `seat_code`는 운영 데이터에서 유지한다. `createdAt`은 `Clock`에서 얻은 KST 시각을 DB `DATETIME`의 초 단위로 맞춰 저장하고, 두 응답 모두 초와 `+09:00`을 포함해 직렬화한다.

### 트랜잭션과 충돌 처리

- 신규 생성에서 예매와 확정 좌석을 하나의 쓰기 트랜잭션으로 저장한다. `reservation`을 먼저 INSERT 및 flush해 `uk_reservation_request`를 검사한 뒤, `seat.id` 오름차순으로 확정 좌석을 삽입하고 flush한다. 별도의 좌석 선점 조회 없이 `uk_reservation_seat_showtime`이 회차·좌석 중복을 판정한다. 하나라도 실패하면 예매와 확정 좌석 전부를 롤백한다. 201은 쓰기 트랜잭션이 커밋된 뒤 응답한다.
- `ReservationService`는 트랜잭션 밖에서 예외를 잡는다. `TransactionTemplate`로 쓰기 트랜잭션과 복구용 읽기 트랜잭션을 분리한다. UUID 중복으로 롤백되면 새 트랜잭션에서 커밋된 예매와 좌석을 다시 조회하고 최초 조회와 같은 비교를 수행한다. 좌석 유일 제약 위반은 `SEAT_ALREADY_RESERVED`로 처리한다. 예외 원인에서 MySQL 중복 키 오류와 알려진 제약명을 함께 확인한다. 제약을 식별할 수 없는 오류나 복구 시 기대한 UUID 행이 없는 경우 서버 오류로 처리한다.
- 좌석 저장 순서를 고정해 겹치는 요청의 잠금 순서를 맞춘다. 교착 상태와 잠금 대기 실패는 좌석 충돌 코드로 바꾸지 않는다. 잠금 장애는 전체 롤백 후 500으로 처리하며, 결과가 불확실한 클라이언트는 같은 UUID로 재시도할 수 있다.
- 최초 UUID 조회 시점에 커밋된 예매에는 재응답 우선 규칙을 적용한다. 첫 조회에서 아직 커밋되지 않은 동일 UUID라도 두 요청이 삽입까지 진행하면 UUID 제약과 롤백 이후 조회로 201 한 건과 200 한 건을 얻는다. 기존 좌석에 대한 INSERT가 실패하기 전에 UUID를 검사하므로 동일 제출을 좌석 충돌로 분류하지 않는다.

## 구현 검증 기준

| 계층 | 시나리오 | 기대 결과 |
| --- | --- | --- |
| MVC | 정상 생성 및 재응답 | 실제 HTTP 상태와 본문 `status` 일치, `Created` 및 `OK`, 최초 `data` 유지 |
| MVC | 문자·0 경로 ID, 잘못된 JSON·UUID, 문자열·소수·불리언 정수값, 배열 대신 문자열 | 각각 `INVALID_PARAMETER_TYPE`, `VALIDATION_FAILED`, `MALFORMED_REQUEST_BODY` 적용 |
| MVC·서비스 | 누락·`null`, 유형별 0~8과 총 1~8 경계, 중복 좌석, 인원·좌석 불일치, 타 상영관 좌석, 대소문자만 다른 좌석 | 400과 `errors`, 본문 중첩 필드 및 `showtimeId` 경로명 확인 |
| MVC | 지원하지 않는 `Accept` 및 `Content-Type` | 각각 본문 없는 406 및 공통 JSON 415 |
| 서비스 | 회차 없음, 고정 `Clock` 기준 상영 시작 직전·정각·이후, 예상 금액 불일치 | 각각 404, 직전 허용·정각부터 409, 409. 가격 검증에 원 단위 정수 사용 |
| 서비스·MySQL | 좌석 배열 역순 재전송, 단가 변경 또는 회차 시작 뒤 재전송, UUID 재사용으로 회차·인원·좌석·금액 변경 | 일치하면 같은 `data`의 200, 달라지면 `RESERVATION_REQUEST_CONFLICT` |
| MySQL | 같은 회차에서 겹치는 여러 좌석의 동시 예매 | 한 요청만 201. 실패 요청은 409이며 그 요청의 예매 및 겹치지 않은 좌석 행도 남지 않는다. |
| MySQL | 동일 UUID 동시 제출과 동일 UUID의 다른 내용 동시 제출 | 동일 내용은 201과 200으로 한 건 저장. 다른 내용은 201과 `RESERVATION_REQUEST_CONFLICT`로 한 건 저장. |
| MySQL | 회차가 다른 동일 물리 좌석 예약 | 두 예매 모두 성공하며 회차별 좌석 점유가 분리된다. |

동시성 검증은 실제 MySQL에서 두 연결의 독립 트랜잭션과 동기화 지점을 사용한다. 응답과 함께 커밋된 `reservation` 및 `reservation_seat`의 행을 별도 트랜잭션에서 조회해 확인한다. 구현 후 Windows PowerShell에서 로컬 MySQL과 `DB_PASSWORD`를 설정하고 `./gradlew.bat build`를 실행한다.
