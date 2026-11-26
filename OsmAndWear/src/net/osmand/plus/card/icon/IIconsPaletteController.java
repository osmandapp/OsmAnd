package net.osmand.plus.card.icon;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;

import java.util.List;

public interface IIconsPaletteController<IconData> extends IDialogController {

	String ALL_ICONS_PROCESS_ID = "show_all_icons_palette";

	void bindPalette(@NonNull IIconsPalette<IconData> palette);

	void unbindPalette(@NonNull IIconsPalette<IconData> palette);
	default void onAllIconsScreenClosed() { }

	void setPaletteListener(@NonNull OnIconsPaletteListener<IconData> onIconsPaletteListener);

	@ColorInt
	int getIconsAccentColor(boolean nightMode);

	@ColorInt
	int getControlsAccentColor(boolean nightMode);

	boolean isAccentColorCanBeChanged();

	void onSelectIconFromPalette(@NonNull IconData icon);

	void selectIcon(@NonNull IconData icon);

	void onAllIconsButtonClicked(@NonNull FragmentActivity activity);

	@NonNull
	List<IconData> getIcons();

	@Nullable
	IconData getSelectedIcon();

	boolean isSelectedIcon(@NonNull IconData icon);

	@NonNull
	IconsPaletteElements<IconData> getPaletteElements(@NonNull Context context, boolean nightMode);

	@Nullable
	String getPaletteTitle();
	int getHorizontalIconsSpace();
	int getRecycleViewHorizontalPadding();
}
