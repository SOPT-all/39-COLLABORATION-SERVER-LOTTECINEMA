-- 로컬 영화 목록 조회 API 테스트용 임시 데이터
-- 같은 제목의 영화가 이미 있으면 해당 데이터는 추가하지 않는다.

INSERT INTO movie (
    title,
    poster_url,
    genre,
    runtime_minutes,
    age_rating,
    release_date,
    curation_type,
    curation_order,
    popularity_rank,
    booking_rate,
    movie_rating
)
SELECT
    seed.title,
    seed.poster_url,
    seed.genre,
    seed.runtime_minutes,
    seed.age_rating,
    seed.release_date,
    seed.curation_type,
    seed.curation_order,
    seed.popularity_rank,
    seed.booking_rate,
    seed.movie_rating
FROM (
         SELECT
             '[테스트] 영화 A' AS title,
             'https://example.com/posters/movie-a.jpg' AS poster_url,
             '애니메이션' AS genre,
             100 AS runtime_minutes,
             '전체' AS age_rating,
             '2026-11-01' AS release_date,
             'LOTSI_PICK' AS curation_type,
             1 AS curation_order,
             2 AS popularity_rank,
             25.00 AS booking_rate,
             8.5 AS movie_rating

         UNION ALL

         SELECT
             '[테스트] 영화 B',
             'https://example.com/posters/movie-b.jpg',
             '드라마',
             120,
             '12세',
             '2099-01-01',
             'LOTSI_PICK',
             2,
             1,
             30.00,
             9.0

         UNION ALL

         SELECT
             '[테스트] 영화 C',
             'https://example.com/posters/movie-c.jpg',
             '공연',
             90,
             '전체',
             '2026-10-01',
             'LOTSI_PLAY',
             1,
             NULL,
             NULL,
             NULL

         UNION ALL

         SELECT
             '[테스트] 영화 D',
             'https://example.com/posters/movie-d.jpg',
             '액션',
             110,
             '15세',
             '2026-09-01',
             NULL,
             NULL,
             3,
             15.00,
             7.5
     ) AS seed
WHERE NOT EXISTS (
    SELECT 1
    FROM movie existing_movie
    WHERE existing_movie.title = seed.title
);