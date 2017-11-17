package net.osmand.plus.mapmarkers;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;

public class SelectionMarkersGroupBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public final static String TAG = "SelectionMarkersGroupBottomSheetDialogFragment";

	private AddMarkersGroupFragmentListener listener;

	public void setListener(AddMarkersGroupFragmentListener listener) {
		this.listener = listener;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		final View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_marker_add_markers_group_bottom_sheet_dialog, container);

		if (nightMode) {
			((TextView) mainView.findViewById(R.id.add_group_title)).setTextColor(getResources().getColor(R.color.ctx_menu_info_text_dark));
		}

		((ImageView) mainView.findViewById(R.id.favourites_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_fav_dark));
		((ImageView) mainView.findViewById(R.id.waypoints_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_polygom_dark));

		mainView.findViewById(R.id.favourites_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.favouritesOnClick();
				}
				dismiss();
			}
		});
		mainView.findViewById(R.id.waypoints_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.waypointsOnClick();
				}
				dismiss();
			}
		});

		mainView.findViewById(R.id.close_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		setupHeightAndBackground(mainView, R.id.add_markers_group_scroll_view);

		return mainView;
	}

	interface AddMarkersGroupFragmentListener {

		void favouritesOnClick();

		void waypointsOnClick();
	}
}
