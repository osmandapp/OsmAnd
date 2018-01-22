package net.osmand.plus.mapmarkers;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;

public class PlanRouteOptionsBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public final static String TAG = "PlanRouteOptionsBottomSheetDialogFragment";

	private PlanRouteOptionsFragmentListener listener;
	private boolean selectAll;

	public void setListener(PlanRouteOptionsFragmentListener listener) {
		this.listener = listener;
	}

	public void setSelectAll(boolean selectAll) {
		this.selectAll = selectAll;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		boolean portrait = AndroidUiHelper.isOrientationPortrait(getActivity());
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		final View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_plan_route_options_bottom_sheet_dialog, container);

		if (nightMode) {
			((TextView) mainView.findViewById(R.id.title)).setTextColor(ContextCompat.getColor(getActivity(), R.color.ctx_menu_info_text_dark));
		}

		((ImageView) mainView.findViewById(R.id.navigate_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_gdirections_dark));
		((ImageView) mainView.findViewById(R.id.make_round_trip_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_trip_round));
		((ImageView) mainView.findViewById(R.id.door_to_door_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_sort_door_to_door));
		((ImageView) mainView.findViewById(R.id.reverse_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_sort_reverse_order));

		((CompoundButton) mainView.findViewById(R.id.make_round_trip_switch)).setChecked(getMyApplication().getSettings().ROUTE_MAP_MARKERS_ROUND_TRIP.get());

		if (!portrait) {
			((ImageView) mainView.findViewById(R.id.select_icon))
					.setImageDrawable(getContentIcon(selectAll ? R.drawable.ic_action_select_all : R.drawable.ic_action_deselect_all));

			((TextView) mainView.findViewById(R.id.select_title))
					.setText(getString(selectAll ? R.string.shared_string_select_all : R.string.shared_string_deselect_all));

			View selectRow = mainView.findViewById(R.id.select_row);
			selectRow.setVisibility(View.VISIBLE);
			selectRow.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					listener.selectOnClick();
					dismiss();
				}
			});
		}
		mainView.findViewById(R.id.navigate_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.navigateOnClick();
					dismiss();
				}
			}
		});
		mainView.findViewById(R.id.make_round_trip_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.makeRoundTripOnClick();
					dismiss();
				}
			}
		});
		mainView.findViewById(R.id.door_to_door_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.doorToDoorOnClick();
					dismiss();
				}
			}
		});
		mainView.findViewById(R.id.reverse_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.reverseOrderOnClick();
					dismiss();
				}
			}
		});

		mainView.findViewById(R.id.cancel_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		setupHeightAndBackground(mainView, R.id.sort_by_scroll_view);

		return mainView;
	}

	interface PlanRouteOptionsFragmentListener {

		void selectOnClick();

		void navigateOnClick();

		void makeRoundTripOnClick();

		void doorToDoorOnClick();

		void reverseOrderOnClick();
	}
}
