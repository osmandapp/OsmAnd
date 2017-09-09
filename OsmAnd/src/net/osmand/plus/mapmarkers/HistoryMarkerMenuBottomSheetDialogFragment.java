package net.osmand.plus.mapmarkers;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HistoryMarkerMenuBottomSheetDialogFragment extends BottomSheetDialogFragment {

	public final static String TAG = "HistoryMarkerMenuBottomSheetDialogFragment";

	public static final String MARKER_POSITION = "marker_position";
	public static final String MARKER_NAME = "marker_name";
	public static final String MARKER_COLOR_INDEX = "marker_color_index";
	public static final String MARKER_VISITED_DATE = "marker_visited_date";

	private HistoryMarkerMenuFragmentListener listener;
	private boolean portrait;

	public void setListener(HistoryMarkerMenuFragmentListener listener) {
		this.listener = listener;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		portrait = AndroidUiHelper.isOrientationPortrait(getActivity());
		boolean nightMode = getMyApplication().getDaynightHelper().isNightModeForMapControls();
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		final View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_marker_history_bottom_sheet_dialog, container);
		if (portrait) {
			AndroidUtils.setBackground(getActivity(), mainView, nightMode, R.drawable.bg_bottom_menu_light, R.drawable.bg_bottom_menu_dark);
		}

		Bundle arguments = getArguments();
		if (arguments != null) {
			final int pos = arguments.getInt(MARKER_POSITION);
			String markerName = arguments.getString(MARKER_NAME);
			int markerColorIndex = arguments.getInt(MARKER_COLOR_INDEX);
			long markerVisitedDate = arguments.getLong(MARKER_VISITED_DATE);
			((TextView) mainView.findViewById(R.id.map_marker_title)).setText(markerName);
			((ImageView) mainView.findViewById(R.id.map_marker_icon)).setImageDrawable(getIcon(R.drawable.ic_action_flag_dark, MapMarker.getColorId(markerColorIndex)));
			((TextView) mainView.findViewById(R.id.map_marker_passed_info)).setText(getString(R.string.passed, new SimpleDateFormat("MMM dd", Locale.getDefault()).format(new Date(markerVisitedDate))));

			mainView.findViewById(R.id.make_active_row).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (listener != null) {
						listener.onMakeMarkerActive(pos);
					}
					dismiss();
				}
			});
			mainView.findViewById(R.id.delete_row).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (listener != null) {
						listener.onDeleteMarker(pos);
					}
					dismiss();
				}
			});
		}

		((ImageView) mainView.findViewById(R.id.make_active_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_reset_to_default_dark));
		((ImageView) mainView.findViewById(R.id.delete_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_delete_dark));

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
				final View scrollView = mainView.findViewById(R.id.history_marker_scroll_view);
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

	interface HistoryMarkerMenuFragmentListener {
		void onMakeMarkerActive(int pos);
		void onDeleteMarker(int pos);
	}
}
