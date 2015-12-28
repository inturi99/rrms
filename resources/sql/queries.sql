-- name: get-documents-by-title
-- search by title
SELECT * from documents
WHERE title = :title
