# encoding: UTF-8
# This file is auto-generated from the current state of the database. Instead
# of editing this file, please use the migrations feature of Active Record to
# incrementally modify your database, and then regenerate this schema definition.
#
# Note that this schema.rb definition is the authoritative source for your
# database schema. If you need to create the application database on another
# system, you should be using db:schema:load, not running all the migrations
# from scratch. The latter is a flawed and unsustainable approach (the more migrations
# you'll amass, the slower it'll run and the greater likelihood for issues).
#
# It's strongly recommended to check this file into your version control system.

ActiveRecord::Schema.define(:version => 20121127110232) do

  create_table "cars", :force => true do |t|
    t.integer  "car_id"
    t.integer  "user_id"
    t.string   "make"
    t.string   "model"
    t.string   "plate_num"
    t.string   "vin"
    t.datetime "created_at", :null => false
    t.datetime "updated_at", :null => false
  end

  add_index "cars", ["user_id"], :name => "index_cars_on_user_id"

  create_table "points", :force => true do |t|
    t.decimal  "lat",        :precision => 16, :scale => 6, :default => 0.0
    t.decimal  "lon",        :precision => 16, :scale => 6, :default => 0.0
    t.string   "name"
    t.integer  "time"
    t.decimal  "elevation",  :precision => 10, :scale => 2, :default => 0.0
    t.decimal  "speed",      :precision => 10, :scale => 2, :default => 0.0
    t.decimal  "hdop",       :precision => 10, :scale => 2, :default => 0.0
    t.integer  "car_id"
    t.datetime "created_at",                                                 :null => false
    t.datetime "updated_at",                                                 :null => false
  end

  add_index "points", ["car_id"], :name => "index_points_on_car_id"

  create_table "users", :force => true do |t|
    t.string   "name"
    t.string   "username"
    t.string   "password"
    t.datetime "created_at",                             :null => false
    t.datetime "updated_at",                             :null => false
    t.string   "email",                  :default => "", :null => false
    t.string   "encrypted_password",     :default => "", :null => false
    t.string   "reset_password_token"
    t.datetime "reset_password_sent_at"
    t.datetime "remember_created_at"
    t.integer  "sign_in_count",          :default => 0
    t.datetime "current_sign_in_at"
    t.datetime "last_sign_in_at"
    t.string   "current_sign_in_ip"
    t.string   "last_sign_in_ip"
  end

  add_index "users", ["email"], :name => "index_users_on_email", :unique => true
  add_index "users", ["reset_password_token"], :name => "index_users_on_reset_password_token", :unique => true

end
