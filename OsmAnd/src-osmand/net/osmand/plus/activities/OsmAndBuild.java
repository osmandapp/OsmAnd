package net.osmand.plus.activities;

import java.util.Date;

public class OsmAndBuild {

	public String path;
	public String size;
	public Date date;
	public String tag;

	public OsmAndBuild(String path, String size, Date date, String tag) {
		this.path = path;
		this.size = size;
		this.date = date;
		this.tag = tag;
	}
}
