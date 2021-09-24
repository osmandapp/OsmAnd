package net.osmand;

import java.util.Calendar;
import java.util.Date;

import androidx.annotation.NonNull;

import static java.util.Calendar.DAY_OF_YEAR;
import static java.util.Calendar.ERA;
import static java.util.Calendar.YEAR;


public class OsmAndDateUtils {

	public static boolean isSameDay(@NonNull Date firstDate, @NonNull Date secondDate) {
		Calendar firstCal = Calendar.getInstance();
		firstCal.setTime(firstDate);
		Calendar secondCal = Calendar.getInstance();
		secondCal.setTime(secondDate);
		return isSameDay(firstCal, secondCal);
	}

	public static boolean isSameDay(@NonNull Calendar first, @NonNull Calendar second) {
		return first.get(ERA) == second.get(ERA)
				&& first.get(YEAR) == second.get(YEAR)
				&& first.get(DAY_OF_YEAR) == second.get(DAY_OF_YEAR);
	}
}
