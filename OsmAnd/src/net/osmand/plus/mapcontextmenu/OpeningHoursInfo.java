package net.osmand.plus.mapcontextmenu;

public class OpeningHoursInfo {

	private boolean opened;
	private boolean opened24_7;
	private String openedFromTime = "";
	private String closedAtTime = "";
	private String openedTillTime = "";

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

	public String getOpenedFromTime() {
		return openedFromTime;
	}

	public void setOpenedFromTime(String openFromTime) {
		this.openedFromTime = openFromTime;
	}

	public String getClosedAtTime() {
		return closedAtTime;
	}

	public void setClosedAtTime(String closedAtTime) {
		this.closedAtTime = closedAtTime;
	}

	public String getOpenedTillTime() {
		return openedTillTime;
	}

	public void setOpenedTillTime(String openedTillTime) {
		this.openedTillTime = openedTillTime;
	}
}
