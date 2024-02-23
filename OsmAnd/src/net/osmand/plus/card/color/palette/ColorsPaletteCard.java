package net.osmand.plus.card.color.palette;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.card.color.palette.data.PaletteColor;
import net.osmand.plus.card.color.palette.data.PaletteSortingMode;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.utils.UiUtilities;

import java.util.List;

public class ColorsPaletteCard extends BaseCard implements IColorsPalette {

	private final IColorsPaletteController controller;
	private final ColorsPaletteElements paletteElements;
	private final ColorsPaletteAdapter paletteAdapter;
	private RecyclerView rvColors;

	public ColorsPaletteCard(@NonNull FragmentActivity activity,
	                         @NonNull IColorsPaletteController controller) {
		super(activity);
		this.controller = controller;
		controller.bindPalette(this);
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
		rvColors = view.findViewById(R.id.colors_list);
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
		int controlsAccentColor = controller.getControlsAccentColor(nightMode);
		UiUtilities.setupSelectableBackground(activity, buttonAllColors, controlsAccentColor);
		buttonAllColors.setOnClickListener(v -> controller.onAllColorsButtonClicked(activity));
	}

	@Override
	public void updatePaletteColors(@Nullable PaletteColor targetPaletteColor) {
		paletteAdapter.updateColorsList();
		if (targetPaletteColor != null) {
			List<PaletteColor> colors = controller.getColors(PaletteSortingMode.LAST_USED_TIME);
			int index = colors.indexOf(targetPaletteColor);
			rvColors.smoothScrollToPosition(index);
		}
	}

	@Override
	public void updatePaletteSelection(@Nullable PaletteColor oldColor, @NonNull PaletteColor newColor) {
		List<PaletteColor> paletteColors = controller.getColors(PaletteSortingMode.LAST_USED_TIME);
		int selectedColorIndex = paletteColors.indexOf(newColor);
		paletteAdapter.notifyItemChanged(paletteColors.indexOf(oldColor));
		paletteAdapter.notifyItemChanged(selectedColorIndex);
		rvColors.scrollToPosition(selectedColorIndex);
	}
}
