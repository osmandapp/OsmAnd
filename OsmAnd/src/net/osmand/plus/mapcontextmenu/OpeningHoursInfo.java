package net.osmand.plus.mapcontextmenu;

import net.osmand.util.Algorithms;

public class OpeningHoursInfo {

	private boolean opened;
	private boolean opened24_7;
	private String openingTime = "";
	private String nearToOpeningTime = "";
	private String closingTime = "";
	private String nearToClosingTime = "";

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

	public boolean containsInfo() {
		return opened24_7
				|| !Algorithms.isEmpty(openingTime)
				|| !Algorithms.isEmpty(nearToOpeningTime)
				|| !Algorithms.isEmpty(closingTime)
				|| !Algorithms.isEmpty(nearToClosingTime);
	}
}
