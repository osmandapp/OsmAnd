package net.osmand.plus.mapmarkers;

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
import android.widget.ImageView;
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
	private CoordinateInputFormatChangeListener listener;

	public void setListener(CoordinateInputFormatChangeListener listener) {
		this.listener = listener;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		if (args != null) {
			coordinateFormat = getArguments().getInt(CoordinateInputDialogFragment.COORDINATE_FORMAT, -1);
		}
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final MapActivity mapActivity = (MapActivity) getActivity();
		portrait = AndroidUiHelper.isOrientationPortrait(getActivity());
		night = !mapActivity.getMyApplication().getSettings().isLightContent();
		final int themeRes = night ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_marker_coordinate_input_bottom_sheet_dialog, container);
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
		} else {
			degreesIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_coordinates_latitude));
		}
		degreesText.setText(PointDescription.formatToHumanString(getContext(), PointDescription.FORMAT_DEGREES));

		ImageView minutesIcon = (ImageView) mainView.findViewById(R.id.minutes_icon);
		TextView minutesText = (TextView) mainView.findViewById(R.id.minutes_text);
		if (coordinateFormat == PointDescription.FORMAT_MINUTES) {
			minutesIcon.setImageDrawable(getIcon(R.drawable.ic_action_coordinates_latitude, R.color.dashboard_blue));
			minutesText.setTextColor(ContextCompat.getColor(mapActivity, R.color.dashboard_blue));
		} else {
			minutesIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_coordinates_latitude));
		}
		minutesText.setText(PointDescription.formatToHumanString(getContext(), PointDescription.FORMAT_MINUTES));

		ImageView secondsIcon = (ImageView) mainView.findViewById(R.id.seconds_icon);
		TextView secondsText = (TextView) mainView.findViewById(R.id.seconds_text);
		if (coordinateFormat == PointDescription.FORMAT_SECONDS) {
			secondsIcon.setImageDrawable(getIcon(R.drawable.ic_action_coordinates_latitude, R.color.dashboard_blue));
			secondsText.setTextColor(ContextCompat.getColor(mapActivity, R.color.dashboard_blue));
		} else {
			secondsIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_coordinates_latitude));
		}
		secondsText.setText(PointDescription.formatToHumanString(getContext(), PointDescription.FORMAT_SECONDS));

		ImageView utmIcon = (ImageView) mainView.findViewById(R.id.utm_icon);
		TextView utmText = (TextView) mainView.findViewById(R.id.utm_text);
		if (coordinateFormat == PointDescription.UTM_FORMAT) {
			utmIcon.setImageDrawable(getIcon(R.drawable.ic_action_coordinates_latitude, R.color.dashboard_blue));
			utmText.setTextColor(ContextCompat.getColor(mapActivity, R.color.dashboard_blue));
		} else {
			utmIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_coordinates_latitude));
		}
		utmText.setText(PointDescription.formatToHumanString(getContext(), PointDescription.UTM_FORMAT));

		ImageView olcIcon = (ImageView) mainView.findViewById(R.id.olc_icon);
		TextView olcText = (TextView) mainView.findViewById(R.id.olc_text);
		if (coordinateFormat == PointDescription.OLC_FORMAT) {
			olcIcon.setImageDrawable(getIcon(R.drawable.ic_action_coordinates_latitude, R.color.dashboard_blue));
			olcText.setTextColor(ContextCompat.getColor(mapActivity, R.color.dashboard_blue));
		} else {
			olcIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_coordinates_latitude));
		}
		olcText.setText(PointDescription.formatToHumanString(getContext(), PointDescription.OLC_FORMAT));

		View.OnClickListener formatChangeListener = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				int format;
				switch (view.getId()) {
					case R.id.degrees_row:
						format = PointDescription.FORMAT_DEGREES;
						break;
					case R.id.minutes_row:
						format = PointDescription.FORMAT_MINUTES;
						break;
					case R.id.seconds_row:
						format = PointDescription.FORMAT_SECONDS;
						break;
					case R.id.utm_row:
						format = PointDescription.UTM_FORMAT;
						break;
					case R.id.olc_row:
						format = PointDescription.OLC_FORMAT;
						break;
					default:
						throw new IllegalArgumentException("Unsupported format");
				}
				if (listener != null) {
					listener.onCoordinateFormatChanged(format);
				}
				dismiss();
			}
		};

		mainView.findViewById(R.id.degrees_row).setOnClickListener(formatChangeListener);
		mainView.findViewById(R.id.minutes_row).setOnClickListener(formatChangeListener);
		mainView.findViewById(R.id.seconds_row).setOnClickListener(formatChangeListener);
		mainView.findViewById(R.id.utm_row).setOnClickListener(formatChangeListener);
		mainView.findViewById(R.id.olc_row).setOnClickListener(formatChangeListener);

		mainView.findViewById(R.id.cancel_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
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
						AndroidUtils.setBackground(getActivity(), mainView, false,
								R.drawable.bg_bottom_sheet_topsides_landscape_light, R.drawable.bg_bottom_sheet_topsides_landscape_dark);
					} else {
						AndroidUtils.setBackground(getActivity(), mainView, false,
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
	protected Drawable getContentIcon(@DrawableRes int id) {
		return getIcon(id, night ? R.color.ctx_menu_info_text_dark : R.color.on_map_icon_color);
	}

	interface CoordinateInputFormatChangeListener {
		void onCoordinateFormatChanged(int format);
	}
}
