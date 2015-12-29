-- name: get-documents-by-title
-- search by title
SELECT * from documents
WHERE title = :title

--name: get-documents-by-id
SELECT documentname,title,employeename,location,barcode,isactive from documents WHERE id = :id

-- name: insert-documents
INSERT INTO documents(documentname,title,employeename,date,location,barcode,isactive,createdatetime,updateddatetime) VALUES(:documentname,:title,:employeename,CURRENT_DATE,:location,:barcode,:isactive,now(),now())

--name: update-documents
UPDATE documents set documentname = :documentname,title = :title,employeename = :employeename,date = CURRENT_DATE,location = :location,isactive = :isactive,updateddatetime = now()  where id = :id

-- name: delete-documents-by-id
UPDATE documents set isactive = 'FALSE' where id = :id
