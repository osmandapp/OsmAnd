package net.osmand.plus.configmap.tracks.appearance.subcontrollers;

import static net.osmand.plus.utils.ColorUtilities.getPrimaryTextColor;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.card.base.multistate.BaseMultiStateCardController;
import net.osmand.plus.card.base.multistate.CardState;
import net.osmand.plus.configmap.tracks.appearance.data.AppearanceData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class DirectionArrowsController extends BaseMultiStateCardController {

	private final List<CardState> supportedCardStates;
	private final AppearanceData appearanceData;
	private CardState selectedState;

	public DirectionArrowsController(@NonNull OsmandApplication app,
	                                 @NonNull AppearanceData appearanceData) {
		super(app);
		this.appearanceData = appearanceData;
		supportedCardStates = collectSupportedCardStates();
		selectedState = findCardState(appearanceData.shouldShowArrows());
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
		appearanceData.setShowArrows((Boolean) selectedState.getTag());
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
	private CardState findCardState(@Nullable Object tag) {
		for (CardState cardState : supportedCardStates) {
			if (Objects.equals(tag, cardState.getTag())) {
				return cardState;
			}
		}
		return supportedCardStates.get(0);
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
