package net.osmand.plus.configmap.tracks.appearance.subcontrollers;

import static net.osmand.shared.gpx.GpxParameter.SHOW_START_FINISH;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.card.base.multistate.BaseMultiStateCardController;
import net.osmand.plus.card.base.multistate.CardState;
import net.osmand.plus.configmap.tracks.appearance.data.AppearanceData;

import java.util.ArrayList;
import java.util.List;

public class StartFinishCardController extends BaseMultiStateCardController {

	private final AppearanceData data;
	private final boolean addUnchanged;

	public StartFinishCardController(@NonNull OsmandApplication app, @NonNull AppearanceData data, boolean addUnchanged) {
		super(app);
		this.data = data;
		this.addUnchanged = addUnchanged;
		this.selectedState = findCardState(data.getParameter(SHOW_START_FINISH));
	}

	@NonNull
	@Override
	public String getCardTitle() {
		return app.getString(R.string.track_show_start_finish_icons);
	}

	@NonNull
	@Override
	public String getCardStateSelectorTitle() {
		return selectedState.toHumanString(app);
	}

	@Override
	public void onBindCardContent(@NonNull FragmentActivity activity, @NonNull ViewGroup container,
	                              boolean nightMode, boolean usedOnMap) {
		container.setVisibility(View.GONE);
	}

	@Override
	protected void onSelectCardState(@NonNull CardState cardState) {
		if (cardState.getTitleId() != selectedState.getTitleId()) {
			selectedState = cardState;
			card.updateSelectedCardState();

			if (cardState.isOriginal()) {
				data.resetParameter(SHOW_START_FINISH);
			} else {
				data.setParameter(SHOW_START_FINISH, cardState.getTag());
			}
		}
	}

	@NonNull
	protected List<CardState> collectSupportedCardStates() {
		List<CardState> states = new ArrayList<>();
		if (addUnchanged) {
			states.add(new CardState(R.string.shared_string_unchanged));
		}
		states.add(new CardState(R.string.shared_string_original));
		states.add(new CardState(R.string.shared_string_on).setTag(true).setShowTopDivider(true));
		states.add(new CardState(R.string.shared_string_off).setTag(false));

		return states;
	}
}
