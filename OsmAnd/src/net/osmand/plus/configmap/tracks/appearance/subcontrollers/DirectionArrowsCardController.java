package net.osmand.plus.configmap.tracks.appearance.subcontrollers;

import static net.osmand.plus.utils.ColorUtilities.getPrimaryTextColor;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.card.base.multistate.BaseMultiStateCardController;
import net.osmand.plus.card.base.multistate.CardState;
import net.osmand.plus.widgets.popup.PopUpMenuItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DirectionArrowsCardController extends BaseMultiStateCardController {

	private final List<CardState> supportedCardStates;
	private CardState selectedState;

	public DirectionArrowsCardController(@NonNull OsmandApplication app) {
		super(app);
		supportedCardStates = collectSupportedCardStates();
		selectedState = supportedCardStates.get(0);
	}

	@NonNull
	@Override
	public List<PopUpMenuItem> getPopUpMenuItems() {
		List<PopUpMenuItem> menuItems = new ArrayList<>();
		for (CardState cardState : supportedCardStates) {
			menuItems.add(new PopUpMenuItem.Builder(app)
					.setTitle(cardState.getTitle())
					.showTopDivider(shouldShowTopDivider(cardState))
					.setTitleColor(getPrimaryTextColor(app, cardInstance.isNightMode()))
					.setTag(cardState)
					.create()
			);
		}
		return menuItems;
	}

	@Override
	public void onPopUpMenuItemSelected(@NonNull FragmentActivity activity, @NonNull View view, @NonNull PopUpMenuItem item) {
		selectedState = (CardState) item.getTag();
		cardInstance.updateSelectedCardState();
		// TODO notify
	}

	@NonNull
	@Override
	public String getCardTitle() {
		return app.getString(R.string.gpx_direction_arrows);
	}

	@NonNull
	@Override
	public String getSelectorTitle() {
		return selectedState.getTitle();
	}

	@Override
	public void onBindCardContent(@NonNull FragmentActivity activity,
	                              @NonNull ViewGroup container, boolean nightMode) {
		container.setVisibility(View.GONE);
	}

	private boolean shouldShowTopDivider(@NonNull CardState cardState) {
		Boolean value = (Boolean) cardState.getTag();
		return value != null && value;
	}

	@NonNull
	private List<CardState> collectSupportedCardStates() {
		return Arrays.asList(
				new CardState(app.getString(R.string.shared_string_unchanged), null),
				new CardState(app.getString(R.string.shared_string_on), true),
				new CardState(app.getString(R.string.shared_string_off), false)
		);
	}
}
