package net.osmand.plus.configmap;

import static net.osmand.plus.widgets.dialogbutton.DialogButtonType.PRIMARY;
import static net.osmand.plus.widgets.dialogbutton.DialogButtonType.STROKED;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.controls.maphudbuttons.Map3DButton;
import net.osmand.plus.views.layers.MapControlsLayer;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.widgets.RulerWidget;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

import java.util.Collections;

public abstract class ConfigureMapOptionFragment extends BaseOsmAndFragment {

	public static final String TAG = ConfigureMapOptionFragment.class.getSimpleName();

	private RulerWidget rulerWidget;
	private DialogButton applyButton;

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		boolean portrait = AndroidUiHelper.isOrientationPortrait(requireMapActivity());
		return portrait ? ColorUtilities.getListBgColorId(nightMode) : R.color.status_bar_transparent_light;
	}

	public boolean getContentStatusBarNightMode() {
		return nightMode;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		MapActivity activity = requireMapActivity();
		View view = themedInflater.inflate(R.layout.configure_map_option_fragment, container, false);
		AndroidUtils.addStatusBarPadding21v(activity, view);

		applyButton = view.findViewById(R.id.apply_button);
		applyButton.setOnClickListener(viewOnCLick -> {
			applyChanges();
			dismiss();
		});

		setupToolBar(view);
		buildZoomButtons(view);
		moveCompassButton(view);
		moveMap3DButton(view);
		setupBackgroundShadow(view);
		setupBottomContainer(view.findViewById(R.id.bottom_container));
		setupMainContent(view.findViewById(R.id.main_content));
		updateApplyButton(false);

		refreshMap();
		refreshControlsButtons();

		return view;
	}

	protected void setupBottomContainer(@NonNull View bottomContainer) {
	}

	@Nullable
	protected abstract String getToolbarTitle();

	protected void resetToDefault() {
	}

	protected void applyChanges() {
	}

	protected abstract void setupMainContent(@NonNull ViewGroup container);

	protected void updateApplyButton(boolean enable) {
		applyButton.setEnabled(enable);
		applyButton.setButtonType(enable ? PRIMARY : STROKED);
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
		MapControlsLayer controlsLayer = mapLayers.getMapControlsLayer();

		controlsLayer.addCustomMapButton(view.findViewById(R.id.map_zoom_in_button));
		controlsLayer.addCustomMapButton(view.findViewById(R.id.map_zoom_out_button));
		controlsLayer.addCustomMapButton(view.findViewById(R.id.map_my_location_button));

		AndroidUiHelper.updateVisibility(zoomButtonsView, true);

		MapInfoLayer mapInfoLayer = mapLayers.getMapInfoLayer();
		rulerWidget = mapInfoLayer.setupRulerWidget(view.findViewById(R.id.map_ruler_layout));
	}

	private void setupBackgroundShadow(@NonNull View view) {
		MapActivity activity = requireMapActivity();
		if (!AndroidUiHelper.isOrientationPortrait(activity)) {
			TypedValue typedValueAttr = new TypedValue();
			int bgAttrId = AndroidUtils.isLayoutRtl(activity) ? R.attr.right_menu_view_bg : R.attr.left_menu_view_bg;
			activity.getTheme().resolveAttribute(bgAttrId, typedValueAttr, true);
			view.findViewById(R.id.main_view).setBackgroundResource(typedValueAttr.resourceId);
		}
	}

	protected void setupToolBar(@NonNull View view) {
		View appbar = view.findViewById(R.id.appbar);
		ViewCompat.setElevation(appbar, 5.0f);

		TextView title = appbar.findViewById(R.id.title);
		title.setText(getToolbarTitle());

		ImageView backButton = appbar.findViewById(R.id.back_button);
		backButton.setImageDrawable(getContentIcon(R.drawable.ic_action_close));
		backButton.setOnClickListener(v -> dismiss());

		ImageButton resetButton = appbar.findViewById(R.id.reset_button);
		resetButton.setImageDrawable(getIcon(R.drawable.ic_action_reset, ColorUtilities.getDefaultIconColorId(nightMode)));
		resetButton.setOnClickListener(v -> resetToDefault());
	}

	private void moveCompassButton(@NonNull View view) {
		MapActivity activity = getMapActivity();
		if (activity != null) {
			boolean portrait = AndroidUiHelper.isOrientationPortrait(activity);
			int toolbarHeight = portrait ? getDimensionPixelSize(R.dimen.toolbar_height) : 0;
			int topMargin = getDimensionPixelSize(portrait ? R.dimen.map_small_button_margin : R.dimen.content_padding_half);
			int startMargin = portrait ? getDimensionPixelSize(R.dimen.map_button_margin) : (getDimensionPixelSize(R.dimen.dashboard_land_width) + getDimensionPixelSize(R.dimen.content_padding));

			int buttonSize = getDimensionPixelSize(R.dimen.map_small_button_size);
			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(buttonSize, buttonSize);
			AndroidUtils.setMargins(params, startMargin, topMargin + toolbarHeight, 0, 0);

			MapLayers mapLayers = activity.getMapLayers();
			MapControlsLayer mapControlsLayer = mapLayers.getMapControlsLayer();
			mapControlsLayer.moveCompassButton((ViewGroup) view, params);
		}
	}

	private void moveMap3DButton(@NonNull View view) {
		MapActivity activity = getMapActivity();
		if (activity != null) {
			ViewGroup container = view.findViewById(R.id.hud_button_container);
			Map3DButton map3DButton = (Map3DButton) themedInflater.inflate(R.layout.map_3d_button, container, false);
			activity.getMapLayers().getMapControlsLayer().addCustomMapButton(map3DButton);
			container.addView(map3DButton);
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		MapActivity activity = requireMapActivity();
		activity.disableDrawer();
		updateWidgetsVisibility(activity, View.GONE);
	}

	@Override
	public void onPause() {
		super.onPause();

		MapActivity activity = requireMapActivity();
		activity.enableDrawer();
		updateWidgetsVisibility(activity, View.VISIBLE);
	}

	protected void dismiss() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			activity.onBackPressed();
		}
	}

	private void updateWidgetsVisibility(@NonNull MapActivity activity, int visibility) {
		AndroidUiHelper.setVisibility(activity, visibility, R.id.map_left_widgets_panel,
				R.id.map_right_widgets_panel, R.id.map_center_info);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		MapActivity activity = getMapActivity();
		if (activity != null) {
			MapLayers mapLayers = activity.getMapLayers();

			MapControlsLayer layer = mapLayers.getMapControlsLayer();
			layer.clearCustomMapButtons();
			layer.restoreCompassButton();

			if (rulerWidget != null) {
				MapInfoLayer mapInfoLayer = mapLayers.getMapInfoLayer();
				mapInfoLayer.removeRulerWidgets(Collections.singletonList(rulerWidget));
			}
		}
		refreshMap();
		refreshControlsButtons();
	}

	private void refreshControlsButtons() {
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