package net.osmand.wiki;

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
}
