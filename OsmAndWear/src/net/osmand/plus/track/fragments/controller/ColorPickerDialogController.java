package net.osmand.plus.track.fragments.controller;

import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.track.fragments.ColorPickerBottomSheet;

public class ColorPickerDialogController extends BaseDialogController implements IColorPickerDialogController {

	public static final String PROCESS_ID = "show_color_picker_to_select_custom_color";

	private final Integer initialColor;
	private int selectedColor;
	private ColorPickerListener listener;

	public ColorPickerDialogController(@NonNull OsmandApplication app,
	                                   @Nullable Integer initialColor) {
		super(app);
		this.initialColor = initialColor;
		selectedColor = initialColor != null ? initialColor : Color.RED;
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	@Override
	public int getSelectedColor() {
		return selectedColor;
	}

	@Override
	public boolean onSelectColor(int color) {
		this.selectedColor = color;
		return true;
	}

	public void setListener(@NonNull ColorPickerListener listener) {
		this.listener = listener;
	}

	@Override
	public void onApplyColorSelection() {
		listener.onApplyColorPickerSelection(initialColor, selectedColor);
	}

	public static void showDialog(@NonNull FragmentActivity activity,
	                              @NonNull ColorPickerListener listener, @Nullable Integer initialColor) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		ColorPickerDialogController controller = new ColorPickerDialogController(app, initialColor);
		controller.setListener(listener);

		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(PROCESS_ID, controller);

		FragmentManager manager = activity.getSupportFragmentManager();
		ColorPickerBottomSheet.showInstance(manager, PROCESS_ID);
	}

	public interface ColorPickerListener {
		void onApplyColorPickerSelection(@ColorInt Integer oldColor, @ColorInt int newColor);
	}
}
