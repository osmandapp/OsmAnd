package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.settings.backend.backup.SettingsItemType;

public abstract class ExportType {

	public ExportType(@StringRes int titleId, @DrawableRes int iconId, @NonNull SettingsItemType settingsItemType) {
		this.titleId = titleId;
		this.iconId = iconId;
		this.settingsItemType = settingsItemType;
	}

	private final int titleId;
	private final int iconId;
	private final SettingsItemType settingsItemType;

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	@NonNull
	public SettingsItemType getSettingsItemType() {
		return settingsItemType;
	}

	public boolean isAllowedInFreeVersion() {
		return ExportTypes.availableInFreeValues().contains(this);
	}

	public boolean isEnabled() {
		return ExportTypes.enabledValues().contains(this);
	}

	public boolean isNotEnabled() {
		return !isEnabled();
	}

	public boolean isMap() {
		return ExportTypes.mapValues().contains(this);
	}

	public boolean isSettingsCategory() {
		return ExportTypes.settingsValues().contains(this);
	}

	public boolean isMyPlacesCategory() {
		return ExportTypes.myPlacesValues().contains(this);
	}

	public boolean isResourcesCategory() {
		return ExportTypes.resourcesValues().contains(this);
	}

	@NonNull
	public abstract String getId();

}
