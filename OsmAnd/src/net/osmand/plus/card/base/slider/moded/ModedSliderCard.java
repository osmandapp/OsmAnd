package net.osmand.plus.card.base.slider.moded;

import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.card.base.slider.limited.LimitedSliderCard;
import net.osmand.plus.card.base.slider.moded.data.SliderMode;
import net.osmand.plus.widgets.multistatetoggle.IconToggleButton;
import net.osmand.plus.widgets.multistatetoggle.IconToggleButton.IconRadioItem;

import java.util.ArrayList;
import java.util.List;

public class ModedSliderCard extends LimitedSliderCard implements IModedSliderComponent {

	private final IModedSliderController controller;

	public ModedSliderCard(@NonNull FragmentActivity activity,
	                       @NonNull IModedSliderController controller, boolean usedOnMap) {
		super(activity, controller, usedOnMap);
		this.controller = controller;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.card_moded_slider_component;
	}

	@NonNull
	public View getSliderContainer() {
		return view.findViewById(R.id.slider_component_container);
	}

	@Override
	protected void updateContent() {
		super.updateContent();
		setupSegmentedButton();
		updateSliderVisibility();
	}

	private void setupSegmentedButton() {
		LinearLayout container = view.findViewById(R.id.custom_radio_buttons);
		IconToggleButton segmentedButton = new IconToggleButton(app, container, nightMode);

		IconRadioItem selectedRadioItem = null;
		List<IconRadioItem> radioItems = new ArrayList<>();
		for (SliderMode sliderMode : controller.getSliderModes()) {
			int iconId = sliderMode.getIconId();
			IconRadioItem radioItem = new IconRadioItem(iconId);
			radioItem.setOnClickListener((r, v) -> controller.askSelectSliderMode(sliderMode));
			radioItems.add(radioItem);
			if (controller.isSelectedSliderMode(sliderMode)) {
				selectedRadioItem = radioItem;
			}
		}
		segmentedButton.setItems(radioItems);
		segmentedButton.setSelectedItem(selectedRadioItem);
	}

	@Override
	public void updateControlsColor() {
		super.updateControlsColor();
		updateSegmentedButtonColor();
	}

	private void updateSegmentedButtonColor() {

	}

	@Override
	public void updateSliderVisibility() {
		updateVisibility(R.id.slider_component_container, controller.isSliderVisible());
	}
}
