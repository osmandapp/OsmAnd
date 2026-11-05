package net.osmand.plus.mapcontextmenu.builders.cards.dialogs;

import android.os.Bundle;
import android.view.Menu;
import android.view.View;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.mapillary.MapillaryImageDialog;
import net.osmand.plus.plugins.mapillary.MapillaryPlugin;

import androidx.annotation.NonNull;

public abstract class ContextMenuCardDialog {

	private final MapActivity mapActivity;

	private static final String KEY_CARD_DIALOG_TYPE = "key_card_dialog_type";
	private static final String KEY_CARD_DIALOG_TITLE = "key_card_dialog_title";
	private static final String KEY_CARD_DIALOG_DESCRIPTION = "key_card_dialog_description";

	private final CardDialogType type;
	protected String title;
	protected String description;

	private final boolean portrait;

	public enum CardDialogType {
		REGULAR,
		MAPILLARY
	}

	protected ContextMenuCardDialog(MapActivity mapActivity, @NonNull CardDialogType type) {
		this.mapActivity = mapActivity;
		this.type = type;
		this.portrait = isOrientationPortrait();
	}

	public MapActivity getMapActivity() {
		return mapActivity;
	}

	public boolean isPortrait() {
		return portrait;
	}

	public CardDialogType getType() {
		return type;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	protected boolean haveMenuItems() {
		return false;
	}

	protected void createMenuItems(Menu menu) {
	}

	public void saveMenu(Bundle bundle) {
		bundle.putString(KEY_CARD_DIALOG_TYPE, type.name());
		if (title != null) {
			bundle.putString(KEY_CARD_DIALOG_TITLE, title);
		}
		if (description != null) {
			bundle.putString(KEY_CARD_DIALOG_DESCRIPTION, description);
		}
	}

	protected void restoreFields(Bundle bundle) {
		this.title = bundle.getString(KEY_CARD_DIALOG_TITLE);
		this.description = bundle.getString(KEY_CARD_DIALOG_TITLE);
	}

	static ContextMenuCardDialog restoreMenu(@NonNull Bundle bundle, @NonNull MapActivity mapActivity) {

		try {
			CardDialogType type = CardDialogType.valueOf(bundle.getString(KEY_CARD_DIALOG_TYPE));
			ContextMenuCardDialog dialog = null;
			switch (type) {
				case MAPILLARY:
					dialog = new MapillaryImageDialog(mapActivity, bundle);
					break;
				case REGULAR:
					break;
			}
			return dialog;
		} catch (Exception e) {
			return null;
		}
	}

	public void onResume() {
		updateLayers(true);
	}

	public void onPause() {
		updateLayers(false);
	}

	protected boolean isOrientationPortrait() {
		return AndroidUiHelper.isOrientationPortrait(mapActivity);
	}

	public abstract View getContentView();

	private void updateLayers(boolean activate) {
		MapActivity mapActivity = getMapActivity();
		switch (type) {
			case MAPILLARY: {
				MapillaryPlugin plugin = PluginsHelper.getPlugin(MapillaryPlugin.class);
				if (plugin != null && !plugin.SHOW_MAPILLARY.get()) {
					plugin.updateLayers(mapActivity, mapActivity, activate);
				}
				break;
			}
			case REGULAR:
				break;
		}
	}
}
