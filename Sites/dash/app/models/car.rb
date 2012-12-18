class Car < ActiveRecord::Base
  belongs_to :user
  attr_accessible :car_id, :make, :model, :plate_num, :vin

  validates_uniqueness_of :car_id
  validates :car_id, :presence => true

  has_many :points
end
