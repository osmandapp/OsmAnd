package net.osmand.plus.quickaction;

import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.card.base.multistate.BaseMultiStateCardController;
import net.osmand.plus.card.base.multistate.CardState;
import net.osmand.plus.quickaction.actions.SelectMapLocationAction;

import java.util.ArrayList;
import java.util.List;

public class PointLocationCardController extends BaseMultiStateCardController {

	private final SelectMapLocationAction action;

	public PointLocationCardController(@NonNull OsmandApplication app,
	                                   @NonNull SelectMapLocationAction action) {
		super(app);
		this.action = action;
		this.selectedState = findCardState(action.isManualLocationSelection());
	}

	@Override
	public void onCardViewBuilt(@NonNull View view) {
		TextView tvCardTitle = card.getCardTitleView();
		if (tvCardTitle != null) {
			tvCardTitle.setTypeface(null, Typeface.NORMAL);
		}
	}

	@NonNull
	@Override
	public String getCardTitle() {
		return app.getString(R.string.point_location);
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
		selectedState = cardState;
		card.updateSelectedCardState();
		action.setUseManualSelection(cardState.getTag() instanceof Boolean && (Boolean) cardState.getTag());
	}

	@NonNull
	@Override
	protected List<CardState> collectSupportedCardStates() {
		List<CardState> states = new ArrayList<>();
		states.add(new CardState(R.string.shared_string_manual).setTag(true));
		states.add(new CardState(R.string.shared_string_map_center).setTag(false));
		return states;
	}
}

