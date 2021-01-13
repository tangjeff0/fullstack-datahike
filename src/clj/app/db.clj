(ns app.db
  (:require #_[datahike.api :as d]
            [datascript.core :as d]))

;; Datascript

(def conn (d/create-conn))
(d/transact conn [{:db/id -1 :block/message "first"}])




;; Datahike stuff below


;;(def cfg {:store {:backend :file :path "/home/jeff/Dropbox/datahike"}})


;;(d/create-database cfg)


;;(def conn (d/connect cfg))


#_(prn (d/q '[:find ?e ?a ?v
              :where [?e ?a ?v]]
            @conn))


;;;; Schema

;;(d/datoms @conn :eavt)



#_(d/transact conn [{:db/ident       :name
                     :db/valueType   :db.type/string
                     :db/cardinality :db.cardinality/one}
                    {:db/ident       :age
                     :db/valueType   :db.type/long
                     :db/cardinality :db.cardinality/one}])


;;;; you might need to release the connection for specific stores like leveldb
;;(d/release conn)


;;;; clean up the database if it is not need any more
;;(d/delete-database cfg)
