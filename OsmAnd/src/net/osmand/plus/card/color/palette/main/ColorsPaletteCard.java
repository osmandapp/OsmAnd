package net.osmand.plus.card.color.palette.main;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.card.color.palette.main.v2.IColorsPalette;
import net.osmand.plus.card.color.palette.main.v2.IColorsPaletteController;
import net.osmand.plus.card.color.palette.v2.PaletteCardAdapter;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.tools.HorizontalSpaceItemDecoration;
import net.osmand.shared.palette.domain.PaletteItem;

public class ColorsPaletteCard extends BaseCard implements IColorsPalette {

	private final IColorsPaletteController controller;
	private final ColorsPaletteElements paletteElements;
	private final PaletteCardAdapter paletteAdapter;
	private RecyclerView rvColors;

	public ColorsPaletteCard(@NonNull FragmentActivity activity,
	                         @NonNull IColorsPaletteController controller) {
		this(activity, controller, null, true);
	}

	public ColorsPaletteCard(@NonNull FragmentActivity activity,
	                         @NonNull IColorsPaletteController controller,
	                         @Nullable ApplicationMode appMode, boolean usedOnMap) {
		super(activity, appMode, usedOnMap);
		this.controller = controller;
		controller.bindPalette(this);
		paletteElements = new ColorsPaletteElements(activity, nightMode);
		paletteAdapter = new PaletteCardAdapter(activity, controller, nightMode);
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
		askScrollToTargetColorPosition(controller.getSelectedPaletteItem(), false);
	}

	private void setupColorsPalette() {
		rvColors = view.findViewById(R.id.colors_list);
		rvColors.setLayoutManager(new LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false));
		rvColors.addItemDecoration(new HorizontalSpaceItemDecoration(getDimen(R.dimen.content_padding_small_half)));
		rvColors.setClipToPadding(false);
		rvColors.setAdapter(paletteAdapter);
	}

	private void setupAddCustomColorButton() {
		// TODO: check should button be visible
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
	public void updatePaletteItems(@Nullable PaletteItem targetItem) {
		paletteAdapter.updateItemsList();
		if (targetItem != null) {
			askScrollToTargetColorPosition(targetItem, true);
		}
		if (controller.isAccentColorCanBeChanged()) {
			updateAllColorsButton();
		}
	}

	@Override
	public void updatePaletteSelection(@Nullable PaletteItem oldItem, @NonNull PaletteItem newItem) {
		paletteAdapter.askNotifyItemChanged(oldItem);
		paletteAdapter.askNotifyItemChanged(newItem);
		askScrollToTargetColorPosition(newItem, true);
		if (controller.isAccentColorCanBeChanged()) {
			updateAllColorsButton();
		}
	}

	private void updateAllColorsButton() {
		View buttonAllColors = view.findViewById(R.id.button_all_colors);
		int controlsAccentColor = controller.getControlsAccentColor(nightMode);
		UiUtilities.setupListItemBackground(activity, buttonAllColors, controlsAccentColor);
	}

	private void askScrollToTargetColorPosition(@Nullable PaletteItem targetItem,
	                                            boolean useSmoothScroll) {
		if (targetItem == null) {
			return;
		}
		int targetPosition = paletteAdapter.indexOf(targetItem);
		LinearLayoutManager lm = (LinearLayoutManager) rvColors.getLayoutManager();
		int firstVisiblePosition = lm != null ? lm.findFirstCompletelyVisibleItemPosition() : 0;
		int lastVisiblePosition = lm != null ? lm.findLastCompletelyVisibleItemPosition() : paletteAdapter.getItemCount();
		if (targetPosition >= 0 && (targetPosition < firstVisiblePosition || targetPosition > lastVisiblePosition)) {
			if (useSmoothScroll) {
				rvColors.smoothScrollToPosition(targetPosition);
			} else {
				rvColors.scrollToPosition(targetPosition);
			}
		}
	}
}
