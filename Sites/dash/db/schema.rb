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

ActiveRecord::Schema.define(:version => 20121120044906) do

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
    t.datetime "created_at", :null => false
    t.datetime "updated_at", :null => false
  end

end
