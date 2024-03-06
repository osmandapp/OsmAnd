package net.osmand.plus.card.color.palette.main;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.utils.UiUtilities;

public class ColorsPaletteCard extends BaseCard implements IColorsPalette {

	private final IColorsPaletteController controller;
	private final ColorsPaletteElements paletteElements;
	private final ColorsPaletteAdapter paletteAdapter;
	private RecyclerView rvColors;

	public ColorsPaletteCard(@NonNull FragmentActivity activity,
	                         @NonNull IColorsPaletteController controller) {
		this(activity, controller, true);
	}

	public ColorsPaletteCard(@NonNull FragmentActivity activity,
	                         @NonNull IColorsPaletteController controller,
	                         boolean usedOnMap) {
		super(activity, usedOnMap);
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
		setupAddCustomColorButton();
		setupAllColorsButton();
		askScrollToTargetColorPosition(controller.getSelectedColor(), false);
	}

	private void setupColorsPalette() {
		rvColors = view.findViewById(R.id.colors_list);
		rvColors.setLayoutManager(new LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false));
		rvColors.setClipToPadding(false);
		rvColors.setAdapter(paletteAdapter);
	}

	private void setupAddCustomColorButton() {
		ViewGroup container = view.findViewById(R.id.add_button_container);
		container.addView(paletteElements.createButtonAddColorView(container));
		container.setOnClickListener(v -> controller.onAddColorButtonClicked(activity));
	}

	private void setupAllColorsButton() {
		View buttonAllColors = view.findViewById(R.id.button_all_colors);
		buttonAllColors.setOnClickListener(v -> controller.onAllColorsButtonClicked(activity));
		updateAllColorsButton();
	}

	@Override
	public void updatePaletteColors(@Nullable PaletteColor targetPaletteColor) {
		paletteAdapter.updateColorsList();
		if (targetPaletteColor != null) {
			askScrollToTargetColorPosition(targetPaletteColor, true);
		}
		if (controller.isAccentColorCanBeChanged()) {
			updateAllColorsButton();
		}
	}

	@Override
	public void updatePaletteSelection(@Nullable PaletteColor oldColor, @NonNull PaletteColor newColor) {
		paletteAdapter.askNotifyItemChanged(oldColor);
		paletteAdapter.askNotifyItemChanged(newColor);
		askScrollToTargetColorPosition(newColor, true);
		if (controller.isAccentColorCanBeChanged()) {
			updateAllColorsButton();
		}
	}

	private void updateAllColorsButton() {
		View buttonAllColors = view.findViewById(R.id.button_all_colors);
		int controlsAccentColor = controller.getControlsAccentColor(nightMode);
		UiUtilities.setupListItemBackground(activity, buttonAllColors, controlsAccentColor);
	}

	private void askScrollToTargetColorPosition(@Nullable PaletteColor targetPaletteColor,
	                                            boolean useSmoothScroll) {
		if (targetPaletteColor == null) {
			return;
		}
		int targetPosition = paletteAdapter.indexOf(targetPaletteColor);
		LinearLayoutManager lm = (LinearLayoutManager) rvColors.getLayoutManager();
		int firstVisiblePosition = lm != null ? lm.findFirstCompletelyVisibleItemPosition() : 0;
		int lastVisiblePosition = lm != null ? lm.findLastCompletelyVisibleItemPosition() : paletteAdapter.getItemCount();
		if (targetPosition < firstVisiblePosition || targetPosition > lastVisiblePosition) {
			if (useSmoothScroll) {
				rvColors.smoothScrollToPosition(targetPosition);
			} else {
				rvColors.scrollToPosition(targetPosition);
			}
		}
	}
}
