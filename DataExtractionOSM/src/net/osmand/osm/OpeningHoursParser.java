package net.osmand.osm;



import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;


/**
 * Class used to parse opening hours
 * 
 * the method "parseOpenedHours" will parse an OSM opening_hours string and return
 * an object of the type OpeningHours. That object can be used to check if the OSM feature
 * is open at a certain time. 
 */
public class OpeningHoursParser {
	private static final String[] daysStr = new String[] {"Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
	/**
	 * default values for sunrise and sunset. Might be computed afterwards, not final
	 */
	private static String sunrise = "07:00", sunset = "21:00";
	/**
	 * hour of when you would expect a day to be ended.
	 * This is to be used when no end hour is known (like pubs that open at a certain time, 
	 * but close at a variable time, depending on the number of clients).
	 * OsmAnd needs to show a value, so there is some arbitrary default value chosen.
	 */
	private static String endOfDay = "24:00";
	
	/**
	 * This class contains the entire OpeningHours schema and 
	 * offers methods to check directly weather something is open
	 * 
	 * @author sander
	 */
	public static class OpeningHours {
		
		/**
		 * list of the different rules
		 */
		private ArrayList<OpeningHoursRule> rules;
		
		/**
		 * Constructor
		 * 
		 * @param rules  List of OpeningHoursRule to be given
		 */
		public OpeningHours(ArrayList<OpeningHoursRule> rules) {
			this.rules = rules;
		}
		
		/**
		 * Empty constructor
		 */
		public OpeningHours(){
			rules = new ArrayList<OpeningHoursRule>();
		}
		
		/**
		 * add a rule to the opening hours
		 * @param r  rule to add
		 */
		public void addRule(OpeningHoursRule r){
			rules.add(r);
		}
		
		/**
		 * return the list of rules
		 * 
		 * @return the rules
		 */
		public ArrayList<OpeningHoursRule> getRules(){
			return rules;
		}
		
		/**
		 * check if the feature is opened at time "cal"
		 * @param cal  the time to check
		 * @return true if feature is open
		 */
		public boolean isOpenedForTime(Calendar cal){
			/*
			 * first check for rules that contain the current day
			 * afterwards check for rules that contain the previous
			 * day with overlapping times (times after midnight)
			 */
			boolean isOpenDay = false;
			for (OpeningHoursRule r : rules){
				if(r.containsDay(cal)){
					isOpenDay = r.isOpenedForTime(cal, false);
				}
			}
			boolean isOpenPrevious = false;
			for (OpeningHoursRule r : rules){
				if(r.containsPreviousDay(cal)){
					isOpenPrevious = r.isOpenedForTime(cal, true);
				}
			}
			return isOpenDay || isOpenPrevious;
		}
		
		@Override
		public String toString(){
			StringBuilder s = new StringBuilder();
			
			if (rules.isEmpty()) {
				return "";
			}
			
			for (OpeningHoursRule r : rules) {
				s.append(r.toString()).append("; ");
			}
			
			return s.substring(0, s.length()-2);
		}
		
	}
	
	/**
	 * Interface to represent a single rule
	 * 
	 * A rule consist out of 
	 *  - a collection of days/dates
	 *  - a time range
	 */
	public static interface OpeningHoursRule {
		
		/**
		 * Check if, for this rule, the feature is opened for time "cal"
		 * @param cal  the time to check
		 * @param checkPrevious only check for overflowing times (after midnight) or don't check for it
		 * @return true if the feature is open
		 */
		public boolean isOpenedForTime(Calendar cal, boolean checkPrevious);
		
		/**
		 * Check if the previous day before "cal" is part of this rule
		 * @param cal; the time to check 
		 * @return true if the previous day is part of the rule
		 */
		public boolean containsPreviousDay(Calendar cal);

		/**
		 * Check if the day of "cal" is part of this rule
		 * @param cal; the time to check 
		 * @return true if the day is part of the rule
		 */
		public boolean containsDay(Calendar cal);
		
		public String toRuleString();
	}
	
	/**
	 * implementation of the basic OpeningHoursRule
	 * 
	 * This implementation only supports day of weeks and numeral times, or the value "off"
	 *
	 */
	public static class BasicDayOpeningHourRule  implements OpeningHoursRule {
		/**
		 * represents the list on which days it is open.
		 * Day number 0 is ???? TODO
		 */
		private boolean[] days = new boolean[7];
		/**
		 * lists of equal size representing the start and end times
		 */
		private int[] startTimes, endTimes;
		
		/**
		 * return an array representing the days of the rule
		 * @return the days of the rule
		 */
		public boolean[] getDays() {
			return days;
		}
		
		@Deprecated
		/**
		 * set a single start time, erase all previously added start times
		 * @param s startTime to set
		 */
		public void setStartTime(int s) {
			startTimes = new int[]{s};
			if(endTimes.length != 1) {
				endTimes = new int[]{0};
			}
		}
		
		@Deprecated
		/**
		 * set a single end time, erase all previously added end times
		 * @param e endTime to set
		 */
		public void setEndTime(int e) {
			endTimes = new int[]{e};
			if(startTimes.length != 1) {
				startTimes = new int[]{0};
			}
		}
		
		@Deprecated
		/**
		 * get a single start time
		 * @return a single start time
		 */
		public int getStartTime() {
			try {
				return startTimes[0];
			} catch (Exception e){
				return 0;
			}
		}
		
		@Deprecated
		/**
		 * get a single end time
		 * @return a single end time
		 */
		public int getEndTime() {
			try {
				return endTimes[0];
			} catch (Exception e){
				return 0;
			}
		}
		
		@Override 
		/**
		 * Check if the weekday of time "cal" is part of this rule
		 * @param cal the time to check
		 * @return true if this day is part of the rule
		 */
		public boolean containsDay(Calendar cal){
			int i = cal.get(Calendar.DAY_OF_WEEK);
			int d = (i + 5) % 7;
			if (days[d]) {	
				return true;	
			}
			return false;
			
		}
		
		@Override 
		/**
		 * Check if the previous weekday of time "cal" is part of this rule
		 * @param cal the time to check
		 * @return true if the previous day is part of the rule
		 */
		public boolean containsPreviousDay(Calendar cal){
			int i = cal.get(Calendar.DAY_OF_WEEK);
			int p = (i + 4) % 7;
			if (days[p]) {	
				return true;	
			}
			return false;
			
		}
		
		@Override
		/**
		 * Check if this rule says the feature is open at time "cal"
		 * @param cal the time to check
		 * @return true if this rule contains the day to check and the start and end times denote it's open
		 * @return true if this rule contains the previous day and an endTime>startTime such that the time to check falls before the endtime
		 * @return false in all other cases, also if only day is wrong
		 */
		public boolean isOpenedForTime(Calendar cal, boolean checkPrevious) {
			if (startTimes == null) {
				return false;
			}
			int i = cal.get(Calendar.DAY_OF_WEEK);
			int d = (i + 5) % 7;
			int p = d - 1;
			if (p < 0) {
				p += 7;
			}
			int time = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
			for (i = 0; i<startTimes.length; i++){
				int startTime = this.startTimes[i];
				int endTime = this.endTimes[i];
				// one day working 10 - 20 (not 20 - 04)
				if (startTime < endTime || endTime == -1) {
					if (days[d] && !checkPrevious) {
						if (time >= startTime && (endTime == -1 || time <= endTime)) {
							return true;
						}
					}
				} else {
					if (time >= startTime && days[p] && checkPrevious) {
						// check in previous day
						return true;
					} else if (time <= endTime && days[d] && !checkPrevious) {
						return true;
					}
				}
			}
			return false;
		}
		
		
		@Override
		public String toRuleString() {
			StringBuilder b = new StringBuilder(25);
			boolean dash = false;
			boolean first = true;
			boolean open24_7 = true;
			for (int i = 0; i < 7; i++) {
				if (days[i]) {
					if (i > 0 && days[i - 1] && i < 6 && days[i + 1]) {
						if (!dash) {
							dash = true;
							b.append("-"); //$NON-NLS-1$
						}
						continue;
					}
					if (first) {
						first = false;
					} else if (!dash) {
						b.append(", "); //$NON-NLS-1$
					}
					b.append(daysStr[i]);
					dash = false;
				} else {
					open24_7 = false;
				}
			}
			if (startTimes == null || startTimes.length == 0){
				b.append(" off ");
			} else {
				for (int i = 0; i<startTimes.length; i++) {
					int startTime = startTimes[i];
					int endTime = endTimes[i];
					if (open24_7 && startTime == 0 && endTime / 60 == 24) {
						return "24/7";
					}
					b.append(" "); //$NON-NLS-1$
					int stHour = startTime / 60;
					int stTime = startTime - stHour * 60;
					int enHour = endTime / 60;
					int enTime = endTime - enHour * 60;
					formatTime(stHour, stTime, b);
					b.append("-"); //$NON-NLS-1$
					formatTime(enHour, enTime, b);
					b.append(",");
				}
			}
			return b.substring(0, b.length()-1);
		}
		
		@Override
		public String toString() {
			return toRuleString();
		}

		/**
		 * Add a time range (startTime-endTime) to this rule
		 * @param startTime startTime to add
		 * @param endTime endTime to add
		 */
		public void addTimeRange(int startTime, int endTime) {
			if (startTimes == null){
				startTimes = new int[] {startTime};
				endTimes = new int[] {endTime};
			} else {
				int l = startTimes.length;
				int[] newStartTimes = new int[l+1];
				int[] newEndTimes = new int[l+1];
				for (int i=0; i<l; i++){
					newStartTimes[i] = startTimes[i];
					newEndTimes[i] = endTimes[i];
				}
				newStartTimes[l] = startTime;
				newEndTimes[l] = endTime;
				
				startTimes = newStartTimes;
				endTimes = newEndTimes;
			}
		}
		
	}
	
	/**
	 * Parse an opening_hours string from OSM to an OpeningHours object which can be used to check
	 * @param r the string to parse
	 * @param rs the resulting object representing the opening hours of the feature
	 * @return true if the String is successfully parsed
	 */
	public static boolean parseRule(String r, OpeningHours rs){
		// replace words "sunrise" and "sunset" by real hours
		r = r.replaceAll("sunset", sunset);
		r = r.replaceAll("sunrise", sunrise);
		// replace the '+' by an arbitrary value
		r = r.replaceAll("\\+", "-" + endOfDay);
		int startDay = -1;
		int previousDay = -1;
		int k = 0;
		// check 24/7
		BasicDayOpeningHourRule basic = new BasicDayOpeningHourRule();
		boolean[] days = basic.getDays();
		if("24/7".equals(r)){
			Arrays.fill(days, true);
			basic.addTimeRange(0, 24*60);
			rs.addRule(basic);
			return true;
		}
		
		for (; k < r.length(); k++) {
			char ch = r.charAt(k);
			if (Character.isDigit(ch)) {
				// time starts
				break;
			}
			if (ch == 'o' && r.charAt(k+1) == 'f' && r.charAt(k+1) == 'f'){
				// value "off" is found
				break;
			}
			if(Character.isWhitespace(ch) || ch == ','){
				continue;
			} else if(ch == '-'){
				if(previousDay != -1){
					startDay = previousDay; 
				} else {
					return false;
				}
			} else if(k < r.length() - 1){
				int i = 0;
				for(String s : daysStr){
					if(s.charAt(0) == ch && s.charAt(1) == r.charAt(k+1)){
						break;
					}
					i++;
				}
				if(i < daysStr.length){
					if(startDay != -1){
						for (int j = startDay; j <= i; j++) {
							days[j] = true;
						}
						if(startDay > i){// overflow handling, e.g. Su-We
							for (int j = startDay; j <= 6; j++) {
								days[j] = true;
							}
							for (int j = 0; j <= i; j++){
								days[j] = true;
							}
						}
						startDay = -1;
					} else {
						days[i] = true;
					}
					previousDay = i;
				}
			} else {
				return false;
			}
		}
		if(previousDay == -1){
			// no days given => take all days.
			for (int i = 0; i<7; i++){
				days[i] = true;
			}
		}
		String timeSubstr = r.substring(k);
		String[] times = timeSubstr.split(",");
		boolean timesExist = true;
		for (String time : times) {
			time = time.trim();
			if(time.length() == 0){
				continue;
			}
			if(time.equals("off")){
				break; // add no time values
			}
			if(time.equals("24/7")){
				// for some reason, this is used. See tagwatch.
				basic.addTimeRange(0, 24*60);
				break;
			}
			String[] stEnd = time.split("-"); //$NON-NLS-1$
			if (stEnd.length != 2) {
				continue;
			}
			timesExist = true;
			int st;
			int end;
			try {
				int i1 = stEnd[0].indexOf(':');
				int i2 = stEnd[1].indexOf(':');
				int startHour, startMin, endHour, endMin;
				if(i1 == -1) {
					// if no minutes are given, try complete value as hour
					startHour = Integer.parseInt(stEnd[0].trim());
					startMin = 0;
				} else {
					startHour = Integer.parseInt(stEnd[0].substring(0, i1).trim());
					startMin = Integer.parseInt(stEnd[0].substring(i1 + 1).trim());
				}
				if(i2 == -1) {
					// if no minutes are given, try complete value as hour
					endHour = Integer.parseInt(stEnd[1].trim());
					endMin = 0;
				} else {
					endHour = Integer.parseInt(stEnd[1].substring(0, i2).trim());
					endMin = Integer.parseInt(stEnd[1].substring(i2 + 1).trim());
				}
				st = startHour * 60 + startMin;
				end = endHour * 60 + endMin;
			} catch (NumberFormatException e) {
				return false;
			}
			basic.addTimeRange(st, end);
		}
		rs.addRule(basic);
		if(!timesExist){
			return false;
		}
		return true;
	}
	
	/**
	 * parse OSM opening_hours string to an OpeningHours object
	 * @param format the string to parse
	 * @return the OpeningHours object when parsing was successful
	 * @return null when parsing was unsuccessful
	 */
	public static OpeningHours parseOpenedHours(String format){
		// split the OSM string in multiple rules
		String[] rules = format.split(";"); //$NON-NLS-1$
		OpeningHours rs = new OpeningHours();
		for(String r : rules){
			r = r.trim();
			if(r.length() == 0){
				continue;
			}
			// check if valid
			boolean rule = parseRule(r, rs);
			if(!rule){
				return null;
			}
		}
		return rs;
	}
	
	
	private static void formatTime(int h, int t, StringBuilder b){
		if(h < 10){
			b.append("0"); //$NON-NLS-1$
		} 
		b.append(h).append(":"); //$NON-NLS-1$
		if(t < 10){
			b.append("0"); //$NON-NLS-1$
		}
		b.append(t);
	}
	
	
	private static void testOpened(String time, OpeningHours hours, boolean expected) throws ParseException {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new SimpleDateFormat("dd.MM.yyyy HH:mm").parse(time));
		System.out.println("Expected " + time+": " + expected +" = " + hours.isOpenedForTime(cal));
	}
	
	public static void main(String[] args) throws ParseException {
		
		
		//Test basic case
		OpeningHours hours = parseOpenedHours("Mo-Fr 08:30-14:40" ); //$NON-NLS-1$
		System.out.println(hours);
		testOpened("09.08.2012 11:00", hours, true);
		testOpened("09.08.2012 16:00", hours, false);
		// two time and date ranges
		hours = parseOpenedHours("Mo-We, Fr 08:30-14:40,15:00-19:00"); //$NON-NLS-1$
		System.out.println(hours);
		testOpened("08.08.2012 14:00", hours, true);
		testOpened("10.08.2012 15:00", hours, true);
		testOpened("08.08.2012 14:50", hours, false);
		// test exception on general schema
		hours = parseOpenedHours("Mo-Sa 08:30-14:40; Tu 08:00 - 14:00"); //$NON-NLS-1$
		System.out.println(hours);
		testOpened("07.08.2012 14:20", hours, false);
		// test off value
		hours = parseOpenedHours("Mo-Sa 09:00-18:25; Th off"); //$NON-NLS-1$
		System.out.println(hours);
		testOpened("08.08.2012 12:00", hours, true);
		testOpened("09.08.2012 12:00", hours, false);
		//test 24/7
		hours = parseOpenedHours("24/7"); //$NON-NLS-1$
		System.out.println(hours);
		testOpened("08.08.2012 23:59", hours, true);
		// some people seem to use the following syntax:
		hours = parseOpenedHours("Sa-Su 24/7");
		System.out.println(hours);
		hours = parseOpenedHours("Mo-Fr 9-19");
		System.out.println(hours);
		hours = parseOpenedHours("09:00-17:00");
		System.out.println(hours);
		hours = parseOpenedHours("sunrise-sunset");
		System.out.println(hours);
		hours = parseOpenedHours("10:00+");
		System.out.println(hours);
		hours = parseOpenedHours("Su-Th sunset-24:00, 04:00-sunrise; Fr-Sa sunset-sunrise");
		System.out.println(hours);
		testOpened("12.08.2012 04:00", hours, true);
		testOpened("12.08.2012 23:00", hours, true);
		testOpened("08.08.2012 12:00", hours, false);
		testOpened("08.08.2012 05:00", hours, true);
	}
	
}
