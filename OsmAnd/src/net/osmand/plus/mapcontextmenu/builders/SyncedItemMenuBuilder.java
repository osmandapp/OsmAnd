package net.osmand.plus.mapcontextmenu.builders;

import android.view.View;

import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Amenity;
import net.osmand.data.FavouritePoint;
import net.osmand.data.QuadRect;
import net.osmand.data.TransportStop;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.views.POIMapLayer;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class SyncedItemMenuBuilder extends MenuBuilder {

	protected FavouritePoint favouritePoint;
	protected Object originObject;
	protected GPXUtilities.WptPt wptPt;

	public SyncedItemMenuBuilder(MapActivity mapActivity) {
		super(mapActivity);
		setShowNearestWiki(true);
	}

	protected void acquireOriginObject() {
		String originObjectName = favouritePoint.getOriginObjectName();
		if (originObjectName.length() > 0) {
			if (originObjectName.startsWith(Amenity.class.getSimpleName())) {
				originObject = findAmenity(originObjectName, favouritePoint.getLatitude(), favouritePoint.getLongitude());
			} else if (originObjectName.startsWith(TransportStop.class.getSimpleName())) {
				originObject = findTransportStop(originObjectName, favouritePoint.getLatitude(), favouritePoint.getLongitude());
			}
		}
	}

	public Object getOriginObject() {
		return originObject;
	}

	protected Amenity findAmenity(String nameStringEn, double lat, double lon) {
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

	protected TransportStop findTransportStop(String nameStringEn, double lat, double lon) {

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

	protected void buildFavouriteInternal(View view) {
		if (originObject != null && originObject instanceof Amenity) {
			AmenityMenuBuilder builder = new AmenityMenuBuilder(mapActivity, (Amenity) originObject);
			builder.setLatLon(getLatLon());
			builder.setLight(light);
			builder.buildInternal(view);
		}
	}

	protected void buildWptPtInternal(View view) {
		if (wptPt.time > 0) {
			DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(view.getContext());
			DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(view.getContext());
			Date date = new Date(wptPt.time);
			buildRow(view, R.drawable.ic_action_data,
					dateFormat.format(date) + " — " + timeFormat.format(date), 0, false, null, false, 0, false, null);
		}
		if (wptPt.speed > 0) {
			buildRow(view, R.drawable.ic_action_speed,
					OsmAndFormatter.getFormattedSpeed((float)wptPt.speed, app), 0, false, null, false, 0, false, null);
		}
		if (!Double.isNaN(wptPt.ele)) {
			buildRow(view, R.drawable.ic_action_altitude,
					OsmAndFormatter.getFormattedDistance((float) wptPt.ele, app), 0, false, null, false, 0, false, null);
		}
		if (!Double.isNaN(wptPt.hdop)) {
			buildRow(view, R.drawable.ic_action_gps_info,
					Algorithms.capitalizeFirstLetterAndLowercase(app.getString(R.string.plugin_distance_point_hdop)) + ": " + (int)wptPt.hdop, 0,
					false, null, false, 0, false, null);
		}
		if (!Algorithms.isEmpty(wptPt.desc)) {
			final View row = buildRow(view, R.drawable.ic_action_note_dark, wptPt.desc, 0, false, null, true, 10, false, null);
			row.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					POIMapLayer.showDescriptionDialog(row.getContext(), app, wptPt.desc,
							row.getResources().getString(R.string.description));
				}
			});
		}
		if (!Algorithms.isEmpty(wptPt.comment)) {
			final View rowc = buildRow(view, R.drawable.ic_action_note_dark, wptPt.comment, 0,
					false, null, true, 10, false, null);
			rowc.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					POIMapLayer.showDescriptionDialog(rowc.getContext(), app, wptPt.comment,
							rowc.getResources().getString(R.string.poi_dialog_comment));
				}
			});
		}
	}
}
