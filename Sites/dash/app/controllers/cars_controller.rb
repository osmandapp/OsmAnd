class CarsController < ApplicationController
  # GET /cars
  # GET /cars.json
  def index
    @cars = Car.all

    respond_to do |format|
      format.html # index.html.erb
      format.json { render json: @cars }
    end
  end

  # GET /cars/1
  # GET /cars/1.json
  def show
    @car = Car.find(params[:id])
    @points = @car.points.all
    @json = @points.to_gmaps4rails

    respond_to do |format|
      format.html # show.html.erb
      format.json { render json: @car }
    end
  end

  # GET /cars/new
  # GET /cars/new.json
  def new
    @car = Car.new

    respond_to do |format|
      format.html # new.html.erb
      format.json { render json: @car }
    end
  end

  # GET /cars/1/edit
  def edit
    @user = User.find(params[:user_id])
    @car = @user.cars.find(params[:id])
  end

  # POST /cars
  # POST /cars.json
  def create
    @user = User.find(params[:user_id])
    @car = @user.cars.create(params[:car])
    redirect_to user_path(@user)
  end

  # PUT /cars/1
  # PUT /cars/1.json
  def update
    @car = Car.find(params[:id])

    respond_to do |format|
      if @car.update_attributes(params[:car])
        format.html { redirect_to @car, notice: 'Car was successfully updated.' }
        format.json { head :no_content }
      else
        format.html { render action: "edit" }
        format.json { render json: @car.errors, status: :unprocessable_entity }
      end
    end
  end

  # DELETE /cars/1
  # DELETE /cars/1.json
  def destroy
    @user = User.find(params[:user_id])
    @car = @user.cars.find(params[:id])
    @car.destroy
    redirect_to user_path(@user)
  end
end
