package net.osmand.plus.card.icon;

import android.annotation.SuppressLint;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.tools.HorizontalSpaceItemDecoration;

public class IconsPaletteCard<IconData> extends BaseCard implements IIconsPalette<IconData> {

	private final IIconsPaletteController<IconData> controller;
	private final IconsPaletteAdapter<IconData> paletteAdapter;
	private RecyclerView rvIcons;

	public IconsPaletteCard(@NonNull FragmentActivity activity,
	                        @NonNull IIconsPaletteController<IconData> controller, boolean usedOnMap) {
		this(activity, controller, null, usedOnMap);
	}

	public IconsPaletteCard(@NonNull FragmentActivity activity,
	                        @NonNull IIconsPaletteController<IconData> controller,
	                        @Nullable ApplicationMode appMode, boolean usedOnMap) {
		super(activity, appMode, usedOnMap);
		this.controller = controller;
		controller.bindPalette(this);
		paletteAdapter = new IconsPaletteAdapter<>(activity, controller, nightMode);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.card_icons_palette;
	}

	@Override
	protected void updateContent() {
		setupIconsPalette();
		setupAllIconsButton();
		askScrollToTargetIconPosition(controller.getSelectedIcon(), false);
	}

	private void setupIconsPalette() {
		rvIcons = view.findViewById(R.id.icons_list);
		rvIcons.setLayoutManager(new LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false));
		rvIcons.setPadding(controller.getRecycleViewHorizontalPadding(), rvIcons.getPaddingTop(), controller.getRecycleViewHorizontalPadding(), rvIcons.getPaddingBottom());
		rvIcons.addItemDecoration(new HorizontalSpaceItemDecoration(controller.getHorizontalIconsSpace()));
		rvIcons.setClipToPadding(false);
		rvIcons.setAdapter(paletteAdapter);
	}

	@SuppressLint("NotifyDataSetChanged")
	@Override
	public void updatePaletteColors() {
		if (controller.isAccentColorCanBeChanged()) {
			paletteAdapter.notifyDataSetChanged();
			setupAllIconsButton();
		}
	}

	public void setupAllIconsButton() {
		View buttonAllIcons = view.findViewById(R.id.button_all_icons);
		if (buttonAllIcons != null) {
			buttonAllIcons.setOnClickListener(v -> controller.onAllIconsButtonClicked(activity));
			updateAllIconsButton(buttonAllIcons);
		}
	}

	@Override
	public void updatePaletteIcons(@Nullable IconData targetIcon) {
		paletteAdapter.updateIconsList();
		askScrollToTargetIconPosition(targetIcon, true);
	}

	@Override
	public void updatePaletteSelection(@Nullable IconData oldIcon, @NonNull IconData newIcon) {
		paletteAdapter.askNotifyItemChanged(oldIcon);
		paletteAdapter.askNotifyItemChanged(newIcon);
		askScrollToTargetIconPosition(newIcon, true);
	}

	private void updateAllIconsButton(@NonNull View buttonAllIcons) {
		int controlsAccentColor = controller.getControlsAccentColor(nightMode);
		UiUtilities.setupListItemBackground(activity, buttonAllIcons, controlsAccentColor);
	}

	private void askScrollToTargetIconPosition(@Nullable IconData targetIcon, boolean useSmoothScroll) {
		int targetPosition = paletteAdapter.indexOf(targetIcon);
		LinearLayoutManager layoutManager = (LinearLayoutManager) rvIcons.getLayoutManager();
		if (layoutManager != null && targetPosition >= 0 && targetPosition < paletteAdapter.getItemCount()) {
			int firstVisiblePosition = layoutManager.findFirstCompletelyVisibleItemPosition();
			int lastVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition();
			if (targetPosition < firstVisiblePosition || targetPosition > lastVisiblePosition) {
				if (useSmoothScroll) {
					rvIcons.smoothScrollToPosition(targetPosition);
				} else {
					rvIcons.scrollToPosition(targetPosition);
				}
			}
		}
	}
}
