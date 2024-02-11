package net.osmand.plus.card.color.palette;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.plus.track.fragments.controller.ColorPickerDialogController.ColorPickerListener;

import java.util.List;

public interface IColorsPaletteUIController extends IDialogController, ColorPickerListener {

	String ALL_COLORS_PROCESS_ID = "show_all_colors_palette";

	void bindCard(@NonNull ColorsPaletteCard card);

	void setColorsPaletteListener(@NonNull OnColorsPaletteListener onColorsPaletteListener);

	@ColorInt
	int getControlsAccentColor();

	boolean onSelectColorFromPalette(@ColorInt int color);

	void onColorItemLongClicked(@NonNull FragmentActivity activity, @ColorInt int color);

	void onAllColorsButtonClicked(@NonNull FragmentActivity activity);

	void onAddColorButtonClicked(@NonNull FragmentActivity activity);

	@NonNull
	List<Integer> getAllColors();

	int getSelectedColor();

	boolean isCustomColor(@ColorInt int color);

}
