package net.osmand.plus.mapmarkers;

import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;

public class CoordinateInputBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public final static String TAG = "CoordinateInputBottomSheetDialogFragment";

	private View mainView;
	private boolean useOsmandKeyboard = true;
	private boolean goToNextField;
	private int accuracy = -1;
	private CoordinateInputFormatChangeListener listener;

	public void setListener(CoordinateInputFormatChangeListener listener) {
		this.listener = listener;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		if (args != null) {
			useOsmandKeyboard = args.getBoolean(CoordinateInputDialogFragment.USE_OSMAND_KEYBOARD);
			goToNextField = args.getBoolean(CoordinateInputDialogFragment.GO_TO_NEXT_FIELD);
			accuracy = args.getInt(CoordinateInputDialogFragment.ACCURACY);
		}
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final MapActivity mapActivity = (MapActivity) getActivity();
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_marker_coordinate_input_options_bottom_sheet_helper, container);
		if (portrait) {
			AndroidUtils.setBackground(getActivity(), mainView, night, R.drawable.bg_bottom_menu_light, R.drawable.bg_bottom_menu_dark);
		}

		if (nightMode) {
			((TextView) mainView.findViewById(R.id.coordinate_input_title)).setTextColor(getResources().getColor(R.color.ctx_menu_info_text_dark));
		}

		((TextView) mainView.findViewById(R.id.coordinate_input_accuracy_descr)).setText(getString(R.string.coordinate_input_accuracy_description, accuracy));

		((CompoundButton) mainView.findViewById(R.id.go_to_next_field_switch)).setChecked(goToNextField);
		((ImageView) mainView.findViewById(R.id.go_to_next_field_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_keyboard));
		mainView.findViewById(R.id.go_to_next_field_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				goToNextField = !goToNextField;
				((CompoundButton) mainView.findViewById(R.id.go_to_next_field_switch)).setChecked(goToNextField);
				if (listener != null) {
					listener.onGoToNextFieldChanged(goToNextField);
				}
			}
		});

		if (accuracy == 4) {
			((RadioButton) mainView.findViewById(R.id.four_digits_radio_button)).setChecked(true);
		}
		((TextView) mainView.findViewById(R.id.four_digits_title)).setText(getString(R.string.coordinate_input_accuracy, 4));
		((TextView) mainView.findViewById(R.id.four_digits_description)).setText("00:00." + "5555");

		if (accuracy == 3) {
			((RadioButton) mainView.findViewById(R.id.three_digits_radio_button)).setChecked(true);
		}
		((TextView) mainView.findViewById(R.id.three_digits_title)).setText(getString(R.string.coordinate_input_accuracy, 3));
		((TextView) mainView.findViewById(R.id.three_digits_description)).setText("00:00." + "555");

		if (accuracy == 2) {
			((RadioButton) mainView.findViewById(R.id.two_digits_radio_button)).setChecked(true);
		}
		((TextView) mainView.findViewById(R.id.two_digits_title)).setText(getString(R.string.coordinate_input_accuracy, 2));
		((TextView) mainView.findViewById(R.id.two_digits_description)).setText("00:00." + "55");

		((CompoundButton) mainView.findViewById(R.id.use_system_keyboard_switch)).setChecked(!useOsmandKeyboard);
		((ImageView) mainView.findViewById(R.id.use_system_keyboard_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_keyboard));
		mainView.findViewById(R.id.use_system_keyboard_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				useOsmandKeyboard = !useOsmandKeyboard;
				((CompoundButton) mainView.findViewById(R.id.use_system_keyboard_switch)).setChecked(!useOsmandKeyboard);
				if (listener != null) {
					listener.onKeyboardChanged(useOsmandKeyboard);
				}
			}
		});
		highlightSelectedItem(true);

		View.OnClickListener accuracyChangedListener = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				highlightSelectedItem(false);
				switch (view.getId()) {
					case R.id.four_digits_row:
						accuracy = 4;
						break;
					case R.id.three_digits_row:
						accuracy = 3;
						break;
					case R.id.two_digits_row:
						accuracy = 2;
						break;
					default:
						throw new IllegalArgumentException("Unsupported accuracy");
				}
				highlightSelectedItem(true);
				if (listener != null) {
					listener.onAccuracyChanged(accuracy);
				}
			}
		};

		mainView.findViewById(R.id.four_digits_row).setOnClickListener(accuracyChangedListener);
		mainView.findViewById(R.id.three_digits_row).setOnClickListener(accuracyChangedListener);
		mainView.findViewById(R.id.two_digits_row).setOnClickListener(accuracyChangedListener);

		mainView.findViewById(R.id.cancel_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		setupHeightAndBackground(mainView, R.id.marker_coordinate_input_scroll_view);

		return mainView;
	}

	@Override
	protected Drawable getContentIcon(@DrawableRes int id) {
		return getIcon(id, night ? R.color.ctx_menu_info_text_dark : R.color.on_map_icon_color);
	}

	private void highlightSelectedItem(boolean check) {
		switch (accuracy) {
			case 4:
				((RadioButton) mainView.findViewById(R.id.four_digits_radio_button)).setChecked(check);
				break;
			case 3:
				((RadioButton) mainView.findViewById(R.id.three_digits_radio_button)).setChecked(check);
				break;
			case 2:
				((RadioButton) mainView.findViewById(R.id.two_digits_radio_button)).setChecked(check);
				break;
		}
	}

	interface CoordinateInputFormatChangeListener {

		void onKeyboardChanged(boolean useOsmandKeyboard);

		void onGoToNextFieldChanged(boolean goToNextField);

		void onAccuracyChanged(int accuracy);

	}
}
