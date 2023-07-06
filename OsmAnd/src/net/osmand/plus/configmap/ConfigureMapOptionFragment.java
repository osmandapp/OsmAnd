package net.osmand.plus.configmap;

import static net.osmand.plus.routepreparationmenu.ChooseRouteFragment.BACK_TO_LOC_BUTTON_ID;
import static net.osmand.plus.routepreparationmenu.ChooseRouteFragment.ZOOM_IN_BUTTON_ID;
import static net.osmand.plus.routepreparationmenu.ChooseRouteFragment.ZOOM_OUT_BUTTON_ID;
import static net.osmand.plus.utils.UiUtilities.setupDialogButton;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.controls.maphudbuttons.MyLocationButton;
import net.osmand.plus.views.controls.maphudbuttons.ZoomInButton;
import net.osmand.plus.views.controls.maphudbuttons.ZoomOutButton;
import net.osmand.plus.views.layers.MapControlsLayer;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.widgets.RulerWidget;

import java.util.Arrays;
import java.util.Collections;

public abstract class ConfigureMapOptionFragment extends BaseOsmAndFragment {

	public static final String TAG = ConfigureMapOptionFragment.class.getSimpleName();
	private RulerWidget rulerWidget;
	private View applyButton;
	protected LinearLayout contentContainer;


	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getListBgColorId(nightMode);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		MapActivity activity = requireMapActivity();
		activity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				activity.getSupportFragmentManager().popBackStack();
				activity.getDashboard().setDashboardVisibility(true, DashboardOnMap.DashboardType.TERRAIN, false);
			}
		});
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		MapActivity activity = requireMapActivity();
		View view = themedInflater.inflate(R.layout.configure_map_option_fragment, container, false);
		AndroidUtils.addStatusBarPadding21v(activity, view);

		contentContainer = view.findViewById(R.id.main_content);
		applyButton = view.findViewById(R.id.apply_button);
		applyButton.setOnClickListener(viewOnCLick -> {
			onApplyButtonClick();
			activity.onBackPressed();
		});

		setupToolBar(view);
		buildZoomButtons(view);
		moveCompassButton(view);
		moveMap3DButton(view);

		setupMainContent();
		updateApplyButton(false);

		refreshMap();
		refreshControlsButtons();

		return view;
	}


	protected abstract String getToolbarTitle();

	protected void onResetToDefault() {
	}

	protected void onApplyButtonClick() {
	}

	protected abstract void setupMainContent();

	protected void updateApplyButton(boolean enable) {
		applyButton.setEnabled(enable);
		setupDialogButton(nightMode, applyButton, enable ? UiUtilities.DialogButtonType.PRIMARY : UiUtilities.DialogButtonType.STROKED, getString(R.string.shared_string_apply));
	}

	protected void refreshMap() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.refreshMap();
		}
	}

	private void buildZoomButtons(@NonNull View view) {
		View zoomButtonsView = view.findViewById(R.id.map_hud_controls);

		MapActivity activity = requireMapActivity();
		MapLayers mapLayers = activity.getMapLayers();
		MapControlsLayer layer = mapLayers.getMapControlsLayer();

		layer.addMapButton(new ZoomInButton(activity, view.findViewById(R.id.map_zoom_in_button), ZOOM_IN_BUTTON_ID));
		layer.addMapButton(new ZoomOutButton(activity, view.findViewById(R.id.map_zoom_out_button), ZOOM_OUT_BUTTON_ID));
		layer.addMapButton(new MyLocationButton(activity, view.findViewById(R.id.map_my_location_button), BACK_TO_LOC_BUTTON_ID, false));

		AndroidUiHelper.updateVisibility(zoomButtonsView, true);

		MapInfoLayer mapInfoLayer = mapLayers.getMapInfoLayer();
		rulerWidget = mapInfoLayer.setupRulerWidget(view.findViewById(R.id.map_ruler_layout));
	}

	private void setupToolBar(@NonNull View view) {
		ViewCompat.setElevation(view.findViewById(R.id.appbar), 5.0f);

		TextView title = view.findViewById(R.id.title);
		title.setText(getToolbarTitle());

		ImageView backButton = view.findViewById(R.id.back_button);
		backButton.setImageDrawable(getIcon(AndroidUtils.getNavigationIconResId(view.getContext()), ColorUtilities.getDefaultIconColorId(nightMode)));
		backButton.setOnClickListener(v -> {
			MapActivity activity = getMapActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});

		ImageButton resetButton = view.findViewById(R.id.reset_button);
		resetButton.setImageDrawable(getIcon(R.drawable.ic_action_reset, ColorUtilities.getDefaultIconColorId(nightMode)));
		resetButton.setOnClickListener(v -> onResetToDefault());
	}

	private void moveCompassButton(@NonNull View view) {
		int btnSizePx = getDimensionPixelSize(R.dimen.map_small_button_size);
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(btnSizePx, btnSizePx);
		int toolbarHeight = getDimensionPixelSize(R.dimen.toolbar_height);
		int topMargin = getDimensionPixelSize(R.dimen.map_small_button_margin);
		int startMargin = getDimensionPixelSize(R.dimen.map_button_margin);
		AndroidUtils.setMargins(params, startMargin, topMargin + toolbarHeight, 0, 0);

		MapActivity activity = getMapActivity();
		if (activity != null) {
			MapLayers mapLayers = activity.getMapLayers();
			MapControlsLayer mapControlsLayer = mapLayers.getMapControlsLayer();
			mapControlsLayer.moveCompassButton((ViewGroup) view, params);
		}
	}

	private void moveMap3DButton(@NonNull View view) {
		MapActivity activity = getMapActivity();
		if (activity != null) {
			int size = getDimensionPixelSize(R.dimen.map_button_size);
			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);

			MapLayers mapLayers = activity.getMapLayers();
			MapControlsLayer mapControlsLayer = mapLayers.getMapControlsLayer();
			mapControlsLayer.moveMap3DButton(view.findViewById(R.id.hud_button_container), params);
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		MapActivity mapActivity = requireMapActivity();
		mapActivity.disableDrawer();
		updateWidgetsVisibility(mapActivity, View.GONE);
	}

	@Override
	public void onPause() {
		super.onPause();

		MapActivity mapActivity = requireMapActivity();
		mapActivity.enableDrawer();
		updateWidgetsVisibility(mapActivity, View.VISIBLE);
	}

	private void updateWidgetsVisibility(@NonNull MapActivity activity, int visibility) {
		AndroidUiHelper.setVisibility(activity, visibility, R.id.map_left_widgets_panel,
				R.id.map_right_widgets_panel, R.id.map_center_info);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			MapLayers mapLayers = mapActivity.getMapLayers();

			MapControlsLayer layer = mapLayers.getMapControlsLayer();
			layer.removeMapButtons(Arrays.asList(ZOOM_IN_BUTTON_ID, ZOOM_OUT_BUTTON_ID, BACK_TO_LOC_BUTTON_ID));
			layer.restoreCompassButton();
			layer.restoreMap3DButton();

			if (rulerWidget != null) {
				MapInfoLayer mapInfoLayer = mapLayers.getMapInfoLayer();
				mapInfoLayer.removeRulerWidgets(Collections.singletonList(rulerWidget));
			}
		}
		refreshMap();
		refreshControlsButtons();
	}

	private void refreshControlsButtons(){
		app.getOsmandMap().getMapLayers().getMapControlsLayer().refreshButtons();
	}

	@Nullable
	public MapActivity getMapActivity() {
		FragmentActivity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		} else {
			return null;
		}
	}

	@NonNull
	protected MapActivity requireMapActivity() {
		return ((MapActivity) requireActivity());
	}
}