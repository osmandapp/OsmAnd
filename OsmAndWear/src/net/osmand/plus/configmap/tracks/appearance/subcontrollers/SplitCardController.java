package net.osmand.plus.configmap.tracks.appearance.subcontrollers;

import static net.osmand.shared.gpx.GpxParameter.SPLIT_INTERVAL;
import static net.osmand.shared.gpx.GpxParameter.SPLIT_TYPE;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.card.base.multistate.BaseMultiStateCardController;
import net.osmand.plus.card.base.multistate.CardState;
import net.osmand.plus.card.base.simple.DescriptionCard;
import net.osmand.plus.configmap.tracks.appearance.data.AppearanceData;
import net.osmand.plus.track.GpxSplitType;
import net.osmand.plus.track.fragments.SplitIntervalBottomSheet;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class SplitCardController extends BaseMultiStateCardController {

	private static final int CARD_STATE_SELECT_ID = 1;

	private final AppearanceData appearanceData;
	private final boolean addUnchanged;

	public SplitCardController(@NonNull OsmandApplication app, @NonNull AppearanceData data, boolean addUnchanged) {
		super(app);
		this.appearanceData = data;
		this.addUnchanged = addUnchanged;
		this.selectedState = findCardState(data.getParameter(SPLIT_TYPE));
	}

	@NonNull
	@Override
	public String getCardTitle() {
		return app.getString(R.string.gpx_split_interval);
	}

	@NonNull
	@Override
	public String getCardStateSelectorTitle() {
		if (selectedState.getTag() == null) {
			return selectedState.toHumanString(app);
		}
		return getSplitIntervalSummary();
	}

	@Override
	public void onBindCardContent(@NonNull FragmentActivity activity, @NonNull ViewGroup container,
	                              boolean nightMode, boolean usedOnMap) {
		if (selectedState.getTag() == null) {
			if (container.getChildCount() == 0) {
				LayoutInflater inflater = UiUtilities.getInflater(activity, nightMode);
				inflater.inflate(R.layout.list_item_divider_with_padding_basic, container, true);
				container.addView(new DescriptionCard(activity, R.string.unchanged_parameter_summary).build());
			}
			container.setVisibility(View.VISIBLE);
		} else {
			container.setVisibility(View.GONE);
		}
	}

	@NonNull
	@Override
	protected List<CardState> collectSupportedCardStates() {
		List<CardState> list = new ArrayList<>();
		if (addUnchanged) {
			list.add(new CardState(R.string.shared_string_unchanged));
		}
		list.add(new CardState(R.string.shared_string_original));
		list.add(new CardState(R.string.shared_string_select).setTag(CARD_STATE_SELECT_ID).setShowTopDivider(true));

		return list;
	}

	@Override
	protected void onSelectCardState(@NonNull CardState cardState) {
		if (cardState.isOriginal()) {
			selectedState = cardState;
			card.updateSelectedCardState();
			appearanceData.resetParameter(SPLIT_TYPE);
			appearanceData.resetParameter(SPLIT_INTERVAL);
		} else if (cardState.getTag() == null) {
			onSplitSelected(cardState, null, null);
		} else {
			FragmentActivity activity = card.getActivity();
			SplitIntervalBottomSheet.showInstance(activity.getSupportFragmentManager(), null);
		}
	}

	public void onSplitSelected(@Nullable Integer splitType, @Nullable Double splitInterval) {
		onSplitSelected(findCardState(CARD_STATE_SELECT_ID), splitType, splitInterval);
	}

	private void onSplitSelected(@NonNull CardState cardState, @Nullable Integer splitType, @Nullable Double splitInterval) {
		selectedState = cardState;
		appearanceData.setParameter(SPLIT_TYPE, splitType);
		appearanceData.setParameter(SPLIT_INTERVAL, splitInterval);
		card.updateSelectedCardState();
	}

	@NonNull
	private String getSplitIntervalSummary() {
		String summary = "";
		Integer splitType = appearanceData.getParameter(SPLIT_TYPE);
		Double splitInterval = appearanceData.getParameter(SPLIT_INTERVAL);
		if (splitInterval != null && splitType != null) {
			if (splitType == GpxSplitType.NO_SPLIT.getType()) {
				summary = GpxSplitType.NO_SPLIT.getHumanString(app);
			} else if (splitType == GpxSplitType.DISTANCE.getType()) {
				String formattedDistance = OsmAndFormatter.getFormattedDistanceInterval(app, splitInterval, OsmAndFormatter.OsmAndFormatterParams.NO_TRAILING_ZEROS);
				summary = app.getString(R.string.ltr_or_rtl_combine_via_comma, GpxSplitType.DISTANCE.getHumanString(app), formattedDistance);
			} else if (splitType == GpxSplitType.TIME.getType()) {
				String formattedTime = OsmAndFormatter.getFormattedTimeInterval(app, splitInterval);
				summary = app.getString(R.string.ltr_or_rtl_combine_via_comma, GpxSplitType.TIME.getHumanString(app), formattedTime);
			}
		}
		return summary;
	}
}
