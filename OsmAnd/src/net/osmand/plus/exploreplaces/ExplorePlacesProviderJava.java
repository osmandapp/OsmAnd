package net.osmand.plus.exploreplaces;

import androidx.annotation.NonNull;

import com.squareup.picasso.Picasso;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.ObfConstants;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.NearbyPlacePoint;
import net.osmand.data.QuadRect;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.search.GetNearbyPlacesImagesTask;
import net.osmand.search.core.SearchCoreFactory;
import net.osmand.shared.data.KLatLon;
import net.osmand.shared.data.KQuadRect;
import net.osmand.shared.util.KMapUtils;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;
import net.osmand.util.MapUtils;
import net.osmand.wiki.WikiCoreHelper.OsmandApiFeatureData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExplorePlacesProviderJava implements ExplorePlacesProvider {
	private OsmandApplication app;
	private long lastModifiedTime = 0;
	private static final int PLACES_LIMIT = 50000;
	private static final int NEARBY_MIN_RADIUS = 50;

	private KQuadRect prevMapRect = new KQuadRect();
	private int prevZoom = 0;
	private String prevLang = "";

	public ExplorePlacesProviderJava(OsmandApplication app) {
		this.app = app;
	}

	private List<ExplorePlacesListener> listeners = Collections.emptyList();
	private List<NearbyPlacePoint> dataCollection;

	private GetNearbyPlacesImagesTask.GetImageCardsListener loadNearbyPlacesListener =
			new GetNearbyPlacesImagesTask.GetImageCardsListener() {
				@Override
				public void onTaskStarted() {
				}

				@Override
				public void onFinish(@NonNull List<? extends OsmandApiFeatureData> result) {
					if (result == null) {
						dataCollection = Collections.emptyList();
					} else {
						List<NearbyPlacePoint> filteredList = new ArrayList<>();
						for (OsmandApiFeatureData item: result) {
							if (!Algorithms.isEmpty(item.properties.photoTitle)) {
								filteredList.add(new NearbyPlacePoint(item));
							}
						}
						int newListSize = Math.min(filteredList.size(), PLACES_LIMIT);
						dataCollection = filteredList.subList(0, newListSize);

						if (dataCollection!= null) {
							for (NearbyPlacePoint point: dataCollection) {
								Picasso.get()
										.load(point.getIconUrl())
										.fetch();
							}
						}
					}
					updateLastModifiedTime();
					notifyListeners();
				}

			};

	public void addListener(ExplorePlacesListener listener) {
		listeners = CollectionUtils.addToList(listeners, listener);
	}

	public void removeListener(ExplorePlacesListener listener) {
		listeners = CollectionUtils.removeFromList(listeners, listener);
	}

	public void notifyListeners() {
		app.runInUIThread(new Runnable() {
			@Override
			public void run() {
				for (ExplorePlacesListener listener: listeners) {
					listener.onNearbyPlacesUpdated();
				}
			}
		});
	}

	public List<NearbyPlacePoint> getDataCollection() {
		return this.dataCollection == null? Collections.emptyList(): this.dataCollection;
	}

	public void onCacheLoaded(List<NearbyPlacePoint> cachedPLaces) {
		if (!Algorithms.isEmpty(cachedPLaces)) {
			NearbyPlacePoint firstPoint = cachedPLaces.get(0);
			KQuadRect qRect = new KQuadRect(
					firstPoint.getLatitude(),
					firstPoint.getLongitude(),
					firstPoint.getLatitude(),
					firstPoint.getLongitude());
			for (NearbyPlacePoint point: cachedPLaces) {
				qRect.setLeft(Math.min(firstPoint.getLatitude(), qRect.getLeft()));
				qRect.setRight(Math.max(firstPoint.getLatitude(), qRect.getRight()));
				qRect.setTop(Math.min(firstPoint.getLongitude(), qRect.getTop()));
				qRect.setBottom(Math.max(firstPoint.getLongitude(), qRect.getBottom()));
			}
			dataCollection = cachedPLaces;
			prevMapRect = qRect;
		}
	}

	public List<NearbyPlacePoint> getDataCollection(QuadRect rect) {
		KQuadRect qRect = new KQuadRect(rect.left, rect.top, rect.right, rect.bottom);
		List<NearbyPlacePoint> fullCollection = this.dataCollection == null? Collections.emptyList(): this.dataCollection;
		List<NearbyPlacePoint> filteredList = new ArrayList<>();
		for (NearbyPlacePoint point: fullCollection) {
			if (qRect.contains(new KLatLon(point.getLatitude(), point.getLongitude()))) {
				filteredList.add(point);
			}
		}
		return filteredList;
	}

	public void startLoadingNearestPhotos() {
		net.osmand.data.RotatedTileBox rotatedTileBox = app.getOsmandMap().getMapView().getCurrentRotatedTileBox();
		QuadRect rect = new QuadRect(rotatedTileBox.getLatLonBounds());
		KQuadRect qRect = new KQuadRect(rect.left, rect.top, rect.right, rect.bottom);

		String preferredLang = app.getSettings().MAP_PREFERRED_LOCALE.get();
		if (Algorithms.isEmpty(preferredLang)) {
			preferredLang = app.getLanguage();
		}
		if (!prevMapRect.contains(qRect) ||
				!prevLang.equals(preferredLang)) {
			LatLon mapCenter = rotatedTileBox.getCenterLatLon();
			prevMapRect =
					KMapUtils.INSTANCE.calculateLatLonBbox(mapCenter.getLatitude(), mapCenter.getLongitude(), 30000);
			prevZoom = app.getOsmandMap().getMapView().getZoom();
			prevLang = preferredLang;
			new GetNearbyPlacesImagesTask(
					app,
					prevMapRect, prevZoom,
					prevLang, loadNearbyPlacesListener).execute();
//					executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			notifyListeners();
		}
	}

	private void updateLastModifiedTime() {
		lastModifiedTime = System.currentTimeMillis();
	}

	public long getLastModifiedTime() {
		return lastModifiedTime;
	}

	public void showPointInContextMenu(MapActivity mapActivity, NearbyPlacePoint point) {
		double latitude = point.getLatitude();
		double longitude = point.getLongitude();
		app.getSettings().setMapLocationToShow(
				latitude,
				longitude,
				SearchCoreFactory.PREFERRED_NEARBY_POINT_ZOOM,
				point.getPointDescription(app),
				true,
				point);
		MapActivity.launchMapActivityMoveToTop(mapActivity);
	}

	public Amenity getAmenity(LatLon latLon, long osmId) {
		final Amenity[] foundAmenity = new Amenity[]{null};
		int radius = NEARBY_MIN_RADIUS;
		QuadRect rect = MapUtils.calculateLatLonBbox(latLon.getLatitude(), latLon.getLongitude(), radius);
		app.getResourceManager().searchAmenities(
				BinaryMapIndexReader.ACCEPT_ALL_POI_TYPE_FILTER,
				rect.top, rect.left, rect.bottom, rect.right,
				-1, true,
				new ResultMatcher<Amenity>() {
					@Override
					public boolean publish(Amenity amenity) {
						long id = ObfConstants.getOsmObjectId(amenity);
						if (osmId == id) {
							foundAmenity[0] = amenity;
							return true;
						}
						return false;
					}

					@Override
					public boolean isCancelled() {
						return foundAmenity[0] != null;
					}
				});
		return foundAmenity[0];
	}
}