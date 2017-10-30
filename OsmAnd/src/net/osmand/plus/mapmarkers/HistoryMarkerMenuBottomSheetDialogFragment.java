package net.osmand.plus.mapmarkers;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HistoryMarkerMenuBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public final static String TAG = "HistoryMarkerMenuBottomSheetDialogFragment";

	public static final String MARKER_POSITION = "marker_position";
	public static final String MARKER_NAME = "marker_name";
	public static final String MARKER_COLOR_INDEX = "marker_color_index";
	public static final String MARKER_VISITED_DATE = "marker_visited_date";

	private HistoryMarkerMenuFragmentListener listener;

	public void setListener(HistoryMarkerMenuFragmentListener listener) {
		this.listener = listener;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		final View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_marker_history_bottom_sheet_dialog, container);

		Bundle arguments = getArguments();
		if (arguments != null) {
			final int pos = arguments.getInt(MARKER_POSITION);
			String markerName = arguments.getString(MARKER_NAME);
			int markerColorIndex = arguments.getInt(MARKER_COLOR_INDEX);
			long markerVisitedDate = arguments.getLong(MARKER_VISITED_DATE);
			((TextView) mainView.findViewById(R.id.map_marker_title)).setText(markerName);
			((ImageView) mainView.findViewById(R.id.map_marker_icon)).setImageDrawable(getIcon(R.drawable.ic_action_flag_dark, MapMarker.getColorId(markerColorIndex)));
			Date date = new Date(markerVisitedDate);
			String month = new SimpleDateFormat("MMM", Locale.getDefault()).format(date);
			if (month.length() > 1) {
				month = Character.toUpperCase(month.charAt(0)) + month.substring(1);
			}
			month = month.replaceAll("\\.", "");
			String day = new SimpleDateFormat("d", Locale.getDefault()).format(date);
			((TextView) mainView.findViewById(R.id.map_marker_passed_info)).setText(getString(R.string.passed, month + " " + day));

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

		setupHeightAndBackground(mainView, R.id.history_marker_scroll_view);

		return mainView;
	}

	interface HistoryMarkerMenuFragmentListener {

		void onMakeMarkerActive(int pos);

		void onDeleteMarker(int pos);
	}
}
