package net.osmand.plus.liveupdates;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.ArrayAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

class MonthsForReportsAdapter extends ArrayAdapter<String> {
	private static final SimpleDateFormat queryFormat = new SimpleDateFormat("yyyy-MM", Locale.US);
	@SuppressLint("SimpleDateFormat")
	private static final SimpleDateFormat humanFormat = new SimpleDateFormat("LLLL yyyy");

	ArrayList<String> queryString = new ArrayList<>();

	public MonthsForReportsAdapter(Context context) {
		super(context, android.R.layout.simple_spinner_item);
		Calendar startDate = Calendar.getInstance();
		startDate.set(Calendar.MONTH, Calendar.SEPTEMBER);
		startDate.set(Calendar.YEAR, 2015);
		startDate.set(Calendar.DAY_OF_MONTH, 1);
		startDate.set(Calendar.HOUR_OF_DAY, 0);
		Calendar endDate = Calendar.getInstance();
		while (startDate.before(endDate)) {
			queryString.add(queryFormat.format(endDate.getTime()));
			add(humanFormat.format(endDate.getTime()));
			endDate.add(Calendar.MONTH, -1);
		}
		setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	}

	public String getQueryString(int position) {
		return queryString.get(position);
	}
}
