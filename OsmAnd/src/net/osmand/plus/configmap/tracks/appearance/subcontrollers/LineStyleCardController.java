package net.osmand.plus.configmap.tracks.appearance.subcontrollers;

import static net.osmand.shared.gpx.GpxParameter.LINE_STYLE;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.card.base.multistate.BaseMultiStateCardController;
import net.osmand.plus.card.base.multistate.CardState;
import net.osmand.plus.card.base.simple.DescriptionCard;
import net.osmand.plus.configmap.tracks.appearance.data.AppearanceData;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.multistatetoggle.IconToggleButton;
import net.osmand.plus.widgets.multistatetoggle.IconToggleButton.IconRadioItem;
import net.osmand.shared.gpx.enums.GpxLineStyleType;

import java.util.ArrayList;
import java.util.List;

public class LineStyleCardController extends BaseMultiStateCardController {

	private static final int UNCHANGED_STYLE_CARD_ID = 0;
	private static final int LINE_STYLE_COMPONENT_CARD_ID = 1;

	private final AppearanceData appearanceData;
	private final boolean addUnchanged;

	private IconToggleButton lineStyleToggleButton;

	public LineStyleCardController(@NonNull OsmandApplication app, @NonNull AppearanceData data, boolean addUnchanged) {
		super(app);
		this.appearanceData = data;
		this.addUnchanged = addUnchanged;
		this.selectedState = findCardState(getLineStyleType(data.getParameter(LINE_STYLE)));
	}

	@NonNull
	@Override
	public String getCardTitle() {
		return app.getString(R.string.gpx_line_style);
	}

	@NonNull
	@Override
	public String getCardStateSelectorTitle() {
		return selectedState.toHumanString(app);
	}

	@Override
	public void onBindCardContent(@NonNull FragmentActivity activity, @NonNull ViewGroup container,
	                              boolean nightMode, boolean usedOnMap) {
		if (selectedState.getTag() == null) {
			bindSummaryCard(activity, container, nightMode);
		} else {
			bindLineStyleComponentCardIfNeeded(activity, container, nightMode);
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

	private void bindLineStyleComponentCardIfNeeded(@NonNull FragmentActivity activity,
	                                                @NonNull ViewGroup container, boolean nightMode) {
		Integer cardId = (Integer) container.getTag();
		// We only create and bind the "Line style" toggle only if it wasn't attached before
		// or if there is other card visible at the moment.
		if (cardId == null || cardId == UNCHANGED_STYLE_CARD_ID) {
			container.removeAllViews();
			LayoutInflater inflater = UiUtilities.getInflater(activity, nightMode);
			inflater.inflate(R.layout.card_line_style_content, container, true);

			LinearLayout radioButtonsContainer = container.findViewById(R.id.custom_radio_buttons);
			lineStyleToggleButton = new IconToggleButton(app, radioButtonsContainer, nightMode);
			lineStyleToggleButton.setItems(getRadioItems());
		}
		if (lineStyleToggleButton != null) {
			lineStyleToggleButton.setSelectedItemByTag(selectedState.getTag());
		}
		container.setTag(LINE_STYLE_COMPONENT_CARD_ID);
	}

	@NonNull
	private List<IconRadioItem> getRadioItems() {
		List<IconRadioItem> items = new ArrayList<>();
		for (GpxLineStyleType style : GpxLineStyleType.values()) {
			IconRadioItem item = new IconRadioItem(getIconId(style));
			item.setTag(style);
			item.setContentDescription(style.getDisplayName());
			item.setOnClickListener((radioItem, view) -> lineStyleValueSelected(style));
			items.add(item);
		}
		return items;
	}

	@DrawableRes
	private static int getIconId(@NonNull GpxLineStyleType style) {
		return switch (style) {
			case SOLID -> R.drawable.ic_action_line_style_solid;
			case DASHED -> R.drawable.ic_action_line_style_dashed;
			case DOTTED -> R.drawable.ic_action_line_style_dotted;
		};
	}

	@Override
	protected void onSelectCardState(@NonNull CardState cardState) {
		if (cardState.isOriginal()) {
			selectedState = cardState;
			card.updateSelectedCardState();
			appearanceData.resetParameter(LINE_STYLE);
		} else {
			lineStyleValueSelected(cardState.getTag() instanceof GpxLineStyleType style ? style : null);
		}
	}

	private boolean lineStyleValueSelected(@Nullable GpxLineStyleType style) {
		selectedState = findCardState(style);
		card.updateSelectedCardState();
		appearanceData.setParameter(LINE_STYLE, style != null ? style.getTypeName() : null);
		return true;
	}

	@NonNull
	@Override
	protected List<CardState> collectSupportedCardStates() {
		List<CardState> states = new ArrayList<>();
		if (addUnchanged) {
			states.add(new CardState(R.string.shared_string_unchanged));
		}
		states.add(new CardState(R.string.shared_string_original));

		GpxLineStyleType[] values = GpxLineStyleType.values();
		for (int i = 0; i < values.length; i++) {
			states.add(new CardState(getTitleId(values[i]))
					.setTag(values[i])
					.setShowTopDivider(i == 0));
		}
		return states;
	}

	@StringRes
	private static int getTitleId(@NonNull GpxLineStyleType style) {
		return switch (style) {
			case SOLID -> R.string.gpx_line_style_solid;
			case DASHED -> R.string.gpx_line_style_dashed;
			case DOTTED -> R.string.gpx_line_style_dotted;
		};
	}

	@Nullable
	private static GpxLineStyleType getLineStyleType(@Nullable String typeName) {
		return typeName != null ? GpxLineStyleType.Companion.getLineStyleType(typeName) : null;
	}
}
