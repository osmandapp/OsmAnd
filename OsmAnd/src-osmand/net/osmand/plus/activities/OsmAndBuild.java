package net.osmand.plus.activities;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OsmAndBuild {

	protected static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy", Locale.US);

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

	@NonNull
	@Override
	public @NotNull String toString() {
		String dateStr = date != null ? DATE_FORMAT.format(date) : "N/A";
		return "Tag: " + (tag != null ? tag : "N/A") +
				", Date: " + dateStr +
				", Size: " + (size != null ? size : "N/A");
	}
}

