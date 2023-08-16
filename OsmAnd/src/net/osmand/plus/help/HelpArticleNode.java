package net.osmand.plus.help;

import androidx.annotation.NonNull;

import java.util.LinkedHashMap;
import java.util.Map;

public class HelpArticleNode {

	public final String url;
	public final Map<String, HelpArticleNode> articles = new LinkedHashMap<>();

	public HelpArticleNode(@NonNull String url) {
		this.url = url;
	}
}
