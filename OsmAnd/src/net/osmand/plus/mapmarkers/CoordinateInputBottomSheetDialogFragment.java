package net.osmand.plus.mapmarkers;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CompoundButtonCompat;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapmarkers.CoordinateInputFormats.CoordinateInputFormatDef;

import static net.osmand.plus.mapmarkers.CoordinateInputDialogFragment.USE_OSMAND_KEYBOARD;

public class CoordinateInputBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public final static String TAG = "CoordinateInputBottomSheetDialogFragment";

	private CoordinateInputFormatChangeListener listener;

	private boolean useOsmandKeyboard;

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
			}
		} else {
			useOsmandKeyboard = savedInstanceState.getBoolean(USE_OSMAND_KEYBOARD);
		}
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final Context context = getContext();
		final OsmandSettings settings = getMyApplication().getSettings();
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		boolean portrait = AndroidUiHelper.isOrientationPortrait(getActivity());

		View mainView = View.inflate(new ContextThemeWrapper(context, themeRes),
				R.layout.fragment_marker_coordinate_input_options_bottom_sheet_dialog, container);

		if (nightMode) {
			((TextView) mainView.findViewById(R.id.coordinate_input_title))
					.setTextColor(ContextCompat.getColor(context, R.color.ctx_menu_info_text_dark));
		}

		((ImageView) mainView.findViewById(R.id.use_system_keyboard_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_keyboard));
		((CompoundButton) mainView.findViewById(R.id.use_system_keyboard_switch)).setChecked(!useOsmandKeyboard);

		View.OnClickListener itemsOnClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (listener != null) {
					int id = v.getId();
					if (id == R.id.use_system_keyboard_row) {
						listener.onKeyboardChanged(!useOsmandKeyboard);
					} else if (id == R.id.hand_row) {
						OsmandSettings.CommonPreference<Boolean> pref = settings.COORDS_INPUT_USE_RIGHT_SIDE;
						pref.set(!pref.get());
						listener.onHandChanged();
					}
				}
				dismiss();
			}
		};

		mainView.findViewById(R.id.use_system_keyboard_row).setOnClickListener(itemsOnClickListener);
		mainView.findViewById(R.id.cancel_row).setOnClickListener(itemsOnClickListener);

		View handRow = mainView.findViewById(R.id.hand_row);
		if (portrait) {
			handRow.setVisibility(View.GONE);
		} else {
			boolean rightHand = settings.COORDS_INPUT_USE_RIGHT_SIDE.get();
			((ImageView) mainView.findViewById(R.id.hand_icon)).setImageDrawable(getContentIcon(rightHand
					? R.drawable.ic_action_show_keypad_right : R.drawable.ic_action_show_keypad_left));
			((TextView) mainView.findViewById(R.id.hand_text_view)).setText(getString(rightHand
					? R.string.shared_string_right : R.string.shared_string_left));
			((TextView) mainView.findViewById(R.id.hand_text_view)).setTextColor(getResolvedActiveColor());
			handRow.setOnClickListener(itemsOnClickListener);
		}

		View.OnClickListener formatsOnClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int format = (int) v.getTag();
				settings.COORDS_INPUT_FORMAT.set(format);
				if (listener != null) {
					listener.onFormatChanged();
				}
				dismiss();
			}
		};

		LinearLayout formatsContainer = (LinearLayout) mainView.findViewById(R.id.formats_container);
		Drawable formatIcon = getContentIcon(R.drawable.ic_action_coordinates_latitude);
		int selectedFormat = settings.COORDS_INPUT_FORMAT.get();
		for (@CoordinateInputFormatDef int format : CoordinateInputFormats.VALUES) {
			boolean selectedRow = format == selectedFormat;

			View row = View.inflate(new ContextThemeWrapper(context, themeRes),
					R.layout.bottom_sheet_item_with_radio_btn, null);
			row.setTag(format);
			row.setOnClickListener(formatsOnClickListener);

			((ImageView) row.findViewById(R.id.icon_iv)).setImageDrawable(selectedRow
					? getActiveIcon(R.drawable.ic_action_coordinates_latitude) : formatIcon);
			TextView nameTv = (TextView) row.findViewById(R.id.name_tv);
			nameTv.setText(CoordinateInputFormats.formatToHumanString(context, format));
			if (selectedRow) {
				nameTv.setTextColor(getResolvedActiveColor());
				RadioButton rb = (RadioButton) row.findViewById(R.id.radio_button);
				rb.setChecked(true);
				CompoundButtonCompat.setButtonTintList(rb, ColorStateList.valueOf(getResolvedActiveColor()));
			}

			formatsContainer.addView(row);
		}

		setupHeightAndBackground(mainView, R.id.marker_coordinate_input_scroll_view);

		return mainView;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(USE_OSMAND_KEYBOARD, useOsmandKeyboard);
		super.onSaveInstanceState(outState);
	}

	@ColorInt
	private int getResolvedActiveColor() {
		return ContextCompat.getColor(getContext(), nightMode ? R.color.osmand_orange : R.color.color_myloc_distance);
	}

	interface CoordinateInputFormatChangeListener {

		void onKeyboardChanged(boolean useOsmandKeyboard);

		void onHandChanged();

		void onFormatChanged();
	}
}
