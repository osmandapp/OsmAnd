package net.osmand.plus.wikipedia;

import net.osmand.plus.R;

public enum WikiArticleShowImages {
	ON(R.string.shared_string_on),
	OFF(R.string.shared_string_off),
	WIFI(R.string.shared_string_wifi_only);

	public final int name;

	WikiArticleShowImages(int name) {
		this.name = name;
	}
}
