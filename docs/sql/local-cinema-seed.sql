-- 로컬 좌석 시딩 검증용 데이터다. 현재 선택한 DB에만 적용된다.
-- 선행 조건: region, cinema, screen 테이블이 이미 생성되어 있어야 한다.
-- 지역 2개, 영화관 2개, 상영관 3개를 등록한다. 같은 이름이 있으면 재사용한다.
-- 지역·영화관·상영관 이름은 테스트 데이터임을 구분하려고 접두사를 붙였다.

START TRANSACTION;

-- 1. 영화관이 참조할 지역을 먼저 등록한다.
INSERT INTO region (name, display_order)
SELECT '[좌석시딩검증] 서울', 1
WHERE NOT EXISTS (SELECT 1 FROM region WHERE name = '[좌석시딩검증] 서울');

INSERT INTO region (name, display_order)
SELECT '[좌석시딩검증] 경기', 2
WHERE NOT EXISTS (SELECT 1 FROM region WHERE name = '[좌석시딩검증] 경기');

-- 2. 지역 이름으로 실제 ID를 찾아 영화관에 연결한다.
INSERT INTO cinema (region_id, name, display_order)
SELECT r.id, '[좌석시딩검증] 강남', 1
FROM region r
WHERE r.name = '[좌석시딩검증] 서울'
  AND NOT EXISTS (
      SELECT 1 FROM cinema c WHERE c.region_id = r.id AND c.name = '[좌석시딩검증] 강남'
  );

INSERT INTO cinema (region_id, name, display_order)
SELECT r.id, '[좌석시딩검증] 수원', 1
FROM region r
WHERE r.name = '[좌석시딩검증] 경기'
  AND NOT EXISTS (
      SELECT 1 FROM cinema c WHERE c.region_id = r.id AND c.name = '[좌석시딩검증] 수원'
  );

-- 3. 강남 영화관에 일반관 1개와 리클라이너관 1개를 등록한다.
INSERT INTO screen (cinema_id, name, seat_type)
SELECT c.id, '[좌석시딩검증] 1관', 'STANDARD'
FROM cinema c
JOIN region r ON r.id = c.region_id
WHERE r.name = '[좌석시딩검증] 서울' AND c.name = '[좌석시딩검증] 강남'
  AND NOT EXISTS (
      SELECT 1 FROM screen s WHERE s.cinema_id = c.id AND s.name = '[좌석시딩검증] 1관'
  );

INSERT INTO screen (cinema_id, name, seat_type)
SELECT c.id, '[좌석시딩검증] 2관', 'RECLINER'
FROM cinema c
JOIN region r ON r.id = c.region_id
WHERE r.name = '[좌석시딩검증] 서울' AND c.name = '[좌석시딩검증] 강남'
  AND NOT EXISTS (
      SELECT 1 FROM screen s WHERE s.cinema_id = c.id AND s.name = '[좌석시딩검증] 2관'
  );

-- 4. 수원 영화관에 일반관 1개를 등록한다.
INSERT INTO screen (cinema_id, name, seat_type)
SELECT c.id, '[좌석시딩검증] 1관', 'STANDARD'
FROM cinema c
JOIN region r ON r.id = c.region_id
WHERE r.name = '[좌석시딩검증] 경기' AND c.name = '[좌석시딩검증] 수원'
  AND NOT EXISTS (
      SELECT 1 FROM screen s WHERE s.cinema_id = c.id AND s.name = '[좌석시딩검증] 1관'
  );

COMMIT;

-- 등록된 상영관과 현재 좌석 수를 확인한다. 앱을 다시 시작한 뒤에는 각 상영관이 77석이어야 한다.
SELECT r.name AS region_name, c.name AS cinema_name, s.id AS screen_id,
       s.name AS screen_name, s.seat_type, COUNT(seat.id) AS seat_count
FROM screen s
JOIN cinema c ON c.id = s.cinema_id
JOIN region r ON r.id = c.region_id
LEFT JOIN seat ON seat.screen_id = s.id
WHERE (r.name = '[좌석시딩검증] 서울' AND c.name = '[좌석시딩검증] 강남')
   OR (r.name = '[좌석시딩검증] 경기' AND c.name = '[좌석시딩검증] 수원')
GROUP BY r.name, c.name, s.id, s.name, s.seat_type
ORDER BY r.name, c.name, s.name;
