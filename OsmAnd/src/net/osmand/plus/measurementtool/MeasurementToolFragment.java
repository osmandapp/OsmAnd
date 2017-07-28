package net.osmand.plus.measurementtool;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import net.osmand.AndroidUtils;
import net.osmand.plus.IconsCache;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public class MeasurementToolFragment extends Fragment {

	public static final String TAG = "MeasurementToolFragment";

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_measurement_tool, container, false);

		MapActivity mapActivity = (MapActivity) getActivity();
		IconsCache iconsCache = mapActivity.getMyApplication().getIconsCache();
		final boolean nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();

		View mainView = view.findViewById(R.id.main_view);
		AndroidUtils.setBackground(mapActivity, mainView, nightMode, R.drawable.bg_bottom_menu_light, R.drawable.bg_bottom_menu_dark);

		((ImageView) mainView.findViewById(R.id.ruler_icon))
				.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_ruler, R.color.color_myloc_distance));
		((ImageView) mainView.findViewById(R.id.up_down_icon))
				.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_arrow_up));
		((ImageView) mainView.findViewById(R.id.previous_dot_icon))
				.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_undo_dark));
		((ImageView) mainView.findViewById(R.id.next_dot_icon))
				.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_redo_dark));

		return view;
	}
}
