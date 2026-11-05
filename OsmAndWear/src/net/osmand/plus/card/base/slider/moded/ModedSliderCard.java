package net.osmand.plus.card.base.slider.moded;

import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.card.base.slider.SliderCard;
import net.osmand.plus.card.base.slider.moded.data.SliderMode;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.widgets.multistatetoggle.IconToggleButton;
import net.osmand.plus.widgets.multistatetoggle.IconToggleButton.IconRadioItem;
import net.osmand.plus.widgets.multistatetoggle.RadioItem;

import java.util.ArrayList;
import java.util.List;

public class ModedSliderCard extends SliderCard implements IModedSliderComponent {

	private final IModedSliderController controller;
	private IconToggleButton segmentedButton;

	public ModedSliderCard(@NonNull FragmentActivity activity, @NonNull IModedSliderController controller) {
		this(activity, controller, true);
	}

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
	@Override
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
		segmentedButton = new IconToggleButton(app, container, nightMode);

		List<IconRadioItem> radioItems = new ArrayList<>();
		for (SliderMode sliderMode : controller.getSliderModes()) {
			int iconId = sliderMode.getIconId();
			IconRadioItem radioItem = new IconRadioItem(iconId);
			radioItem.setOnClickListener((r, v) -> {
				controller.askSelectSliderMode(sliderMode);
				return false;
			});
			radioItem.setTag(sliderMode);
			radioItems.add(radioItem);

		}
		segmentedButton.setItems(radioItems);
		updateSegmentedButtonSelection();
	}

	@Override
	public void updateSegmentedButtonSelection() {
		SliderMode selectedMode = controller.getSelectedSliderMode();
		segmentedButton.setSelectedItemByTag(selectedMode);
	}

	@Override
	public void updateSliderVisibility() {
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.slider_component_container), controller.isSliderVisible());
	}
}
