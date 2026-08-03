package net.osmand.plus.track.cards;


import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.track.GpxSplitType;
import net.osmand.plus.track.TrackDrawInfo;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;

public class SplitIntervalCard extends BaseCard {

	private final TrackDrawInfo trackDrawInfo;

	public SplitIntervalCard(@NonNull FragmentActivity activity, @NonNull TrackDrawInfo trackDrawInfo) {
		super(activity);
		this.trackDrawInfo = trackDrawInfo;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.bottom_sheet_item_with_right_descr;
	}

	@Override
	public void updateContent() {
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.icon), false);

		TextView titleView = view.findViewById(R.id.title);
		titleView.setText(R.string.gpx_split_interval);

		int secondaryTextColor = AndroidUtils.getColorFromAttr(view.getContext(), R.attr.active_color_basic);

		String splitInterval = getSplitInterval();
		SpannableStringBuilder spannableSplitInterval = new SpannableStringBuilder(splitInterval);
		spannableSplitInterval.setSpan(new ForegroundColorSpan(secondaryTextColor), 0, spannableSplitInterval.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		spannableSplitInterval.setSpan(new CustomTypefaceSpan(FontCache.getMediumFont()), 0, spannableSplitInterval.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		TextView descriptionView = view.findViewById(R.id.description);
		descriptionView.setText(spannableSplitInterval);

		view.setOnClickListener(v -> notifyCardPressed());
	}

	private String getSplitInterval() {
		String intervalStr = "";
		int splitInterval = (int) trackDrawInfo.getSplitInterval();
		if (splitInterval == 0) {
			intervalStr = GpxSplitType.NO_SPLIT.getHumanString(app);
		} else if (trackDrawInfo.getSplitType() == GpxSplitType.DISTANCE.getType()) {
			intervalStr = OsmAndFormatter.getFormattedDistanceInterval(app, trackDrawInfo.getSplitInterval(), OsmAndFormatter.OsmAndFormatterParams.NO_TRAILING_ZEROS);
		} else if (trackDrawInfo.getSplitType() == GpxSplitType.TIME.getType()) {
			intervalStr = OsmAndFormatter.getFormattedTimeInterval(app, splitInterval);
		}
		return intervalStr;
	}
}