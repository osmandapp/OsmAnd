package net.osmand.plus.track.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.jaredrummler.android.colorpicker.ColorPanelView;
import com.jaredrummler.android.colorpicker.ColorPickerView;
import com.jaredrummler.android.colorpicker.ColorPickerView.OnColorChangedListener;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.bottomsheets.CustomizableBottomSheet;
import net.osmand.plus.track.fragments.controller.IColorPickerDialogController;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

public class ColorPickerBottomSheet extends CustomizableBottomSheet implements OnColorChangedListener {

	private static final String TAG = ColorPickerBottomSheet.class.getSimpleName();

	private static final Log LOG = PlatformUtil.getLog(ColorPickerBottomSheet.class);

	private ColorPickerView colorPicker;
	private ColorPanelView colorPanel;

	private EditText hexEditText;
	private boolean fromEditText;

	private IColorPickerDialogController controller;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		controller = (IColorPickerDialogController) manager.findController(processId);
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.select_color)));

		items.add(new SimpleBottomSheetItem.Builder().setCustomView(createPickerView()).create());
	}

	private View createPickerView() {
		View colorView = inflate(R.layout.custom_color_picker);
		colorPicker = colorView.findViewById(R.id.color_picker_view);
		colorPanel = colorView.findViewById(R.id.color_panel_new);
		hexEditText = colorView.findViewById(R.id.color_hex_edit_text);

		int selectedColor = controller.getSelectedColor();
		setHex(selectedColor);
		colorPanel.setColor(selectedColor);
		colorPicker.setColor(selectedColor, true);
		colorPicker.setOnColorChangedListener(this);
		hexEditText.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				if (hexEditText.isFocused()) {
					int color = colorPicker.getColor();
					try {
						color = Algorithms.parseColor("#" + s.toString());
					} catch (IllegalArgumentException e) {
						hexEditText.setError(getString(R.string.wrong_input));
						LOG.error(e);
					}
					if (color != colorPicker.getColor()) {
						fromEditText = true;
						colorPicker.setColor(color, true);
					}
				}
			}
		});
		return colorView;
	}

	@Override
	public void onColorChanged(int newColor) {
		if (controller.onSelectColor(newColor)) {
			if (colorPanel != null) {
				colorPanel.setColor(newColor);
			}
			if (!fromEditText && hexEditText != null) {
				setHex(newColor);
				Activity activity = getActivity();
				if (activity != null && hexEditText.hasFocus()) {
					AndroidUtils.hideSoftKeyboard(activity, hexEditText);
					hexEditText.clearFocus();
				}
			}
			fromEditText = false;
		}
	}

	private void setHex(int color) {
		hexEditText.setText(String.format("%08X", color));
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Override
	protected void onRightBottomButtonClick() {
		controller.onApplyColorSelection();
		dismiss();
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull String processId) {
		try {
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				ColorPickerBottomSheet fragment = new ColorPickerBottomSheet();
				fragment.setProcessId(processId);
				fragment.show(manager, TAG);
			}
		} catch (RuntimeException e) {
			LOG.error(e);
		}
	}
}
