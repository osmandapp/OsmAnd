package net.osmand.plus.mapcontextmenu;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

public abstract class BottomButtonController {

	protected final MenuController controller;

	@DrawableRes
	protected final int iconId;
	protected final String caption;

	public BottomButtonController(@NonNull MenuController controller,
	                              @DrawableRes int iconId, @StringRes int captionId) {
		this.controller = controller;
		this.iconId = iconId;
		this.caption = controller.getString(captionId);
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	@NonNull
	public String getCaption() {
		return caption;
	}

	public abstract void buttonPressed();
}
