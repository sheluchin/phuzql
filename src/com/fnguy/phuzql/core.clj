(ns com.fnguy.phuzql.core
  (:require
   [com.fnguy.phuzql.swapi :as swapi]
   [clojure.pprint :as pprint :refer [pprint]]
   [fzf.core :refer [fzf]]))

(defn nest-path
  "Transforms a raw vector of keywords into a nested structure.
  For example, a vector [a b c] becomes [{a [{b [c]}]}]."
  [p]
  (cond
    (empty? p) nil
    (= 1 (count p)) (first p)
    :else [(let [head (first p)
                 tail (nest-path (rest p))]
             {head (if (vector? tail) tail [tail])})]))

(defn transform-partial
  "Wraps the nest-path result in a vector if needed."
  [p]
  (let [np (nest-path p)]
    (if (or (keyword? np) (nil? np))
      [np]
      np)))

(defn build-nested-from-path
  "Builds a nested query from a saved path and candidate leaves.
  Given a path (a vector of keywords) and a collection of candidate leaves,
  it builds a nested query by folding the path from left to right,
  inserting the candidate leaves at the deepest level."
  [path leaves]
  (if (empty? path)
    leaves
    (let [k (first path)]
      [{k (build-nested-from-path (rest path) leaves)}])))

(defn- preview-fn
  [partial selections]
  (println "Selections: " selections)
  (let [candidates (mapv read-string selections)
        current-query (build-nested-from-path partial candidates)]
    (binding [clojure.pprint/*print-right-margin* 80]
      (try
        (let [result (swapi/query swapi/env current-query)
              result-str (with-out-str (pprint result))]
          (str "Current Query:\n" (with-out-str (pprint current-query))
               "\nQuery Result:\n"
               result-str))
        (catch Exception e
          (str "Current Query:\n" (with-out-str (pprint current-query))
               "\nQuery Result: Query not complete or error.\n"
               (.getMessage e)))))))

(defn build-branch
  "Recursively builds a query branch interactively.
  Uses fzf with an external preview command. Before invoking fzf,
  the current (raw) partial branch (a vector of keywords) is saved to a temporary file.
  The external preview command will read that saved partial as well as the
  currently selected candidate(s) and build the final nested query using build-nested-from-path.
  Returns a nested structure that is later used by execute-query."
  [attrs partial]
  (let [reachable (swapi/reachable-attributes attrs)
        choices   (vec (keys reachable))]
    (if (empty? choices)
      nil
      (do
        (println "\n--- Reachable choices ---")
        (doseq [c choices]
          (println (pr-str c)))
        (println "Multi-select one or more fields (or hit Enter to finish this branch):")
        (let [header-text (str "Current Query: " (with-out-str (pprint/pprint (transform-partial partial))))
              opts {:multi true
                    :header {:header-str header-text}
                    :preview-fn #(preview-fn partial %1)}
              candidate-list (map pr-str choices)
              result (fzf opts candidate-list)
              selected-lines (if (string? result) [result] result)]
          (if (or (nil? selected-lines) (empty? selected-lines))
            nil
            (vec
             (for [line selected-lines]
               (let [chosen  (read-string line)
                     children (get reachable chosen)
                     new-attrs (into #{chosen} (keys children))
                     sub     (build-branch new-attrs (conj partial chosen))]
                 (if sub
                   {chosen sub}
                   chosen))))))))))

(defn build-query
  "Builds a query by initiating the query branch building process."
  []
  (let [initial-attrs #{:swapi.types/Root}
        branch        (build-branch initial-attrs [])]
    (if branch
      branch
      (do
        (println "No query was built.")
        nil))))

(defn execute-query
  "Executes the built query if available, otherwise indicates no query was built."
  []
  (if-let [query (build-query)]
    (do
      (println "\nFinal query:" (with-out-str (pprint/pprint query)))
      (println "Executing query:" query)
      (swapi/query swapi/env query))
    (println "No query to execute.")))

;; Example usage:
;; In an interactive terminal session, run: (execute-query)

(comment
  (execute-query))
