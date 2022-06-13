package net.osmand.plus.plugins.osmedit.data;

import net.osmand.data.LatLon;

public class OsmNotesPoint extends OsmPoint {
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
		return new StringBuffer("OsmBugs Point ").append(this.getAction()).append(" ").append(this.getText())
			.append(" ").append(this.getAuthor())
			.append(" (").append(this.getId()).append("): [")
			.append(" (").append(this.getLatitude()).append(", ").append(this.getLongitude())
			.append(")]").toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof OsmNotesPoint)) return false;
		OsmNotesPoint that = (OsmNotesPoint) o;
		LatLon thisPos = new LatLon(latitude, longitude);
		LatLon thatPos = new LatLon(that.latitude, that.longitude);
		boolean res = thisPos.equals(thatPos);
		if (text != null) {
			res = res && text.equals(that.text);
		}
		return res;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 0;
		int temp;
		temp = (int) Math.floor(latitude * 10000);
		result = prime * result + temp;
		temp = (int) Math.floor(longitude * 10000);
		result = prime * result + temp;
		result = prime * result + (text != null ? text.hashCode() : 0);
		return result;
	}
}
