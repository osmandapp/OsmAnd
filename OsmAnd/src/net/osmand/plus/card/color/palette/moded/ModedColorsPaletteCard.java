package net.osmand.plus.card.color.palette.moded;

import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.card.color.palette.main.ColorsPaletteCard;
import net.osmand.plus.card.color.palette.main.data.PaletteMode;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ModedColorsPaletteCard extends ColorsPaletteCard {

	private final ModedColorsPaletteController controller;

	public ModedColorsPaletteCard(@NonNull FragmentActivity activity,
	                              @NonNull ModedColorsPaletteController controller,
	                              boolean usedOnMap) {
		super(activity, controller, usedOnMap);
		this.controller = controller;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.card_moded_colors_palette;
	}

	@Override
	protected void updateContent() {
		super.updateContent();
		setupToggleButtons(view.findViewById(R.id.custom_radio_buttons));
	}

	private void setupToggleButtons(LinearLayout buttonsContainer) {
		TextToggleButton radioGroup = new TextToggleButton(app, buttonsContainer, nightMode);
		List<TextRadioItem> toggleButtons = collectRadioItems();
		radioGroup.setItems(toggleButtons);

		PaletteMode selectedPaletteMode = controller.getSelectedPaletteMode();
		TextRadioItem selectedItem = findRadioItem(toggleButtons, selectedPaletteMode.getTag());
		radioGroup.setSelectedItem(selectedItem);
	}

	@NonNull
	private List<TextRadioItem> collectRadioItems() {
		List<TextRadioItem> radioItems = new ArrayList<>();
		for (PaletteMode paletteMode : controller.getAvailablePaletteModes()) {
			TextRadioItem radioItem = new TextRadioItem(paletteMode.getTitle());
			radioItem.setTag(paletteMode.getTag());
			radioItem.setOnClickListener((radio, view) -> {
				controller.selectPaletteMode(paletteMode);
				return true;
			});
			radioItems.add(radioItem);
		}
		return radioItems;
	}

	private TextRadioItem findRadioItem(List<TextRadioItem> radioItems, @Nullable Object tag) {
		if (tag != null) {
			for (TextRadioItem radioItem : radioItems) {
				if (Objects.equals(radioItem.getTag(), tag)) {
					return radioItem;
				}
			}
		}
		return radioItems.get(0);
	}
}
