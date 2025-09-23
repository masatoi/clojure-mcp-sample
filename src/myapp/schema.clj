(ns myapp.schema
  (:require [malli.core :as m]))

(def User
  "A map representing a user."
  [:map
   [:user/id :uuid]
   [:user/name :string]
   [:user/age :int]
   [:user/address {:optional true} :string]
   [:user/created-at inst?]
   [:user/updated-at inst?]])

(def Paging
  "A map representing pagination info."
  [:map
   [:next {:optional true} [:or :string :nil]]
   [:prev {:optional true} [:or :string :nil]]])

(def UsersResponse
  "The response body for the user list endpoint."
  [:map
   [:data [:vector User]]
   [:paging Paging]])

(def CreateUserRequest
  "The request body for creating a new user."
  [:map
   [:user/name :string]
   [:user/age :int]
   [:user/address {:optional true} :string]])

(def UpdateUserRequest
  "The request body for updating a user."
  [:map
   [:user/name {:optional true} :string]
   [:user/age {:optional true} :int]
   [:user/address {:optional true} :string]])
