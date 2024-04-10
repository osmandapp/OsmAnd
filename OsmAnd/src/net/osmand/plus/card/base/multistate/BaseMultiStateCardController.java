package net.osmand.plus.card.base.multistate;

import static net.osmand.plus.utils.ColorUtilities.getDisabledTextColor;
import static net.osmand.plus.utils.ColorUtilities.getPrimaryTextColor;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class BaseMultiStateCardController implements IMultiStateCardController {

	protected final OsmandApplication app;

	protected IMultiStateCard cardInstance;
	protected List<CardState> supportedCardStates;
	protected CardState selectedCardState;

	public BaseMultiStateCardController(@NonNull OsmandApplication app,
	                                    @Nullable Object selectedStateTag) {
		this.app = app;
		this.selectedCardState = findCardState(selectedStateTag);
	}

	@Override
	public void bindComponent(@NonNull IMultiStateCard cardInstance) {
		this.cardInstance = cardInstance;
	}

	@Override
	public void onSelectorButtonClicked(@NonNull View selectorView) {
		boolean nightMode = cardInstance.isNightMode();
		List<PopUpMenuItem> menuItems = new ArrayList<>();
		for (CardState cardState : getSupportedCardStates()) {
			int titleColor = isCardStateAvailable(cardState)
					? getPrimaryTextColor(app, nightMode)
					: getDisabledTextColor(app, nightMode);
			menuItems.add(new PopUpMenuItem.Builder(app)
					.setTitle(cardState.toHumanString(app))
					.showTopDivider(cardState.isShowTopDivider())
					.setTitleColor(titleColor)
					.setTag(cardState)
					.create()
			);
		}
		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = selectorView;
		displayData.menuItems = menuItems;
		displayData.nightMode = nightMode;
		displayData.onItemClickListener = item -> onSelectCardState((CardState) item.getTag());
		PopUpMenu.show(displayData);
	}

	@NonNull
	protected List<CardState> getSupportedCardStates() {
		if (supportedCardStates == null) {
			supportedCardStates = collectSupportedCardStates();
		}
		return supportedCardStates;
	}

	@NonNull
	protected CardState findCardState(@Nullable Object tag) {
		for (CardState cardState : getSupportedCardStates()) {
			if (Objects.equals(tag, cardState.getTag())) {
				return cardState;
			}
		}
		return supportedCardStates.get(0);
	}

	protected boolean isCardStateAvailable(@NonNull CardState cardState) {
		return true;
	}

	@NonNull
	protected abstract List<CardState> collectSupportedCardStates();

	protected abstract void onSelectCardState(@NonNull CardState cardState);
}
