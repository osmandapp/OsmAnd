class PointsController < ApplicationController
  def create
    @user = User.find(params[:user_id])
    @car = Car.find(params[:car_id])
    @point = @car.points.create()
    @point.lat = params[:point][:lat].to_f
    @point.lon = params[:point][:lon].to_f
    @point.save()
    redirect_to user_path(@user)
  end

  def edit
    @car = Car.find(params[:car_id])
    @point = Point.find(params[:id])
  end
  
  def index
    @points = Point.all
    @json = @points.to_gmaps4rails
  end

  def destroy
    @car = Car.find(params[:car_id])
    @point = @car.points.find(params[:id])
    @point.destroy
    redirect_to user_path(@user)
  end
end
