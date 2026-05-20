package net.osmand.plus.plugins.srtm.building;

import static net.osmand.plus.dashboard.DashboardType.BUILDINGS_3D;
import static net.osmand.plus.plugins.srtm.SRTMPlugin.BUILDINGS_3D_ALPHA_DEF_VALUE;

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

import java.util.Locale;

public class Buildings3DVisibilityFragment extends ConfigureMapOptionFragment {

	private TextView tvVisibility;
	private Slider visibilitySlider;

	private Buildings3DVisibilityController controller;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		controller = Buildings3DVisibilityController.getExistedInstance(app);
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
	public void onDestroy() {
		if (controller != null) {
			controller.finishProcessIfNeeded(getActivity());
		}
		super.onDestroy();
	}

	@Nullable
	@Override
	protected String getToolbarTitle() {
		return getString(R.string.enable_3d_objects);
	}

	@Override
	protected void resetToDefault() {
		controller.setVisibilityPercent(BUILDINGS_3D_ALPHA_DEF_VALUE * 100);
		setupSlider();
		updateApplyButton(controller.hasChanges());
		refreshMap();
	}

	@Override
	protected void setupMainContent(@NonNull ViewGroup container) {
		View view = inflate(R.layout.fragment_buildings_3d_visibility, container, false);
		visibilitySlider = view.findViewById(R.id.transparency_slider);
		tvVisibility = view.findViewById(R.id.transparency_value_tv);

		setupSlider();
		container.addView(view);
		updateApplyButton(controller.hasChanges());
	}

	@Override
	protected void applyChanges() {
		controller.onApplyChanges();
	}

	private void setupSlider() {
		float percent = controller.getValidVisibility();
		tvVisibility.setText(formatVisibilityPercent(percent));
		visibilitySlider.addOnChangeListener(transparencySliderChangeListener);
		visibilitySlider.setValueFrom(controller.getMinVisibility());
		visibilitySlider.setValueTo(controller.getMaxVisibility());
		visibilitySlider.setValue(percent);
		UiUtilities.setupSlider(visibilitySlider, nightMode, getAppModeColor(nightMode));
	}

	private final Slider.OnChangeListener transparencySliderChangeListener = new Slider.OnChangeListener() {
		@Override
		public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
			if (fromUser) {
				controller.setVisibilityPercent(value);
				tvVisibility.setText(formatVisibilityPercent(value));
				updateApplyButton(controller.hasChanges());
				refreshMap();
			}
		}
	};

	@NonNull
	private String formatVisibilityPercent(float alphaPercent) {
		return String.format(Locale.getDefault(), "%d%%", (int) alphaPercent);
	}

	public static boolean showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, new Buildings3DVisibilityFragment(), TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss();
			return true;
		}
		return false;
	}
}