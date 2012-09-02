package net.osmand;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class TestSunriseSunset {
	
	public static final String MARCH = "05.03.2012";
	public static final String AUGUST = "05.08.2012";
	public static final String OCTOBER = "05.10.2012";
	public static final String DECEMBER = "05.12.2012";
	public static final String FEBRUARY = "05.02.2012";
	public static final String MAY = "05.05.2012";
	public static final String[] DATES = new String[] {MARCH, AUGUST, OCTOBER, DECEMBER, FEBRUARY, MAY};
	
	public static void printSunriseSunset(String date, float lat, float lon, TimeZone tz) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
		SimpleDateFormat prt = new SimpleDateFormat("dd.MM.yyyy HH:mm");
		prt.setTimeZone(TimeZone.getTimeZone("GMT"));
		Date time = sdf.parse(date);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(time);
		SunriseSunset ss = new SunriseSunset(lat, lon, time, TimeZone.getTimeZone("GMT"));
//		Location location = new Location(lat, lon);
//		SunriseSunsetCalculator calc = new SunriseSunsetCalculator(location, TimeZone.getTimeZone("GMT"));
		System.out.println("Sunrise : " + prt.format(ss.getSunrise()) + " GMT");
//		System.out.println("Sunrise : " + prt.format(calc.getOfficialSunriseCalendarForDate(calendar).getTime()));
//		System.out.println("Sunset  : " + prt.format( calc.getOfficialSunsetCalendarForDate(calendar).getTime()));
		System.out.println("Sunset  : " + prt.format(ss.getSunset()) + " GMT");
	}
	
	public static void testPCT() throws ParseException{
		System.out.println("California");
		for (String d : DATES) {
			printSunriseSunset(d, 34.08f, -118f, TimeZone.getTimeZone("PST"));
		}
	}
	
	public static void testCET() throws ParseException{
		System.out.println("Amsterdam");
		for (String d : DATES) {
			printSunriseSunset(d, 52.88f, 4.86f, TimeZone.getTimeZone("CET"));
		}
	}
	
	public static void testEDT() throws ParseException{
		System.out.println("New york");
		for (String d : DATES) {
			printSunriseSunset(d, 40.88f, -73.86f, TimeZone.getTimeZone("America/New_York"));
		}
	}
	
	public static void main(String[] args) throws ParseException {
		testPCT();
		testCET();
		testEDT();
	}
	
}
