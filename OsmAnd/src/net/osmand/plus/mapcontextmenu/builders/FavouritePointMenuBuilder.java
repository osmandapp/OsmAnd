package net.osmand.plus.mapcontextmenu.builders;

import android.graphics.Color;
import android.view.View;

import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Amenity;
import net.osmand.data.FavouritePoint;
import net.osmand.data.QuadRect;
import net.osmand.data.TransportStop;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.List;

public class FavouritePointMenuBuilder extends MenuBuilder {

	private final FavouritePoint fav;
	private Object originObject;

	public FavouritePointMenuBuilder(MapActivity mapActivity, final FavouritePoint fav) {
		super(mapActivity);
		this.fav = fav;
		setShowNearestWiki(true);
		acquireOriginObject();
	}

	public void acquireOriginObject()
	{
		String originObjectName = fav.getOriginObjectName();
		if (originObjectName.length() > 0) {
			if (originObjectName.startsWith(Amenity.class.getSimpleName())) {
				originObject = findAmenity(originObjectName, fav.getLatitude(), fav.getLongitude());
			} else if (originObjectName.startsWith(TransportStop.class.getSimpleName())) {
				originObject = findTransportStop(originObjectName, fav.getLatitude(), fav.getLongitude());
			}
		}
	}

	public Object getOriginObject() {
		return originObject;
	}

	@Override
	protected void buildNearestWikiRow(View view) {
		if (originObject == null || !(originObject instanceof Amenity)) {
			super.buildNearestWikiRow(view);
		}
	}

	@Override
	public void buildInternal(View view) {
		if (originObject != null && originObject instanceof Amenity) {
			AmenityMenuBuilder builder = new AmenityMenuBuilder(mapActivity, (Amenity) originObject);
			builder.setLatLon(getLatLon());
			builder.setLight(light);
			builder.buildInternal(view);
		}
		buildGroupFavsView(view);
	}

	private void buildGroupFavsView(View view) {
		FavoriteGroup favoriteGroup = app.getFavorites().getGroup(fav);
		List<FavouritePoint> groupFavs = favoriteGroup.points;
		if (groupFavs.size() > 0) {
			int color = favoriteGroup.color == 0 || favoriteGroup.color == Color.BLACK ? view.getResources().getColor(R.color.color_favorite) : favoriteGroup.color;
			int disabledColor = light ? R.color.secondary_text_light : R.color.secondary_text_dark;
			color = favoriteGroup.visible ? (color | 0xff000000) : view.getResources().getColor(disabledColor);
			String name = view.getContext().getString(R.string.context_menu_points_of_group);
			buildRow(view, app.getIconsCache().getPaintedIcon(R.drawable.ic_action_folder, color), name, 0,
					true, getCollapsableItemsView(view.getContext(), true, groupFavs, fav),
					false, 0, false, null);
		}
	}

	private Amenity findAmenity(String nameStringEn, double lat, double lon) {
		QuadRect rect = MapUtils.calculateLatLonBbox(lat, lon, 15);
		List<Amenity> amenities = app.getResourceManager().searchAmenities(
				new BinaryMapIndexReader.SearchPoiTypeFilter() {
					@Override
					public boolean accept(PoiCategory type, String subcategory) {
						return true;
					}

					@Override
					public boolean isEmpty() {
						return false;
					}
				}, rect.top, rect.left, rect.bottom, rect.right, -1, null);

		for (Amenity amenity : amenities) {
			String stringEn = amenity.toStringEn();
			if (stringEn.equals(nameStringEn)) {
				return amenity;
			}
		}
		return null;
	}

	private TransportStop findTransportStop(String nameStringEn, double lat, double lon) {

		QuadRect rect = MapUtils.calculateLatLonBbox(lat, lon, 15);
		List<TransportStop> res = app.getResourceManager().searchTransportSync(rect.top, rect.left,
				rect.bottom, rect.right, new ResultMatcher<TransportStop>() {

					@Override
					public boolean publish(TransportStop object) {
						return true;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}
				});

		for (TransportStop stop : res) {
			String stringEn = stop.toStringEn();
			if (stringEn.equals(nameStringEn)) {
				return stop;
			}
		}
		return null;
	}
}