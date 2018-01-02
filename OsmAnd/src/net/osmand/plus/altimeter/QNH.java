package net.osmand.plus.altimeter;
import java.lang.Math;


//http://en.wikipedia.org/wiki/Atmospheric_pressure

class QNH{
	public static final double standardAtmospherePressure = 1013.25; //standard sea level pressure (hPa)
	private static final double L = 0.0065;    //temperature lapse rate
	private static final double cp = 1007;     //constant pressure specific heat
	private static final double T = 288.15;    //sea level standard temperature
	private static final double g = 9.80665;   //Earth-surface gravitational acceleration
	private static final double M = 0.0289644; //molar mass of dry air
	private static final double R = 8.31447;   //universal gas constant

	public static double qnh(double h, double p){
		double qnh = 0;
		qnh = p * Math.pow( (1-(L*h)/T), -(g*M)/(R*L) );
		return qnh;
	}	

	public static double altitude(double qnh, double p){
		double h;
		h = (T/L) * (1 - Math.pow(p/qnh , (R*L)/(g*M) ));
		return h;
	}	

}
