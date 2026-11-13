-- 롯데시네마 모바일 웹 2조 시연용 원본 더미데이터 (MySQL 8 / UTF-8)
-- 용도: 현실의 상영 일정이 아니라 고정된 시연 흐름을 재현한다.

SET NAMES utf8mb4;
START TRANSACTION;

-- 1. 지역: '1_지역' A6:B13.
INSERT INTO region (id, name, display_order) VALUES
    (1, '서울', 1),
    (2, '경기/인천', 2),
    (3, '충청/대전', 3),
    (4, '전라/광주', 4),
    (5, '경북/대구', 5),
    (6, '경남/부산/울산', 6),
    (7, '강원', 7),
    (8, '제주', 8);

-- 2. 영화관: '2_영화관' A6:C26. 용산 cinemaId=16, 홍대입구 cinemaId=21.
INSERT INTO cinema (id, region_id, name, display_order) VALUES
    (1, 1, '가산디지털', 1),
    (2, 1, '가양', 2),
    (3, 1, '강동', 3),
    (4, 1, '건대입구', 4),
    (5, 1, '김포공항', 5),
    (6, 1, '노원', 6),
    (7, 1, '도곡', 7),
    (8, 1, '독산', 8),
    (9, 1, '서울대입구', 9),
    (10, 1, '수락산', 10),
    (11, 1, '수유', 11),
    (12, 1, '신대방(구로디지털역)', 12),
    (13, 1, '신도림', 13),
    (14, 1, '신림', 14),
    (15, 1, '영등포', 15),
    (16, 1, '용산', 16),
    (17, 1, '월드타워', 17),
    (18, 1, '은평(롯데몰)', 18),
    (19, 1, '청량리', 19),
    (20, 1, '합정', 20),
    (21, 1, '홍대입구', 21);

-- 3. 상영관: '3_상영관' A6:C7. screenId=1은 용산 1관, screenId=2는 용산 4관.
INSERT INTO screen (id, cinema_id, name, seat_type) VALUES
    (1, 16, '1관', 'RECLINER'),
    (2, 16, '4관', 'RECLINER');

-- 4. 영화: '4_영화(인기순위)' A7:K17. 대표 영화 movieId=1.
INSERT INTO movie (id, title, poster_url, genre, runtime_minutes, age_rating, release_date, curation_type, popularity_rank, booking_rate, movie_rating) VALUES
    (1, '극장판 치이카와: 인어 섬의 비밀', 'https://cf.lottecinema.co.kr//Media/MovieFile/MovieImg/202609/24708_201_1.jpg', '애니메이션', 99, '전체', '2026-09-30', NULL, 1, 20, 9.3),
    (2, '하트 오브 비스트', 'https://cf.lottecinema.co.kr//Media/MovieFile/MovieImg/202610/24867_201_1.jpg', '드라마, 액션', 101, '12세', '2026-10-28', NULL, 2, 30, NULL),
    (3, '최종면접', 'https://cf.lottecinema.co.kr//Media/MovieFile/MovieImg/202611/24998_201_1.jpg', '미스터리', 107, '15세', '2026-11-04', NULL, 3, 25, NULL),
    (4, '극장판 마법소녀 마도카 마기카 <발푸르기스의 회천>', 'https://i.namu.wiki/i/7d4ylf5q7ylWeKbAKTdReQVVXWag9trHw-nm8DT5vKzmu0Nc9AGSVCaTl8ZDn46-pEamc1Clmi4c6T5xmTXV3uNhgImg-mm1XkwkzPZCA6B_kUzf3BTbITmfWXwf3HvT5VxhWch6DHeaxNSG6HivuQ.webp', '애니메이션', 119, '15세', '2026-11-04', 'LOTSI_PICK', 4, 17, NULL),
    (5, '수능, 출제의 비밀', 'https://cf.lottecinema.co.kr//Media/MovieFile/MovieImg/202610/24869_201_1.jpg', '코미디', 99, '12세', '2026-10-28', NULL, 5, 11, NULL),
    (6, '그대, 별처럼', 'https://cf.lottecinema.co.kr//Media/MovieFile/MovieImg/202610/25018_201_1.jpg', '멜로/로맨스', 134, '15세', '2026-10-28', 'LOTSI_PICK', 6, 7, NULL),
    (7, '숀 더 쉽: 공포의 털보 괴물', 'https://cf.lottecinema.co.kr//Media/MovieFile/MovieImg/202610/25075_201_1.jpg', '애니메이션', 81, '전체', '2026-10-28', 'LOTSI_PICK', NULL, NULL, NULL),
    (8, '변이', 'https://cf.lottecinema.co.kr//Media/MovieFile/MovieImg/202611/25023_201_1.jpg', '스릴러, 미스터리, 공포(호러)', 103, '청불', '2026-11-04', 'LOTSI_PICK', NULL, NULL, NULL),
    (9, '엔하이픈 브이알콘서트 : 데스티니', 'https://cf.lottecinema.co.kr//Media/MovieFile/MovieImg/202609/24971_201_1.jpg', '공연', 52, '전체', '2026-09-18', 'LOTSI_PLAY', NULL, NULL, NULL),
    (10, 'Stray Kids World Tour<RUN IT>LIVE VIEWING', 'https://cf.lottecinema.co.kr//Media/MovieFile/MovieImg/202611/25106_201_1.jpg', '공연', 195, '전체', '2026-11-07', 'LOTSI_PLAY', NULL, NULL, NULL),
    (11, 'BTS WORLD TOUR ''ARIRANG'' IN SAO PAULO: LIVE VIEWING', 'http://cf.lottecinema.co.kr//Media/MovieFile/MovieImg/202610/24987_201_1.jpg', '공연', 195, '전체', '2026-10-31', 'LOTSI_PLAY', NULL, NULL, NULL);

-- 원본 curationOrder는 최신 엔티티에 추가된 별도 컬럼에 저장한다.
UPDATE movie SET curation_order = 1 WHERE id = 4;
UPDATE movie SET curation_order = 2 WHERE id = 6;
UPDATE movie SET curation_order = 3 WHERE id = 7;
UPDATE movie SET curation_order = 4 WHERE id = 8;
UPDATE movie SET curation_order = 1 WHERE id = 9;
UPDATE movie SET curation_order = 2 WHERE id = 10;
UPDATE movie SET curation_order = 3 WHERE id = 11;

-- 큐레이션의 표시 순서:
-- LOTSI_PICK 1번: movieId=4, 극장판 마법소녀 마도카 마기카 <발푸르기스의 회천>
-- LOTSI_PICK 2번: movieId=6, 그대, 별처럼
-- LOTSI_PICK 3번: movieId=7, 숀 더 쉽: 공포의 털보 괴물
-- LOTSI_PICK 4번: movieId=8, 변이
-- LOTSI_PLAY 1번: movieId=9, 엔하이픈 브이알콘서트 : 데스티니
-- LOTSI_PLAY 2번: movieId=10, Stray Kids World Tour<RUN IT>LIVE VIEWING
-- LOTSI_PLAY 3번: movieId=11, BTS WORLD TOUR 'ARIRANG' IN SAO PAULO: LIVE VIEWING

-- 5. 상영 회차: '5_상영회차' A6:G14. C10만 4관으로 수정. KST 현지 시각 DATETIME.
INSERT INTO showtime (id, movie_id, screen_id, start_at, end_at, format_type, language_type) VALUES
    (1, 1, 1, '2026-10-27 13:25:00', '2026-10-27 15:14:00', '2D', 'DUBBED'),
    (2, 1, 1, '2026-10-27 15:35:00', '2026-10-27 17:24:00', '2D', 'DUBBED'),
    (3, 1, 1, '2026-10-27 17:45:00', '2026-10-27 19:34:00', '2D', 'DUBBED'),
    (4, 1, 1, '2026-10-27 19:55:00', '2026-10-27 21:44:00', '2D', 'DUBBED'),
    (5, 1, 2, '2026-10-27 13:55:00', '2026-10-27 15:44:00', '2D', 'SUBTITLED'),
    (6, 1, 2, '2026-10-27 14:25:00', '2026-10-27 16:14:00', '2D', 'SUBTITLED'),
    (7, 1, 2, '2026-10-27 16:35:00', '2026-10-27 18:24:00', '2D', 'SUBTITLED'),
    (8, 1, 2, '2026-10-27 18:45:00', '2026-10-27 20:34:00', '2D', 'SUBTITLED'),
    (9, 1, 2, '2026-10-27 21:05:00', '2026-10-27 22:54:00', '2D', 'SUBTITLED');

-- 6. 좌석: '좌석 배치도' B20:P31, 각 상영관에 같은 77석을 넣는다.
-- 빈 칸은 통로/공간이다. 화면은 12행×15열을 유지해야 하며 아래 빈 행을 잘라내지 않는다.
INSERT INTO seat (screen_id, seat_code, row_index, column_index) VALUES
    (1, 'A10', 0, 10),
    (1, 'A11', 0, 11),
    (1, 'B1', 1, 0),
    (1, 'B2', 1, 1),
    (1, 'B3', 1, 3),
    (1, 'B4', 1, 4),
    (1, 'B5', 1, 5),
    (1, 'B6', 1, 6),
    (1, 'B7', 1, 7),
    (1, 'B8', 1, 8),
    (1, 'B9', 1, 9),
    (1, 'B10', 1, 10),
    (1, 'B11', 1, 11),
    (1, 'C1', 2, 0),
    (1, 'C2', 2, 1),
    (1, 'C3', 2, 3),
    (1, 'C4', 2, 4),
    (1, 'C5', 2, 5),
    (1, 'C6', 2, 6),
    (1, 'C7', 2, 7),
    (1, 'C8', 2, 8),
    (1, 'C9', 2, 9),
    (1, 'C10', 2, 10),
    (1, 'C11', 2, 11),
    (1, 'C12', 2, 13),
    (1, 'C13', 2, 14),
    (1, 'D1', 3, 0),
    (1, 'D2', 3, 1),
    (1, 'D3', 3, 3),
    (1, 'D4', 3, 4),
    (1, 'D5', 3, 5),
    (1, 'D6', 3, 6),
    (1, 'D7', 3, 7),
    (1, 'D8', 3, 8),
    (1, 'D9', 3, 9),
    (1, 'D10', 3, 10),
    (1, 'D11', 3, 11),
    (1, 'D12', 3, 13),
    (1, 'D13', 3, 14),
    (1, 'E1', 4, 0),
    (1, 'E2', 4, 1),
    (1, 'E3', 4, 3),
    (1, 'E4', 4, 4),
    (1, 'E5', 4, 5),
    (1, 'E6', 4, 6),
    (1, 'E7', 4, 7),
    (1, 'E8', 4, 8),
    (1, 'E9', 4, 9),
    (1, 'E10', 4, 10),
    (1, 'E11', 4, 11),
    (1, 'E12', 4, 13),
    (1, 'F1', 5, 0),
    (1, 'F2', 5, 1),
    (1, 'F3', 5, 3),
    (1, 'F4', 5, 4),
    (1, 'F5', 5, 5),
    (1, 'F6', 5, 6),
    (1, 'F7', 5, 7),
    (1, 'F8', 5, 8),
    (1, 'F9', 5, 9),
    (1, 'F10', 5, 10),
    (1, 'F11', 5, 11),
    (1, 'F12', 5, 13),
    (1, 'F13', 5, 14),
    (1, 'G1', 6, 0),
    (1, 'G2', 6, 1),
    (1, 'G3', 6, 3),
    (1, 'G4', 6, 4),
    (1, 'G5', 6, 5),
    (1, 'G6', 6, 6),
    (1, 'G7', 6, 7),
    (1, 'G8', 6, 8),
    (1, 'G9', 6, 9),
    (1, 'G10', 6, 10),
    (1, 'G11', 6, 11),
    (1, 'G12', 6, 13),
    (1, 'G13', 6, 14),
    (2, 'A10', 0, 10),
    (2, 'A11', 0, 11),
    (2, 'B1', 1, 0),
    (2, 'B2', 1, 1),
    (2, 'B3', 1, 3),
    (2, 'B4', 1, 4),
    (2, 'B5', 1, 5),
    (2, 'B6', 1, 6),
    (2, 'B7', 1, 7),
    (2, 'B8', 1, 8),
    (2, 'B9', 1, 9),
    (2, 'B10', 1, 10),
    (2, 'B11', 1, 11),
    (2, 'C1', 2, 0),
    (2, 'C2', 2, 1),
    (2, 'C3', 2, 3),
    (2, 'C4', 2, 4),
    (2, 'C5', 2, 5),
    (2, 'C6', 2, 6),
    (2, 'C7', 2, 7),
    (2, 'C8', 2, 8),
    (2, 'C9', 2, 9),
    (2, 'C10', 2, 10),
    (2, 'C11', 2, 11),
    (2, 'C12', 2, 13),
    (2, 'C13', 2, 14),
    (2, 'D1', 3, 0),
    (2, 'D2', 3, 1),
    (2, 'D3', 3, 3),
    (2, 'D4', 3, 4),
    (2, 'D5', 3, 5),
    (2, 'D6', 3, 6),
    (2, 'D7', 3, 7),
    (2, 'D8', 3, 8),
    (2, 'D9', 3, 9),
    (2, 'D10', 3, 10),
    (2, 'D11', 3, 11),
    (2, 'D12', 3, 13),
    (2, 'D13', 3, 14),
    (2, 'E1', 4, 0),
    (2, 'E2', 4, 1),
    (2, 'E3', 4, 3),
    (2, 'E4', 4, 4),
    (2, 'E5', 4, 5),
    (2, 'E6', 4, 6),
    (2, 'E7', 4, 7),
    (2, 'E8', 4, 8),
    (2, 'E9', 4, 9),
    (2, 'E10', 4, 10),
    (2, 'E11', 4, 11),
    (2, 'E12', 4, 13),
    (2, 'F1', 5, 0),
    (2, 'F2', 5, 1),
    (2, 'F3', 5, 3),
    (2, 'F4', 5, 4),
    (2, 'F5', 5, 5),
    (2, 'F6', 5, 6),
    (2, 'F7', 5, 7),
    (2, 'F8', 5, 8),
    (2, 'F9', 5, 9),
    (2, 'F10', 5, 10),
    (2, 'F11', 5, 11),
    (2, 'F12', 5, 13),
    (2, 'F13', 5, 14),
    (2, 'G1', 6, 0),
    (2, 'G2', 6, 1),
    (2, 'G3', 6, 3),
    (2, 'G4', 6, 4),
    (2, 'G5', 6, 5),
    (2, 'G6', 6, 6),
    (2, 'G7', 6, 7),
    (2, 'G8', 6, 8),
    (2, 'G9', 6, 9),
    (2, 'G10', 6, 10),
    (2, 'G11', 6, 11),
    (2, 'G12', 6, 13),
    (2, 'G13', 6, 14);

COMMIT;

-- 7. 적재 검증: region=8, cinema=21, screen=2, movie=11, showtime=9, seat=154.
SELECT 'region' AS table_name, COUNT(*) AS actual_count, 8 AS expected_count FROM region
UNION ALL SELECT 'cinema', COUNT(*), 21 FROM cinema
UNION ALL SELECT 'screen', COUNT(*), 2 FROM screen
UNION ALL SELECT 'movie', COUNT(*), 11 FROM movie
UNION ALL SELECT 'showtime', COUNT(*), 9 FROM showtime
UNION ALL SELECT 'seat', COUNT(*), 154 FROM seat;

-- 프론트 요청 예: GET /api/v1/showtimes?movieId=1&cinemaId=16&date=2026-10-27
-- 4관 14:25~16:14 대표 회차는 showtimeId=6.
-- GET /api/v1/showtimes/6/seats -> 초기 총77/잔여77, E4=(4,4), E5=(4,5).
-- isReserved=false가 선택 가능(T)이다. 프론트에서 반대로 해석하지 않는다.
SELECT st.id AS showtime_id, m.title, c.name AS cinema_name, s.name AS screen_name,
       st.start_at, st.end_at, st.format_type, st.language_type, COUNT(seat.id) AS total_seat_count
FROM showtime st
JOIN movie m ON m.id = st.movie_id
JOIN screen s ON s.id = st.screen_id
JOIN cinema c ON c.id = s.cinema_id
LEFT JOIN seat ON seat.screen_id = s.id
GROUP BY st.id, m.title, c.name, s.name, st.start_at, st.end_at, st.format_type, st.language_type
ORDER BY st.start_at, st.id;

-- 저장된 큐레이션 순서가 원본 순서와 같은지 확인한다.
SELECT m.id AS movie_id, m.title, m.curation_type, m.curation_order,
       source.curation_order AS source_curation_order,
       m.curation_order = source.curation_order AS matches_source
FROM movie m JOIN (
    SELECT 4 AS movie_id, 1 AS curation_order
    UNION ALL SELECT 6 AS movie_id, 2 AS curation_order
    UNION ALL SELECT 7 AS movie_id, 3 AS curation_order
    UNION ALL SELECT 8 AS movie_id, 4 AS curation_order
    UNION ALL SELECT 9 AS movie_id, 1 AS curation_order
    UNION ALL SELECT 10 AS movie_id, 2 AS curation_order
    UNION ALL SELECT 11 AS movie_id, 3 AS curation_order
) source ON source.movie_id = m.id
ORDER BY m.curation_type, source.curation_order;
