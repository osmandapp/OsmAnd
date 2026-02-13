package net.osmand.plus.plugins.srtm;

import static net.osmand.plus.dashboard.DashboardType.BUILDINGS_3D;

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
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

import java.util.Locale;

public class Buildings3DVisibilityFragment extends ConfigureMapOptionFragment {
	public static final String VISIBILITY = "visibility";

	private SRTMPlugin srtmPlugin;
	private TextView visibilityTv;
	private Slider visibilitySlider;

	private float originalVisibilityValue;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		srtmPlugin = PluginsHelper.getPlugin(SRTMPlugin.class);

		if (savedInstanceState != null && savedInstanceState.containsKey(VISIBILITY)) {
			originalVisibilityValue = savedInstanceState.getInt(VISIBILITY);
		} else if (srtmPlugin != null) {
			originalVisibilityValue = srtmPlugin.BUILDINGS_3D_ALPHA.get();
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
	public void onDestroy() {
		srtmPlugin.apply3DBuildingsAlpha(originalVisibilityValue);
		super.onDestroy();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putFloat(VISIBILITY, visibilitySlider.getValue() / 100);
	}

	@Nullable
	@Override
	protected String getToolbarTitle() {
		return getString(R.string.enable_3d_objects);
	}

	@Override
	protected void resetToDefault() {
		srtmPlugin.reset3DBuildingAlphaToDefault();
		updateApplyButton(isChangesMade());
		setupSlider();
		refreshMap();
	}

	@Override
	protected void setupMainContent(@NonNull ViewGroup container) {
		View view = inflate(R.layout.buildings_3d_visibility_fragment, container, false);
		visibilitySlider = view.findViewById(R.id.transparency_slider);
		visibilityTv = view.findViewById(R.id.transparency_value_tv);

		setupSlider();
		container.addView(view);
	}

	@Override
	protected void applyChanges() {
		originalVisibilityValue = visibilitySlider.getValue() / 100;
		float currentAlpha = originalVisibilityValue;
		srtmPlugin.BUILDINGS_3D_ALPHA.set(currentAlpha);
	}

	private void setupSlider() {
		float transparencyValue = srtmPlugin.BUILDINGS_3D_ALPHA.get() * 100;
		String transparency = String.format(Locale.getDefault(), "%d%%", (int) transparencyValue);
		visibilityTv.setText(transparency);

		visibilitySlider.addOnChangeListener(transparencySliderChangeListener);
		visibilitySlider.setValueTo(100);
		visibilitySlider.setValueFrom(10);
		visibilitySlider.setValue(transparencyValue);
		int profileColor = settings.getApplicationMode().getProfileColor(nightMode);
		UiUtilities.setupSlider(visibilitySlider, nightMode, profileColor);
	}

	private final Slider.OnChangeListener transparencySliderChangeListener = new Slider.OnChangeListener() {
		@Override
		public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
			if (fromUser) {
				String transparencyStr = (int) value + "%";
				visibilityTv.setText(transparencyStr);
				srtmPlugin.apply3DBuildingsAlpha(value / 100);
				refreshMap();
				updateApplyButton(isChangesMade());
			}
		}
	};

	private boolean isChangesMade() {
		return (int) (srtmPlugin.BUILDINGS_3D_ALPHA.get() * 100) != (int) visibilitySlider.getValue();
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, new Buildings3DVisibilityFragment(), TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss();
		}
	}
}