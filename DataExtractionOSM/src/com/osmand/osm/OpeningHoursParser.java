package com.osmand.osm;

import java.util.ArrayList;
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
		private int endTime = - 1;
		
		public boolean[] getDays() {
			return days;
		}
		
		public void setStartTime(int startTime) {
			this.startTime = startTime;
		}
		public int getStartTime() {
			return startTime;
		}
		
		public int getEndTime() {
			return endTime;
		}
		public void setEndTime(int endTime) {
			this.endTime = endTime;
		}
		
		@Override
		public boolean isOpenedForTime(Calendar cal) {
			if(startTime == -1){
				return false;
			}
			int i = cal.get(Calendar.DAY_OF_WEEK);
			int d = (i + 5) % 7;
			int p = d - 1;
			if(p < 0){
				p+=7;
			}
			int time = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
			// one day working 10 - 20 (not 20 - 04)
			if(startTime < endTime || endTime == -1){
				if(days[d]){
					if(time >= startTime && (endTime == -1 || time <= endTime)){
						return true;
					}
				}
				return false;
			} else {
				if (time <= endTime && days[p]) {
					// check in previous day
					return true;
				} else if (time <= startTime && days[d]) {
					// check in previous day
					return true;
				}
				return false;
			}
		}
		@Override
		public String toRuleString() {
			StringBuilder b = new StringBuilder(25);
			boolean dash = false;
			boolean first = true;
			for(int i=0; i< 7; i++){
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
				}
			}
			int stHour = startTime / 60;
			int stTime = startTime - stHour * 60;
			int enHour = endTime / 60;
			int enTime = endTime - enHour * 60;
			b.append(" "); //$NON-NLS-1$
			formatTime(stHour, stTime, b);
			b.append("-"); //$NON-NLS-1$
			formatTime(enHour, enTime, b);
			return b.toString();
		}
		
		@Override
		public String toString() {
			return toRuleString();
		}
	}
	
	
	public static OpeningHoursRule parseRule(String r){
		int startDay = -1;
		int previousDay = -1;
		BasicDayOpeningHourRule basic = new BasicDayOpeningHourRule();
		int k = 0;
		boolean[] days = basic.getDays();
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
		String time = r.substring(k);
		String[] stEnd = time.split("-"); //$NON-NLS-1$
		if(stEnd.length != 2){
			return null;
		}
		int st;
		int end;
		try {
			int i1 = stEnd[0].indexOf(':');
			int i2 = stEnd[1].indexOf(':');
			st = Integer.parseInt(stEnd[0].substring(0, i1).trim())* 60 + Integer.parseInt(stEnd[0].substring(i1 + 1).trim());
			end = Integer.parseInt(stEnd[1].substring(0, i2).trim())* 60 + Integer.parseInt(stEnd[1].substring(i2 + 1).trim());
		} catch (NumberFormatException e) {
			return null;
		}
		basic.setStartTime(st);
		basic.setEndTime(end);
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
			OpeningHoursRule rule = parseRule(r);
			if(rule == null){
				return null;
			}
			rs.add(rule);
			
		}
		return rs;
	}
	public static String toStringOpenedHours(List<? extends OpeningHoursRule> rules){
		StringBuilder b = new StringBuilder(100);
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
	
	public static void main(String[] args) {
		List<OpeningHoursRule> hours = parseOpenedHours("Mo-Fr 08:30-14:40; Sa 08:00 - 14:00"); //$NON-NLS-1$
		System.out.println(hours);
		System.out.println(toStringOpenedHours(hours));
		hours = parseOpenedHours("Mo, We-Fr, Th, Sa 08:30-14:40; Sa 08:00 - 14:00"); //$NON-NLS-1$
		System.out.println(hours);
		System.out.println(toStringOpenedHours(hours));
	}
	
}
