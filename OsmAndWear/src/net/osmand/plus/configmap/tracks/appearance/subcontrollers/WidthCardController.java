package net.osmand.plus.configmap.tracks.appearance.subcontrollers;

import static net.osmand.shared.gpx.GpxParameter.WIDTH;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.containers.Limits;
import net.osmand.plus.card.base.multistate.BaseMultiStateCardController;
import net.osmand.plus.card.base.multistate.CardState;
import net.osmand.plus.card.base.simple.DescriptionCard;
import net.osmand.plus.card.base.slider.moded.ModedSliderCard;
import net.osmand.plus.card.color.IControlsColorProvider;
import net.osmand.plus.card.width.WidthComponentController;
import net.osmand.plus.card.width.WidthMode;
import net.osmand.plus.configmap.tracks.appearance.data.AppearanceData;
import net.osmand.plus.track.fragments.TrackAppearanceFragment.OnNeedScrollListener;
import net.osmand.plus.track.fragments.controller.TrackWidthController.ITrackWidthSelectedListener;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class WidthCardController extends BaseMultiStateCardController {

	private static final int UNCHANGED_STYLE_CARD_ID = 0;
	private static final int WIDTH_COMPONENT_CARD_ID = 1;

	private static final int CUSTOM_WIDTH_MIN = 1;
	private static final int CUSTOM_WIDTH_MAX = 24;

	private final AppearanceData appearanceData;
	private final boolean addUnchanged;

	private IControlsColorProvider controlsColorProvider;
	private WidthComponentController widthComponentController;
	private OnNeedScrollListener onNeedScrollListener;
	private ITrackWidthSelectedListener listener;

	public WidthCardController(@NonNull OsmandApplication app, @NonNull AppearanceData data, boolean addUnchanged) {
		super(app);
		this.appearanceData = data;
		this.addUnchanged = addUnchanged;
		this.selectedState = findCardState(data.getParameter(WIDTH));
	}

	public void setListener(@Nullable ITrackWidthSelectedListener listener) {
		this.listener = listener;
	}

	public void setOnNeedScrollListener(@NonNull OnNeedScrollListener onNeedScrollListener) {
		this.onNeedScrollListener = onNeedScrollListener;
	}

	public void setControlsColorProvider(@NonNull IControlsColorProvider controlsColorProvider) {
		this.controlsColorProvider = controlsColorProvider;
	}

	@NonNull
	@Override
	public String getCardTitle() {
		return app.getString(R.string.shared_string_width);
	}

	@NonNull
	@Override
	public String getCardStateSelectorTitle() {
		return selectedState.getTag() == null
				? selectedState.toHumanString(app)
				: getWidthComponentController().getSummary(app);
	}

	@Override
	protected void onSelectCardState(@NonNull CardState cardState) {
		if (cardState.isOriginal()) {
			selectedState = cardState;
			card.updateSelectedCardState();
			appearanceData.resetParameter(WIDTH);
		} else {
			widthValueSelected(getWidthValue(cardState));
		}
	}

	@Override
	public void onBindCardContent(@NonNull FragmentActivity activity, @NonNull ViewGroup container,
	                              boolean nightMode, boolean usedOnMap) {
		if (selectedState.getTag() == null) {
			bindSummaryCard(activity, container, nightMode);
		} else {
			bindWidthComponentCardIfNeeded(activity, container);
		}
	}

	private void bindSummaryCard(@NonNull FragmentActivity activity,
	                             @NonNull ViewGroup container, boolean nightMode) {
		container.removeAllViews();
		LayoutInflater inflater = UiUtilities.getInflater(activity, nightMode);
		inflater.inflate(R.layout.list_item_divider_with_padding_basic, container, true);

		String summary = app.getString(R.string.unchanged_parameter_summary);
		DescriptionCard descriptionCard = new DescriptionCard(activity, summary);
		container.addView(descriptionCard.build(activity));
		container.setTag(UNCHANGED_STYLE_CARD_ID);
	}

	private void bindWidthComponentCardIfNeeded(@NonNull FragmentActivity activity,
	                                            @NonNull ViewGroup container) {
		WidthComponentController controller = getWidthComponentController();
		controller.setOnNeedScrollListener(onNeedScrollListener);
		// We only create and bind "Width Component" card only if it wasn't attached before
		// or if there is other card visible at the moment.
		Integer cardId = (Integer) container.getTag();
		if (cardId == null || cardId == UNCHANGED_STYLE_CARD_ID) {
			container.removeAllViews();
			ModedSliderCard widthComponentCard = new ModedSliderCard(activity, controller);
			container.addView(widthComponentCard.build(activity));
		}
		controller.askSelectWidthMode(getWidthValue(selectedState));
		container.setTag(WIDTH_COMPONENT_CARD_ID);
	}

	private void widthValueSelected(@Nullable String widthValue) {
		selectedState = findCardStateByWidthValue(widthValue);
		card.updateSelectedCardState();
		appearanceData.setParameter(WIDTH, widthValue);

		if (listener != null) {
			listener.onTrackWidthSelected(widthValue);
		}
	}

	@NonNull
	private WidthComponentController getWidthComponentController() {
		if (widthComponentController == null) {
			String selectedWidth = appearanceData.getParameter(WIDTH);
			WidthMode widthMode = WidthMode.valueOfKey(selectedWidth);
			int customValue = Algorithms.parseIntSilently(selectedWidth, CUSTOM_WIDTH_MIN);
			widthComponentController = new WidthComponentController(widthMode, customValue, this::widthValueSelected) {
				@NonNull
				@Override
				public Limits getSliderLimits() {
					return new Limits(CUSTOM_WIDTH_MIN, CUSTOM_WIDTH_MAX);
				}
			};
		}
		return widthComponentController;
	}

	@NonNull
	@Override
	protected CardState findCardState(@Nullable Object tag) {
		if (tag instanceof String) {
			return findCardStateByWidthValue((String) tag);
		}
		return super.findCardState(tag);
	}

	@NonNull
	private CardState findCardStateByWidthValue(@Nullable String width) {
		return findCardState(width != null ? WidthMode.valueOfKey(width) : null);
	}

	@Nullable
	private String getWidthValue(@NonNull CardState cardState) {
		if (isCustomValue(cardState)) {
			WidthComponentController controller = getWidthComponentController();
			return controller.getSelectedCustomValue();
		}
		if (cardState.getTag() instanceof WidthMode) {
			return ((WidthMode) cardState.getTag()).getKey();
		}
		return null;
	}

	private boolean isCustomValue(@NonNull CardState cardState) {
		return cardState.getTag() == WidthMode.CUSTOM;
	}

	@NonNull
	@Override
	protected List<CardState> collectSupportedCardStates() {
		List<CardState> list = new ArrayList<>();
		if (addUnchanged) {
			list.add(new CardState(R.string.shared_string_unchanged));
		}
		list.add(new CardState(R.string.shared_string_original));

		for (WidthMode widthMode : WidthMode.values()) {
			list.add(new CardState(widthMode.getTitleId())
					.setShowTopDivider(widthMode.ordinal() == 0)
					.setTag(widthMode));
		}
		return list;
	}
}
