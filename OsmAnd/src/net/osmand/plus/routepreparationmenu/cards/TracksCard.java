package net.osmand.plus.routepreparationmenu.cards;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.IndexConstants;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXInfo;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenuFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.track.TrackSelectSegmentBottomSheet;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TracksCard extends BaseCard {

	private final List<GPXFile> gpxFiles;
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
		return app.getGpxDbHelper().getItem(new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR), info.getFileName()));
	}

	@SuppressLint("DefaultLocale")
	@Override
	protected void updateContent() {
		String gpxDir = app.getAppPath(IndexConstants.GPX_INDEX_DIR).getAbsolutePath();
		final List<GpxItem> list = new ArrayList<>();
		for (GPXFile gpx : gpxFiles) {
			File f = new File(gpx.path);
			String fileName = gpx.path.startsWith(gpxDir) ? gpx.path.substring(gpxDir.length() + 1) : f.getName();
			list.add(new GpxItem(GpxUiHelper.getGpxTitle(f.getName()), gpx, new GPXInfo(fileName, f.lastModified(), f.length())));
		}
		Collections.sort(list, new Comparator<GpxItem>() {
			@Override
			public int compare(GpxItem i1, GpxItem i2) {
				return i1.title.toLowerCase().compareTo(i2.title.toLowerCase());
			}
		});

		LinearLayout tracks = view.findViewById(R.id.items);
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

			ImageView img = v.findViewById(R.id.icon);
			img.setImageDrawable(getActiveIcon(R.drawable.ic_action_polygom_dark));
			img.setVisibility(View.VISIBLE);
			LinearLayout container = v.findViewById(R.id.container);
			container.setMinimumHeight(minCardHeight);
			AndroidUtils.setPadding(container, listContentPadding, 0, 0, 0);
			v.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					List<GPXUtilities.WptPt> points = item.file.getRoutePoints();
					if (!points.isEmpty()) {
						ApplicationMode mode = ApplicationMode.valueOfStringKey(points.get(0).getProfileType(), null);
						if (mode != null) {
							app.getRoutingHelper().setAppMode(mode);
							app.initVoiceCommandPlayer(mapActivity, mode, true, null, false, false, true);
						}
					}
					if (item.file.getNonEmptySegmentsCount() > 1) {
						Fragment targetFragment = mapActivity.getSupportFragmentManager().findFragmentByTag(MapRouteInfoMenuFragment.TAG);
						TrackSelectSegmentBottomSheet.showInstance(mapActivity.getSupportFragmentManager(), item.file, targetFragment);
					} else {
						mapActivity.getMapActions().setGPXRouteParams(item.file);
						app.getTargetPointsHelper().updateRouteAndRefresh(true);
					}
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