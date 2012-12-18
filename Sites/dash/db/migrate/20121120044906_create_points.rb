class CreatePoints < ActiveRecord::Migration
  def change
    create_table :points do |t|
      t.decimal :lat, :precision => 16, :scale => 6, :default => 0
      t.decimal :lon, :precision => 16, :scale => 6, :default => 0
      t.string :name
      t.integer :time
      t.decimal :elevation, :precision => 10, :scale => 2, :default => 0
      t.decimal :speed, :precision => 10, :scale => 2, :default => 0
      t.decimal :hdop, :precision => 10, :scale => 2, :default => 0
      t.references :car

      t.timestamps
    end
    add_index :points, :car_id
  end
end
