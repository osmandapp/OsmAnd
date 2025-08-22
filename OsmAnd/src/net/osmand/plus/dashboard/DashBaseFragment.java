package net.osmand.plus.dashboard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseNestedFragment;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.widgets.InterceptorFrameLayout;
import net.osmand.plus.widgets.tools.SwipeDismissTouchListener;

public abstract class DashBaseFragment extends BaseNestedFragment {

	protected DashboardOnMap dashboard;
	protected DismissListener dismissListener;

	public interface DismissListener {
		void onDismiss();
	}

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		if (context instanceof MapActivity mapActivity) {
			dashboard = mapActivity.getDashboard();
			dashboard.onAttach(this);
		}
	}

	@NonNull
	@Override
	public final View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                               @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View childView = initView(container, savedInstanceState);

		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		InterceptorFrameLayout frameLayout = new InterceptorFrameLayout(getActivity());
		frameLayout.setLayoutParams(lp);

		FrameLayout.LayoutParams childLayoutParams = new FrameLayout.LayoutParams(
						(ViewGroup.MarginLayoutParams) childView.getLayoutParams());
		frameLayout.addView(childView, childLayoutParams);

		SwipeDismissTouchListener listener = new SwipeDismissTouchListener(childView, null,
				new SwipeDismissTouchListener.DismissCallbacks() {
					@Override
					public boolean canDismiss(Object token) {
						return true;
					}

					@Override
					public void onDismiss(View view, Object token, boolean isSwipeRight) {
						dismissListener.onDismiss();
					}
				});
		if (dismissListener == null) {
			dismissListener = new DefaultDismissListener(getParentView(), dashboard, getTag(), childView);
		}
		frameLayout.setOnTouchListener(listener);
		frameLayout.setListener(listener);

		return frameLayout;
	}

	public abstract View initView(@Nullable ViewGroup container, @Nullable Bundle savedState);

	public void onOpenDash() {
	}

	public void onCloseDash() {
	}

	@Override
	public final void onPause() {
		// use on close 
		super.onPause();
		onCloseDash();
	}

	public void closeDashboard() {
		if (dashboard != null) {
			dashboard.hideDashboard(false);
		}
	}

	@Override
	public final void onResume() {
		// use on open update
		super.onResume();
		if (dashboard != null && dashboard.isVisible() && getView() != null) {
			onOpenDash();
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		if (dashboard != null) {
			dashboard.onDetach(this);
			dashboard = null;
		}
	}

	protected void startMyPlacesActivity(int tab) {
		Activity activity = getActivity();
		if (activity != null) {
			OsmAndAppCustomization appCustomization = app.getAppCustomization();
			Intent favorites = new Intent(activity, appCustomization.getMyPlacesActivity());
			favorites.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			settings.FAVORITES_TAB.set(tab);
			activity.startActivity(favorites);
		}
	}

	protected View getParentView() {
		return dashboard.getParentView();
	}

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}
}
