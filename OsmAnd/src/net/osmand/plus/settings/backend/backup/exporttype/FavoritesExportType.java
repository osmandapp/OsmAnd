package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.SettingsItemType;

class FavoritesExportType extends ExportType {

	public FavoritesExportType() {
		super(R.string.shared_string_favorites, R.drawable.ic_action_favorite, SettingsItemType.FAVOURITES);
	}

	@NonNull
	@Override
	public String getId() {
		return "FAVORITES";
	}
}
