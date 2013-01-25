class User < ActiveRecord::Base
  attr_accessible :name, :username, :password

  validates_uniqueness_of :username
  validates_confirmation_of :password, :on => :create
  validates_length_of :password, :within => 5..40
  validates :name, :presence => true
  
  has_many :cars, :dependent => :destroy
end
