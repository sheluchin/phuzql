(ns com.fnguy.phuzql.swapi
  (:require
   [cheshire.core :as cjson]
   [clojure.data.json :as json]
   [com.wsscode.pathom3.graphql :as p.gql]
   [com.wsscode.pathom3.interface.eql :as p.eql]
   [com.wsscode.pathom3.connect.indexes :as pci]
   [org.httpkit.client :as http]))

(defn request-swapi-graphql
  "Sends a GraphQL query to the Star Wars API and returns the parsed response."
  [query]
  (-> @(http/request
        {:url     "https://swapi-graphql.netlify.app/graphql"
         :method  :post
         :headers {"Content-Type" "application/json"
                   "Accept"       "*/*"}
         :body    (cjson/generate-string {:query query})})
      :body
      (cjson/parse-string)))

(def env
  "Creates the environment by connecting to the Star Wars GraphQL API."
  (-> {}
      (p.gql/connect-graphql
       {::p.gql/namespace "swapi"}
       request-swapi-graphql)))

(def index-io (::pci/index-io env))

(defn reachable-attributes
  "Returns a map of reachable attributes from the given input attributes."
  [input-attrs]
  (reduce (fn [acc attr]
            (let [reachable (get index-io #{attr})]
              (if reachable
                (merge acc reachable)
                acc)))
          {}
          input-attrs))

(defn query
  "Processes a query using the provided environment."
  [env q]
  (p.eql/process env q))

(comment
  ;; Request all people and the title of the films they participate
  (p.eql/process
   env
   [{:swapi.Root/allPeople
     [{:swapi.PeopleConnection/people
       [:swapi.Person/name
        {:swapi.Person/filmConnection
         [{:swapi.PersonFilmsConnection/films
           [:swapi.Film/title]}]}]}]}])

  (get (::pci/index-io env) #{:swapi.types/Vehicle
                              :swapi.types/VehicleId})

  (do
    ;; Example usage
    (def input-attrs #{:swapi.types/Vehicle :swapi.Vehicle/id})
    (def reachable (reachable-attributes input-attrs))

    (keys reachable)))
