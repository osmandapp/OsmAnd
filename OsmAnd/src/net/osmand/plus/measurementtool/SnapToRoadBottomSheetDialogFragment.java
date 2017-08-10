package net.osmand.plus.measurementtool;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;

public class SnapToRoadBottomSheetDialogFragment extends BottomSheetDialogFragment {

	public static final String TAG = "SnapToRoadBottomSheetDialogFragment";

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final boolean nightMode = getMyApplication().getDaynightHelper().isNightModeForMapControls();
		final int backgroundColor = ContextCompat.getColor(getActivity(),
				nightMode ? R.color.ctx_menu_info_view_bg_dark : R.color.ctx_menu_info_view_bg_light);

		View view = inflater.inflate(R.layout.fragment_snap_to_road_bottom_sheet_dialog, container, false);
		view.setBackgroundColor(backgroundColor);

		FrameLayout navigationTypesContainer = (FrameLayout) view.findViewById(R.id.navigation_types_container);

		View carNavigation = inflater.inflate(R.layout.list_item_icon_and_title, navigationTypesContainer, true);
		((ImageView) carNavigation.findViewById(R.id.icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_car_dark));
		((TextView) carNavigation.findViewById(R.id.title)).setText(getString(R.string.rendering_value_car_name));
		carNavigation.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Toast.makeText(getActivity(), "Car", Toast.LENGTH_SHORT).show();
			}
		});

		view.findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		return view;
	}
}
