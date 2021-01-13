(ns app.core
  (:require
    [app.db :as db]
    [immutant.web :as web]
    [clojure.tools.logging :as log]
    ;;[datahike.api :as d]
    [cognitect.transit :as t]
    [datascript.core :as d]
    [datascript.transit :as dt]
    [immutant.web.async :as async]
    [immutant.web.middleware :as web-middleware]
    [compojure.route :as route]
    [compojure.core :refer (ANY GET defroutes)]
    [nrepl.server :as nrepl]
    [ring.util.response :refer (response redirect content-type)])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream])
  (:gen-class))

(def out (ByteArrayOutputStream. 4096))
(def json-writer (t/writer out :json))

(def transit-str (->> "./index.transit"
                      slurp))

(defonce channels (atom #{}))

(defn connect! [channel]
  (log/info "channel open")
  (swap! channels conj channel)
  (async/send! channel (dt/write-transit-str {:type :connect :message transit-str})))

(defn disconnect! [channel {:keys [code reason]}]
  (log/info "close code:" code "reason:" reason)
  (swap! channels #(remove #{channel} %)))

(defn notify-clients! [channel msg]
  (doseq [channel @channels]
    (prn "NOTIFY" msg)
    (async/send! channel msg)))

(def websocket-callbacks
  "WebSocket callback functions"
  {:on-open connect!
   :on-close disconnect!
   :on-message notify-clients!})

(defn ws-handler [request]
  (async/as-channel request websocket-callbacks))

(def websocket-routes
  [["/ws" ws-handler]])

(defroutes routes
           (GET "/" [] "<title>huh</title>")
           (route/resources "/"))


(defn nrepl-start
  "Start a network repl for debugging on specified port followed by
  an optional parameters map. The :bind, :transport-fn, :handler,
  :ack-port and :greeting-fn will be forwarded to
  clojure.tools.nrepl.server/start-server as they are."
  [{:keys [port bind transport-fn handler ack-port greeting-fn]}]
  (try
    (log/info "starting nREPL server on port" port)
    (nrepl/start-server :port port
                        :bind bind
                        :transport-fn transport-fn
                        :handler handler
                        :ack-port ack-port
                        :greeting-fn greeting-fn)

    (catch Throwable t
      (log/error t "failed to start nREPL")
      (throw t))))



(defn -main [& {:as args}]

  (nrepl-start {:port 7000})

  (web/run
    (-> routes
        (web-middleware/wrap-session {:timeout 20})
        ;; wrap the handler with websocket support
        ;; websocket requests will go to the callbacks, ring requests to the handler
        (web-middleware/wrap-websocket websocket-callbacks))
    (merge {"host" "localhost"
            "port" 3001}
           args)))
