(ns app.ws
  (:require [datascript.transit :as dt]))

(defonce ws-chan (atom nil))

(defn receive-transit-msg!
  [update-fn]
  (fn [msg]
    (update-fn
      (->> msg
           .-data
           dt/read-transit-str))))

(defn send-transit-msg!
  [msg]
  (if @ws-chan
    (.send @ws-chan
           (dt/write-transit-str msg))
    (throw (js/Error. "Websocket is not available!"))))

(defn make-websocket! [url receive-handler]
  (println "attempting to connect websocket")
  (if-let [chan (js/WebSocket. url)]
    (do
      (set! (.-onmessage chan) (receive-transit-msg! receive-handler))
      (reset! ws-chan chan)
      (println "Websocket connection established with: " url))
    (throw (js/Error. "Websocket connection failed!"))))
