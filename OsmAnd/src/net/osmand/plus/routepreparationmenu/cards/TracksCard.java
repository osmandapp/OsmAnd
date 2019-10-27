package net.osmand.plus.routepreparationmenu.cards;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TracksCard extends BaseCard {

	private List<GPXFile> gpxFiles;
	private boolean showLimited = true;

	private static class GpxItem {
		String title;
		GPXFile file;
		GPXInfo info;

		GpxItem(String title, GPXFile file, GPXInfo info) {
			this.title = title;
			this.file = file;
			this.info = info;
		}
	}

	public TracksCard(MapActivity mapActivity, List<GPXFile> gpxFiles) {
		super(mapActivity);
		this.gpxFiles = gpxFiles;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.gpx_route_card;
	}

	private GpxDataItem getDataItem(GPXInfo info) {
		return app.getGpxDbHelper().getItem(new File(info.getFileName()));
	}

	@SuppressLint("DefaultLocale")
	@Override
	protected void updateContent() {
		final List<GpxItem> list = new ArrayList<>();
		for (GPXFile gpx : gpxFiles) {
			File f = new File(gpx.path);
			list.add(new GpxItem(GpxUiHelper.getGpxTitle(f.getName()), gpx, new GPXInfo(f.getName(), f.lastModified(), f.length())));
		}
		Collections.sort(list, new Comparator<GpxItem>() {
			@Override
			public int compare(GpxItem i1, GpxItem i2) {
				return i1.title.toLowerCase().compareTo(i2.title.toLowerCase());
			}
		});

		LinearLayout tracks = (LinearLayout) view.findViewById(R.id.items);
		tracks.removeAllViews();

		int minCardHeight = app.getResources().getDimensionPixelSize(R.dimen.route_info_card_item_height);
		int listContentPadding = app.getResources().getDimensionPixelSize(R.dimen.list_content_padding);
		int listTextPadding = app.getResources().getDimensionPixelSize(R.dimen.route_info_list_text_padding);

		int mainFontColor = getMainFontColor();
		int descriptionColor = getSecondaryColor();
		int dividerColor = ContextCompat.getColor(mapActivity, nightMode ? R.color.divider_color_dark : R.color.divider_color_light);

		int i = 0;
		boolean showLimitExceeds = list.size() > 4;
		ContextThemeWrapper ctx = new ContextThemeWrapper(mapActivity, !nightMode ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme);
		LayoutInflater inflater = LayoutInflater.from(ctx);
		for (final GpxItem item : list) {
			if (showLimitExceeds && i >= 3 && showLimited) {
				break;
			}
			View v = inflater.inflate(R.layout.gpx_track_item, tracks, false);
			GpxUiHelper.updateGpxInfoView(v, item.title, item.info, getDataItem(item.info), false, app);

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

			ImageView img = (ImageView) v.findViewById(R.id.icon);
			img.setImageDrawable(getActiveIcon(R.drawable.ic_action_polygom_dark));
			img.setVisibility(View.VISIBLE);
			LinearLayout container = (LinearLayout) v.findViewById(R.id.container);
			container.setMinimumHeight(minCardHeight);
			container.setPadding(listContentPadding, 0, 0, 0);
			v.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mapActivity.getMapActions().setGPXRouteParams(item.file);
					app.getTargetPointsHelper().updateRouteAndRefresh(true);
					app.getRoutingHelper().recalculateRouteDueToSettingsChange();
				}
			});
			tracks.addView(v);
			i++;
		}

		View showAllButton = view.findViewById(R.id.show_all_button);
		if (showLimited && showLimitExceeds) {
			((TextView) view.findViewById(R.id.show_all_title)).setText(
					String.format("%s — %d", app.getString(R.string.shared_string_show_all).toUpperCase(), list.size()));
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
				String.format("%s (%d)", app.getString(R.string.tracks_on_map), list.size()));
	}

	@Override
	public void applyState(@NonNull BaseCard card) {
		super.applyState(card);
		if (card instanceof TracksCard) {
			showLimited = ((TracksCard) card).showLimited;
		}
	}
}