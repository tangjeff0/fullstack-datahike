(ns app.core
  "This namespace contains your application and is the entrypoint for 'yarn start'."
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [app.ws :as ws]
            [datahike.api :as d]
            [datahike.impl.entity :as de]
            ;;[clojure.edn :as edn]
            [clojure.core.async :as async :refer [go <!]]))


;; Datahike

(def schema [{:db/ident       :name
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one}
             {:db/ident       :age
              :db/valueType   :db.type/long
              :db/cardinality :db.cardinality/one}])

(def cfg {:store              {:backend :indexeddb :id "idb-sandbox"}
          :keep-history?      false
          :schema-flexibility :write
          :initial-tx         schema})

(comment
  (d/create-database cfg)

  (go (def conn-idb (<! (d/connect cfg))))

  (d/transact conn-idb [{:name "Alice"
                         :age  26}
                        {:name    "Bob"
                         :age     35
                         :_friend [{:name "Mike"
                                    :age  28}]}
                        {:name    "Charlie"
                         :age     45
                         :sibling [[:name "Alice"] [:name "Bob"]]}])

  (go (println (<! (d/q '[:find ?e ?a ?v
                          :in $ ?a
                          :where [?e ?a ?v ?t]]
                        @conn-idb
                        35)))))

;; Websockets

(defonce messages (r/atom []))

(defn message-list []
  [:ul
   (for [[i message] (map-indexed vector @messages)]
     ^{:key i}
     [:li message])])

(defn message-input []
  (let [value (r/atom nil)]
    (fn []
      [:textarea
       {:type        :text
        :placeholder "Type in a message and press enter"
        :style       {:width  "400px"
                      :height "300px"}
        :value       @value
        :on-change   #(reset! value (-> % .-target .-value))
        :on-key-down
                     #(when (= (.-keyCode %) 13)
                        (ws/send-transit-msg!
                          {:message @value})
                        (reset! value nil))}])))

(defn home-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     [:h2 "Welcome to chat"]]]
   [:div.row
    [:div.col-sm-6
     [message-list]]]
   [:div.row
    [:div.col-sm-6
     [message-input]]]])


(defn update-messages! [{:keys [message]}]
  (swap! messages #(vec (take 10 (conj % message)))))


(defn mount-components []
  (rdom/render home-page (.getElementById js/document "app")))

(defn ^:export main []
  (ws/make-websocket! (str "ws://" "localhost:3001" #_(.-host js/location) "/ws") update-messages!)
  (mount-components))
