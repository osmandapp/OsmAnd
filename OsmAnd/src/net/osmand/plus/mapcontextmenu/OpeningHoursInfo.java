package net.osmand.plus.mapcontextmenu;

import android.content.Context;

import net.osmand.plus.R;
import net.osmand.util.Algorithms;

public class OpeningHoursInfo {

	private boolean opened;
	private boolean opened24_7;
	private String openingTime = "";
	private String nearToOpeningTime = "";
	private String closingTime = "";
	private String nearToClosingTime = "";
	private String openingDay = "";

	public boolean isOpened() {
		return opened;
	}

	public void setOpened(boolean opened) {
		this.opened = opened;
	}

	public boolean isOpened24_7() {
		return opened24_7;
	}

	public void setOpened24_7(boolean opened24_7) {
		this.opened24_7 = opened24_7;
	}

	public String getOpeningTime() {
		return openingTime;
	}

	public void setOpeningTime(String openFromTime) {
		this.openingTime = openFromTime;
	}

	public String getNearToOpeningTime() {
		return nearToOpeningTime;
	}

	public void setNearToOpeningTime(String nearToOpeningTime) {
		this.nearToOpeningTime = nearToOpeningTime;
	}

	public String getClosingTime() {
		return closingTime;
	}

	public void setClosingTime(String closingTime) {
		this.closingTime = closingTime;
	}

	public String getNearToClosingTime() {
		return nearToClosingTime;
	}

	public void setNearToClosingTime(String nearToClosingTime) {
		this.nearToClosingTime = nearToClosingTime;
	}

	public String getOpeningDay() {
		return openingDay;
	}

	public void setOpeningDay(String openingDay) {
		this.openingDay = openingDay;
	}

	public String getInfo(Context context) {
		if (isOpened24_7()) {
			return context.getString(R.string.shared_string_is_open_24_7);
		} else if (!Algorithms.isEmpty(getNearToOpeningTime())) {
			return context.getString(R.string.will_open_at) + " " + getNearToOpeningTime();
		} else if (!Algorithms.isEmpty(getOpeningTime())) {
			return context.getString(R.string.open_from) + " " + getOpeningTime();
		} else if (!Algorithms.isEmpty(getNearToClosingTime())) {
			return context.getString(R.string.will_close_at) + " " + getNearToClosingTime();
		} else if (!Algorithms.isEmpty(getClosingTime())) {
			return context.getString(R.string.open_till) + " " + getClosingTime();
		} else if (!Algorithms.isEmpty(getOpeningDay())) {
			return context.getString(R.string.will_open_on) + " " + getOpeningDay() + ".";
		}
		return "";
	}
}
