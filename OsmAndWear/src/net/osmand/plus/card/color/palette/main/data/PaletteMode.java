package net.osmand.plus.card.color.palette.main.data;

import androidx.annotation.NonNull;

public class PaletteMode {

	private final String title;
	private final Object tag;

	public PaletteMode(@NonNull String title, @NonNull Object tag) {
		this.title = title;
		this.tag = tag;
	}

	@NonNull
	public Object getTag() {
		return tag;
	}

	@NonNull
	public String getTitle() {
		return title;
	}
}
