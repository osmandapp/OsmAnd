package net.osmand.plus.card.color.palette;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.utils.UiUtilities;

public class ColorsPaletteCard extends BaseCard {

	private final IColorsPaletteUIController controller;
	private final ColorsPaletteElements paletteElements;
	private final ColorsPaletteAdapter paletteAdapter;

	public ColorsPaletteCard(@NonNull FragmentActivity activity,
	                         @NonNull IColorsPaletteUIController controller) {
		super(activity);
		this.controller = controller;
		controller.bindCard(this);
		paletteElements = new ColorsPaletteElements(activity, nightMode);
		paletteAdapter = new ColorsPaletteAdapter(activity, controller, nightMode);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.card_colors_palette;
	}

	@Override
	protected void updateContent() {
		setupColorsPalette();
		setupButtonAddCustomColor();
		setupButtonShowAllColors();
	}

	private void setupColorsPalette() {
		RecyclerView rvColors = view.findViewById(R.id.colors_list);
		rvColors.setLayoutManager(new LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false));
		rvColors.setClipToPadding(false);
		rvColors.setAdapter(paletteAdapter);
	}

	private void setupButtonAddCustomColor() {
		ViewGroup container = view.findViewById(R.id.add_button_container);
		container.addView(paletteElements.createButtonAddColorView(container, true));
		container.setOnClickListener(v -> controller.onAddColorButtonClicked(activity));
	}

	private void setupButtonShowAllColors() {
		View buttonAllColors = view.findViewById(R.id.button_all_colors);
		int controlsAccentColor = controller.getControlsAccentColor();
		UiUtilities.setupSelectableBackground(activity, buttonAllColors, controlsAccentColor);
		buttonAllColors.setOnClickListener(v -> controller.onAllColorsButtonClicked(activity));
	}

	public void updateColorsPalette() {
		RecyclerView rvColors = view.findViewById(R.id.colors_list);
		paletteAdapter.updateColorsList();
		int index = controller.getAllColors().indexOf(controller.getSelectedColor());
		rvColors.scrollToPosition(index);
	}

}
