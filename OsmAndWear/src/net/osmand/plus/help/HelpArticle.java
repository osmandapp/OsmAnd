package net.osmand.plus.help;

import androidx.annotation.NonNull;

import java.util.LinkedHashMap;
import java.util.Map;

public class HelpArticle {

	public final String url;
	public final String label;
	public final Map<String, HelpArticle> articles = new LinkedHashMap<>();
	public final int level;

	public HelpArticle(@NonNull String url, @NonNull String label, int level) {
		this.url = url;
		this.label = label;
		this.level = level;
	}

	@NonNull
	@Override
	public String toString() {
		return label;
	}
}
