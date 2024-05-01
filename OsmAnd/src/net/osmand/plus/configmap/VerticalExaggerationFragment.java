package net.osmand.plus.configmap;

import static net.osmand.plus.plugins.srtm.SRTMPlugin.MAX_VERTICAL_EXAGGERATION;
import static net.osmand.plus.plugins.srtm.SRTMPlugin.MIN_VERTICAL_EXAGGERATION;
import static net.osmand.plus.widgets.dialogbutton.DialogButtonType.PRIMARY;
import static net.osmand.plus.widgets.dialogbutton.DialogButtonType.STROKED;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.slider.Slider;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.ContextMenuScrollFragment;
import net.osmand.plus.track.TrackDrawInfo;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

import java.text.DecimalFormat;

public class VerticalExaggerationFragment extends ContextMenuScrollFragment {

	public static final String SCALE = "scale";
	public static final String ORIGINAL_SCALE = "original_scale";
	public static final String DESCRIPTION_RES_ID = "descriptionResId";
	private static final String TRACK_DRAW_INFO_KEY = "track_draw_info_key";

	private TextView scaleTv;
	private TextView verticalExaggerationDescription;
	private Slider scaleSlider;
	private float originalScaleValue;
	private float scaleValue;
	private DialogButton applyButton;
	private int menuTitleHeight;
	private View routeMenuTopShadowAll;
	private View controlButtons;
	private View buttonsShadow;
	@StringRes
	private int descriptionResId;
	@Nullable
	private TrackDrawInfo trackDrawInfo;

	@Override
	public int getMainLayoutId() {
		return R.layout.track_exaggeration;
	}

	@Override
	public int getStatusBarColorId() {
		return ColorUtilities.getListBgColorId(nightMode);
	}

	public boolean getContentStatusBarNightMode() {
		return nightMode;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			originalScaleValue = savedInstanceState.getFloat(ORIGINAL_SCALE);
			scaleValue = savedInstanceState.getFloat(SCALE);
			descriptionResId = savedInstanceState.getInt(DESCRIPTION_RES_ID);
			if (savedInstanceState.containsKey(TRACK_DRAW_INFO_KEY) && savedInstanceState.getBoolean(TRACK_DRAW_INFO_KEY)) {
				trackDrawInfo = new TrackDrawInfo(savedInstanceState);
			}
		} else {
			scaleValue = originalScaleValue;
		}
		MapActivity activity = requireMapActivity();
		activity.getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
		setHasOptionsMenu(true);
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		onBackPressedCallback.remove();
	}

	private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
		@Override
		public void handleOnBackPressed() {
			cancelSelectExaggeration();
		}
	};

	private void cancelSelectExaggeration() {
		requireActivity().getSupportFragmentManager().popBackStack();
		setElevationScaleFactor(originalScaleValue, true);
	}


	public void setOriginalScaleValue(float scaleValue) {
		originalScaleValue = scaleValue;
	}

	public void setElevationScaleFactor(float scale, boolean isFinished) {
		((ExaggerationChangeListener) getTargetFragment()).onExaggerationChanged(scale, isFinished);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putFloat(SCALE, scaleValue);
		outState.putFloat(ORIGINAL_SCALE, originalScaleValue);
		outState.putInt(DESCRIPTION_RES_ID, descriptionResId);
		if (trackDrawInfo != null) {
			outState.putBoolean(TRACK_DRAW_INFO_KEY, true);
			trackDrawInfo.saveToBundle(outState);
		}
	}

	protected void onResetToDefault() {
		scaleValue = originalScaleValue;
		updateApplyButton(isChangesMade());
		setupSlider();
		setElevationScaleFactor(scaleValue, false);
	}

	protected void onApplyButtonClick() {
		setElevationScaleFactor(scaleValue, true);
	}

	private void setupSlider() {
		float scaleFactor = scaleValue;
		scaleTv.setText(getFormattedScaleValue(app, scaleFactor));
		scaleSlider.addOnChangeListener(transparencySliderChangeListener);
		scaleSlider.setValueTo(MAX_VERTICAL_EXAGGERATION);
		scaleSlider.setValueFrom(MIN_VERTICAL_EXAGGERATION);
		scaleSlider.setValue(scaleFactor);
		scaleSlider.setStepSize(0.1f);
		int profileColor = settings.getApplicationMode().getProfileColor(nightMode);
		UiUtilities.setupSlider(scaleSlider, nightMode, profileColor);
	}

	private final Slider.OnChangeListener transparencySliderChangeListener = new Slider.OnChangeListener() {
		@Override
		public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
			if (fromUser) {
				scaleValue = value;
				if (trackDrawInfo != null) {
					trackDrawInfo.setAdditionalExaggeration(scaleValue);
				}
				scaleTv.setText(getFormattedScaleValue(app, value));
				updateApplyButton(isChangesMade());
				setElevationScaleFactor(scaleValue, false);
			}
		}
	};

	protected void updateApplyButton(boolean enable) {
		applyButton.setEnabled(enable);
		applyButton.setButtonType(enable ? PRIMARY : STROKED);
	}

	static public String getFormattedScaleValue(@NonNull OsmandApplication app, float scale) {
		DecimalFormat decimalFormat = new DecimalFormat("#");
		String formattedScale = "x" + (scale % 1 == 0 ? decimalFormat.format(scale) : scale);
		return scale == MIN_VERTICAL_EXAGGERATION ? app.getString(R.string.shared_string_none) : formattedScale;
	}

	private boolean isChangesMade() {
		return scaleValue != originalScaleValue;
	}

	private void setupToolBar(@NonNull View view) {
		ViewCompat.setElevation(view.findViewById(R.id.appbar), 5.0f);
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getContentIcon(R.drawable.ic_action_close));
		toolbar.setNavigationOnClickListener(v -> cancelSelectExaggeration());
		toolbar.setTitle(R.string.vertical_exaggeration);
		toolbar.setOnMenuItemClickListener((item) -> {
			if (item.getItemId() == R.id.reset_to_default) {
				onResetToDefault();
			}
			return false;
		});
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		verticalExaggerationDescription = view.findViewById(R.id.vertical_exaggeration_description);
		buttonsShadow = view.findViewById(R.id.buttons_shadow);
		controlButtons = view.findViewById(R.id.control_buttons);
		routeMenuTopShadowAll = view.findViewById(R.id.route_menu_top_shadow_all);
		scaleSlider = view.findViewById(R.id.scale_slider);
		scaleTv = view.findViewById(R.id.scale_value_tv);
		applyButton = view.findViewById(R.id.apply_button);
		applyButton.setOnClickListener(viewOnCLick -> {
			requireActivity().onBackPressed();
			onApplyButtonClick();
		});
		verticalExaggerationDescription.setText(descriptionResId);
		setupSlider();
		setupToolBar(view);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (trackDrawInfo != null) {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				mapActivity.getMapLayers().getGpxLayer().setTrackDrawInfo(trackDrawInfo);
			}
		}
	}

	@Override
	public int getHeaderViewHeight() {
		return menuTitleHeight;
	}

	@Override
	public boolean isHeaderViewDetached() {
		return false;
	}

	@Override
	public int getToolbarHeight() {
		return 0;
	}

	public interface ExaggerationChangeListener {
		void onExaggerationChanged(float exaggeration, boolean isFinished);
	}

	public static boolean showInstance(@NonNull FragmentManager manager,
	                                   @NonNull Fragment exaggerationChangeListener,
	                                   float originalScaleValue,
	                                   @StringRes int descriptionResId) {
		return showInstance(manager, exaggerationChangeListener, originalScaleValue, descriptionResId, null);
	}

	public static boolean showInstance(@NonNull FragmentManager manager,
	                                   @NonNull Fragment exaggerationChangeListener,
	                                   float originalScaleValue,
	                                   @StringRes int descriptionResId,
	                                   @Nullable TrackDrawInfo trackDrawInfo) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			VerticalExaggerationFragment fragment = new VerticalExaggerationFragment();
			fragment.setRetainInstance(true);
			fragment.setOriginalScaleValue(originalScaleValue);
			fragment.setTargetFragment(exaggerationChangeListener, 0);
			fragment.descriptionResId = descriptionResId;
			fragment.trackDrawInfo = trackDrawInfo;
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss();
			return true;
		}
		return false;
	}
}