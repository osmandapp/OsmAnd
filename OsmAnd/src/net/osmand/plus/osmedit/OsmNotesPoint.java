package net.osmand.plus.osmedit;

import java.io.Serializable;

public class OsmNotesPoint extends OsmPoint implements Serializable {
	private static final long serialVersionUID = 729654300829771468L;

	private long id;
	private String text;
	private double latitude;
	private double longitude;
	private String author;

	public OsmNotesPoint(){
	}

	@Override
	public long getId() {
		return id;
	}

	public String getText() {
		return text;
	}

	@Override
	public double getLatitude() {
		return latitude;
	}

	@Override
	public double getLongitude() {
		return longitude;
	}

	@Override
	public Group getGroup() {
		return Group.BUG;
	}

	public String getAuthor() {
		return author;
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

	public void setAuthor(String author) {
		this.author = author;
	}

	@Override
	public String toString() {
		return "OsmBugs Point " + this.getAction() + " " + this.getText() + " " + this.getAuthor() + " (" + this.getId() + "): [" + " (" + this.getLatitude() + ", " + this.getLongitude() + ")]";
	}
}
