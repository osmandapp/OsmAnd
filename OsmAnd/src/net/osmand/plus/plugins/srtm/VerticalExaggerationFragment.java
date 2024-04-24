package net.osmand.plus.plugins.srtm;

import static net.osmand.plus.dashboard.DashboardOnMap.*;
import static net.osmand.plus.plugins.srtm.SRTMPlugin.MAX_VERTICAL_EXAGGERATION;
import static net.osmand.plus.plugins.srtm.SRTMPlugin.MIN_VERTICAL_EXAGGERATION;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.slider.Slider;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.configmap.ConfigureMapOptionFragment;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

import java.text.DecimalFormat;

public class VerticalExaggerationFragment extends ConfigureMapOptionFragment {

	private SRTMPlugin srtmPlugin;

	public static final String SCALE = "scale";

	private TextView scaleTv;
	private Slider scaleSlider;
	private float originalScaleValue;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		srtmPlugin = PluginsHelper.getPlugin(SRTMPlugin.class);

		if (savedInstanceState != null && savedInstanceState.containsKey(SCALE)) {
			originalScaleValue = savedInstanceState.getInt(SCALE);
		} else {
			originalScaleValue = getElevationScaleFactor();
		}
	}

	@Override
	protected DashboardType getBaseDashboardType() {
		return DashboardType.RELIEF_3D;
	}

	public float getElevationScaleFactor() {
		return srtmPlugin.getVerticalExaggerationScale();
	}

	public void setElevationScaleFactor(float scale) {
		srtmPlugin.setVerticalExaggerationScale(scale);
	}

	@Override
	public void onDestroy() {
		setElevationScaleFactor(originalScaleValue);
		super.onDestroy();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putFloat(SCALE, originalScaleValue);
	}

	@Override
	protected String getToolbarTitle() {
		return getString(R.string.vertical_exaggeration);
	}

	@Override
	protected void onResetToDefault() {
		srtmPlugin.resetVerticalExaggerationToDefault();
		updateApplyButton(isChangesMade());
		setupSlider();
		refreshMap();
	}

	@Override
	protected void setupMainContent() {
		View view = themedInflater.inflate(R.layout.vertical_exaggeration_fragment, null, false);
		scaleSlider = view.findViewById(R.id.scale_slider);
		scaleTv = view.findViewById(R.id.scale_value_tv);

		setupSlider();
		contentContainer.addView(view);
	}

	@Override
	protected void onApplyButtonClick() {
		originalScaleValue = getElevationScaleFactor();
	}

	private void setupSlider() {
		float scaleFactor = getElevationScaleFactor();
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
				scaleTv.setText(getFormattedScaleValue(app, value));
				setElevationScaleFactor(value);
				updateApplyButton(isChangesMade());
				refreshMap();
			}
		}
	};

	static public String getFormattedScaleValue(@NonNull OsmandApplication app, float scale) {
		DecimalFormat decimalFormat = new DecimalFormat("#");
		String formattedScale = "x" + (scale % 1 == 0 ? decimalFormat.format(scale) : scale);
		return scale == MIN_VERTICAL_EXAGGERATION ? app.getString(R.string.shared_string_none) : formattedScale;
	}

	private boolean isChangesMade() {
		return getElevationScaleFactor() != originalScaleValue;
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, new VerticalExaggerationFragment(), TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss();
		}
	}
}