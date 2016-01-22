-- name: create-user
-- creates a new user record
INSERT INTO users
(firstname, lastname, email,lastlogin,isactive, password,createdatetime,updateddatetime,role)
VALUES (:firstname, :lastname, :email, now(), 'TRUE', :password,now(),now(),:role) RETURNING id

-- name: get-user-by-email

SELECT firstname, lastname, email,lastlogin,role,password FROM users
WHERE email = :email AND isactive = 'TRUE'

-- name: get-users
SELECT id, firstname, lastname, email FROM users

-- name: delete-user!
-- delete a user given the id
UPDATE users set isactive = 'FALSE' where id = :id
RETURNING id


-- name: get-documents-by-title
-- search by title
SELECT * from documents
WHERE lower(documentname) LIKE ('%' || lower(:srcstr) || '%') OR lower(title) LIKE ('%' || lower(:srcstr) || '%')

-- name: search-documents-by-date
select * from documents where date = :date1 AND isactive = 'TRUE' ORDER BY createdatetime DESC

-- name: search-documents-between-two-dates
select * from documents where (date BETWEEN :date1  AND :date2) AND isactive = 'TRUE' ORDER BY createdatetime DESC

-- name: search-documents
select * from documents where (date BETWEEN :date1  AND :date2)  AND (lower(documentname) LIKE ('%' || lower(:srcstr) || '%') OR lower(title) LIKE ('%' || lower(:srcstr) || '%'))  AND isactive = 'TRUE' ORDER BY createdatetime DESC


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

 --name: search-documents1
SELECT * FROM documents WHERE
    CASE
    WHEN :date1 <> '0000-00-00' AND :date2  <> '0000-00-00' AND :srcstr = '0' THEN  (date BETWEEN :date1  AND :date2) AND isactive = 'TRUE'
    ELSE (date BETWEEN :date1  AND :date2) AND (lower(documentname) LIKE ('%' || lower(:srcstr) || '%') OR lower(title) LIKE ('%' || lower(:srcstr) || '%'))
    AND isactive = 'TRUE'
    END
