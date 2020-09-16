package net.osmand.plus.track;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.jaredrummler.android.colorpicker.ColorPanelView;
import com.jaredrummler.android.colorpicker.ColorPickerView;
import com.jaredrummler.android.colorpicker.ColorPickerView.OnColorChangedListener;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

public class CustomColorBottomSheet extends MenuBottomSheetDialogFragment implements OnColorChangedListener {

	private static final String TAG = CustomColorBottomSheet.class.getSimpleName();

	private static final Log log = PlatformUtil.getLog(CustomColorBottomSheet.class);

	private static final String NEW_SELECTED_COLOR = "new_selected_color";
	private static final String PREV_SELECTED_COLOR = "prev_selected_color";

	private ColorPickerView colorPicker;
	private ColorPanelView newColorPanel;

	private EditText hexEditText;
	private boolean fromEditText;

	@ColorInt
	private Integer prevColor;
	@ColorInt
	private int newColor;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			newColor = savedInstanceState.getInt(NEW_SELECTED_COLOR);
			if (savedInstanceState.containsKey(PREV_SELECTED_COLOR)) {
				prevColor = savedInstanceState.getInt(PREV_SELECTED_COLOR);
			}
		} else {
			Bundle args = getArguments();
			if (args != null) {
				if (args.containsKey(PREV_SELECTED_COLOR)) {
					prevColor = args.getInt(PREV_SELECTED_COLOR);
					newColor = prevColor;
				} else {
					newColor = Color.RED;
				}
			}
		}

		items.add(new TitleItem(getString(R.string.select_color)));

		BaseBottomSheetItem item = new SimpleBottomSheetItem.Builder()
				.setCustomView(createPickerView())
				.create();
		items.add(item);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt(NEW_SELECTED_COLOR, newColor);
		if (prevColor != null) {
			outState.putInt(PREV_SELECTED_COLOR, prevColor);
		}
		super.onSaveInstanceState(outState);
	}

	private View createPickerView() {
		LayoutInflater themedInflater = UiUtilities.getInflater(getActivity(), nightMode);
		View colorView = themedInflater.inflate(R.layout.custom_color_picker, null);
		colorPicker = colorView.findViewById(R.id.color_picker_view);
		newColorPanel = colorView.findViewById(R.id.color_panel_new);
		hexEditText = colorView.findViewById(R.id.color_hex_edit_text);

		setHex(newColor);
		newColorPanel.setColor(newColor);
		colorPicker.setColor(newColor, true);
		colorPicker.setOnColorChangedListener(this);
		hexEditText.addTextChangedListener(new TextWatcher() {

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				if (hexEditText.isFocused()) {
					int color = colorPicker.getColor();
					try {
						color = Algorithms.parseColor("#" + s.toString());
					} catch (IllegalArgumentException e) {
						hexEditText.setError(getString(R.string.wrong_input));
						log.error(e);
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
		this.newColor = newColor;
		if (newColorPanel != null) {
			newColorPanel.setColor(newColor);
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

	private void setHex(int color) {
		hexEditText.setText(String.format("%08X", color));
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Override
	protected void onRightBottomButtonClick() {
		Fragment target = getTargetFragment();
		if (target instanceof ColorPickerListener) {
			((ColorPickerListener) target).onColorSelected(prevColor, newColor);
		}
		dismiss();
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, Fragment target, @Nullable Integer prevColor) {
		try {
			if (!fragmentManager.isStateSaved() && fragmentManager.findFragmentByTag(CustomColorBottomSheet.TAG) == null) {
				Bundle args = new Bundle();
				if (prevColor != null) {
					args.putInt(PREV_SELECTED_COLOR, prevColor);
				}

				CustomColorBottomSheet customColorBottomSheet = new CustomColorBottomSheet();
				customColorBottomSheet.setArguments(args);
				customColorBottomSheet.setTargetFragment(target, 0);
				customColorBottomSheet.show(fragmentManager, CustomColorBottomSheet.TAG);
			}
		} catch (RuntimeException e) {
			log.error(e);
		}
	}

	public interface ColorPickerListener {

		void onColorSelected(@ColorInt Integer prevColor, @ColorInt int newColor);

	}
}