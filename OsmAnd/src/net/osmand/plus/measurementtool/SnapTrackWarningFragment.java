package net.osmand.plus.measurementtool;

import android.app.Activity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityLayers;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.MapControlsLayer;
import net.osmand.plus.views.layers.MapInfoLayer;

import org.apache.commons.logging.Log;

import static android.view.Gravity.TOP;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.BACK_TO_LOC_HUD_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.ZOOM_IN_HUD_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.ZOOM_OUT_HUD_ID;
import static net.osmand.plus.UiUtilities.DialogButtonType.PRIMARY;
import static net.osmand.plus.UiUtilities.DialogButtonType.SECONDARY;

public class SnapTrackWarningFragment extends BaseOsmAndFragment {

	public static final int REQUEST_CODE = 1000;
	public static final int CANCEL_RESULT_CODE = 2;
	public static final int CONTINUE_RESULT_CODE = 3;

	public static final String TAG = SnapTrackWarningFragment.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(SnapTrackWarningFragment.class);

	private static final String ZOOM_IN_BUTTON_ID = ZOOM_IN_HUD_ID + TAG;
	private static final String ZOOM_OUT_BUTTON_ID = ZOOM_OUT_HUD_ID + TAG;
	private static final String BACK_TO_LOC_BUTTON_ID = BACK_TO_LOC_HUD_ID + TAG;

	protected View mainView;
	private boolean continued = false;
	private View cancelButton;
	private View applyButton;
	private boolean nightMode;
	private boolean portrait;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requireMyActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			public void handleOnBackPressed() {
				dismissImmediate();
			}
		});
	}

	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			nightMode = app.getDaynightHelper().isNightModeForMapControls();
			portrait = AndroidUiHelper.isOrientationPortrait(getMapActivity());
		}
		View rootView = UiUtilities.getInflater(getContext(), nightMode)
				.inflate(R.layout.fragment_plan_route_warning, container, false);
		if (rootView == null) {
			return null;
		}
		applyButton = rootView.findViewById(R.id.right_bottom_button);
		cancelButton = rootView.findViewById(R.id.dismiss_button);
		View mapControlsContainer = rootView.findViewById(R.id.map_controls_container);
		updateButtons(rootView);
		if (portrait) {
			setupControlButtons(rootView);
			mapControlsContainer.setVisibility(View.VISIBLE);
		} else {
			mapControlsContainer.setVisibility(View.GONE);
			final TypedValue typedValueAttr = new TypedValue();
			int bgAttrId = AndroidUtils.isLayoutRtl(app) ? R.attr.right_menu_view_bg : R.attr.left_menu_view_bg;
			getMapActivity().getTheme().resolveAttribute(bgAttrId, typedValueAttr, true);
			rootView.setBackgroundResource(typedValueAttr.resourceId);
			LinearLayout mainView = rootView.findViewById(R.id.main_view);
			FrameLayout.LayoutParams params;
			params = (FrameLayout.LayoutParams) mainView.getLayoutParams();
			params.gravity = TOP;
			int landscapeWidth = getResources().getDimensionPixelSize(R.dimen.dashboard_land_width);
			rootView.setLayoutParams(new FrameLayout.LayoutParams(landscapeWidth, MATCH_PARENT));
		}
		return rootView;
	}

	private void setupControlButtons(@NonNull View view) {
		MapActivity mapActivity = getMapActivity();
		View zoomInButtonView = view.findViewById(R.id.map_zoom_in_button);
		View zoomOutButtonView = view.findViewById(R.id.map_zoom_out_button);
		View myLocButtonView = view.findViewById(R.id.map_my_location_button);
		View mapRulerView = view.findViewById(R.id.map_ruler_layout);

		MapActivityLayers mapLayers = mapActivity.getMapLayers();

		OsmandMapTileView mapTileView = mapActivity.getMapView();
		View.OnLongClickListener longClickListener = MapControlsLayer.getOnClickMagnifierListener(mapTileView);

		MapControlsLayer mapControlsLayer = mapLayers.getMapControlsLayer();
		mapControlsLayer.setupZoomInButton(zoomInButtonView, longClickListener, ZOOM_IN_BUTTON_ID);
		mapControlsLayer.setupZoomOutButton(zoomOutButtonView, longClickListener, ZOOM_OUT_BUTTON_ID);
		mapControlsLayer.setupBackToLocationButton(myLocButtonView, false, BACK_TO_LOC_BUTTON_ID);

		MapInfoLayer mapInfoLayer = mapLayers.getMapInfoLayer();
		mapInfoLayer.setupRulerWidget(mapRulerView);
	}

	private void updateButtons(View view) {
		View buttonsContainer = view.findViewById(R.id.buttons_container);
		buttonsContainer.setBackgroundColor(AndroidUtils.getColorFromAttr(view.getContext(), R.attr.list_background_color));
		applyButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Fragment fragment = getTargetFragment();
				if (fragment != null) {
					continued = true;
					dismissImmediate();
					fragment.onActivityResult(REQUEST_CODE, CONTINUE_RESULT_CODE, null);
				}
			}
		});
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					activity.onBackPressed();
				}
			}
		});
		UiUtilities.setupDialogButton(nightMode, cancelButton, SECONDARY, R.string.shared_string_cancel);
		UiUtilities.setupDialogButton(nightMode, applyButton, PRIMARY, R.string.shared_string_continue);
		AndroidUiHelper.updateVisibility(applyButton, true);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.buttons_divider), true);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			activity.findViewById(R.id.snap_to_road_image_button).setVisibility(View.VISIBLE);
		}
		Fragment fragment = getTargetFragment();
		if (fragment != null && !continued) {
			fragment.onActivityResult(REQUEST_CODE, CANCEL_RESULT_CODE, null);
		}
	}

	public MapActivity getMapActivity() {
		FragmentActivity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		} else {
			return null;
		}
	}

	public static void showInstance(FragmentManager fm, Fragment targetFragment) {
		try {
			if (!fm.isStateSaved()) {
				SnapTrackWarningFragment fragment = new SnapTrackWarningFragment();
				fragment.setTargetFragment(targetFragment, REQUEST_CODE);
				fm.beginTransaction()
						.replace(R.id.fragmentContainer, fragment, TAG)
						.addToBackStack(TAG)
						.commitAllowingStateLoss();
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}

	public void dismissImmediate() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			try {
				mapActivity.getSupportFragmentManager().popBackStackImmediate(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			} catch (Exception e) {
				LOG.error(e);
			}
		}
	}
}
