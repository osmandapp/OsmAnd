package net.osmand.plus.configmap;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.slider.RangeSlider;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.containers.Limits;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;

import java.text.NumberFormat;
import java.util.List;

public class ZoomLevelsFragment extends ConfigureMapOptionFragment {

	private ZoomLevelsController controller;
	private RangeSlider slider;
	private TextView minText;
	private TextView maxText;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		controller = ZoomLevelsController.getExistedInstance(app);
		if (controller != null) {
			MapActivity activity = requireMapActivity();
			activity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
				@Override
				public void handleOnBackPressed() {
					controller.onCloseScreen(activity);
				}
			});
		} else {
			dismiss();
		}
	}

	@Nullable
	@Override
	protected String getToolbarTitle() {
		return controller.getDialogTitle();
	}

	@Override
	protected void setupMainContent(@NonNull ViewGroup container) {
		View view = inflate(R.layout.zoom_levels_fragment, container, false);
		slider = view.findViewById(R.id.zoom_slider);
		minText = view.findViewById(R.id.zoom_value_min);
		maxText = view.findViewById(R.id.zoom_value_max);

		setupSlider();
		container.addView(view);
	}

	private void setupSlider() {
		Limits<Integer> supportedLimits = controller.getSupportedLimits();
		Limits<Integer> selectedLimits = controller.getSelectedLimits();
		slider.setValueFrom(supportedLimits.min());
		slider.setValueTo(supportedLimits.max());
		slider.setValues((float) selectedLimits.min(), (float) selectedLimits.max());
		updateLabels();

		slider.addOnChangeListener((slider, value, fromUser) -> {
			List<Float> values = slider.getValues();
			if (values.size() > 1) {
				controller.setSelectedLimits(values.get(0), values.get(1));
				updateApplyButton(controller.hasChanges());
				updateLabels();
				refreshMap();
			}
		});

		int profileColor = settings.getApplicationMode().getProfileColor(nightMode);
		UiUtilities.setupSlider(slider, nightMode, profileColor, true);
	}

	private void updateLabels() {
		Limits<Integer> selectedLimits = controller.getSelectedLimits();
		NumberFormat numberFormat = OsmAndFormatter.getNumberFormat(app);
		minText.setText(getString(R.string.ltr_or_rtl_combine_via_colon, getString(R.string.shared_string_min), numberFormat.format(selectedLimits.min())));
		maxText.setText(getString(R.string.ltr_or_rtl_combine_via_colon, getString(R.string.shared_string_max), numberFormat.format(selectedLimits.max())));
	}

	@Override
	protected void resetToDefault() {
		controller.onResetToDefault();
		updateApplyButton(controller.hasChanges());
		setupSlider();
		refreshMap();
	}

	@Override
	protected void applyChanges() {
		controller.onApplyChanges();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		controller.finishProcessIfNeeded(getActivity());
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, new ZoomLevelsFragment(), TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss();
		}
	}
}
