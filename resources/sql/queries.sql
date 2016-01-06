-- name: get-documents-by-title
-- search by title
SELECT * from documents
WHERE lower(title) LIKE ('%' || lower(:title) || '%')

--name: get-all-documents
SELECT id,documentname,title,employeename,date,location,isactive from documents WHERE isactive = 'TRUE' ORDER BY createdatetime DESC

--name: get-documents-by-id
SELECT id, documentname,title,employeename,date,location from documents WHERE id = :id

-- name: insert-documents
INSERT INTO documents(documentname,title,employeename,date,location,barcode,isactive,createdatetime,updateddatetime) VALUES(:documentname,:title,:employeename,:date,:location,:barcode,'TRUE',now(),now())
RETURNING id,documentname,title,employeename,date,location,barcode,isactive

--name: update-documents
UPDATE documents set documentname = :documentname,title = :title,employeename = :employeename,date = :date,location = :location,updateddatetime = now()  where id = :id
RETURNING id,documentname,title,employeename,date,location,barcode,isactive

-- name: delete-documents-by-id
UPDATE documents set isactive = 'FALSE' where id = :id
RETURNING id
