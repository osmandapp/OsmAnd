package net.osmand.plus.mapcontextmenu.editors.data;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class IconData {
	private final String key;
	@DrawableRes
	private final int iconId;
	private final String iconName;

	public IconData(@NonNull String key,
	                @DrawableRes int iconId) {
		this(key, iconId, null);
	}

	public IconData(@NonNull String key,
	                @DrawableRes int iconId,
	                @Nullable String localizedName) {
		this.key = key;
		this.iconId = iconId;
		this.iconName = localizedName;
	}

	@NonNull
	public String getKey() {
		return key;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	@Nullable
	public String getIconName() {
		return iconName;
	}

}
