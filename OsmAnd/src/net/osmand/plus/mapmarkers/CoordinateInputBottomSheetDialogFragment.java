package net.osmand.plus.mapmarkers;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.ListPopupWindow;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;

import static net.osmand.plus.mapmarkers.CoordinateInputDialogFragment.ACCURACY;
import static net.osmand.plus.mapmarkers.CoordinateInputDialogFragment.GO_TO_NEXT_FIELD;
import static net.osmand.plus.mapmarkers.CoordinateInputDialogFragment.RIGHT_HAND;
import static net.osmand.plus.mapmarkers.CoordinateInputDialogFragment.USE_OSMAND_KEYBOARD;

public class CoordinateInputBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public final static String TAG = "CoordinateInputBottomSheetDialogFragment";

	private View mainView;
	private boolean useOsmandKeyboard;
	private boolean rightHand;
	private boolean goToNextField;
	private int accuracy;
	private CoordinateInputFormatChangeListener listener;

	public void setListener(CoordinateInputFormatChangeListener listener) {
		this.listener = listener;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			Bundle args = getArguments();
			if (args != null) {
				useOsmandKeyboard = args.getBoolean(USE_OSMAND_KEYBOARD);
				rightHand = args.getBoolean(RIGHT_HAND);
				goToNextField = args.getBoolean(GO_TO_NEXT_FIELD);
				accuracy = args.getInt(ACCURACY);
			}
		} else {
			useOsmandKeyboard = savedInstanceState.getBoolean(USE_OSMAND_KEYBOARD);
			rightHand = savedInstanceState.getBoolean(RIGHT_HAND);
			goToNextField = savedInstanceState.getBoolean(GO_TO_NEXT_FIELD);
			accuracy = savedInstanceState.getInt(ACCURACY);
		}
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		boolean portrait = AndroidUiHelper.isOrientationPortrait(getActivity());

		mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_marker_coordinate_input_options_bottom_sheet_helper, container);

		if (nightMode) {
			((TextView) mainView.findViewById(R.id.coordinate_input_title)).setTextColor(getResources().getColor(R.color.ctx_menu_info_text_dark));
		}

		((TextView) mainView.findViewById(R.id.coordinate_input_accuracy_descr)).setText(getString(R.string.coordinate_input_accuracy_description, accuracy));

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

		View handRow = mainView.findViewById(R.id.hand_row);
		if (portrait) {
			handRow.setVisibility(View.GONE);
		} else {
			handRow.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					rightHand = !rightHand;
					populateChangeHandRow();
					if (listener != null) {
						listener.onHandChanged(rightHand);
					}
				}
			});
			populateChangeHandRow();
		}

		((CompoundButton) mainView.findViewById(R.id.go_to_next_field_switch)).setChecked(goToNextField);
		((ImageView) mainView.findViewById(R.id.go_to_next_field_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_next_field_stroke));
		mainView.findViewById(R.id.go_to_next_field_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				goToNextField = !goToNextField;
				((CompoundButton) mainView.findViewById(R.id.go_to_next_field_switch)).setChecked(goToNextField);
				switchSelectedAccuracy();
				if (listener != null) {
					listener.onGoToNextFieldChanged(goToNextField);
				}
			}
		});

		switchSelectedAccuracy();
		populateSelectedAccuracy();

		mainView.findViewById(R.id.accuracy_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (goToNextField) {
					final ListPopupWindow listPopupWindow = new ListPopupWindow(getContext());
					listPopupWindow.setAnchorView(view);
					listPopupWindow.setContentWidth(AndroidUtils.dpToPx(getMyApplication(), 100));
					listPopupWindow.setModal(true);
					listPopupWindow.setDropDownGravity(Gravity.END | Gravity.TOP);
					listPopupWindow.setAdapter(new ArrayAdapter<>(getContext(), R.layout.popup_list_text_item, new Integer[]{0, 1, 2, 3, 4, 5, 6}));
					listPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
						@Override
						public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
							accuracy = i;
							populateSelectedAccuracy();
							if (listener != null) {
								listener.onAccuracyChanged(accuracy);
							}
							listPopupWindow.dismiss();
						}
					});
					listPopupWindow.show();
				}
			}
		});

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
	public void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(USE_OSMAND_KEYBOARD, useOsmandKeyboard);
		outState.putBoolean(RIGHT_HAND, rightHand);
		outState.putBoolean(GO_TO_NEXT_FIELD, goToNextField);
		outState.putInt(ACCURACY, accuracy);
		super.onSaveInstanceState(outState);
	}

	private void populateChangeHandRow() {
		((ImageView) mainView.findViewById(R.id.hand_icon)).setImageDrawable(getContentIcon(rightHand ? R.drawable.ic_action_show_keypad_right : R.drawable.ic_action_show_keypad_left));
		((TextView) mainView.findViewById(R.id.hand_text_view)).setText(getString(rightHand ? R.string.shared_string_right : R.string.shared_string_left));
		((TextView) mainView.findViewById(R.id.hand_text_view)).setTextColor(ContextCompat.getColor(getContext(), nightMode ? R.color.color_dialog_buttons_dark : R.color.map_widget_blue_pressed));
	}

	private void populateSelectedAccuracy() {
		((TextView) mainView.findViewById(R.id.selected_accuracy)).setText(String.valueOf(accuracy));
		((TextView) mainView.findViewById(R.id.selected_accuracy_hint)).setText("00:00." + new String(new char[accuracy]).replace("\0", "0"));
	}

	private void switchSelectedAccuracy() {
		((TextView) mainView.findViewById(R.id.selected_accuracy)).setTextColor(ContextCompat.getColor(getContext(), goToNextField ? R.color.map_widget_blue : android.R.color.darker_gray));
		((ImageView) mainView.findViewById(R.id.accuracy_arrow)).setImageDrawable(goToNextField ? getContentIcon(R.drawable.ic_action_arrow_drop_down) : getIcon(R.drawable.ic_action_arrow_drop_down, android.R.color.darker_gray));
	}

	interface CoordinateInputFormatChangeListener {

		void onKeyboardChanged(boolean useOsmandKeyboard);

		void onHandChanged(boolean rightHand);

		void onGoToNextFieldChanged(boolean goToNextField);

		void onAccuracyChanged(int accuracy);

	}
}
