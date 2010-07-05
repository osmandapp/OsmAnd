package com.osmand.osm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OpeningHoursParser {
	private static final String[] daysStr = new String[] {"Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
	public static boolean parseRule(String r, int[][] hours, boolean[] days){
		Arrays.fill(days, false);
		int startDay = -1;
		int previousDay = -1;
		int k = 0;
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
			return false;
		}
		String time = r.substring(k);
		String[] stEnd = time.split("-"); //$NON-NLS-1$
		if(stEnd.length != 2){
			return false;
		}
		int st;
		int end;
		try {
			int i1 = stEnd[0].indexOf(':');
			int i2 = stEnd[1].indexOf(':');
			st = Integer.parseInt(stEnd[0].substring(0, i1).trim())* 60 + Integer.parseInt(stEnd[0].substring(i1 + 1).trim());
			end = Integer.parseInt(stEnd[1].substring(0, i2).trim())* 60 + Integer.parseInt(stEnd[1].substring(i2 + 1).trim());
		} catch (NumberFormatException e) {
			return false;
		}
		for(int i=0; i<7; i++){
			if(days[i]){
				hours[i][0] = st;
				hours[i][1] = end;
			}
		}
		return true;
	}
	
	public static int[][] parseOpenedHours(String format){
		int[][] hours = new int[7][2];
		for(int k = 0; k<7;k++){
			hours[k][0] = hours[k][1] = -1;
		}
		boolean days[] = new boolean[7];
		String[] rules = format.split(";"); //$NON-NLS-1$
		for(String r : rules){
			r = r.trim();
			if(r.length() == 0){
				continue;
			}
			// check if valid
			if(!parseRule(r, hours, days)){
				return null;
			}
		}
		return hours;
	}
	public static String toStringOpenedHours(int[][] hours){
		Map<Integer, List<Integer>> groups  = new LinkedHashMap<Integer, List<Integer>>();
		for (int k = 0; k < 7; k++) {
			if (hours[k][0] >= 0 && hours[k][1] >= 0) {
				int uniqueInt = hours[k][1] * 60 * 24 + hours[k][0];
				if (!groups.containsKey(uniqueInt)) {
					groups.put(uniqueInt, new ArrayList<Integer>());
				}
				groups.get(uniqueInt).add(k);
			}
		}
		StringBuilder b = new StringBuilder(100);
		boolean first = true;
		for(Integer time : groups.keySet()){
			if(first){
				first = false;
			} else {
				b.append("; "); //$NON-NLS-1$
			}
			int end = time / (60 * 24);
			int st = time - end * (60 * 24);
			int stHour = st / 60;
			int stTime = st - stHour * 60;
			int endHour = end / 60;
			int endTime = end - endHour * 60;
			List<Integer> list = groups.get(time);
			boolean dash = false;
			for(int k = 0; k < list.size(); k++){
				Integer val = list.get(k);
				if(k > 0){
					if(k < list.size() - 1 && list.get(k + 1) == val + 1 && list.get(k - 1) == val - 1){
						if(!dash){
							b.append("-"); //$NON-NLS-1$
							dash = true;
						}
					} else if(dash){
						b.append(daysStr[val]);
						dash = false;
					} else {
						b.append(", ").append(daysStr[val]); //$NON-NLS-1$
					}
				} else {
					b.append(daysStr[val]);
				}
			}
			
			b.append(" "); //$NON-NLS-1$
			formatTime(stHour, stTime, b);
			b.append("-"); //$NON-NLS-1$
			formatTime(endHour, endTime, b);
			
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
		int[][] hours = parseOpenedHours("Mo-Fr 08:30-14:40; Sa 08:00 - 14:00"); //$NON-NLS-1$
		System.out.println(Arrays.deepToString(hours));
		System.out.println(toStringOpenedHours(hours));
		hours = parseOpenedHours("Mo, We-Fr 08:30-14:40; Sa 08:00 - 14:00"); //$NON-NLS-1$
		System.out.println(Arrays.deepToString(hours));
		System.out.println(toStringOpenedHours(hours));
	}
	
}
