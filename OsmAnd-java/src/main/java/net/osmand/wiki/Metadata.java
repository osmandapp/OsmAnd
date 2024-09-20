package net.osmand.wiki;

import java.util.Map;

public class Metadata {
	private String date;
	private String author;
	private String license;

	public void setDate(String date) {
		this.date = date;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public void setLicense(String license) {
		this.license = license;
	}

	public String getDate() {
		return date;
	}

	public String getAuthor() {
		return author;
	}

	public String getLicense() {
		return license;
	}

	public void parse(Map<String, Object> image){
		String date = (String) image.get("date");
		if (date != null) {
			setDate(date);
		}
		String author = (String) image.get("author");
		if (date != null) {
			setAuthor(author);
		}
		String license = (String) image.get("license");
		if (date != null) {
			setLicense(license);
		}
	}
}
