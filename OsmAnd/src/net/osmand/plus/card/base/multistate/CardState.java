package net.osmand.plus.card.base.multistate;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CardState {

	private final String title;
	private final Object tag;

	public CardState(@NonNull String title, @Nullable Object tag) {
		this.title = title;
		this.tag = tag;
	}

	@NonNull
	public String getTitle() {
		return title;
	}

	@Nullable
	public Object getTag() {
		return tag;
	}
}
