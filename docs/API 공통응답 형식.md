# API 공통 응답 형식 안내

예시의 데이터와 검증 메시지는 형식 설명용이며, 개별 API의 데이터 구조와 오류 조건은 개별 API 명세를 참고해 주세요. [좌석 조회 SPEC](specs/get-showtime-seats.md)

## 성공 응답

공통 성공 응답은 `status`, `message`, `data`를 포함합니다.

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `status` | number | 응답 본문에 담긴 상태 코드. 공통 생성 메서드는 200 또는 201을 사용 |
| `message` | string | 200은 `OK`, 201은 `Created` |
| `data` | API별로 다름 또는 null | API가 반환하는 데이터. 반환할 데이터가 없으면 `null` |

HTTP 200 성공 예시입니다.

```json
{
  "status": 200,
  "message": "OK",
  "data": {
    "id": 1
  }
}
```

공통 생성 응답의 본문 예시입니다. 생성 API가 HTTP 201을 반환하는지 개별 API 명세도 확인해 주세요.

```json
{
  "status": 201,
  "message": "Created",
  "data": {
    "id": 1
  }
}
```

데이터가 없는 공통 성공 응답은 다음과 같습니다.

```json
{
  "status": 200,
  "message": "OK",
  "data": null
}
```

공통 성공 본문의 `status`와 실제 HTTP 상태는 서버에서 각각 설정합니다. 프론트에서는 실제 HTTP 상태를 기준으로 성공 여부를 판단하고, API별 실제 상태는 개별 명세를 확인해 주세요. HTTP 204 응답에는 본문이 없습니다.

## 오류 응답

JSON 본문이 있는 공통 오류 응답은 status, code, message를 포함합니다. 검증 상세가 있으면 `errors`도 포함합니다. 오류 응답에는 `data`가 없습니다.

```json
{
  "status": 404,
  "code": "NOT_FOUND",
  "message": "대상을 찾을 수 없습니다."
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `status` | number | 서버가 응답한 HTTP 상태 코드 |
| `code` | string | 오류 종류를 구분하는 코드 |
| `message` | string | 오류 설명. 같은 코드여도 상황에 따라 달라질 수 있음 |
| `errors` | 배열, 선택 사항 | 요청 검증 오류의 상세 목록 |

### 요청 검증 오류

요청값 검증 실패 시 HTTP 400과 `VALIDATION_FAILED` 코드를 반환하며, 확인된 검증 오류를 `errors`에 담습니다.

```json
{
  "status": 400,
  "code": "VALIDATION_FAILED",
  "message": "입력값을 확인해 주세요.",
  "errors": [
    {
      "field": "email",
      "message": "이메일을 입력해 주세요."
    },
    {
      "field": "password",
      "message": "비밀번호를 입력해 주세요."
    }
  ]
}
```

각 항목의 `field`는 검증 대상 필드 또는 파라미터 이름이며, 필드를 특정할 수 없는 객체 전체 검증 오류에서는 생략될 수 있습니다. 같은 필드에 여러 오류가 있을 수 있으므로 항목 순서에 의존하지 않고 처리해 주세요.

### 공통 오류 코드

| HTTP 상태 | `code` | 기본 메시지 |
| --- | --- | --- |
| 400 | `BAD_REQUEST` | 잘못된 요청입니다. |
| 400 | `VALIDATION_FAILED` | 입력값을 확인해 주세요. |
| 400 | `MALFORMED_REQUEST_BODY` | 요청 본문을 읽을 수 없습니다. |
| 400 | `MISSING_PARAMETER` | 필수 요청 파라미터가 누락되었습니다. |
| 400 | `INVALID_PARAMETER_TYPE` | 요청 파라미터의 형식이 올바르지 않습니다. |
| 404 | `NOT_FOUND` | 대상을 찾을 수 없습니다. |
| 405 | `METHOD_NOT_ALLOWED` | 지원하지 않는 HTTP 메서드입니다. |
| 406 | 응답 본문 없음 | 요청한 응답 형식을 제공할 수 없습니다. |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | 지원하지 않는 요청 형식입니다. |
| 500 | `INTERNAL_SERVER_ERROR` | 서버 내부 오류가 발생했습니다. |

위 상태는 각 코드의 기본값입니다. 별도로 분류하지 않은 Spring 예외는 실제 HTTP 상태를 유지하면서 4xx에 `CLIENT_ERROR`, 그 외 상태에 `INTERNAL_SERVER_ERROR` 코드를 사용할 수 있습니다.

### 응답 본문이 없는 경우

- HTTP 204 응답에는 본문이 없습니다.
- HTTP 406 응답은 현재 서버 구현에서 본문 없이 반환합니다.
- 네트워크 오류에는 HTTP 응답 자체가 없을 수 있으며, 서버 외부에서 발생한 오류는 공통 JSON 형식이 아니거나 본문이 없을 수 있습니다.

## 프론트엔드 처리 참고사항

- 실제 HTTP 상태를 기준으로 성공과 실패를 판단해 주세요. 공통 응답의 본문 `status`는 HTTP 상태와 일치하도록 서버에서 설정합니다.
- 오류 본문이 공통 형식이면 `code`를 기준으로 세부 처리하고, 알 수 없는 코드나 공통 형식이 아닌 응답에는 기본 오류 처리를 사용해 주세요. `message`는 상황에 따라 달라질 수 있으므로 분기 기준으로 사용하지 않습니다.
- 본문이 없는 응답이나 JSON이 아닌 응답도 처리할 수 있도록 파싱 오류에 대비해 주세요. 네트워크 요청 자체가 실패하는 경우도 별도로 처리해야 합니다.
- `errors`와 각 항목의 `field`는 생략될 수 있습니다.
- 추후 도메인별 오류 코드가 추가될 수 있습니다.

## TypeScript 타입 예시

다음은 공통 JSON 응답의 타입 예시입니다. HTTP 204·406처럼 본문이 없는 응답에는 적용되지 않습니다.

```ts
interface ApiSuccess<T> {
  status: number;
  message: string;
  data: T;
}

interface ValidationError {
  field?: string;
  message: string;
}

interface ApiError {
  status: number;
  code: string;
  message: string;
  errors?: ValidationError[];
}

// 데이터가 없는 성공 응답: ApiSuccess<null>
```

타입 선언만으로 실제 응답 형식이 검증되지는 않습니다. 본문을 읽은 뒤 공통 형식인지 확인하고 사용해 주세요.
