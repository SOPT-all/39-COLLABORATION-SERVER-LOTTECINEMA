-- 영화 시딩 후 실행: 결과가 있으면 해당 큐레이션 영화의 표시 순서를 입력해야 한다.
-- curation_order가 NULL인 영화는 큐레이션 목록 조회 결과에서 제외된다.
SELECT id, title, curation_type, curation_order
FROM movie
WHERE curation_type IS NOT NULL
  AND curation_order IS NULL
ORDER BY curation_type, id;
