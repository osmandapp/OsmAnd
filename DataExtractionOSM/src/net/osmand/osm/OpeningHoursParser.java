package net.osmand.osm;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class OpeningHoursParser {
	private static final String[] daysStr = new String[] {"Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
	
	public static interface OpeningHoursRule {
		
		public boolean isOpenedForTime(Calendar cal);
		
		public String toRuleString();
	}
	
	public static class BasicDayOpeningHourRule  implements OpeningHoursRule {
		private boolean[] days = new boolean[7];
		private int startTime = -1;
		private int endTime = -1;
		
		public boolean[] getDays() {
			return days;
		}
		
		public void setStartEndTime(int startTime, int endTime) {
			this.startTime = startTime;
			this.endTime = endTime;
		}
		
		public int getStartTime() {
			return startTime;
		}
		
		public int getEndTime() {
			return endTime;
		}
		
		@Override
		public boolean isOpenedForTime(Calendar cal) {
			if (startTime == -1) {
				return false;
			}
			int i = cal.get(Calendar.DAY_OF_WEEK);
			int d = (i + 5) % 7;
			int p = d - 1;
			if (p < 0) {
				p += 7;
			}
			int time = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
			int startTime = this.startTime;
			int endTime = this.endTime;
			// one day working 10 - 20 (not 20 - 04)
			if (startTime < endTime || endTime == -1) {
				if (days[d]) {
					if (time >= startTime && (endTime == -1 || time <= endTime)) {
						return true;
					}
				}
			} else {
				if (time >= startTime && days[p]) {
					// check in previous day
					return true;
				} else if (time <= endTime && days[d]) {
					// check in previous day
					return true;
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
			return b.toString();
		}
		
		@Override
		public String toString() {
			return toRuleString();
		}

		public void setStartTime(int startTime) {
			this.startTime = startTime;
		}
		
		public void setEndTime(int endTime) {
			this.endTime = endTime;
		}
	}
	
	
	public static OpeningHoursRule parseRule(String r, List<OpeningHoursRule> rs){
		int startDay = -1;
		int previousDay = -1;
		int k = 0;
		// check 24/7
		BasicDayOpeningHourRule basic = new BasicDayOpeningHourRule();
		boolean[] days = basic.getDays();
		if("24/7".equals(r)){
			Arrays.fill(days, true);
			basic.setStartEndTime(0, 24*60);
			rs.add(basic);
			return basic;
		}
		
		for (; k < r.length(); k++) {
			char ch = r.charAt(k);
			if (Character.isDigit(ch)) {
				// time starts
				break;
			}
			if(Character.isWhitespace(ch) || ch == ','){
				continue;
			} else if(ch == '-'){
				if(previousDay != -1){
					startDay = previousDay; 
				} else {
					return null;
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
						startDay = -1;
					} else {
						days[i] = true;
					}
					previousDay = i;
				}
			} else {
				return null;
			}
		}
		if(previousDay == -1){
			return null;
		}
		String timeSubstr = r.substring(k);
		String[] times = timeSubstr.split(",");
		boolean timesExist = true;
		for (String time : times) {
			time = time.trim();
			if(time.length() == 0){
				continue;
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
				if (i1 == -1 || i2 == -1) {
					return null;
				}
				st = Integer.parseInt(stEnd[0].substring(0, i1).trim()) * 60 + Integer.parseInt(stEnd[0].substring(i1 + 1).trim());
				end = Integer.parseInt(stEnd[1].substring(0, i2).trim()) * 60 + Integer.parseInt(stEnd[1].substring(i2 + 1).trim());
			} catch (NumberFormatException e) {
				return null;
			}
			basic.setStartEndTime(st, end);
			rs.add(basic);
			BasicDayOpeningHourRule nbasic = new BasicDayOpeningHourRule();
			nbasic.days = basic.days;
			basic = nbasic;
		}
		if(!timesExist){
			return null;
		}
		return basic;
	}
	
	
	public static List<OpeningHoursRule> parseOpenedHours(String format){
		String[] rules = format.split(";"); //$NON-NLS-1$
		List<OpeningHoursRule> rs = new ArrayList<OpeningHoursRule>();
		for(String r : rules){
			r = r.trim();
			if(r.length() == 0){
				continue;
			}
			// check if valid
			OpeningHoursRule rule = parseRule(r, rs);
			if(rule == null){
				return null;
			}
		}
		return rs;
	}
	public static String toStringOpenedHours(List<? extends OpeningHoursRule> rules){
		StringBuilder b = new StringBuilder(100);
		// check 24/7
		boolean first = true;
		for (OpeningHoursRule p : rules) {
			if(p == null){
				continue;
			}
			if (first) {
				first = false;
			} else {
				b.append("; "); //$NON-NLS-1$
			}
			b.append(p.toRuleString());
		}
		
		return b.toString();
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
	
	private static void testOpened(String time, List<OpeningHoursRule> hours, boolean expected) throws ParseException {
		boolean opened = false;
		Calendar cal = Calendar.getInstance();
		cal.setTime(new SimpleDateFormat("dd.MM.yyyy hh:mm").parse(time));
		for(OpeningHoursRule r : hours) {
			if(r.isOpenedForTime(cal)){
				opened = true;
				break;
			}
		}
		System.out.println("Expected " + expected +" = " + opened);
	}
	
	public static void main(String[] args) throws ParseException {
		
		List<OpeningHoursRule> hours = parseOpenedHours("Mo-Fr 08:30-14:40; Sa 08:00 - 14:00"); //$NON-NLS-1$
		System.out.println(hours);
		System.out.println(toStringOpenedHours(hours));
		hours = parseOpenedHours("Mo-Fr 08:30-14:40,15:00-19:00"); //$NON-NLS-1$
		testOpened("20.04.2012 14:00", hours, true);
		testOpened("20.04.2012 15:00", hours, true);
		testOpened("20.04.2012 14:50", hours, false);
		System.out.println(hours);
		System.out.println(toStringOpenedHours(hours));
		hours = parseOpenedHours("Mo, We-Fr, Th, Sa 08:30-14:40; Sa 08:00 - 14:00"); //$NON-NLS-1$
		System.out.println(hours);
		System.out.println(toStringOpenedHours(hours));
		hours = parseOpenedHours("Mo-Su 00:00-24:00"); //$NON-NLS-1$
		System.out.println(hours);
		System.out.println(toStringOpenedHours(hours));
		hours = parseOpenedHours("24/7"); //$NON-NLS-1$
		System.out.println(hours);
		System.out.println(toStringOpenedHours(hours));
	}
	
}
