package net.osmand.plus.settings.vehiclespecs.containers;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.base.containers.ThemedIconId;

public class Assets {

	private final ThemedIconId themedIconId;
	private final int descriptionId;

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
