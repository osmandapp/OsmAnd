package net.osmand.util;

import java.util.Calendar;
import java.util.Date;

public class DateUtils {

	public static Date createDateInCurrentYear(int month, int date, int hour, int minute) {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.DATE, date);
		calendar.set(Calendar.HOUR, hour);
		calendar.set(Calendar.MINUTE, minute);
		return calendar.getTime();
	}

}
