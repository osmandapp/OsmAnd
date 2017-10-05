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

public class CoordinateInputBottomSheetDialogFragment extends BottomSheetDialogFragment {

	public final static String TAG = "CoordinateInputBottomSheetDialogFragment";

	private boolean portrait;
	private View mainView;
	private boolean night;
	private int coordinateFormat = -1;
	private boolean useOsmandKeyboard = true;
	private CoordinateInputFormatChangeListener listener;
	private boolean shouldClose;

	public void setListener(CoordinateInputFormatChangeListener listener) {
		this.listener = listener;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		if (args != null) {
			coordinateFormat = args.getInt(CoordinateInputDialogFragment.COORDINATE_FORMAT);
			useOsmandKeyboard = args.getBoolean(CoordinateInputDialogFragment.USE_OSMAND_KEYBOARD);
		} else {
			shouldClose = true;
		}
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final MapActivity mapActivity = (MapActivity) getActivity();
		portrait = AndroidUiHelper.isOrientationPortrait(getActivity());
		night = !mapActivity.getMyApplication().getSettings().isLightContent();
		final int themeRes = night ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), coordinateFormat == -1 ?
				R.layout.fragment_marker_coordinate_input_bottom_sheet_dialog : R.layout.fragment_marker_coordinate_input_options_bottom_sheet_helper, container);
		if (portrait) {
			AndroidUtils.setBackground(getActivity(), mainView, night, R.drawable.bg_bottom_menu_light, R.drawable.bg_bottom_menu_dark);
		}

		if (night) {
			((TextView) mainView.findViewById(R.id.coordinate_input_title)).setTextColor(getResources().getColor(R.color.ctx_menu_info_text_dark));
		}

		ImageView degreesIcon = (ImageView) mainView.findViewById(R.id.degrees_icon);
		TextView degreesText = (TextView) mainView.findViewById(R.id.degrees_text);
		if (coordinateFormat == PointDescription.FORMAT_DEGREES) {
			degreesIcon.setImageDrawable(getIcon(R.drawable.ic_action_coordinates_latitude, R.color.dashboard_blue));
			degreesText.setTextColor(ContextCompat.getColor(mapActivity, R.color.dashboard_blue));
			((RadioButton) mainView.findViewById(R.id.degrees_radio_button)).setChecked(true);
		} else {
			degreesIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_coordinates_latitude));
		}
		degreesText.setText(PointDescription.formatToHumanString(getContext(), PointDescription.FORMAT_DEGREES));

		ImageView minutesIcon = (ImageView) mainView.findViewById(R.id.minutes_icon);
		TextView minutesText = (TextView) mainView.findViewById(R.id.minutes_text);
		if (coordinateFormat == PointDescription.FORMAT_MINUTES) {
			minutesIcon.setImageDrawable(getIcon(R.drawable.ic_action_coordinates_latitude, R.color.dashboard_blue));
			minutesText.setTextColor(ContextCompat.getColor(mapActivity, R.color.dashboard_blue));
			((RadioButton) mainView.findViewById(R.id.minutes_radio_button)).setChecked(true);
		} else {
			minutesIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_coordinates_latitude));
		}
		minutesText.setText(PointDescription.formatToHumanString(getContext(), PointDescription.FORMAT_MINUTES));

		ImageView secondsIcon = (ImageView) mainView.findViewById(R.id.seconds_icon);
		TextView secondsText = (TextView) mainView.findViewById(R.id.seconds_text);
		if (coordinateFormat == PointDescription.FORMAT_SECONDS) {
			secondsIcon.setImageDrawable(getIcon(R.drawable.ic_action_coordinates_latitude, R.color.dashboard_blue));
			secondsText.setTextColor(ContextCompat.getColor(mapActivity, R.color.dashboard_blue));
			((RadioButton) mainView.findViewById(R.id.seconds_radio_button)).setChecked(true);
		} else {
			secondsIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_coordinates_latitude));
		}
		secondsText.setText(PointDescription.formatToHumanString(getContext(), PointDescription.FORMAT_SECONDS));

		if (coordinateFormat != -1) {
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
		}

		View.OnClickListener formatChangeListener = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				highlightSelectedItem(false);
				switch (view.getId()) {
					case R.id.degrees_row:
						coordinateFormat = PointDescription.FORMAT_DEGREES;
						break;
					case R.id.minutes_row:
						coordinateFormat = PointDescription.FORMAT_MINUTES;
						break;
					case R.id.seconds_row:
						coordinateFormat = PointDescription.FORMAT_SECONDS;
						break;
					default:
						throw new IllegalArgumentException("Unsupported format");
				}
				highlightSelectedItem(true);
				if (listener != null) {
					listener.onCoordinateFormatChanged(coordinateFormat);
				}
				if (shouldClose) {
					dismiss();
				}
			}
		};

		mainView.findViewById(R.id.degrees_row).setOnClickListener(formatChangeListener);
		mainView.findViewById(R.id.minutes_row).setOnClickListener(formatChangeListener);
		mainView.findViewById(R.id.seconds_row).setOnClickListener(formatChangeListener);

		mainView.findViewById(R.id.cancel_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
				if (shouldClose && listener != null) {
					listener.onCancel();
				}
			}
		});

		final int screenHeight = AndroidUtils.getScreenHeight(getActivity());
		final int statusBarHeight = AndroidUtils.getStatusBarHeight(getActivity());
		final int navBarHeight = AndroidUtils.getNavBarHeight(getActivity());

		mainView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				final View scrollView = mainView.findViewById(R.id.marker_coordinate_input_scroll_view);
				int scrollViewHeight = scrollView.getHeight();
				int dividerHeight = AndroidUtils.dpToPx(getContext(), 1);
				int cancelButtonHeight = getContext().getResources().getDimensionPixelSize(R.dimen.bottom_sheet_cancel_button_height);
				int spaceForScrollView = screenHeight - statusBarHeight - navBarHeight - dividerHeight - cancelButtonHeight;
				if (scrollViewHeight > spaceForScrollView) {
					scrollView.getLayoutParams().height = spaceForScrollView;
					scrollView.requestLayout();
				}

				if (!portrait) {
					if (screenHeight - statusBarHeight - mainView.getHeight()
							>= AndroidUtils.dpToPx(getActivity(), 8)) {
						AndroidUtils.setBackground(getActivity(), mainView, night,
								R.drawable.bg_bottom_sheet_topsides_landscape_light, R.drawable.bg_bottom_sheet_topsides_landscape_dark);
					} else {
						AndroidUtils.setBackground(getActivity(), mainView, night,
								R.drawable.bg_bottom_sheet_sides_landscape_light, R.drawable.bg_bottom_sheet_sides_landscape_dark);
					}
				}

				ViewTreeObserver obs = mainView.getViewTreeObserver();
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					obs.removeOnGlobalLayoutListener(this);
				} else {
					obs.removeGlobalOnLayoutListener(this);
				}
			}
		});

		return mainView;
	}

	@Override
	public void onStart() {
		super.onStart();
		if (!portrait) {
			final Window window = getDialog().getWindow();
			WindowManager.LayoutParams params = window.getAttributes();
			params.width = getActivity().getResources().getDimensionPixelSize(R.dimen.landscape_bottom_sheet_dialog_fragment_width);
			window.setAttributes(params);
		}
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		super.onCancel(dialog);
		if (shouldClose && listener != null) {
			listener.onCancel();
		}
	}

	@Override
	protected Drawable getContentIcon(@DrawableRes int id) {
		return getIcon(id, night ? R.color.ctx_menu_info_text_dark : R.color.on_map_icon_color);
	}

	private void highlightSelectedItem(boolean check) {
		int iconColor = check ? R.color.dashboard_blue : night ? R.color.ctx_menu_info_text_dark : R.color.on_map_icon_color;
		int textColor = ContextCompat.getColor(getContext(), check ? (night ? R.color.color_dialog_buttons_dark : R.color.dashboard_blue) : night ? R.color.color_white : R.color.color_black);
		switch (coordinateFormat) {
			case PointDescription.FORMAT_DEGREES:
				((TextView) mainView.findViewById(R.id.degrees_text)).setTextColor(textColor);
				((ImageView) mainView.findViewById(R.id.degrees_icon)).setImageDrawable((getIcon(R.drawable.ic_action_coordinates_latitude, iconColor)));
				((RadioButton) mainView.findViewById(R.id.degrees_radio_button)).setChecked(check);
				break;
			case PointDescription.FORMAT_MINUTES:
				((TextView) mainView.findViewById(R.id.minutes_text)).setTextColor(textColor);
				((ImageView) mainView.findViewById(R.id.minutes_icon)).setImageDrawable((getIcon(R.drawable.ic_action_coordinates_latitude, iconColor)));
				((RadioButton) mainView.findViewById(R.id.minutes_radio_button)).setChecked(check);
				break;
			case PointDescription.FORMAT_SECONDS:
				((TextView) mainView.findViewById(R.id.seconds_text)).setTextColor(textColor);
				((ImageView) mainView.findViewById(R.id.seconds_icon)).setImageDrawable((getIcon(R.drawable.ic_action_coordinates_latitude, iconColor)));
				((RadioButton) mainView.findViewById(R.id.seconds_radio_button)).setChecked(check);
				break;
		}
	}

	interface CoordinateInputFormatChangeListener {

		void onCoordinateFormatChanged(int format);

		void onKeyboardChanged(boolean useOsmandKeyboard);

		void onCancel();

	}
}
