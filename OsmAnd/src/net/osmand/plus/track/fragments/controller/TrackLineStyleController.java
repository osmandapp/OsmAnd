package net.osmand.plus.track.fragments.controller;

import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.card.base.headed.IHeadedCardController;
import net.osmand.plus.card.base.headed.IHeadedContentCard;
import net.osmand.plus.track.TrackDrawInfo;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.multistatetoggle.IconToggleButton;
import net.osmand.plus.widgets.multistatetoggle.IconToggleButton.IconRadioItem;
import net.osmand.shared.gpx.enums.GpxLineStyleType;

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
		return drawInfo.getLineStyleType().getDisplayName();
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
		for (GpxLineStyleType style : GpxLineStyleType.values()) {
			IconRadioItem item = new IconRadioItem(getIconId(style));
			item.setTag(style);
			item.setContentDescription(style.getDisplayName());
			item.setOnClickListener((radioItem, view) -> onLineStyleSelected(style));
			items.add(item);
		}
		return items;
	}

	private boolean onLineStyleSelected(@NonNull GpxLineStyleType style) {
		drawInfo.setLineStyleType(style);
		cardInstance.updateCardSummary();
		listener.onTrackLineStyleSelected(style);
		return true;
	}

	@DrawableRes
	private int getIconId(@NonNull GpxLineStyleType style) {
		return switch (style) {
			case SOLID -> R.drawable.ic_action_line_style_solid;
			case DASHED -> R.drawable.ic_action_line_style_dashed;
			case DOTTED -> R.drawable.ic_action_line_style_dotted;
		};
	}

	@NonNull
	private GpxLineStyleType getSelectedStyle() {
		return drawInfo.getLineStyleType();
	}

	public interface ITrackLineStyleSelectedListener {
		void onTrackLineStyleSelected(@NonNull GpxLineStyleType style);
	}
}
