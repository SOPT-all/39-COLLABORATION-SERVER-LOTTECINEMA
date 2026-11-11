# 상영 회차 좌석 조회 및 예매 API 설계

V2-03 좌석 현황과 V3-01 인원·좌석 선택 화면에서 사용하는 API 계약입니다. 좌석 조회 GET은 구현되었으며 예매 생성 POST는 후속 구현을 위한 초안입니다. 유형별 단가는 application.yml에서 공통 관리하며, 예시의 ID, 날짜와 좌석은 설명용입니다.

| 화면 | 메서드 | URL | 동작 |
| --- | --- | --- | --- |
| V2-03 좌석 현황 / V3-01 좌석 선택 | GET | `/api/v1/showtimes/{showtimeId}/seats` | 회차 정보, 좌석 배치 및 예매 여부, 잔여 좌석, 유형별 단가 조회 |
| V3-01 좌석 선택 완료 | POST | `/api/v1/showtimes/{showtimeId}/reservations` | 선택한 좌석과 유형별 인원으로 예매 생성 |

1. V2-02 회차 목록의 각 항목에는 해당 회차의 `showtimeId`를 포함합니다. 프론트는 선택한 ID를 V2-03과 V3-01에서 유지해 좌석 조회와 예매 생성 경로에 사용합니다.
2. V2-03에서 잔여 좌석과 회차 정보를 표시합니다. V3-01에서 같은 조회 결과로 좌석 배치를 그리고, 유형별 단가와 선택 인원으로 예상 금액을 계산합니다.
3. V3-01에서 **결제** 버튼을 누르면 선택한 좌석 번호, 유형별 인원, 화면에 표시한 금액을 POST로 보냅니다. 성공하면 요청한 좌석이 해당 회차에 확정됩니다. 외부 결제 승인, 할인 및 쿠폰 처리는 별도 범위입니다.

성공 JSON은 `status`, `message`, `data`를 포함합니다. 오류 JSON은 `status`, `code`, `message`와 필요한 경우 `errors`를 포함합니다. HTTP 상태로 성공 여부를, 오류 `code`로 화면의 후속 동작을 결정합니다. 날짜·시간은 한국 표준시 오프셋을 포함한 ISO 8601 문자열로 전달합니다. 금액 단위는 원(KRW)입니다.

## 1. 특정 상영 회차 좌석 정보 조회 (V2-03)

### URL

```http
GET /api/v1/showtimes/{showtimeId}/seats
```

### Path parameter

| Key | 설명 | Value 타입 | 옵션 |
| --- | --- | --- | --- |
| `showtimeId` | V2-02 목록에서 선택한 상영 회차 ID | integer | 필수, 1 이상 |

서버는 `showtimeId`로 연결된 영화, 상영 일시, 영화관, 상영관을 조회합니다. 프론트가 해당 값을 따로 전달할 필요는 없습니다.

### Request

요청 본문이 없습니다.

### Query parameter

없습니다.

### Response

| Key | 설명 | Value 타입 | 옵션 |
| --- | --- | --- | --- |
| `status` | HTTP 상태 코드 | integer | 필수, `200` |
| `message` | 결과 메시지 | string | 필수, `OK` |
| `data` | 회차 및 좌석 조회 결과 | object | 필수 |
| `data.startAt` | 상영 시작 일시 | string (ISO 8601) | 필수, 예: `2026-11-12T14:25:00+09:00` |
| `data.endAt` | 상영 종료 일시 | string (ISO 8601) | 필수 |
| `data.formatType` | 상영 포맷 | string | 필수, `2D` / `3D` / `4DX` |
| `data.movie` | 영화 정보 | object | 필수 |
| `data.movie.title` | 영화 제목 | string | 필수 |
| `data.movie.posterUrl` | 포스터 이미지 주소 | string 또는 null | 필수, 미등록 시 `null` |
| `data.movie.ageRating` | 관람 등급 | string | 필수, 예: `ALL` |
| `data.cinema` | 영화관 정보 | object | 필수 |
| `data.cinema.name` | 영화관 이름 | string | 필수, 예: `용산` |
| `data.screen` | 상영관 정보 | object | 필수 |
| `data.screen.name` | 상영관 표시 이름 | string | 필수, 예: `4관 리클라이너(4층)` |
| `data.screen.seatType` | 상영관의 기본 좌석 유형 | string | 필수, `STANDARD` / `RECLINER` |
| `data.availableSeatCount` | 조회 시점에 예매가 확정되지 않은 좌석 수 | integer | 필수, 0 이상 |
| `data.totalSeatCount` | 해당 상영관의 등록 좌석 수 | integer | 필수, 0 이상 |
| `data.seats` | 상영관 전체 좌석 목록 | object[] | 필수, 행·열 순으로 반환 |
| `data.seats[].seatCode` | 좌석 표시 번호. 예매 요청에도 같은 값을 사용 | string | 필수, 최대 10자, 예: `E4` |
| `data.seats[].rowIndex` | 화면상 좌석 행 위치. 스크린에 가까운 쪽부터 증가 | integer | 필수, 0부터 시작 |
| `data.seats[].columnIndex` | 화면상 좌석 열 위치. 왼쪽부터 증가 | integer | 필수, 0부터 시작 |
| `data.seats[].isReserved` | 이 회차에서 해당 좌석의 예매가 확정되었는지 여부 | boolean | 필수, `true`이면 선택 불가 |
| `data.ticketPrices` | 회차에 적용되는 유형별 1인 단가 | object | 필수 |
| `data.ticketPrices.adult` | 성인 1인 단가 | integer | 필수, 0 이상 |
| `data.ticketPrices.teen` | 청소년 1인 단가 | integer | 필수, 0 이상 |
| `data.ticketPrices.senior` | 경로 1인 단가 | integer | 필수, 0 이상 |
| `data.ticketPrices.disabled` | 장애인 1인 단가 | integer | 필수, 0 이상 |

- 화면의 잔여 좌석은 `availableSeatCount / totalSeatCount`로 표시합니다. `totalSeatCount`는 `seats` 길이와 같고, `availableSeatCount`는 목록에서 `isReserved`가 `false`인 좌석 수입니다. 매진이어도 조회는 200을 반환하며 예매 가능 좌석 수가 0입니다.
- 잔여 좌석 수는 좌석 현황을 나타냅니다. 잔여 좌석이 있어도 예매가 마감된 회차는 POST에서 `SHOWTIME_NOT_BOOKABLE`을 반환합니다.
- 좌석의 행·열 위치는 같은 회차를 다시 조회해도 유지됩니다. 통로처럼 좌석이 없는 칸은 목록에 넣지 않으며, 열 번호를 건너뛰어 간격을 표현합니다.
- 조회 이후 다른 요청이 좌석을 먼저 예매할 수 있습니다. 프론트가 화면에서 선택한 좌석은 POST가 성공할 때 확정됩니다.

### Example

**Request**

```http
GET /api/v1/showtimes/1203/seats
Accept: application/json
```

**Response · HTTP 200 OK**

```json
{
  "status": 200,
  "message": "OK",
  "data": {
    "startAt": "2026-11-12T14:25:00+09:00",
    "endAt": "2026-11-12T16:14:00+09:00",
    "formatType": "2D",
    "movie": {
      "title": "극장판 치이카와: 인어 섬의 비밀",
      "posterUrl": "https://example.com/posters/31.jpg",
      "ageRating": "ALL"
    },
    "cinema": {
      "name": "용산"
    },
    "screen": {
      "name": "4관 리클라이너(4층)",
      "seatType": "RECLINER"
    },
    "availableSeatCount": 4,
    "totalSeatCount": 6,
    "seats": [
      { "seatCode": "E1", "rowIndex": 0, "columnIndex": 0, "isReserved": false },
      { "seatCode": "E2", "rowIndex": 0, "columnIndex": 1, "isReserved": false },
      { "seatCode": "E4", "rowIndex": 0, "columnIndex": 3, "isReserved": false },
      { "seatCode": "E5", "rowIndex": 0, "columnIndex": 4, "isReserved": false },
      { "seatCode": "E6", "rowIndex": 0, "columnIndex": 5, "isReserved": true },
      { "seatCode": "E7", "rowIndex": 0, "columnIndex": 6, "isReserved": true }
    ],
    "ticketPrices": {
      "adult": 15000,
      "teen": 12000,
      "senior": 10000,
      "disabled": 10000
    }
  }
}
```

### Status

| Status | Response content |
| --- | --- |
| 200 OK | 회차 및 좌석 조회 결과 |
| 400 Bad Request | `INVALID_PARAMETER_TYPE` — `showtimeId`가 정수로 변환되지 않음 |
| 400 Bad Request | `VALIDATION_FAILED` — `showtimeId`가 1 미만 |
| 404 Not Found | `NOT_FOUND` — 해당 상영 회차를 찾을 수 없음 |
| 406 Not Acceptable | 요청의 `Accept` 조건에 맞는 응답 형식이 없음. 본문 없음 |
| 500 Internal Server Error | `INTERNAL_SERVER_ERROR` — 서버 내부 오류 |

## 2. 특정 상영 회차 인원·좌석 선택 및 예매 (V3-01)

### URL

```http
POST /api/v1/showtimes/{showtimeId}/reservations
```

### Path parameter

| Key | 설명 | Value 타입 | 옵션 |
| --- | --- | --- | --- |
| `showtimeId` | 좌석 조회에 사용한 상영 회차 ID | integer | 필수, 1 이상 |

### Request

`Content-Type: application/json`

| Key | 설명 | Value 타입 | 옵션 |
| --- | --- | --- | --- |
| `requestId` | 프론트가 새 예매를 처음 제출할 때 `crypto.randomUUID()`로 한 번 생성하는 요청 식별자 | string (UUID) | 필수. 동일한 요청을 재시도할 때는 최초에 생성한 UUID를 그대로 사용 |
| `ticketCounts` | 유형별 관람 인원 | object | 필수, 유형별 합계 1~8 |
| `ticketCounts.adult` | 성인 인원 | integer | 필수, 0~8 |
| `ticketCounts.teen` | 청소년 인원 | integer | 필수, 0~8 |
| `ticketCounts.senior` | 경로 인원 | integer | 필수, 0~8 |
| `ticketCounts.disabled` | 장애인 인원 | integer | 필수, 0~8 |
| `seatCodes` | 좌석 조회 응답에서 선택한 `seatCode` 목록 | string[] | 필수, 1~8개, 중복 불가, 각 값 1~10자 |
| `expectedTotalAmount` | 결제 버튼을 누르기 전에 화면에 표시한 총액 | integer | 필수, 0 이상. 서버 계산값과 비교 |

### Query parameter

없습니다.

### 예매 처리 규칙

- 좌석 수와 총인원은 같아야 합니다. 모든 좌석은 해당 회차의 상영관에 속해야 합니다.
- 서버는 현재 공통 유형별 단가에 인원수를 곱해 총액을 다시 계산합니다. 화면에서 보낸 `expectedTotalAmount`와 다르면 409를 반환합니다. 프론트는 좌석 조회를 다시 호출해 단가와 총액을 갱신하고 사용자에게 확인을 받습니다.
- 한 좌석이라도 이미 예매되어 있으면 요청 전체를 409로 처리합니다. 서버는 예매와 좌석을 한 번에 저장해 일부 좌석만 확정되는 일을 막습니다. 프론트는 좌석을 다시 조회해 선택을 갱신합니다.
- 네트워크 문제로 결과를 못 받았다면 동일한 `requestId`와 요청 값으로 재시도합니다. 서버는 신규 예매 가능 여부를 검사하기 전에 성공한 요청이 있는지 확인합니다. 회차, 인원, 좌석 집합 및 금액이 같으면 기존 예매를 200으로 돌려줍니다. 좌석 배열 순서는 비교에 영향을 주지 않습니다. 같은 `requestId`를 다른 요청에 재사용하면 409를 반환합니다. 사용자가 인원, 좌석 또는 금액을 변경해 새로 시도할 때는 새로운 `requestId`를 생성합니다.

### Response

| Key | 설명 | Value 타입 | 옵션 |
| --- | --- | --- | --- |
| `status` | HTTP 상태 코드 | integer | 필수, 신규 생성 `201`, 기존 예매 재응답 `200` |
| `message` | 결과 메시지 | string | 필수, `201`은 `Created`, `200`은 `OK` |
| `data` | 예매 확정 결과 | object | 필수 |
| `data.reservationId` | 생성된 예매 ID | integer | 필수 |
| `data.showtimeId` | 예매한 상영 회차 ID | integer | 필수 |
| `data.seatCodes` | 확정 좌석 번호 목록 | string[] | 필수 |
| `data.ticketCounts` | 확정된 유형별 인원 | object | 필수 |
| `data.ticketCounts.adult` | 성인 인원 | integer | 필수 |
| `data.ticketCounts.teen` | 청소년 인원 | integer | 필수 |
| `data.ticketCounts.senior` | 경로 인원 | integer | 필수 |
| `data.ticketCounts.disabled` | 장애인 인원 | integer | 필수 |
| `data.totalAmount` | 서버에서 계산해 확정한 총액 | integer | 필수, 원(KRW) |
| `data.createdAt` | 예매 생성 일시 | string (ISO 8601) | 필수, 재응답 시 최초 생성 일시 |

### Example

**Request**

```http
POST /api/v1/showtimes/1203/reservations
Content-Type: application/json
Accept: application/json
```

```json
{
  "requestId": "f7527fb5-60db-4814-b1f2-67f7630869e9",
  "ticketCounts": {
    "adult": 2,
    "teen": 0,
    "senior": 0,
    "disabled": 0
  },
  "seatCodes": ["E4", "E5"],
  "expectedTotalAmount": 30000
}
```

**Response · HTTP 201 Created**

```json
{
  "status": 201,
  "message": "Created",
  "data": {
    "reservationId": 9021,
    "showtimeId": 1203,
    "seatCodes": ["E4", "E5"],
    "ticketCounts": {
      "adult": 2,
      "teen": 0,
      "senior": 0,
      "disabled": 0
    },
    "totalAmount": 30000,
    "createdAt": "2026-11-09T16:30:15+09:00"
  }
}
```

같은 요청의 재응답은 HTTP 200과 `"status": 200`, `"message": "OK"`를 반환하고, `data`는 최초 예매 결과와 같습니다.

**Response · HTTP 409 Conflict (좌석 예매 충돌)**

```json
{
  "status": 409,
  "code": "SEAT_ALREADY_RESERVED",
  "message": "선택한 좌석 중 이미 예매된 좌석이 있습니다."
}
```

### Status

| Status | Response content |
| --- | --- |
| 201 Created | 신규 예매 확정 결과 |
| 200 OK | 같은 요청이 이미 성공한 경우 기존 예매 확정 결과 |
| 400 Bad Request | `INVALID_PARAMETER_TYPE` — `showtimeId`가 정수로 변환되지 않음 |
| 400 Bad Request | `MALFORMED_REQUEST_BODY` — JSON이 올바르지 않거나 필드의 값 타입을 읽을 수 없음 |
| 400 Bad Request | `VALIDATION_FAILED` — `showtimeId` 범위 위반, 필수값 누락, 인원 범위 초과, 좌석 중복, 인원과 좌석 수 불일치, 해당 상영관에 속하지 않는 좌석 등. 검증 결과를 `errors`로 반환 |
| 404 Not Found | `NOT_FOUND` — 해당 상영 회차를 찾을 수 없음 |
| 406 Not Acceptable | 요청의 `Accept` 조건에 맞는 응답 형식이 없음. 본문 없음 |
| 409 Conflict | `SEAT_ALREADY_RESERVED` — 요청한 좌석 중 하나 이상이 이미 예매됨 |
| 409 Conflict | `RESERVATION_AMOUNT_CHANGED` — 서버의 현재 계산 금액과 `expectedTotalAmount`가 다름 |
| 409 Conflict | `RESERVATION_REQUEST_CONFLICT` — 같은 `requestId`가 다른 내용의 예매에 사용됨 |
| 409 Conflict | `SHOWTIME_NOT_BOOKABLE` — 해당 회차의 예매 가능 시간이 지남 |
| 415 Unsupported Media Type | `UNSUPPORTED_MEDIA_TYPE` — 지원하지 않는 요청 `Content-Type` |
| 500 Internal Server Error | `INTERNAL_SERVER_ERROR` — 서버 내부 오류 |

## 구현 전 확인 사항

- 기존 `Reservation` 모델의 관람 유형은 성인, 청소년, 경로, 장애인입니다. 유형별 인원은 0~8명, 총인원은 1~8명이며, `requestId`와 총액을 저장할 수 있습니다. 기존 `ReservationSeat`의 회차·좌석 조합 유일 제약을 사용해 동시 예매를 막고, 충돌 시 예매와 좌석 저장을 함께 롤백합니다.
- 현재 `Seat` 모델은 상영관과 좌석 번호를 저장합니다. V3-01의 통로 간격까지 동일하게 렌더링하려면 행·열 위치를 제공할 좌석 배치 데이터가 필요합니다. 출입문 위치는 이 명세의 응답 범위에 포함하지 않습니다.
- 유형별 단가는 모든 회차에 동일하게 적용하며 application.yml의 ticket.prices에서 관리합니다. 예매 마감은 상영 시작 시각을 기본안으로 둡니다.
- 409의 도메인 오류 코드는 두 API 구현 시 공통 오류 코드에 추가합니다.
