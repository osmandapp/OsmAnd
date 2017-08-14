package net.osmand.plus.measurementtool;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public class PointsListFragment extends Fragment {

	public static final String TAG = "pointsListFragment";

	private RecyclerView rv;
	private int height;
	private int width;
	private LinearLayout ll;

	public void setRv(RecyclerView rv) {
		this.rv = rv;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final MapActivity mapActivity = (MapActivity) getActivity();
		final boolean nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		final int backgroundColor = ContextCompat.getColor(getActivity(),
				nightMode ? R.color.ctx_menu_info_view_bg_dark : R.color.ctx_menu_info_view_bg_light);

		ll = new LinearLayout(mapActivity);
		ll.setLayoutParams(new LinearLayout.LayoutParams(width, height));
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setBackgroundColor(backgroundColor);
		ll.addView(rv);

		return ll;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (ll != null) {
			ll.removeAllViews();
		}
	}
}
