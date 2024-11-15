package net.osmand.plus.quickaction;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.google.android.material.slider.Slider;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

public abstract class SliderButtonsCard extends MapBaseCard {

	protected Slider slider;
	protected TextView title;
	protected TextView description;
	protected ImageButton increaseButton;
	protected ImageButton decreaseButton;

	@Override
	public int getCardLayoutId() {
		return R.layout.slider_with_buttons;
	}

	public SliderButtonsCard(@NonNull MapActivity activity) {
		super(activity, false);
	}

	@Override
	protected void updateContent() {
		setupHeader(view);
		setupSlider(view);
		setupButtons(view);
	}

	protected void setupHeader(@NonNull View view) {
		title = view.findViewById(R.id.title);
		description = view.findViewById(R.id.description);
	}

	protected void setupSlider(@NonNull View view) {
		slider = view.findViewById(R.id.slider);
		slider.addOnChangeListener((s, value, fromUser) -> {
			if (fromUser) {
				onValueSelected(value);
			}
		});
	}

	protected void setupButtons(@NonNull View view) {
		increaseButton = view.findViewById(R.id.increase_button);
		increaseButton.setImageDrawable(getPersistentPrefIcon(R.drawable.ic_zoom_in));
		increaseButton.setOnClickListener(v -> {
			int value = (int) (slider.getValue() + slider.getStepSize());
			if (value <= slider.getValueTo()) {
				slider.setValue(value);
				onValueSelected(value);
			}
		});
		decreaseButton = view.findViewById(R.id.decrease_button);
		decreaseButton.setImageDrawable(getPersistentPrefIcon(R.drawable.ic_zoom_out));
		decreaseButton.setOnClickListener(v -> {
			int value = (int) (slider.getValue() - slider.getStepSize());
			if (value >= slider.getValueFrom()) {
				slider.setValue(value);
				onValueSelected(value);
			}
		});
	}

	protected void onValueSelected(float value) {
		increaseButton.setEnabled(value < slider.getValueTo());
		decreaseButton.setEnabled(value > slider.getValueFrom());
	}

	@NonNull
	protected abstract String getFormattedValue(float value);

	@NonNull
	protected Drawable getPersistentPrefIcon(@DrawableRes int iconId) {
		Drawable enabled = getColoredIcon(iconId, ColorUtilities.getActiveColorId(nightMode));
		Drawable disabled = getColoredIcon(iconId, ColorUtilities.getSecondaryIconColorId(nightMode));

		return AndroidUtils.createEnabledStateListDrawable(disabled, enabled);
	}
}
