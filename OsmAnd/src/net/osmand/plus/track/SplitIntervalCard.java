package net.osmand.plus.track;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;

public class SplitIntervalCard extends MapBaseCard {

	private TrackDrawInfo trackDrawInfo;

	public SplitIntervalCard(@NonNull MapActivity mapActivity, @NonNull TrackDrawInfo trackDrawInfo) {
		super(mapActivity);
		this.trackDrawInfo = trackDrawInfo;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.bottom_sheet_item_with_right_descr;
	}

	@Override
	protected void updateContent() {
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.icon), false);

		TextView titleView = view.findViewById(R.id.title);
		titleView.setText(R.string.gpx_split_interval);

		Typeface typeface = FontCache.getFont(app, app.getString(R.string.font_roboto_medium));
		int secondaryTextColor = AndroidUtils.getColorFromAttr(view.getContext(), R.attr.active_color_basic);

		String splitInterval = getSplitInterval();
		SpannableStringBuilder spannableSplitInterval = new SpannableStringBuilder(splitInterval);
		spannableSplitInterval.setSpan(new ForegroundColorSpan(secondaryTextColor), 0, spannableSplitInterval.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		spannableSplitInterval.setSpan(new CustomTypefaceSpan(typeface), 0, spannableSplitInterval.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		TextView descriptionView = view.findViewById(R.id.description);
		descriptionView.setText(spannableSplitInterval);

		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CardListener listener = getListener();
				if (listener != null) {
					listener.onCardPressed(SplitIntervalCard.this);
				}
			}
		});
	}

	private String getSplitInterval() {
		String intervalStr = "";
		int splitInterval = (int) trackDrawInfo.getSplitInterval();
		if (splitInterval == 0) {
			intervalStr = GpxSplitType.NO_SPLIT.getHumanString(app);
		} else if (trackDrawInfo.getSplitType() == GpxSplitType.DISTANCE.getType()) {
			intervalStr = OsmAndFormatter.getFormattedDistanceInterval(app, trackDrawInfo.getSplitInterval());
		} else if (trackDrawInfo.getSplitType() == GpxSplitType.TIME.getType()) {
			intervalStr = OsmAndFormatter.getFormattedTimeInterval(app, splitInterval);
		}
		return intervalStr;
	}
}