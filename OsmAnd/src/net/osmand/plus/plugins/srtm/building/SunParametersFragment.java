package net.osmand.plus.plugins.srtm.building;

import static net.osmand.plus.dashboard.DashboardType.BUILDINGS_3D;
import static net.osmand.plus.plugins.srtm.SRTMPlugin.DEFAULT_HILLSHADE_SUN_ANGLE;
import static net.osmand.plus.plugins.srtm.SRTMPlugin.DEFAULT_HILLSHADE_SUN_AZIMUTH;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.slider.Slider;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.ConfigureMapOptionFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.layers.RadiusRulerControlLayer;

import java.util.Locale;

public class SunParametersFragment extends ConfigureMapOptionFragment {

	private TextView tvAzimuth;
	private Slider azimuthSlider;

	private TextView tvAltitude;
	private Slider altitudeSlider;

	private SunParametersController controller;

	@Nullable
	@Override
	protected String getToolbarTitle() {
		return getString(R.string.astro_name_sun);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		controller = SunParametersController.getExistedInstance(app);
		if (controller == null) {
			dismiss();
			return;
		}
		MapActivity activity = requireMapActivity();
		activity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				activity.getSupportFragmentManager().popBackStack();
				activity.getDashboard().setDashboardVisibility(true, BUILDINGS_3D, false);
			}
		});
	}

	@Override
	protected void setupMainContent(@NonNull ViewGroup container) {
		View view = inflate(R.layout.sun_parameters, container, false);

		azimuthSlider = view.findViewById(R.id.azimuth_slider);
		tvAzimuth = view.findViewById(R.id.azimuth_value_tv);

		altitudeSlider = view.findViewById(R.id.altitude_slider);
		tvAltitude = view.findViewById(R.id.altitude_value_tv);

		setupSliders();
		container.addView(view);
		updateApplyButton(controller.hasChanges());
	}

	private void setupSliders() {
		int azimuth = controller.getValidAzimuth();
		tvAzimuth.setText(formatAzimuth(azimuth));
		azimuthSlider.addOnChangeListener(azimuthSliderChangeListener);
		azimuthSlider.setValueFrom(controller.getMinAzimuth());
		azimuthSlider.setValueTo(controller.getMaxAzimuth());
		azimuthSlider.setValue(azimuth);
		UiUtilities.setupSlider(azimuthSlider, nightMode, getAppModeColor(nightMode));

		int altitude = controller.getValidAltitude();
		tvAltitude.setText(formatAltitude(altitude));
		altitudeSlider.addOnChangeListener(altitudeSliderChangeListener);
		altitudeSlider.setValueFrom(controller.getMinAltitude());
		altitudeSlider.setValueTo(controller.getMaxAltitude());
		altitudeSlider.setValue(altitude);
		UiUtilities.setupSlider(altitudeSlider, nightMode, getAppModeColor(nightMode));
	}

	private final Slider.OnChangeListener azimuthSliderChangeListener = new Slider.OnChangeListener() {
		@Override
		public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
			if (fromUser) {
				int val = (int) value;
				controller.setAzimuth(val);
				tvAzimuth.setText(formatAzimuth(val));
				updateApplyButton(controller.hasChanges());
				refreshMap();
			}
		}
	};

	private final Slider.OnChangeListener altitudeSliderChangeListener = new Slider.OnChangeListener() {
		@Override
		public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
			if (fromUser) {
				int val = (int) value;
				controller.setAltitude(val);
				tvAltitude.setText(formatAltitude(val));
				updateApplyButton(controller.hasChanges());
				refreshMap();
			}
		}
	};

	@Override
	protected void applyChanges() {
		controller.onApplyChanges();
	}

	@Override
	protected void resetToDefault() {
		controller.setAzimuth(DEFAULT_HILLSHADE_SUN_AZIMUTH);
		controller.setAltitude(DEFAULT_HILLSHADE_SUN_ANGLE);
		setupSliders();
		updateApplyButton(controller.hasChanges());
		refreshMap();
	}

	@Override
	public void onDestroy() {
		if (controller != null) {
			controller.finishProcessIfNeeded(getActivity());
		}
		super.onDestroy();
	}

	@NonNull
	private String formatAltitude(int value) {
		return String.format(Locale.getDefault(), "%d°", value);
	}

	@NonNull
	private String formatAzimuth(int value) {
		String cardinalDirection = RadiusRulerControlLayer.getCardinalDirectionForDegrees(value);
		return String.format(Locale.getDefault(), "%d°, %s", value, cardinalDirection);
	}

	public static boolean showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, new SunParametersFragment(), TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss();
			return true;
		}
		return false;
	}
}