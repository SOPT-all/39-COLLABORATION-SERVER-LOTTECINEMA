-- 로컬 좌석 시딩 검증용 데이터다. 현재 선택한 DB에만 적용된다.
-- 선행 조건: region, cinema, screen, movie, showtime, seat 테이블이 이미 생성되어 있어야 한다.
-- 지역 2개, 영화관 2개, 상영관 3개, 영화 1개, 상영 회차 6개를 등록한다.
-- 같은 테스트 지역·영화관·상영관·영화와 상영관별 시작 시각이 있으면 재사용한다.
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

-- 5. 상영 회차가 참조할 테스트 영화를 등록한다.
INSERT INTO movie (title, genre, runtime_minutes, age_rating, release_date)
SELECT '[좌석시딩검증] 테스트 영화', '드라마', 110, 'ALL', '2026-01-01'
WHERE NOT EXISTS (SELECT 1 FROM movie WHERE title = '[좌석시딩검증] 테스트 영화');

-- 6. 테스트 상영관 3곳에 각각 2D 회차 2개를 등록한다.
-- DATETIME은 KST 현지 시각으로 해석한다. 값은 설명을 위한 고정 테스트 일정이다.
-- 같은 상영관과 시작 시각의 회차가 있으면 유일 제약을 지키도록 재사용한다.
INSERT INTO showtime (movie_id, screen_id, start_at, end_at, format_type, language_type)
SELECT m.id, s.id, slots.start_at, slots.end_at, '2D', 'NONE'
FROM screen s
JOIN cinema c ON c.id = s.cinema_id
JOIN region r ON r.id = c.region_id
JOIN movie m ON m.id = (SELECT MIN(id) FROM movie WHERE title = '[좌석시딩검증] 테스트 영화')
CROSS JOIN (
    SELECT CAST('2030-11-12 14:25:00' AS DATETIME) AS start_at,
           CAST('2030-11-12 16:15:00' AS DATETIME) AS end_at
    UNION ALL
    SELECT CAST('2030-11-12 17:00:00' AS DATETIME),
           CAST('2030-11-12 18:50:00' AS DATETIME)
) slots
WHERE (
        (r.name = '[좌석시딩검증] 서울' AND c.name = '[좌석시딩검증] 강남'
            AND s.name IN ('[좌석시딩검증] 1관', '[좌석시딩검증] 2관'))
        OR (r.name = '[좌석시딩검증] 경기' AND c.name = '[좌석시딩검증] 수원'
            AND s.name = '[좌석시딩검증] 1관')
    )
  AND NOT EXISTS (
      SELECT 1 FROM showtime existing
      WHERE existing.screen_id = s.id AND existing.start_at = slots.start_at
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

-- 좌석 조회 API에 넣을 showtime_id를 확인한다. 테스트 영화의 회차가 6개여야 한다.
SELECT st.id AS showtime_id, r.name AS region_name, c.name AS cinema_name,
       s.id AS screen_id, s.name AS screen_name, m.title AS movie_title,
       st.start_at, st.end_at, st.format_type, st.language_type
FROM showtime st
JOIN screen s ON s.id = st.screen_id
JOIN cinema c ON c.id = s.cinema_id
JOIN region r ON r.id = c.region_id
JOIN movie m ON m.id = st.movie_id
WHERE m.title = '[좌석시딩검증] 테스트 영화'
  AND ((r.name = '[좌석시딩검증] 서울' AND c.name = '[좌석시딩검증] 강남'
        AND s.name IN ('[좌석시딩검증] 1관', '[좌석시딩검증] 2관'))
       OR (r.name = '[좌석시딩검증] 경기' AND c.name = '[좌석시딩검증] 수원'
           AND s.name = '[좌석시딩검증] 1관'))
  AND st.start_at IN ('2030-11-12 14:25:00', '2030-11-12 17:00:00')
ORDER BY r.name, c.name, s.name, st.start_at;
