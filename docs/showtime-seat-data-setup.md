# 좌석 조회 데이터 적용 절차

`GET /api/showtimes/{showtimeId}/seats`는 실제 좌석 좌표와 회차별 단가를 사용한다. 자료가 누락된 경우 회차 또는 상영관 ID를 로그에 남기고 공통 500 응답을 반환한다.

## 기존 좌석 배치 채우기

1. 배포 전 기존 DB를 백업하고 테스트 DB에서 적용을 확인한다. 기존 `seat`에 아래 nullable 컬럼을 먼저 추가한다. 신규 DB는 엔티티 매핑으로 생성할 수 있다.

```sql
ALTER TABLE seat
    ADD COLUMN row_index INT NULL,
    ADD COLUMN column_index INT NULL,
    ADD COLUMN is_accessible BOOLEAN NULL;
```

2. 확인된 상영관 배치 자료로 `row_index`와 `column_index`를 함께 채운다. 행과 열은 0부터 시작하며 통로 칸은 건너뛴다. 좌석 번호의 문자와 숫자만으로 통로 위치를 추정하지 않는다. 장애인석 구분이 미확인인 경우 `is_accessible`은 NULL로 유지한다.
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

## 회차별 요금 등록

`showtime_ticket_price`에 노출할 회차마다 한 행을 등록한다. `showtime_id`는 유일한 외래 키다. `adult_price`, `teen_price`, `senior_price`, `disabled_price`는 원 단위 정수이며 각각 0~268435455 범위다. 팀에서 확인한 실제 요금을 입력한다. 단가 0원도 유효하다.

현재 `ddl-auto: update`를 사용하는 환경에서는 테이블이 생성되지만 단가 행은 별도로 등록해야 한다. 자동 스키마 갱신이 없는 환경은 배포 전 동일한 엔티티 스키마를 적용한다. 운영 적용 여부는 `SHOW CREATE TABLE showtime_ticket_price`로 확인한다.
