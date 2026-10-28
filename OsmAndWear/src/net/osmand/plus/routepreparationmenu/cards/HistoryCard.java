package net.osmand.plus.routepreparationmenu.cards;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.IndexConstants;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.plus.search.dialogs.QuickSearchListAdapter;
import net.osmand.plus.search.listitems.QuickSearchListItem;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchResult;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HistoryCard extends MapBaseCard {

	private final List<SearchResult> searchResults;
	private int limit = 3;

	public HistoryCard(MapActivity mapActivity, List<SearchResult> searchResults) {
		super(mapActivity);
		this.searchResults = searchResults;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.gpx_route_card;
	}

	@SuppressLint("DefaultLocale")
	@Override
	protected void updateContent() {
		List<SearchResult> list = new ArrayList<>();
		for (SearchResult searchResult : searchResults) {
			if (searchResult.object instanceof HistoryEntry) {
				list.add(searchResult);
			}
		}

		LinearLayout items = view.findViewById(R.id.items);
		items.removeAllViews();

		boolean showLimitExceeds = list.size() > limit + 1;
		Context context = UiUtilities.getThemedContext(activity, nightMode);

		int iconColorId = nightMode ? R.color.icon_color_default_dark : R.color.icon_color_default_light;
		int iconColor = ContextCompat.getColor(app, iconColorId);

		for (int i = 0; i < list.size(); i++) {
			if (showLimitExceeds && i >= limit) {
				break;
			}
			SearchResult searchResult = list.get(i);

			LinearLayout view;
			QuickSearchListItem listItem = new QuickSearchListItem(app, searchResult);
			if (searchResult.objectType == ObjectType.GPX_TRACK) {
				view = (LinearLayout) themedInflater.inflate(R.layout.search_gpx_list_item, items, false);

				GPXInfo gpxInfo = (GPXInfo) searchResult.relatedObject;
				QuickSearchListAdapter.bindGpxTrack(view, listItem, gpxInfo);

				ImageView icon = view.findViewById(R.id.icon);
				icon.setImageDrawable(UiUtilities.tintDrawable(listItem.getIcon(), iconColor));

				view.setOnClickListener(v -> {
					String filePath = gpxInfo.getFilePath();
					SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(filePath);
					if (selectedGpxFile != null) {
						GpxFile gpxFile = selectedGpxFile.getGpxFile();
						mapActivity.getMapRouteInfoMenu().selectTrack(gpxFile, true);
					} else {
						CallbackWithObject<GpxFile[]> callback = result -> {
							MapActivity mapActivity = getMapActivity();
							if (mapActivity != null) {
								mapActivity.getMapRouteInfoMenu().selectTrack(result[0], true);
							}
							return true;
						};
						String fileName = gpxInfo.getFileName();
						File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
						GpxUiHelper.loadGPXFileInDifferentThread(mapActivity, callback, dir, null, fileName);
					}
				});
			} else {
				view = (LinearLayout) themedInflater.inflate(R.layout.search_list_item, items, false);
				QuickSearchListAdapter.bindSearchResult(view, listItem);

				ImageView icon = view.findViewById(R.id.imageView);
				icon.setImageDrawable(UiUtilities.tintDrawable(listItem.getIcon(), iconColor));

				HistoryEntry entry = (HistoryEntry) searchResult.object;
				view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						app.getTargetPointsHelper().navigateToPoint(searchResult.location, true, -1, entry.getName());
					}
				});
			}
			View itemContainer = view.findViewById(R.id.searchListItemLayout);
			itemContainer.setBackground(UiUtilities.getSelectableDrawable(context));
			itemContainer.setMinimumHeight(getDimen(R.dimen.route_info_card_item_height));

			int margin = getDimen(R.dimen.route_info_list_text_padding);
			LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, AndroidUtils.dpToPx(app, 1));
			AndroidUtils.setMargins(params, margin, 0, 0, 0);
			View divider = view.findViewById(R.id.divider);
			divider.setLayoutParams(params);
			AndroidUiHelper.updateVisibility(divider, !showLimitExceeds || i < limit);

			items.addView(view);
		}

		View showAllButton = view.findViewById(R.id.show_all_button);
		if (showLimitExceeds) {
			((TextView) view.findViewById(R.id.show_all_title))
					.setText(app.getString(R.string.show_more).toUpperCase());
			showAllButton.setVisibility(View.VISIBLE);
			showAllButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (limit < 10) {
						limit = 10;
					} else {
						limit += 10;
					}
					updateContent();
					setLayoutNeeded();
				}
			});
		} else {
			showAllButton.setVisibility(View.GONE);
		}

		((TextView) view.findViewById(R.id.gpx_card_title)).setText(R.string.shared_string_history);
	}

	@Override
	public void applyState(@NonNull BaseCard card) {
		super.applyState(card);
		if (card instanceof HistoryCard) {
			limit = ((HistoryCard) card).limit;
		}
	}
}
