package net.osmand.plus.routepreparationmenu.cards;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.IndexConstants;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.shared.gpx.GpxDataItem;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.shared.gpx.GpxHelper;
import net.osmand.shared.io.KFile;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TracksCard extends MapBaseCard {

	private boolean showLimited = true;
	private final List<GpxItem> gpxItems = new ArrayList<>();

	private static class GpxItem {
		String title;
		GpxFile file;
		GPXInfo info;

		GpxItem(String title, GpxFile file, GPXInfo info) {
			this.title = title;
			this.file = file;
			this.info = info;
		}
	}

	public TracksCard(@NonNull MapActivity mapActivity, @NonNull List<GpxFile> gpxFiles) {
		super(mapActivity);

		String gpxDir = app.getAppPath(IndexConstants.GPX_INDEX_DIR).getAbsolutePath();
		for (GpxFile gpx : gpxFiles) {
			File file = new File(gpx.getPath());
			String fileName = gpx.getPath().startsWith(gpxDir) ? gpx.getPath().substring(gpxDir.length() + 1) : file.getName();
			gpxItems.add(new GpxItem(GpxHelper.INSTANCE.getGpxTitle(file.getName()), gpx, new GPXInfo(fileName, file)));
		}
		Collator collator = Collator.getInstance();
		Collections.sort(gpxItems, new Comparator<GpxItem>() {
			@Override
			public int compare(GpxItem lhs, GpxItem rhs) {
				return collator.compare(lhs.title.toLowerCase(), rhs.title.toLowerCase());
			}
		});
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.gpx_route_card;
	}

	private GpxDataItem getDataItem(GPXInfo info) {
		KFile file = new KFile(app.getAppPathKt(IndexConstants.GPX_INDEX_DIR), info.getFileName());
		return app.getGpxDbHelper().getItem(file);
	}

	@SuppressLint("DefaultLocale")
	@Override
	protected void updateContent() {
		LinearLayout tracks = view.findViewById(R.id.items);
		tracks.removeAllViews();

		int minCardHeight = getDimen(R.dimen.route_info_card_item_height);
		int listContentPadding = getDimen(R.dimen.list_content_padding);
		int listTextPadding = getDimen(R.dimen.route_info_list_text_padding);

		int mainFontColor = getMainFontColor();
		int descriptionColor = getSecondaryColor();
		int dividerColor = ColorUtilities.getDividerColor(mapActivity, nightMode);

		boolean showLimitExceeds = gpxItems.size() > 4;
		for (int i = 0; i < gpxItems.size(); i++) {
			GpxItem item = gpxItems.get(i);
			if (showLimitExceeds && i >= 3 && showLimited) {
				break;
			}
			View v = themedInflater.inflate(R.layout.gpx_track_item, tracks, false);
			GpxDataItem dataItem = getDataItem(item.info);
			GpxTrackAnalysis analysis = null;
			if (dataItem != null) {
				analysis = dataItem.getAnalysis();
			}
			GpxUiHelper.updateGpxInfoView(v, item.title, item.info, analysis, app);

			View div = v.findViewById(R.id.divider);
			LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(div.getLayoutParams().width, div.getLayoutParams().height);
			p.setMargins(listTextPadding, 0, 0, 0);
			div.setBackgroundColor(dividerColor);
			div.setLayoutParams(p);
			div.setVisibility(i == 0 ? View.GONE : View.VISIBLE);

			((TextView) v.findViewById(R.id.name)).setTextColor(mainFontColor);
			((TextView) v.findViewById(R.id.distance)).setTextColor(descriptionColor);
			((TextView) v.findViewById(R.id.points_count)).setTextColor(descriptionColor);
			((TextView) v.findViewById(R.id.time)).setTextColor(descriptionColor);

			ImageView img = v.findViewById(R.id.icon);
			img.setImageDrawable(getActiveIcon(R.drawable.ic_action_polygom_dark));
			img.setVisibility(View.VISIBLE);
			LinearLayout container = v.findViewById(R.id.container);
			container.setMinimumHeight(minCardHeight);
			AndroidUtils.setPadding(container, listContentPadding, 0, 0, 0);
			v.setOnClickListener(v1 -> mapActivity.getMapRouteInfoMenu().selectTrack(item.file, true));
			tracks.addView(v);
		}

		View showAllButton = view.findViewById(R.id.show_all_button);
		if (showLimited && showLimitExceeds) {
			((TextView) view.findViewById(R.id.show_all_title)).setText(
					String.format("%s — %d", app.getString(R.string.shared_string_show_all).toUpperCase(), gpxItems.size()));
			showAllButton.setVisibility(View.VISIBLE);
			showAllButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showLimited = false;
					updateContent();
					setLayoutNeeded();
				}
			});
		} else {
			showAllButton.setVisibility(View.GONE);
		}

		((TextView) view.findViewById(R.id.gpx_card_title)).setText(
				String.format("%s (%d)", app.getString(R.string.tracks_on_map), gpxItems.size()));
	}

	@Override
	public void applyState(@NonNull BaseCard card) {
		super.applyState(card);
		if (card instanceof TracksCard) {
			showLimited = ((TracksCard) card).showLimited;
		}
	}
}