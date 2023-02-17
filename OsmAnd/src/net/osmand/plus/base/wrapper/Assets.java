package net.osmand.plus.base.wrapper;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

public class Assets {

	private ThemedIconId themedIconId;
	private int descriptionId;

	public Assets(@NonNull ThemedIconId themedIconId, int descriptionId) {
		this.themedIconId = themedIconId;
		this.descriptionId = descriptionId;
	}

	@DrawableRes
	public int getIconId(boolean nightMode) {
		return themedIconId.getIconId(nightMode);
	}

	@StringRes
	public int getDescriptionId() {
		return descriptionId;
	}
}
