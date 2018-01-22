package net.osmand.plus.measurementtool;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public class RecyclerViewFragment extends Fragment {

	public static final String TAG = "RecyclerViewFragment";

	private RecyclerView rv;
	private int height;
	private int width;
	private boolean transparentStatusBar;
	private FrameLayout parent;
	private FrameLayout mainView;

	public void setRecyclerView(RecyclerView rv) {
		this.rv = rv;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public void setTransparentStatusBar(boolean transparentStatusBar) {
		this.transparentStatusBar = transparentStatusBar;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final MapActivity mapActivity = (MapActivity) getActivity();
		if (rv == null) {
			return null;
		}
		final boolean nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		final int backgroundColor = ContextCompat.getColor(getActivity(),
				nightMode ? R.color.ctx_menu_info_view_bg_dark : R.color.ctx_menu_info_view_bg_light);
		final TypedValue typedValueAttr = new TypedValue();
		getActivity().getTheme().resolveAttribute(R.attr.left_menu_view_bg, typedValueAttr, true);

		parent = new FrameLayout(mapActivity);
		parent.setLayoutParams(new LayoutParams(width + AndroidUtils.dpToPx(getActivity(), 16), height));
		parent.setBackgroundResource(typedValueAttr.resourceId);

		mainView = new FrameLayout(mapActivity);
		mainView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		mainView.setBackgroundColor(backgroundColor);

		ImageView shadow = new ImageView(mapActivity);
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		params.gravity = Gravity.BOTTOM;
		shadow.setLayoutParams(params);
		shadow.setScaleType(ImageView.ScaleType.FIT_XY);
		shadow.setImageResource(R.drawable.bg_shadow_onmap);

		if (transparentStatusBar) {
			AndroidUtils.addStatusBarPadding21v(getActivity(), rv);
			rv.setClipToPadding(false);
		}

		mainView.addView(rv);
		mainView.addView(shadow);
		parent.addView(mainView);

		return parent;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (parent != null && mainView != null) {
			parent.removeAllViews();
			mainView.removeAllViews();
		}
	}
}
