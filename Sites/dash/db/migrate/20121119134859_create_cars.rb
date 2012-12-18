class CreateCars < ActiveRecord::Migration
  def change
    create_table :cars do |t|
      t.integer :car_id
      t.references :user
      t.string :make
      t.string :model
      t.string :plate_num
      t.string :vin

      t.timestamps
    end
    
    add_index :cars, :user_id
  end
end
