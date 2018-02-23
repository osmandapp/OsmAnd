package net.osmand.plus.mapmarkers;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarkersSortByDef;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;

public class OrderByBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public final static String TAG = "OrderByBottomSheetDialogFragment";

	private OrderByFragmentListener listener;

	public void setListener(OrderByFragmentListener listener) {
		this.listener = listener;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		final View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes),
				R.layout.fragment_marker_order_by_bottom_sheet_dialog, container);

		if (nightMode) {
			((TextView) mainView.findViewById(R.id.order_by_title)).setTextColor(
					ContextCompat.getColor(getContext(), R.color.ctx_menu_info_text_dark)
			);
		}

		Drawable distanceIcon = getContentIcon(R.drawable.ic_action_markers_dark);
		Drawable dateIcon = getContentIcon(R.drawable.ic_action_sort_by_date);
		((ImageView) mainView.findViewById(R.id.name_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_sort_by_name));
		((ImageView) mainView.findViewById(R.id.distance_nearest_icon)).setImageDrawable(distanceIcon);
		((ImageView) mainView.findViewById(R.id.distance_farthest_icon)).setImageDrawable(distanceIcon);
		((ImageView) mainView.findViewById(R.id.date_added_asc_icon)).setImageDrawable(dateIcon);
		((ImageView) mainView.findViewById(R.id.date_added_desc_icon)).setImageDrawable(dateIcon);

		((TextView) mainView.findViewById(R.id.date_added_asc_text)).setText(getString(R.string.date_added) + " (" + getString(R.string.ascendingly) + ")");
		((TextView) mainView.findViewById(R.id.date_added_desc_text)).setText(getString(R.string.date_added) + " (" + getString(R.string.descendingly) + ")");

		View.OnClickListener onClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int sortByMode = -1;
				switch (v.getId()) {
					case R.id.name_row:
						sortByMode = MapMarkersHelper.BY_NAME;
						break;
					case R.id.distance_nearest_row:
						sortByMode = MapMarkersHelper.BY_DISTANCE_ASC;
						break;
					case R.id.distance_farthest_row:
						sortByMode = MapMarkersHelper.BY_DISTANCE_DESC;
						break;
					case R.id.date_added_asc_row:
						sortByMode = MapMarkersHelper.BY_DATE_ADDED_ASC;
						break;
					case R.id.date_added_desc_row:
						sortByMode = MapMarkersHelper.BY_DATE_ADDED_DESC;
						break;
				}
				if (sortByMode != -1 && listener != null) {
					listener.onMapMarkersOrderByModeChanged(sortByMode);
				}
				dismiss();
			}
		};

		mainView.findViewById(R.id.name_row).setOnClickListener(onClickListener);
		mainView.findViewById(R.id.distance_nearest_row).setOnClickListener(onClickListener);
		mainView.findViewById(R.id.distance_farthest_row).setOnClickListener(onClickListener);
		mainView.findViewById(R.id.date_added_asc_row).setOnClickListener(onClickListener);
		mainView.findViewById(R.id.date_added_desc_row).setOnClickListener(onClickListener);
		mainView.findViewById(R.id.close_row).setOnClickListener(onClickListener);

		setupHeightAndBackground(mainView, R.id.marker_order_by_scroll_view);

		return mainView;
	}

	interface OrderByFragmentListener {
		void onMapMarkersOrderByModeChanged(@MapMarkersSortByDef int sortByMode);
	}
}
