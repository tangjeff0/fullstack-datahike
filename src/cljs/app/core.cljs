(ns app.core
  "This namespace contains your application and is the entrypoint for 'yarn start'."
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [app.ws :as ws]
            [posh.reagent :as p]
            [datascript.core :as d]
            [datascript.transit :as dt]))
            ;;[clojure.edn :as edn]
            ;;[clojure.core.async :as async :refer [go <!]]))


(def conn (d/create-conn))
(p/posh! conn)

;; Websockets
(defonce messages (r/atom []))

(defn message-list []
  (let [messages (p/q '[:find ?e ?a ?v
                        :where [?e ?a ?v]]
                      conn)]
    [:ul
     (for [x @messages]
       ^{:key (str x)}
       [:li (str x)])]))

(defn post-message
  [value]
  (let [tx-report (d/with @conn [{:db/id -1 :block/message value}])
        tx-data   (:tx-data tx-report)]
    (ws/send-transit-msg! {:type    :tx
                           :message tx-data})))

(defn message-input []
  (let [value (r/atom nil)]
    (fn []
      [:input
       {:type        :text
        :placeholder "Type in a message and press enter"
        :value       @value
        :on-change   #(reset! value (-> % .-target .-value))
        :on-key-down
                     #(when (= (.-keyCode %) 13)
                        (post-message @value)
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


(defn update-messages! [message]
  (case (:type message)
    :tx (p/transact! conn (:message message))
    ;; FIX: reads transit str twice
    :connect (let [db (dt/read-transit-str (:message message))]
               (d/reset-conn! conn db))))


(defn mount-components []
  (rdom/render home-page (.getElementById js/document "app")))

(defn ^:export main []
  (ws/make-websocket! (str "ws://" "localhost:3001" #_(.-host js/location) "/ws") update-messages!)
  (mount-components))
