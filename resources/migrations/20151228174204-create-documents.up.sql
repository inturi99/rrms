-- Name: Documents; Type: TABLE; Schema: public; Owner: postgres; Tablespace:
CREATE TABLE IF NOT EXISTS  documents
(
 id serial NOT NULL,
  documentname text,
  title text,
  employeename text,
  date date,
  location text,
  barcode text,
  isactive text,
  createdatetime TIMESTAMP WITH TIME ZONE,
  updateddatetime TIMESTAMP WITH TIME ZONE,
  CONSTRAINT "documents_pkey" PRIMARY KEY (id)
);
