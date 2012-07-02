package net.osmand.plus.osmedit;

import java.io.Serializable;

public class OsmbugsPoint implements Serializable {
	private static final long serialVersionUID = 729654300829771468L;

	private long id;
	private String text;
	private double latitude;
	private double longitude;
	private OsmBugsUtil.Action action;
	private String author;
	private boolean stored = false;

	public OsmbugsPoint(){
	}

	public long getId() {
		return id;
	}

	public String getText() {
		return text;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public OsmBugsUtil.Action getAction() {
		return action;
	}

	public String getAuthor() {
		return author;
	}

	public boolean isStored() {
		return stored;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public void setAction(String action) {
		this.action = OsmBugsRemoteUtil.actionString.get(action);
	}

	public void setAction(OsmBugsUtil.Action action) {
		this.action = action;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public void setStored(boolean stored) {
		this.stored = stored;
	}

	@Override
	public String toString() {
		return new StringBuffer("OsmBugs Point ").append(this.getAction()).append(" ").append(this.getText())
			.append(" ").append(this.getAuthor())
			.append(" (").append(this.getId()).append("): [")
			.append(" (").append(this.getLatitude()).append(", ").append(this.getLongitude())
			.append(")]").toString();
	}
}
