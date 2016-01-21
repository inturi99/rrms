-- Name: users; Type: TABLE; Schema: public; Owner: postgres; Tablespace:
CREATE TABLE IF NOT EXISTS  users
(
 id serial NOT NULL,
  firstname text,
  lastname text,
  email text,
  lastlogin TIME WITH TIME ZONE,
  isactive BOOLEAN,
  password text,
  createdatetime TIMESTAMP WITH TIME ZONE,
  updateddatetime TIMESTAMP WITH TIME ZONE,
  CONSTRAINT "users_pkey" PRIMARY KEY (id)
);
