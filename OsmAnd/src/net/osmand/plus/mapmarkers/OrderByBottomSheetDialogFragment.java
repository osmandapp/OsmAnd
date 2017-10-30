package net.osmand.plus.mapmarkers;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.MapMarkersOrderByMode;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;

public class OrderByBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public final static String TAG = "OrderByBottomSheetDialogFragment";

	private OsmandSettings settings;
	private OrderByFragmentListener listener;

	public void setListener(OrderByFragmentListener listener) {
		this.listener = listener;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		settings = getMyApplication().getSettings();
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		final View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_marker_order_by_bottom_sheet_dialog, container);

		if (nightMode) {
			((TextView) mainView.findViewById(R.id.order_by_title)).setTextColor(getResources().getColor(R.color.ctx_menu_info_text_dark));
		}
		((TextView) mainView.findViewById(R.id.order_by_title)).setText(getString(R.string.order_by));

		((ImageView) mainView.findViewById(R.id.distance_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_markers_dark));
		((ImageView) mainView.findViewById(R.id.name_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_sort_by_name));
		((ImageView) mainView.findViewById(R.id.date_added_asc_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_sort_by_date));
		((ImageView) mainView.findViewById(R.id.date_added_desc_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_sort_by_date));

		((TextView) mainView.findViewById(R.id.date_added_asc_text)).setText(getString(R.string.date_added) + " (" + getString(R.string.ascendingly) + ")");
		((TextView) mainView.findViewById(R.id.date_added_desc_text)).setText(getString(R.string.date_added) + " (" + getString(R.string.descendingly) + ")");

		mainView.findViewById(R.id.distance_row).setOnClickListener(orderByModeOnClickListener);
		mainView.findViewById(R.id.name_row).setOnClickListener(orderByModeOnClickListener);
		mainView.findViewById(R.id.date_added_asc_row).setOnClickListener(orderByModeOnClickListener);
		mainView.findViewById(R.id.date_added_desc_row).setOnClickListener(orderByModeOnClickListener);

		mainView.findViewById(R.id.close_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		setupHeightAndBackground(mainView, R.id.marker_order_by_scroll_view);

		return mainView;
	}

	private View.OnClickListener orderByModeOnClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View view) {
			MapMarkersOrderByMode currentOrderByMode = settings.MAP_MARKERS_ORDER_BY_MODE.get();
			MapMarkersOrderByMode modeToSet;
			switch (view.getId()) {
				case R.id.distance_row:
					if (currentOrderByMode == MapMarkersOrderByMode.DISTANCE_ASC) {
						modeToSet = MapMarkersOrderByMode.DISTANCE_DESC;
					} else {
						modeToSet = MapMarkersOrderByMode.DISTANCE_ASC;
					}
					break;
				case R.id.name_row:
					modeToSet = MapMarkersOrderByMode.NAME;
					break;
				case R.id.date_added_asc_row:
					modeToSet = MapMarkersOrderByMode.DATE_ADDED_ASC;
					break;
				case R.id.date_added_desc_row:
					modeToSet = MapMarkersOrderByMode.DATE_ADDED_DESC;
					break;
				default:
					modeToSet = currentOrderByMode;
			}
			settings.MAP_MARKERS_ORDER_BY_MODE.set(modeToSet);
			if (listener != null) {
				listener.onMapMarkersOrderByModeChanged(modeToSet);
			}
			dismiss();
		}
	};

	interface OrderByFragmentListener {
		void onMapMarkersOrderByModeChanged(MapMarkersOrderByMode orderByMode);
	}
}
