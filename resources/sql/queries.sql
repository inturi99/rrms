-- name: get-documents-by-title
-- search by title
SELECT * from documents
WHERE title = :title

-- name: insert-documents
INSERT INTO documents(documentname,title,employeename,date,location,barcode,isactive,createdatetime,updateddatetime) VALUES(:documentname,:title,:employeename,now(),:location,:barcode,:isactive,now(),now())
