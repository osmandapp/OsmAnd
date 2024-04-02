package net.osmand.plus.configmap.tracks.appearance.subcontrollers;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.card.base.multistate.BaseMultiStateCardController;
import net.osmand.plus.card.base.multistate.CardState;
import net.osmand.plus.configmap.tracks.appearance.data.AppearanceData;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class StartFinishCardController extends BaseMultiStateCardController {

	private final AppearanceData appearanceData;

	public StartFinishCardController(@NonNull OsmandApplication app,
	                                 @NonNull AppearanceData appearanceData) {
		super(app, appearanceData.shouldShowStartFinish());
		this.appearanceData = appearanceData;
	}

	@NonNull
	@Override
	public String getCardTitle() {
		return app.getString(R.string.track_show_start_finish_icons);
	}

	@NonNull
	@Override
	public String getCardStateSelectorTitle() {
		return selectedCardState.toHumanString(app);
	}

	@Override
	public void onBindCardContent(@NonNull FragmentActivity activity,
	                              @NonNull ViewGroup container, boolean nightMode) {
		container.setVisibility(View.GONE);
	}

	@Override
	protected void onSelectCardState(@NonNull CardState cardState) {
		if (!Objects.equals(selectedCardState.getTag(), cardState.getTag())) {
			selectedCardState = cardState;
			appearanceData.setShowStartFinish((Boolean) selectedCardState.getTag());
			cardInstance.updateSelectedCardState();
		}
	}

	@NonNull
	protected List<CardState> collectSupportedCardStates() {
		return Arrays.asList(
				new CardState(R.string.shared_string_unchanged),
				new CardState(R.string.shared_string_on).setTag(true).setShowTopDivider(true),
				new CardState(R.string.shared_string_off).setTag(false)
		);
	}

}
