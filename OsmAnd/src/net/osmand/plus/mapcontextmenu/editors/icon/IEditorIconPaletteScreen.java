package net.osmand.plus.mapcontextmenu.editors.icon;

import androidx.annotation.NonNull;

import net.osmand.plus.mapcontextmenu.editors.icon.data.IconsCategory;

public interface IEditorIconPaletteScreen {

	void updateScreenContent();
	void onScreenModeChanged();
	void askSelectCategory(@NonNull IconsCategory category);
	void dismiss();

}
