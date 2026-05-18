package net.osmand.plus.dashboard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.widgets.InterceptorFrameLayout;
import net.osmand.plus.widgets.tools.SwipeDismissTouchListener;

public abstract class DashBaseFragment extends Fragment {

	protected OsmandApplication app;
	protected OsmandSettings settings;
	protected DashboardOnMap dashboard;

	private DismissListener defaultDismissListener;

	public interface DismissListener {
		void onDismiss();
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.app = requireMyApplication();
		this.settings = app.getSettings();
	}

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		if (context instanceof MapActivity) {
			dashboard = ((MapActivity) context).getDashboard();
			dashboard.onAttach(this);
		}
	}

	@NonNull
	@Override
	public final View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
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

	@ColorInt
	protected int getColor(@ColorRes int resId) {
		return ColorUtilities.getColor(getContext(), resId);
	}

	protected void startMyPlacesActivity(int tab) {
		Activity activity = getActivity();
		if (activity == null) {
			return;
		}
		OsmandApplication app = getMyApplication();
		OsmAndAppCustomization appCustomization = app.getAppCustomization();
		Intent favorites = new Intent(activity, appCustomization.getMyPlacesActivity());
		favorites.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		app.getSettings().FAVORITES_TAB.set(tab);
		activity.startActivity(favorites);
	}

	protected View getParentView() {
		return dashboard.getParentView();
	}

	@Nullable
	protected MapActivity getMapActivity() {
		return getActivity() == null ? null : ((MapActivity) getActivity());
	}

	@NonNull
	protected MapActivity requireMapActivity() {
		return ((MapActivity) requireActivity());
	}

	@Nullable
	protected OsmandApplication getMyApplication() {
		return getActivity() == null ? null : ((OsmandApplication) getActivity().getApplication());
	}

	@NonNull
	protected OsmandApplication requireMyApplication() {
		return ((OsmandApplication) requireActivity().getApplication());
	}

	public static class DefaultDismissListener implements DismissListener {
		private final View parentView;
		private final DashboardOnMap dashboardOnMap;
		private final String fragmentTag;
		private final View fragmentView;

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
