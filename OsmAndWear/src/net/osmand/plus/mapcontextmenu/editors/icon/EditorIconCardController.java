package net.osmand.plus.mapcontextmenu.editors.icon;

import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.card.base.multistate.BaseMultiStateCardController;
import net.osmand.plus.card.base.multistate.CardState;
import net.osmand.plus.card.icon.IconsPaletteCard;
import net.osmand.plus.card.icon.IconsPaletteController;
import net.osmand.plus.card.icon.IconsPaletteElements;
import net.osmand.plus.mapcontextmenu.editors.icon.data.IconsCategory;

import java.util.ArrayList;
import java.util.List;

public class EditorIconCardController extends BaseMultiStateCardController {

	private final EditorIconController centralController;
	private IconsPaletteController<String> paletteController;

	public EditorIconCardController(@NonNull OsmandApplication app,
	                                @NonNull EditorIconController centralController) {
		super(app);
		this.centralController = centralController;
		this.selectedState = findCardState(centralController.getSelectedCategory());
		initPaletteController();
	}

	@NonNull
	@Override
	protected List<CardState> collectSupportedCardStates() {
		boolean previousCategoryIsTop = false;
		List<CardState> result = new ArrayList<>();
		for (IconsCategory category : centralController.getCategories()) {
			CardState cardState = new CardState(category.getTranslation());
			cardState.setTag(category);
			cardState.setShowTopDivider(previousCategoryIsTop && !category.isTopCategory());
			previousCategoryIsTop = category.isTopCategory();
			result.add(cardState);
		}
		return result;
	}

	@Override
	protected void onSelectCardState(@NonNull CardState cardState) {
		centralController.setSelectedCategory((IconsCategory) cardState.getTag());
	}

	public void updateSelectedCardState() {
		selectedState = findCardState(centralController.getSelectedCategory());
		card.updateSelectedCardState();
	}

	public void updateIconsSelection() {
		paletteController.onSelectIconFromPalette(centralController.getSelectedIconKey());
	}

	@NonNull
	@Override
	public String getCardTitle() {
		return app.getString(R.string.select_icon_profile_dialog_title);
	}

	@NonNull
	@Override
	public String getCardStateSelectorTitle() {
		IconsCategory selectedCategory = (IconsCategory) selectedState.getTag();
		return selectedCategory != null ? selectedCategory.getTranslation() : "";
	}

	@Override
	public void onBindCardContent(@NonNull FragmentActivity activity, @NonNull ViewGroup container,
	                              boolean nightMode, boolean usedOnMap) {
		container.removeAllViews();
		paletteController.setIcons(getSelectedCategoryIconKeys());
		paletteController.setSelectedIcon(getSelectedIconKey());
		container.addView(new IconsPaletteCard<>(activity, paletteController, usedOnMap).build());
	}

	@NonNull
	private List<String> getSelectedCategoryIconKeys() {
		IconsCategory selectedCategory = (IconsCategory) selectedState.getTag();
		return selectedCategory != null ? selectedCategory.getIconKeys() : new ArrayList<>();
	}

	@NonNull
	private String getSelectedIconKey() {
		String selectedIconKey = centralController.getSelectedIconKey();
		return selectedIconKey != null ? selectedIconKey : "";
	}

	private void initPaletteController() {
		paletteController = new IconsPaletteController<>(app) {
			@NonNull
			@Override
			public IconsPaletteElements<String> getPaletteElements(@NonNull Context context, boolean nightMode) {
				return centralController.getPaletteElements(context, nightMode);
			}

			@Override
			public String getPaletteTitle() {
				return getCardTitle();
			}

			@Override
			public void onAllIconsButtonClicked(@NonNull FragmentActivity activity) {
				EditorIconPaletteFragment.showInstance(activity, centralController.getScreenController());
			}

			@Override
			public int getIconsAccentColor(boolean nightMode) {
				return centralController.getControlsAccentColor();
			}

			@Override
			public boolean isAccentColorCanBeChanged() {
				return true;
			}
		};
		paletteController.setPaletteListener(icon -> centralController.onIconSelectedFromPalette(icon, null));
	}

	public void askUpdateColoredPaletteElements() {
		paletteController.askUpdateColoredPaletteElements();
	}

	public boolean isNightMode() {
		return card.isNightMode();
	}
}
