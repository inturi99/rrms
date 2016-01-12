-- name: get-documents-by-title
-- search by title
SELECT * from documents
WHERE lower(documentname) LIKE ('%' || lower(:documentname) || '%') OR lower(title) LIKE ('%' || lower(:title) || '%')

-- name: get-documents-between-two-dates
select * from documents where date = :date1 OR  (date BETWEEN :date1  AND :date2) OR lower(documentname) LIKE ('%' || lower(:documentname) || '%') OR lower(title) LIKE ('%' || lower(:title) || '%')  AND isactive = 'TRUE' ORDER BY createdatetime DESC

--name: get-all-documents
SELECT id,documentname,title,employeename,date,location,isactive from documents WHERE isactive = 'TRUE' ORDER BY createdatetime DESC

--name: get-total-documents
SELECT COUNT(id) AS totaldocuments from documents where isactive = 'TRUE'

--name: get-all-documents-by-index-pagesize
SELECT MyRowNumber, id,documentname,title,employeename,date,location,isactive  FROM (SELECT  ROW_NUMBER() OVER (ORDER BY id DESC) as MyRowNumber,* FROM Documents WHERE isactive = 'TRUE') tblDocuments WHERE  MyRowNumber BETWEEN ( ((:index - 1) * :pagesize )+ 1) AND :index*:pagesize
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
