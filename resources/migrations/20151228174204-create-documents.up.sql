-- Name: Documents; Type: TABLE; Schema: public; Owner: postgres; Tablespace:
CREATE TABLE IF NOT EXISTS  documents
(
 id serial NOT NULL,
  documentname text,
  title text,
  employeename text,
  date TIMESTAMP WITH TIME ZONE,
  location text,
  barcode text,
  exists text,
  createdatetime TIMESTAMP WITH TIME ZONE,
  updateddatetime TIMESTAMP WITH TIME ZONE,
  CONSTRAINT "Documents_pkey" PRIMARY KEY (id)
);
