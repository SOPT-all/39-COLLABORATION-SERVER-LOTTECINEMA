# 좌석 조회 데이터 적용 절차

좌석 조회는 실제 좌석 좌표와 `application.yml`의 공통 단가를 사용한다. 좌표 자료가 누락된 경우 상영관 ID를 로그에 남기고 공통 500 응답을 반환한다.

## 시작 시 좌석 자동 등록

`seed` 프로필을 활성화하면 `SeatDataSeeder`가 JPA 초기화 후 실행되어 `seed/seats-12x15.csv`의 77석을 등록한다. 먼저 대상 `screen`과 참조하는 영화관·지역 데이터가 있어야 한다. `SEAT_SEED_SCREEN_ID`는 회차 ID가 아닌 상영관 ID다. 아래 `3`은 실행 예시이며 실제 DB의 상영관 ID로 바꾼다.

```powershell
$env:SEAT_SEED_SCREEN_ID = "3"
./gradlew.bat bootRun --args='--spring.profiles.active=seed'
```

IntelliJ에서는 실행 설정의 Active profiles에 `seed`, 환경변수에 `SEAT_SEED_SCREEN_ID=3`을 지정한다. 다른 프로필과 함께 사용할 경우 `local,seed`처럼 활성화한다. 기존 DB 계정 환경변수도 해당 실행에 설정한다.

동일 설정으로 다시 시작하면 기존 좌석 ID를 유지하고, 누락 좌석과 빈 좌표를 채운다. 다른 좌석 번호나 충돌하는 좌표가 존재하면 전체 작업을 롤백하고 시작을 중단한다. 같은 상영관을 동시에 초기화하는 요청은 상영관 행 잠금으로 순서를 보장한다.

좌석이 없는 칸은 CSV에 포함하지 않는다. 화면에서는 12×15 그리드를 만들고 좌표에 좌석을 배치한다.

## 기존 좌석 배치 채우기

1. 배포 전 기존 DB를 백업하고 테스트 DB에서 적용을 확인한다. 기존 `seat`에 아래 nullable 컬럼을 먼저 추가한다. 신규 DB는 엔티티 매핑으로 생성할 수 있다.

```sql
ALTER TABLE seat
    ADD COLUMN row_index INT NULL,
    ADD COLUMN column_index INT NULL;
```

2. 확인된 상영관 배치 자료로 `row_index`와 `column_index`를 함께 채운다. 행과 열은 0부터 시작하며 통로 칸은 건너뛴다. 좌석 번호의 문자와 숫자만으로 통로 위치를 추정하지 않는다.
3. 누락·음수·중복 좌표와 실제 배치 대비 누락 좌석을 확인한다. 아래 조회 결과가 모두 없어야 한다.

```sql
SELECT id, screen_id, seat_code
FROM seat
WHERE row_index IS NULL OR column_index IS NULL
   OR row_index < 0 OR column_index < 0;

SELECT screen_id, row_index, column_index, COUNT(*)
FROM seat
GROUP BY screen_id, row_index, column_index
HAVING COUNT(*) > 1;
```

4. 백필을 마친 DB에는 필수 좌표 제약을 적용한다. 엔티티의 좌표 타입은 기존 DB의 미완성 행을 읽어 명확히 오류 처리할 수 있도록 `Integer`를 유지한다. `SHOW CREATE TABLE seat`로 `uk_seat_screen_position`과 `ck_seat_position`이 생성됐는지 확인한다. 자동 스키마 갱신이 적용되지 않는 환경에는 해당 유일·음수 방지 제약도 수동 적용한다.

```sql
ALTER TABLE seat
    MODIFY COLUMN row_index INT NOT NULL,
    MODIFY COLUMN column_index INT NOT NULL;
```

## 공통 요금 설정

`application.yml`의 `ticket.prices`에 모든 회차가 공유하는 원 단위 단가를 작성한다. 성인 15000원, 청소년 12000원, 경로 10000원, 장애인 10000원을 적용한다. 금액을 바꾸고 재배포·재시작하면 새 단가가 전체 회차에 적용된다. 기존 예매의 확정 총액은 예매 행에 저장한다.

`TicketPriceProperties`가 네 필수값과 각각의 0~268435455 범위를 시작 시 검증한다. 누락·범위 초과·형식 오류가 있으면 애플리케이션 시작이 실패한다. 단가 0원도 유효하다.

이전 버전의 `showtime_ticket_price` 테이블이 있는 DB는 공통 요금 버전으로 전환한 뒤 기존 자료를 확인하고 아래 SQL을 한 번 적용한다.

```sql
DROP TABLE showtime_ticket_price;
```

## 기존 DB의 장애인석 구분 컬럼 제거

장애인석 구분을 제거한 버전부터 좌석 엔티티와 API 응답은 해당 필드를 사용하지 않는다. 이전 버전이 생성한 DB에는 `is_accessible` 컬럼이 남을 수 있으므로 `SHOW COLUMNS FROM seat LIKE 'is_accessible'`로 존재 여부를 확인한 뒤 아래 SQL을 한 번 적용한다.

```sql
ALTER TABLE seat DROP COLUMN is_accessible;
```
