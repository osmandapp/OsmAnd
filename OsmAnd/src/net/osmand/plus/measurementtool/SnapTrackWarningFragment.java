package net.osmand.plus.measurementtool;

import static android.view.Gravity.TOP;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.BACK_TO_LOC_HUD_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.ZOOM_IN_HUD_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.ZOOM_OUT_HUD_ID;
import static net.osmand.plus.widgets.dialogbutton.DialogButtonType.PRIMARY;
import static net.osmand.plus.widgets.dialogbutton.DialogButtonType.SECONDARY;

import android.app.Activity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.layers.MapControlsLayer;
import net.osmand.plus.views.mapwidgets.widgets.RulerWidget;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public class SnapTrackWarningFragment extends BaseFullScreenFragment {

	public static final int REQUEST_CODE = 1000;
	public static final int CANCEL_RESULT_CODE = 2;
	public static final int CONTINUE_RESULT_CODE = 3;
	public static final int CONNECT_STRAIGHT_LINE_RESULT_CODE = 4;

	public static final String TAG = SnapTrackWarningFragment.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(SnapTrackWarningFragment.class);

	private static final String ZOOM_IN_BUTTON_ID = ZOOM_IN_HUD_ID + TAG;
	private static final String ZOOM_OUT_BUTTON_ID = ZOOM_OUT_HUD_ID + TAG;
	private static final String BACK_TO_LOC_BUTTON_ID = BACK_TO_LOC_HUD_ID + TAG;

	private DialogButton cancelButton;
	private DialogButton applyButton;

	private boolean editMode;
	private boolean continued;

	private boolean portrait;

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		portrait = AndroidUiHelper.isOrientationPortrait(getMapActivity());

		Fragment targetFragment = getTargetFragment();
		if (targetFragment instanceof MeasurementToolFragment) {
			MeasurementToolFragment fragment = (MeasurementToolFragment) targetFragment;
			editMode = fragment.isPlanRouteMode() && !fragment.getEditingCtx().isNewData();
		}
		requireMyActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			public void handleOnBackPressed() {
				dismissImmediate();
			}
		});
	}

	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		updateNightMode();
		View rootView = inflate(R.layout.fragment_plan_route_warning, container, false);

		applyButton = rootView.findViewById(R.id.right_bottom_button);
		cancelButton = rootView.findViewById(R.id.dismiss_button);
		View mapControlsContainer = rootView.findViewById(R.id.map_controls_container);
		updateButtons(rootView);

		if (editMode) {
			TextView textView = rootView.findViewById(R.id.description);
			textView.setText(R.string.attach_roads_descr);
			setupActionButtons(rootView);
		}
		if (portrait) {
			setupControlButtons(rootView);
			mapControlsContainer.setVisibility(View.VISIBLE);
		} else {
			mapControlsContainer.setVisibility(View.GONE);
			TypedValue typedValueAttr = new TypedValue();
			int bgAttrId = AndroidUtils.isLayoutRtl(app) ? R.attr.right_menu_view_bg : R.attr.left_menu_view_bg;
			getThemedContext().getTheme().resolveAttribute(bgAttrId, typedValueAttr, true);
			rootView.setBackgroundResource(typedValueAttr.resourceId);
			LinearLayout mainView = rootView.findViewById(R.id.main_view);
			FrameLayout.LayoutParams params;
			params = (FrameLayout.LayoutParams) mainView.getLayoutParams();
			params.gravity = TOP;
			int landscapeWidth = getDimensionPixelSize(R.dimen.dashboard_land_width);
			rootView.setLayoutParams(new FrameLayout.LayoutParams(landscapeWidth, MATCH_PARENT));
		}
		refreshControlsButtons();
		return rootView;
	}

	@Nullable
	@Override
	public List<Integer> getScrollableViewIds() {
		List<Integer> ids = new ArrayList<>();
		ids.add(R.id.buttons_container);
		return ids;
	}

	private void setupControlButtons(@NonNull View view) {
		MapActivity activity = requireMapActivity();
		MapLayers mapLayers = activity.getMapLayers();
		MapControlsLayer controlsLayer = mapLayers.getMapControlsLayer();

		controlsLayer.addCustomizedDefaultMapButton(view.findViewById(R.id.map_zoom_in_button));
		controlsLayer.addCustomizedDefaultMapButton(view.findViewById(R.id.map_zoom_out_button));
		controlsLayer.addCustomizedDefaultMapButton(view.findViewById(R.id.map_my_location_button));

		RulerWidget mapRuler = view.findViewById(R.id.map_ruler_layout);
		mapLayers.getMapInfoLayer().setupRulerWidget(mapRuler);
	}

	private void setupActionButtons(@NonNull View view) {
		ViewGroup container = view.findViewById(R.id.buttons_container);
		LayoutInflater inflater = UiUtilities.getInflater(view.getContext(), nightMode);
		setupAttachRoadsButton(container, inflater);
		container.addView(inflater.inflate(R.layout.divider_half_item_with_background, container, false));
		setupStraitLineButton(container, inflater);
	}

	private void setupAttachRoadsButton(@NonNull ViewGroup container, @NonNull LayoutInflater inflater) {
		View view = inflater.inflate(R.layout.bottom_sheet_item_with_descr_pad_32dp, container, false);
		TextView textView = view.findViewById(R.id.title);
		ImageView imageView = view.findViewById(R.id.icon);
		textView.setText(R.string.attach_to_the_roads);
		imageView.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_attach_track));
		view.setOnClickListener(v -> openApproximation());
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.description), false);
		container.addView(view);
	}

	private void setupStraitLineButton(@NonNull ViewGroup container, @NonNull LayoutInflater inflater) {
		View view = inflater.inflate(R.layout.bottom_sheet_item_with_descr_pad_32dp, container, false);
		TextView textView = view.findViewById(R.id.title);
		ImageView imageView = view.findViewById(R.id.icon);
		textView.setText(R.string.connect_with_straight_line);
		imageView.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_split_interval));
		view.setOnClickListener(v -> connectStraitLine());
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.description), false);
		container.addView(view);
	}

	private void updateButtons(View view) {
		View buttonsContainer = view.findViewById(R.id.buttons_container);
		buttonsContainer.setBackgroundColor(AndroidUtils.getColorFromAttr(view.getContext(), R.attr.list_background_color));
		applyButton.setOnClickListener(v -> openApproximation());
		cancelButton.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});
		cancelButton.setButtonType(SECONDARY);
		cancelButton.setTitleId(R.string.shared_string_cancel);
		applyButton.setButtonType(PRIMARY);
		applyButton.setTitleId(R.string.shared_string_continue);
		AndroidUiHelper.updateVisibility(applyButton, !editMode);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.buttons_divider), !editMode);
	}

	private void connectStraitLine() {
		Fragment fragment = getTargetFragment();
		if (fragment != null) {
			dismissImmediate();
			fragment.onActivityResult(REQUEST_CODE, CONNECT_STRAIGHT_LINE_RESULT_CODE, null);
		}
	}

	private void openApproximation() {
		Fragment fragment = getTargetFragment();
		if (fragment != null) {
			continued = true;
			dismissImmediate();
			fragment.onActivityResult(REQUEST_CODE, CONTINUE_RESULT_CODE, null);
		}
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
		if (!continued) {
			refreshControlsButtons();
		}
	}

	private void refreshControlsButtons() {
		app.getOsmandMap().getMapLayers().getMapControlsLayer().refreshButtons();
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @Nullable Fragment targetFragment) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			SnapTrackWarningFragment fragment = new SnapTrackWarningFragment();
			fragment.setTargetFragment(targetFragment, REQUEST_CODE);
			fragmentManager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
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
