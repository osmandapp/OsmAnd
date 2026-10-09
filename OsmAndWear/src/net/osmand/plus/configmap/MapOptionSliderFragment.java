package net.osmand.plus.configmap;

import static net.osmand.plus.plugins.srtm.SRTMPlugin.MIN_VERTICAL_EXAGGERATION;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.slider.Slider;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import java.text.DecimalFormat;

public abstract class MapOptionSliderFragment extends ConfigureMapOptionFragment {

	private static final String VALUE = "value";
	private static final String ORIGINAL_VALUE = "original_value";
	private static final String VALUE_SAVED = "value_saved";

	protected Slider slider;
	protected TextView valueTv;

	protected float value;
	protected float originalValue;
	protected boolean valueSaved;

	@Override
	public int getStatusBarColorId() {
		return ColorUtilities.getListBgColorId(nightMode);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			value = savedInstanceState.getFloat(VALUE);
			originalValue = savedInstanceState.getFloat(ORIGINAL_VALUE);
			valueSaved = savedInstanceState.getBoolean(VALUE_SAVED);
		}
		MapActivity activity = requireMapActivity();
		activity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				activity.getSupportFragmentManager().popBackStack();

				if (!valueSaved) {
					onValueChanged(originalValue);
				}
			}
		});
	}

	@Override
	protected void setupMainContent(@NonNull ViewGroup container) {
		View view = themedInflater.inflate(R.layout.map_option_slider_fragment, container);
		setupSlider(view);
		updateContent();
	}

	protected void setupSlider(@NonNull View view) {
		slider = view.findViewById(R.id.slider);
		valueTv = view.findViewById(R.id.scale_value);

		slider.addOnChangeListener(getChangeListener());
		int color = settings.getApplicationMode().getProfileColor(nightMode);
		UiUtilities.setupSlider(slider, nightMode, color);
	}

	protected void updateContent() {
		slider.setValue(value);
		valueTv.setText(getFormattedValue(app, value));
		updateApplyButton(isChangesMade());
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle bundle) {
		super.onSaveInstanceState(bundle);
		bundle.putFloat(VALUE, value);
		bundle.putFloat(ORIGINAL_VALUE, originalValue);
		bundle.putBoolean(VALUE_SAVED, valueSaved);
	}

	protected boolean isChangesMade() {
		return value != originalValue;
	}

	@NonNull
	private Slider.OnChangeListener getChangeListener() {
		return (slider, value, fromUser) -> {
			if (fromUser) {
				this.value = value;
				valueTv.setText(getFormattedValue(app, value));
				updateApplyButton(isChangesMade());
				onValueChanged(value);
			}
		};
	}

	protected void applyChanges() {
		valueSaved = true;
		onValueChanged(value);
	}

	protected void resetToDefault() {
		value = originalValue;
		updateContent();
		onValueChanged(value);
	}

	protected void onValueChanged(float value) {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof MapOptionSliderListener) {
			((MapOptionSliderListener) fragment).onMapOptionChanged(value);
		}
	}

	@NonNull
	public static String getFormattedValue(@NonNull OsmandApplication app, float value) {
		DecimalFormat format = new DecimalFormat("#");
		String formattedValue = "x" + (value % 1 == 0 ? format.format(value) : value);
		return value > MIN_VERTICAL_EXAGGERATION ? formattedValue : app.getString(R.string.shared_string_none);
	}

	public interface MapOptionSliderListener {
		void onMapOptionChanged(float value);
	}
}