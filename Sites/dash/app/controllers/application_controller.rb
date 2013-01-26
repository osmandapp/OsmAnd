class ApplicationController < ActionController::Base
  protect_from_forgery
  before_filter :set_user

  protected
    def set_user
      @user = User.find(session[:id]) if @user.nil? && session[:id]
    end

    def login_required?
      return true if @user
      access_denied
      return false
    end

    def access_denied
      session[:return_to] = request.request_uri
      flash[:error] = 'Oops. You need to login before you can view that page.' 
      redirect_to :controller => 'user', :action => 'login'
    end

end
