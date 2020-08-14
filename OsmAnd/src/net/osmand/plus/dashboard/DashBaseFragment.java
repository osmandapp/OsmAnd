package net.osmand.plus.dashboard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.widgets.InterceptorFrameLayout;
import net.osmand.plus.widgets.tools.SwipeDismissTouchListener;

public abstract class DashBaseFragment extends Fragment {
	protected DashboardOnMap dashboard;

	public interface DismissListener {
		void onDismiss();
	}

	public OsmandApplication getMyApplication() {
		if (getActivity() == null) {
			return null;
		}
		return (OsmandApplication) getActivity().getApplication();
	}

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		if (context instanceof MapActivity) {
			dashboard = ((MapActivity) context).getDashboard();
			dashboard.onAttach(this);
		}
	}

	@Nullable
	@Override
	final public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
								   @Nullable Bundle savedInstanceState) {
		View childView = initView(inflater, container, savedInstanceState);
		FrameLayout.LayoutParams layoutParams =
				new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.WRAP_CONTENT);
		InterceptorFrameLayout frameLayout = new InterceptorFrameLayout(getActivity());
		frameLayout.setLayoutParams(layoutParams);

		FrameLayout.LayoutParams childLayoutParams =
				new FrameLayout.LayoutParams(
						(ViewGroup.MarginLayoutParams) childView.getLayoutParams());
		frameLayout.addView(childView, childLayoutParams);

		if (isDismissAllowed()) {
			SwipeDismissTouchListener listener = new SwipeDismissTouchListener(childView, null,
					new SwipeDismissTouchListener.DismissCallbacks() {
						@Override
						public boolean canDismiss(Object token) {
							return true;
						}

						@Override
						public void onDismiss(View view, Object token, boolean isSwipeRight) {
							getDismissCallback().onDismiss();
						}
					});
			frameLayout.setOnTouchListener(listener);
			frameLayout.setListener(listener);
			if (getDismissCallback() == null) {
				defaultDismissListener = new DefaultDismissListener(getParentView(), dashboard, getTag(),
						childView);
			}
		}

		return frameLayout;
	}

	public abstract View initView(LayoutInflater inflater, @Nullable ViewGroup container,
								  @Nullable Bundle savedInstanceState);

	public DismissListener getDismissCallback() {
		return defaultDismissListener;
	}

	public boolean isDismissAllowed() {
		return true;
	}

	@Override
	public boolean getUserVisibleHint() {
		return super.getUserVisibleHint();
	}

	public abstract void onOpenDash();

	public void onCloseDash() {
	}

	@Override
	public final void onPause() {
		// use on close 
		super.onPause();
		onCloseDash();
	}

	public void closeDashboard() {
		dashboard.hideDashboard(false);
	}

	@Override
	public final void onResume() {
		// use on open update
		super.onResume();
		if (dashboard != null && dashboard.isVisible() && getView() != null) {
			onOpenDash();
		}
	}


	public void onLocationCompassChanged(Location l, double compassValue) {
	}

	@Override
	public void onDetach() {
		super.onDetach();
		if (dashboard != null) {
			dashboard.onDetach(this);
			dashboard = null;
		}
	}

	protected void startFavoritesActivity(int tab) {
		Activity activity = getActivity();
		OsmAndAppCustomization appCustomization = getMyApplication().getAppCustomization();
		final Intent favorites = new Intent(activity, appCustomization.getFavoritesActivity());
		favorites.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		getMyApplication().getSettings().FAVORITES_TAB.set(tab);
		activity.startActivity(favorites);
	}

	protected View getParentView() {
		return dashboard.getParentView();
	}

	private DismissListener defaultDismissListener;

	public static class DefaultDismissListener implements DismissListener {
		private View parentView;
		private DashboardOnMap dashboardOnMap;
		private String fragmentTag;
		private View fragmentView;

		public DefaultDismissListener(View parentView, DashboardOnMap dashboardOnMap,
									  String fragmentTag, View fragmentView) {
			this.parentView = parentView;
			this.dashboardOnMap = dashboardOnMap;
			this.fragmentTag = fragmentTag;
			this.fragmentView = fragmentView;
		}

		@Override
		public void onDismiss() {
			dashboardOnMap.blacklistFragmentByTag(fragmentTag);
			fragmentView.setTranslationX(0);
			fragmentView.setAlpha(1);
			Snackbar.make(parentView, dashboardOnMap.getMyApplication().getResources()
					.getString(R.string.shared_string_card_was_hidden), Snackbar.LENGTH_LONG)
					.setAction(R.string.shared_string_undo, new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							DefaultDismissListener.this.onUndo();
						}
					})
					.show();
		}

		public void onUndo() {
			dashboardOnMap.unblacklistFragmentClass(fragmentTag);
			fragmentView.setTranslationX(0);
			fragmentView.setAlpha(1);
		}
	}
}
