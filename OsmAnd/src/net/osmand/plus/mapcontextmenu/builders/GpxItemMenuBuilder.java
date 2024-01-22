package net.osmand.plus.mapcontextmenu.builders;

import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.util.Algorithms;

public class GpxItemMenuBuilder extends MenuBuilder {

	private final GpxDisplayItem item;

	public GpxItemMenuBuilder(@NonNull MapActivity mapActivity, @NonNull GpxDisplayItem item) {
		super(mapActivity);
		this.item = item;
	}

	@Override
	protected boolean needBuildPlainMenuItems() {
		return false;
	}

	@Override
	public void buildInternal(View view) {
		String description = GpxUiHelper.getDescription(app, item.analysis, false);
		String[] lines = description.split("\n");
		for (String line : lines) {
			buildRow(view, R.drawable.ic_action_info_dark, null, line, 0, false, null, false, 0, false, null, false);
		}
	}

	@Override
	public boolean hasCustomAddressLine() {
		return true;
	}

	public void buildCustomAddressLine(LinearLayout ll) {
		int gpxSmallIconMargin = (int) ll.getResources().getDimension(R.dimen.gpx_small_icon_margin);
		int gpxSmallTextMargin = (int) ll.getResources().getDimension(R.dimen.gpx_small_text_margin);
		float gpxTextSize = ll.getResources().getDimension(R.dimen.default_desc_text_size);

		int textColor = ColorUtilities.getPrimaryTextColor(ll.getContext(), !light);

		buildIcon(ll, gpxSmallIconMargin, R.drawable.ic_action_waypoint_16);
		buildTextView(ll, gpxSmallTextMargin, gpxTextSize, textColor, "" + item.analysis.getWptPoints());
		buildIcon(ll, gpxSmallIconMargin, R.drawable.ic_action_distance_16);
		buildTextView(ll, gpxSmallTextMargin, gpxTextSize, textColor,
				OsmAndFormatter.getFormattedDistance(item.analysis.getTotalDistance(), app));
		buildIcon(ll, gpxSmallIconMargin, R.drawable.ic_action_time_16);
		buildTextView(ll, gpxSmallTextMargin, gpxTextSize, textColor, Algorithms.formatDuration(item.analysis.getDurationInSeconds(), app.accessibilityEnabled()) + "");
	}

	private void buildIcon(LinearLayout ll, int gpxSmallIconMargin, int iconId) {
		ImageView icon = new ImageView(ll.getContext());
		LinearLayout.LayoutParams llIconParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		AndroidUtils.setMargins(llIconParams, 0, 0, gpxSmallIconMargin, 0);
		llIconParams.gravity = Gravity.CENTER_VERTICAL;
		icon.setLayoutParams(llIconParams);
		icon.setImageDrawable(app.getUIUtilities().getThemedIcon(iconId));
		ll.addView(icon);
	}

	private void buildTextView(LinearLayout ll, int gpxSmallTextMargin, float gpxTextSize, int textColor, String text) {
		TextView textView = new TextView(ll.getContext());
		LinearLayout.LayoutParams llTextParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		AndroidUtils.setMargins(llTextParams, 0, 0, gpxSmallTextMargin, 0);
		textView.setLayoutParams(llTextParams);
		textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, gpxTextSize);
		textView.setTextColor(textColor);
		textView.setText(text);
		ll.addView(textView);
	}
}
