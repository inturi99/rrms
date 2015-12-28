(ns rrms.db.core
  (:require
   [yesql.core :refer [defqueries]]))

(def conn
  {:classname "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname "//localhost:5432/rrms"
   :user "postgres"
   :password "Design_20"})
