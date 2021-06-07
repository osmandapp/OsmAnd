package net.osmand.plus.routepreparationmenu.cards;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ContextThemeWrapper;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.search.SearchHistoryFragment;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.plus.search.listitems.QuickSearchListItem;
import net.osmand.search.core.SearchResult;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class HistoryCard extends MapBaseCard {

	private List<SearchResult> searchResults;
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
		final List<SearchResult> list = new ArrayList<>();
		for (SearchResult searchResult : searchResults) {
			if (searchResult.object instanceof HistoryEntry) {
				list.add(searchResult);
			}
		}

		LinearLayout items = (LinearLayout) view.findViewById(R.id.items);
		items.removeAllViews();

		int i = 0;
		boolean showLimitExceeds = list.size() > limit + 1;
		ContextThemeWrapper ctx = new ContextThemeWrapper(mapActivity, !nightMode ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme);
		LayoutInflater inflater = LayoutInflater.from(ctx);
		for (final SearchResult searchResult : list) {
			if (showLimitExceeds && i >= limit) {
				break;
			}
			String title = QuickSearchListItem.getName(app, searchResult);
			String description = QuickSearchListItem.getTypeName(app, searchResult);
			boolean hasDesc = !Algorithms.isEmpty(description);
			View v = inflater.inflate(R.layout.history_card_item, items, false);
			v.findViewById(R.id.divider).setVisibility(i == 0 ? View.GONE : View.VISIBLE);
			TextView titleView = (TextView) v.findViewById(R.id.title);
			TextView subtitleView = (TextView) v.findViewById(R.id.subtitle);
			titleView.setText(title);
			if (hasDesc) {
				subtitleView.setText(description);
				subtitleView.setVisibility(View.VISIBLE);
			} else {
				subtitleView.setVisibility(View.GONE);
			}
			Drawable image;
			final HistoryEntry entry = (HistoryEntry) searchResult.object;
			int iconId = QuickSearchListItem.getHistoryIconId(app, entry);
			try {
				image = app.getUIUtilities().getIcon(iconId, nightMode ?
						R.color.route_info_control_icon_color_dark : R.color.route_info_control_icon_color_light);
			} catch (Exception e) {
				image = app.getUIUtilities().getIcon(SearchHistoryFragment.getItemIcon(entry.getName()), nightMode ?
						R.color.route_info_control_icon_color_dark : R.color.route_info_control_icon_color_light);
			}
			ImageView img = (ImageView) v.findViewById(R.id.imageView);
			img.setImageDrawable(image);
			img.setVisibility(View.VISIBLE);

			ImageView typeImg = (ImageView) v.findViewById(R.id.type_name_icon);
			Drawable typeIcon = QuickSearchListItem.getTypeIcon(app, searchResult);
			if (typeIcon != null && hasDesc) {
				typeImg.setImageDrawable(typeIcon);
				typeImg.setVisibility(View.VISIBLE);
			} else {
				typeImg.setVisibility(View.GONE);
			}

			v.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					app.getTargetPointsHelper().navigateToPoint(searchResult.location, true, -1, entry.getName());
				}
			});
			items.addView(v);
			i++;
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
