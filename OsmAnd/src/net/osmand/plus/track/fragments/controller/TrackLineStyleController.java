package net.osmand.plus.track.fragments.controller;

import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.card.base.headed.IHeadedCardController;
import net.osmand.plus.card.base.headed.IHeadedContentCard;
import net.osmand.plus.track.TrackDrawInfo;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.multistatetoggle.IconToggleButton;
import net.osmand.plus.widgets.multistatetoggle.IconToggleButton.IconRadioItem;

import java.util.ArrayList;
import java.util.List;

public class TrackLineStyleController implements IHeadedCardController {

	private final OsmandApplication app;
	private final TrackDrawInfo drawInfo;
	private final ITrackLineStyleSelectedListener listener;

	private IHeadedContentCard cardInstance;

	public TrackLineStyleController(@NonNull OsmandApplication app,
	                                @NonNull TrackDrawInfo drawInfo,
	                                @NonNull ITrackLineStyleSelectedListener listener) {
		this.app = app;
		this.drawInfo = drawInfo;
		this.listener = listener;
	}

	@Override
	public void bindComponent(@NonNull IHeadedContentCard cardInstance) {
		this.cardInstance = cardInstance;
	}

	@NonNull
	@Override
	public String getCardTitle() {
		return app.getString(R.string.gpx_line_style);
	}

	@NonNull
	@Override
	public String getCardSummary() {
		return app.getString(getSelectedStyle().titleId);
	}

	@NonNull
	@Override
	public View getCardContentView(@NonNull FragmentActivity activity, boolean nightMode) {
		View view = UiUtilities.getInflater(activity, nightMode)
				.inflate(R.layout.card_line_style_content, null);
		setupToggleButton(view, nightMode);
		return view;
	}

	private void setupToggleButton(@NonNull View view, boolean nightMode) {
		LinearLayout container = view.findViewById(R.id.custom_radio_buttons);
		IconToggleButton toggleButton = new IconToggleButton(app, container, nightMode);
		toggleButton.setItems(getRadioItems());
		toggleButton.setSelectedItemByTag(getSelectedStyle());
	}

	@NonNull
	private List<IconRadioItem> getRadioItems() {
		List<IconRadioItem> items = new ArrayList<>();
		for (LineStyle style : LineStyle.values()) {
			IconRadioItem item = new IconRadioItem(style.iconId);
			item.setTag(style);
			item.setContentDescription(app.getString(style.titleId));
			item.setOnClickListener((radioItem, view) -> onLineStyleSelected(style));
			items.add(item);
		}
		return items;
	}

	private boolean onLineStyleSelected(@NonNull LineStyle style) {
		if (style == LineStyle.DOTTED) {
			return false;
		}
		drawInfo.setDashedLine(style == LineStyle.DASHED);
		cardInstance.updateCardSummary();
		listener.onTrackLineStyleSelected(style == LineStyle.DASHED);
		return true;
	}

	@NonNull
	private LineStyle getSelectedStyle() {
		return drawInfo.isDashedLine() ? LineStyle.DASHED : LineStyle.SOLID;
	}

	private enum LineStyle {
		SOLID(R.drawable.ic_action_line_style_solid, R.string.track_coloring_solid),
		DASHED(R.drawable.ic_action_line_style_dashed, R.string.gpx_line_style_dashed),
		DOTTED(R.drawable.ic_action_line_style_dotted, R.string.gpx_line_style_dotted);

		@DrawableRes
		private final int iconId;
		@StringRes
		private final int titleId;

		LineStyle(@DrawableRes int iconId, @StringRes int titleId) {
			this.iconId = iconId;
			this.titleId = titleId;
		}
	}

	public interface ITrackLineStyleSelectedListener {
		void onTrackLineStyleSelected(boolean dashed);
	}
}