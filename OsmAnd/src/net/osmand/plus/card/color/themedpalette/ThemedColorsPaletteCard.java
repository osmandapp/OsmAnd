package net.osmand.plus.card.color.themedpalette;

import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.card.color.palette.ColorsPaletteCard;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;

public class ThemedColorsPaletteCard extends ColorsPaletteCard {

	private final ThemedColorsPaletteController controller;

	public ThemedColorsPaletteCard(@NonNull FragmentActivity activity,
	                               @NonNull ThemedColorsPaletteController controller) {
		super(activity, controller);
		this.controller = controller;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.card_themed_colors_palette;
	}

	@Override
	protected void updateContent() {
		super.updateContent();
		setupToggleButtons(view.findViewById(R.id.custom_radio_buttons));
	}

	private void setupToggleButtons(LinearLayout buttonsContainer) {
		TextRadioItem day = createMapThemeButton(false);
		TextRadioItem night = createMapThemeButton(true);
		TextToggleButton radioGroup = new TextToggleButton(app, buttonsContainer, nightMode);
		radioGroup.setItems(day, night);
		radioGroup.setSelectedItem(controller.isUseNightMap() ? night : day);
	}

	private TextRadioItem createMapThemeButton(boolean isNight) {
		TextRadioItem item = new TextRadioItem(app.getString(!isNight ? R.string.day : R.string.night));
		item.setOnClickListener((radioItem, view) -> {
			controller.setUseNightMap(isNight);
			return true;
		});
		return item;
	}
}
