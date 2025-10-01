package net.osmand.plus.mapcontextmenu.other;

import static net.osmand.plus.settings.enums.FavoritesSortMode.SORT_TYPE_CATEGORY;
import static net.osmand.plus.settings.enums.FavoritesSortMode.SORT_TYPE_DIST;

import androidx.annotation.NonNull;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.enums.FavoritesSortMode;
import net.osmand.util.MapUtils;

import java.text.Collator;
import java.util.Comparator;

public class FavouritesComparator implements Comparator<FavouritePoint> {

	private final OsmandApplication app;
	private final Collator collator;
	private final LatLon latLon;

	private final FavoritesSortMode sortMode;

	public FavouritesComparator(@NonNull OsmandApplication app, @NonNull Collator collator,
			@NonNull LatLon latLon, @NonNull FavoritesSortMode sortMode) {
		this.app = app;
		this.collator = collator;
		this.latLon = latLon;
		this.sortMode = sortMode;
	}

	@Override
	public int compare(FavouritePoint lhs, FavouritePoint rhs) {
		if (sortMode == SORT_TYPE_DIST && latLon != null) {
			double ld = MapUtils.getDistance(latLon, lhs.getLatitude(), lhs.getLongitude());
			double rd = MapUtils.getDistance(latLon, rhs.getLatitude(), rhs.getLongitude());
			return Double.compare(ld, rd);
		} else if (sortMode == SORT_TYPE_CATEGORY) {
			int cat = collator.compare(lhs.getCategoryDisplayName(app), rhs.getCategoryDisplayName(app));
			if (cat != 0) {
				return cat;
			}
		}
		int name = collator.compare(lhs.getDisplayName(app), rhs.getDisplayName(app));
		return name;
	}
}