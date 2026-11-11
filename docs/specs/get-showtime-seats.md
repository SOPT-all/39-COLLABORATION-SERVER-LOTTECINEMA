# 상영 회차 좌석 조회 SPEC

| 항목 | 내용 |
| --- | --- |
| API | `GET /api/v1/showtimes/{showtimeId}/seats` |
| 사용 화면 | V2-03 좌석 현황, V3-01 인원·좌석 선택 |
| 상태 | 구현 완료 |
| 기준 문서 | [상영 회차 좌석 조회 및 예매 API 설계](../showtime-seat-reservation-api.md), [공통 응답 형식](../API%20공통응답%20형식.md) |
| 작업 순서 | [좌석 조회·예매 생성 API 구현 계획](../api-implementation-plan.md) |

V2-02의 회차 목록에서 선택한 `showtimeId`로 영화·극장 정보, 좌석 배치, 회차별 예매 여부와 유형별 단가를 조회한다. V2-03은 잔여 좌석 수를 표시하고 V3-01은 좌석 배치와 예상 총액을 표시한다. 선택한 `showtimeId`와 `seatCode`는 [예매 생성](create-showtime-reservation.md)에 그대로 사용한다.

## 요청

```http
GET /api/v1/showtimes/1203/seats
Accept: application/json
```

| 경로 변수 | 타입 | 제약 |
| --- | --- | --- |
| `showtimeId` | integer (`int64`) | 필수, 1 이상 |

쿼리 파라미터와 요청 본문은 없다. 요청의 `Accept`에 JSON이 허용되어야 한다.

## 성공 응답

HTTP 200과 `SuccessResponse<ShowtimeSeatResponse>`를 반환한다. `status`는 `200`, `message`는 `OK`이며 `data`는 다음 구조를 따른다.

| `data` 필드 | 타입 | 의미 및 조건 |
| --- | --- | --- |
| `startAt`, `endAt` | string | 필수. 상영 시작·종료 일시, ISO 8601과 `+09:00` 오프셋 |
| `formatType` | string | 필수. `2D`, `3D`, `4DX` 중 하나 |
| `movie` | object | 필수. 회차의 영화 정보 |
| `movie.title` | string | 필수. 영화 제목 |
| `movie.posterUrl` | string 또는 null | 필수 키. 포스터 미등록 시 `null` |
| `movie.ageRating` | string | 필수. 관람 등급. 예를 들어 `ALL` |
| `cinema` | object | 필수. 상영관이 속한 영화관 정보 |
| `cinema.name` | string | 필수. 영화관 표시 이름 |
| `screen` | object | 필수. 회차의 상영관 정보 |
| `screen.name` | string | 필수. 상영관 표시 이름 |
| `screen.seatType` | string | 필수. `STANDARD` 또는 `RECLINER` |
| `totalSeatCount` | integer | 필수. 해당 상영관에 등록된 전체 좌석 수, 0 이상 |
| `availableSeatCount` | integer | 필수. 해당 회차에서 확정 예매가 없는 좌석 수, 0 이상 |
| `seats` | object[] | 필수. 해당 상영관의 전체 좌석을 행·열 오름차순으로 반환 |
| `seats[].seatCode` | string | 필수. 1~10자의 좌석 표시 번호 |
| `seats[].rowIndex` | integer | 필수. 스크린에 가까운 쪽부터 0 기반 행 위치 |
| `seats[].columnIndex` | integer | 필수. 왼쪽부터 0 기반 열 위치 |
| `seats[].isReserved` | boolean | 필수. 해당 회차에서 확정 예매가 있으면 `true` |
| `ticketPrices` | object | 필수. 해당 회차에 적용되는 유형별 1인 단가 |
| `ticketPrices.adult` | integer | 필수. 성인 단가, 0 이상, 원(KRW) |
| `ticketPrices.teen` | integer | 필수. 청소년 단가, 0 이상, 원(KRW) |
| `ticketPrices.senior` | integer | 필수. 경로 단가, 0 이상, 원(KRW) |
| `ticketPrices.disabled` | integer | 필수. 장애인 단가, 0 이상, 원(KRW) |

- `totalSeatCount`는 `seats`의 길이이며 `availableSeatCount`는 `isReserved: false`인 항목의 개수다. 매진이면 `availableSeatCount: 0`, 등록된 좌석이 0개면 `seats: []`와 두 집계 값 `0`을 반환한다.
- 좌석의 좌표는 저장된 배치를 그대로 반환한다. 열 번호 사이의 빈 칸으로 통로를 표현한다. 예를 들어 `columnIndex`가 `0`과 `2`인 두 좌석 사이의 `1`은 빈 칸이다.
- 같은 물리 좌석도 `showtimeId`마다 확정 여부가 달라질 수 있다. 예매가 마감된 회차도 조회할 수 있으며, 선택한 좌석은 예매 생성이 성공하면 확정된다.

```json
{
  "status": 200,
  "message": "OK",
  "data": {
    "startAt": "2026-11-12T14:25:00+09:00",
    "endAt": "2026-11-12T16:14:00+09:00",
    "formatType": "2D",
    "movie": { "title": "극장판 치이카와", "posterUrl": null, "ageRating": "ALL" },
    "cinema": { "name": "용산" },
    "screen": { "name": "4관 리클라이너(4층)", "seatType": "RECLINER" },
    "availableSeatCount": 3,
    "totalSeatCount": 4,
    "seats": [
      { "seatCode": "E1", "rowIndex": 0, "columnIndex": 0, "isReserved": false },
      { "seatCode": "E3", "rowIndex": 0, "columnIndex": 2, "isReserved": true },
      { "seatCode": "E4", "rowIndex": 0, "columnIndex": 3, "isReserved": false },
      { "seatCode": "E5", "rowIndex": 0, "columnIndex": 4, "isReserved": false }
    ],
    "ticketPrices": { "adult": 15000, "teen": 12000, "senior": 10000, "disabled": 10000 }
  }
}
```

## 오류 응답

| HTTP | `code` | 발생 조건 |
| --- | --- | --- |
| 400 | `INVALID_PARAMETER_TYPE` | 경로 값을 `Long`으로 변환할 수 없음. 예를 들어 `abc` 또는 `Long.MAX_VALUE`를 초과한 값 |
| 400 | `VALIDATION_FAILED` | `showtimeId`가 1 미만. `errors[].field`에 `showtimeId`를 담음 |
| 404 | `NOT_FOUND` | 지정한 회차가 없음 |
| 406 | 본문 없음 | `Accept`에 JSON이 허용되지 않음 |
| 500 | `INTERNAL_SERVER_ERROR` | 저장소 오류, 필수 좌표 누락 등 서버 내부 오류 |

```json
{
  "status": 404,
  "code": "NOT_FOUND",
  "message": "해당 상영 회차를 찾을 수 없습니다."
}
```

## 데이터 준비

| 데이터 | 현재 구조와 구현안 |
| --- | --- |
| 회차·영화관 정보 | `Showtime`의 `movie`, `screen`, `screen.cinema` 관계를 사용한다. 회차의 `LocalDateTime`은 DB에 KST 현지 시각으로 저장된다. |
| 좌석 배치 | 기존 `seat`에 0 이상의 `row_index`, `column_index`를 추가한다. 좌표는 상영관별 실제 배치로 기존 행을 채운 뒤 `NOT NULL`과 `(screen_id, row_index, column_index)` 유일 제약을 적용한다. 공개 전에는 상영관별 좌석 수와 유효한 1~10자 좌석 번호를 원본 배치와 대조해 시딩 누락을 확인한다. `(screen_id, seat_code)`는 좌석 번호 조회에 사용한다. |
| 예매 상태 | `reservation_seat`에서 회차와 물리 좌석 ID가 일치하는 확정 기록을 확인한다. 기존 `(showtime_id, seat_id)` 유일 제약을 사용한다. |
| 단가 | `application.yml`의 `ticket.prices`에 네 유형의 공통 단가를 작성한다. `TicketPriceProperties`가 필수값과 0~268435455 범위를 시작 시 검증한다. 모든 회차는 동일한 설정값을 사용한다. |

서버를 시작하면 `SeatDataSeeder`가 모든 프로필에서 실행되어 기존 상영관마다 공통 77석을 등록한다. 지역·영화관·상영관이 먼저 존재해야 한다. 로컬에서는 앱을 한 번 시작해 스키마를 만든 뒤 검증용 상영관 SQL을 적용하고 앱을 재시작하면 자동 시딩을 확인할 수 있다. [로컬 상영관 데이터](../sql/local-cinema-seed.sql)

시딩은 단일 Spring 서버를 기준으로 한다. 재시작 시 기존 좌석 ID를 유지하고 빈 좌표와 누락 좌석을 채운다. 기존 좌석 번호나 좌표가 공통 배치와 충돌하면 전체 시딩을 롤백하고 시작을 중단한다. 화면은 12×15 그리드에 좌표별로 좌석을 배치해 끝부분의 빈 행도 표현한다.

## 구현 흐름

1. `showtime.controller.ShowtimeApi`에 Swagger 요청·응답 스키마와 상태를 선언하고 `ShowtimeController`에서 `@GetMapping`과 `produces = application/json`을 지정한다. `@PathVariable("showtimeId") @Positive Long showtimeId`는 Spring MVC 메서드 검증을 사용한다.
2. `showtime.service.ShowtimeSeatService#getShowtimeSeats`의 읽기 전용 트랜잭션에서 조회와 DTO 변환을 마친다. `REPEATABLE_READ`를 적용해 두 조회가 MySQL의 동일한 읽기 스냅샷을 사용하도록 한다. `ShowtimeRepository`는 회차 및 `movie`, `screen`, `screen.cinema`를 함께 로딩한다. 회차가 없으면 `BusinessException`과 `ErrorCode.NOT_FOUND`를 사용한다.
3. `SeatRepository`는 해당 상영관의 좌석과 회차별 예매 여부를 좌석당 한 행으로 조회한다. `seat`를 기준으로 `reservation_seat`를 `seat_id`와 요청 `showtime_id` 두 조건으로 LEFT JOIN하고 `screen_id`를 필터링한다. 회차 조건을 조인에 두어 확정 예매가 없는 좌석도 반환한다. `row_index`, `column_index` 순으로 정렬하고, 두 집계 값은 같은 결과 목록에서 계산한다.
4. `TicketPriceProperties`의 공통 단가로 `ticketPrices`를 구성한다. DB는 회차와 좌석을 각각 한 번 조회한다. 등록 좌석에 필수 좌표가 누락되면 서버 오류로 처리하고 상영관 ID를 로그에 남긴다.
5. `showtime.dto.ShowtimeSeatResponse`와 하위 응답 DTO로 매핑한다. 저장 시각에 KST `+09:00`을 적용해 ISO 8601 문자열로 직렬화한다. `FormatType.TWO_D`, `THREE_D`, `FOUR_DX`는 각각 명세의 문자열 `2D`, `3D`, `4DX`로 변환한다. `isReserved`의 JSON 이름을 명시하고, `posterUrl` 미등록 시 `null`을 포함한다. 컨트롤러는 `SuccessResponse.success(data)`를 반환하고 오류 처리는 `GlobalExceptionHandler`를 재사용한다.

## 완료 기준

| 검증 | 기대 결과 |
| --- | --- |
| HTTP 계약 | MockMvc에서 정상 200과 `status`, `message`, `data`, JSON 타입을 확인한다. `abc`·`Long` 범위 초과는 400 `INVALID_PARAMETER_TYPE`, `0`·음수는 400 `VALIDATION_FAILED`, 없는 회차는 404 `NOT_FOUND`, `Accept: application/xml`은 본문 없는 406이다. |
| 직렬화 | KST 오프셋, `2D`·`3D`·`4DX`, `posterUrl: null`을 확인한다. |
| 상영관·회차 분리 | 대상 회차와 같은 상영관의 다른 회차에서 각각 `E1`, `E3`을 예매한 뒤 대상 회차를 조회한다. `E1.isReserved`는 `true`, `E3.isReserved`는 `false`다. 다른 상영관 좌석은 응답에서 제외된다. |
| 배치·집계 | 좌표 순서와 통로 위치를 보존한다. 일부 예매, 매진, 좌석이 0개인 경우 모두 응답 목록과 집계 값이 일치한다. 마감된 회차에서도 조회 결과를 반환한다. |
| 단가·오류 | 서로 다른 회차가 동일한 네 단가를 반환한다. 설정 누락·잘못된 값은 시작 실패, DB 장애는 공통 500으로 처리한다. |
| 조회 비용·커밋 가시성 | 다수 좌석을 조회해 SQL 횟수가 좌석 수에 따라 늘지 않음을 확인한다. 예매 생성 커밋 뒤 새 GET에서 해당 좌석의 `isReserved`와 잔여 좌석 수가 갱신된다. |
