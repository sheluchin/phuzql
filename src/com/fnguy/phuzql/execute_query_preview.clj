#!/usr/bin/env bb

(ns com.fnguy.phuzql.execute-query-preview
  (:require [com.fnguy.phuzql.swapi :as swapi]
            [com.fnguy.phuzql.core :refer [build-nested-from-path]]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]))

(defn -main [& args]
  (try
    (when (empty? args)
      (println "No candidate provided.")
      (System/exit 1))
    (let [candidates (mapv read-string args)
          partial-str (slurp "/tmp/phuzql_partial.txt")
          partial (read-string partial-str)
          current-query (build-nested-from-path partial candidates)]
      (binding [clojure.pprint/*print-right-margin* 80]
        (try
          (let [result (swapi/query swapi/env current-query)
                result-str (with-out-str (pprint result))]
            (println "Current Query:\n" (with-out-str (pprint current-query)))
            (println "\nQuery Result:")
            (println result-str))
          (catch Exception e
            (println "Current Query:\n" (with-out-str (pprint current-query)))
            (println "\nQuery Result: Query not complete or error.")
            (println (.getMessage e))))))
    (catch Exception e
      (println "Error in preview:" (.getMessage e)))))

(apply -main *command-line-args*)
