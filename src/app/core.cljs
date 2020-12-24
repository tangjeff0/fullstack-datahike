(ns app.core
  "This namespace contains your application and is the entrypoint for 'yarn start'."
  (:require [reagent.dom :as rdom]
            [app.hello :refer [hello]]
            [datahike.api :as d]))

(defn ^:dev/after-load render
  "Render the toplevel component for this app."
  []
  (rdom/render [hello] (.getElementById js/document "app")))

(defn ^:export main
  "Run application startup logic."
  []
  (render))
