package net.osmand.plus.card.base.multistate;

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

	protected IMultiStateCard card;
	protected List<CardState> states;
	protected CardState selectedState;

	public BaseMultiStateCardController(@NonNull OsmandApplication app) {
		this.app = app;
	}

	@Override
	public void bindComponent(@NonNull IMultiStateCard card) {
		this.card = card;
	}

	@Override
	public void onSelectorButtonClicked(@NonNull View view) {
		boolean nightMode = card.isNightMode();
		List<PopUpMenuItem> items = new ArrayList<>();
		for (CardState state : getCardStates()) {
			if (isCardStateAvailable(state)) {
				items.add(new PopUpMenuItem.Builder(app)
						.setTitle(state.toHumanString(app))
						.showTopDivider(state.isShowTopDivider())
						.setTitleColor(getPrimaryTextColor(app, nightMode))
						.setTag(state)
						.create()
				);
			}
		}
		PopUpMenuDisplayData data = new PopUpMenuDisplayData();
		data.anchorView = view;
		data.menuItems = items;
		data.nightMode = nightMode;
		data.onItemClickListener = item -> onSelectCardState((CardState) item.getTag());
		PopUpMenu.show(data);
	}

	@NonNull
	protected List<CardState> getCardStates() {
		if (states == null) {
			states = collectSupportedCardStates();
		}
		return states;
	}

	@NonNull
	protected CardState findCardState(@Nullable Object tag) {
		for (CardState cardState : getCardStates()) {
			if (Objects.equals(tag, cardState.getTag())) {
				return cardState;
			}
		}
		return states.get(0);
	}

	protected boolean isCardStateAvailable(@NonNull CardState cardState) {
		return true;
	}

	@NonNull
	protected abstract List<CardState> collectSupportedCardStates();

	protected abstract void onSelectCardState(@NonNull CardState cardState);
}
