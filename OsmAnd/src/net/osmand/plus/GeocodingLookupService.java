package net.osmand.plus;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.GeocodingUtilities;
import net.osmand.binary.GeocodingUtilities.GeocodingResult;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.plus.resources.BinaryMapReaderResource;
import net.osmand.plus.resources.ResourceManager.BinaryMapReaderResourceType;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.router.RoutePlannerFrontEnd;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.RoutingConfiguration.RoutingMemoryLimits;
import net.osmand.router.RoutingContext;
import net.osmand.shared.routing.GeneralRouterProfile;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class GeocodingLookupService {

	private static final Log LOG = PlatformUtil.getLog(GeocodingLookupService.class);

	private final OsmandApplication app;
	private final ConcurrentLinkedQueue<LatLon> lookupLocations = new ConcurrentLinkedQueue<>();
	private final ConcurrentHashMap<LatLon, List<AddressLookupRequest>> addressLookupRequestsMap = new ConcurrentHashMap<>();
	private LatLon currentRequestedLocation;
	// address requests and the lookups run for them (requests of one point share a lookup,
	// cancelled ones run none) with their time; read by the memory log
	private final AtomicInteger requests = new AtomicInteger();
	private final AtomicInteger lookups = new AtomicInteger();
	private final AtomicLong lookupsTimeMs = new AtomicLong();

	private boolean searchDone;
	private String lastFoundAddress;

	// roads near a point: one routing context over the maps around the last point asked, used only
	// from the lookup thread
	private final ExecutorService lookupThread = Executors.newSingleThreadExecutor();
	private final AtomicInteger[] roadsRequests = new AtomicInteger[RoadsLookup.values().length];
	private RoutingContext ctx;
	private RoutingContext defCtx;
	private ApplicationMode ctxAppMode;
	private List<BinaryMapReaderResource> usedReaders = new ArrayList<>();

	public enum RoadsLookup {
		// the road under the current position, kept by CurrentPositionHelper
		CURRENT_POSITION(false, false),
		// a road near a point, also one without a name
		ROAD(true, false),
		// the named roads an address is looked up from, in maps with address data
		ADDRESS(false, true);

		private final boolean allowEmptyNames;
		private final boolean addressData;

		RoadsLookup(boolean allowEmptyNames, boolean addressData) {
			this.allowEmptyNames = allowEmptyNames;
			this.addressData = addressData;
		}
	}

	public interface OnAddressLookupProgress {
		void geocodingInProgress();
	}

	public interface OnAddressLookupResult {
		void geocodingDone(String address);
	}

	public static class AddressLookupRequest {

		private LatLon latLon;
		private final OnAddressLookupResult uiResultCallback;
		private final OnAddressLookupProgress uiProgressCallback;

		public AddressLookupRequest(LatLon latLon, OnAddressLookupResult uiResultCallback,
		                            OnAddressLookupProgress uiProgressCallback) {
			this.latLon = latLon;
			this.uiResultCallback = uiResultCallback;
			this.uiProgressCallback = uiProgressCallback;
		}

		public LatLon getLatLon() {
			return latLon;
		}
	}

	public GeocodingLookupService(OsmandApplication app) {
		this.app = app;
		for (int i = 0; i < roadsRequests.length; i++) {
			roadsRequests[i] = new AtomicInteger();
		}
	}

	public int getRequests() {
		return requests.get();
	}

	public int getLookups() {
		return lookups.get();
	}

	public long getLookupsTimeMs() {
		return lookupsTimeMs.get();
	}

	public void lookupAddress(AddressLookupRequest request) {
		requests.incrementAndGet();
		synchronized (this) {
			LatLon requestedLocation = request.latLon;
			LatLon existingLocation = null;
			if (requestedLocation.equals(currentRequestedLocation)) {
				existingLocation = currentRequestedLocation;
				requestedLocation = existingLocation;
				request.latLon = existingLocation;
			} else if (lookupLocations.contains(requestedLocation)) {
				for (LatLon latLon : lookupLocations) {
					if (latLon.equals(requestedLocation)) {
						existingLocation = latLon;
						requestedLocation = latLon;
						request.latLon = latLon;
						break;
					}
				}
			}
			List<AddressLookupRequest> list = addressLookupRequestsMap.get(requestedLocation);
			if (list == null) {
				list = new ArrayList<>();
				addressLookupRequestsMap.put(requestedLocation, list);
			}
			list.add(request);
			if (existingLocation == null) {
				lookupLocations.add(requestedLocation);
			}

			if (currentRequestedLocation == null && !lookupLocations.isEmpty()) {
				currentRequestedLocation = lookupLocations.peek();
				OsmAndTaskManager.executeTask(new AddressLookupRequestsAsyncTask(app));
			}
		}
	}

	public void cancel(AddressLookupRequest request) {
		synchronized (this) {
			List<AddressLookupRequest> requests = addressLookupRequestsMap.get(request.latLon);
			if (requests != null && requests.size() > 0) {
				requests.remove(request);
			}
		}
	}

	public void cancel(LatLon latLon) {
		synchronized (this) {
			List<AddressLookupRequest> requests = addressLookupRequestsMap.get(latLon);
			if (requests != null && requests.size() > 0) {
				requests.clear();
			}
		}
	}

	private boolean hasAnyRequest(LatLon latLon) {
		synchronized (this) {
			List<AddressLookupRequest> requests = addressLookupRequestsMap.get(latLon);
			return requests != null && requests.size() > 0;
		}
	}

	private boolean geocode(LatLon latLon) {
		Location loc = new Location("");
		loc.setLatitude(latLon.getLatitude());
		loc.setLongitude(latLon.getLongitude());
		findRoads(loc, RoadsLookup.ADDRESS, true, null, roads -> {
			GeocodingResult address = roads == null ? null : findAddress(roads, latLon);
			app.runInUIThread(() -> publishAddress(address));
		});
		return true;
	}

	// the street, the house and the city of the roads found near the point, nearest first; null when
	// cancelled. Runs on the lookup thread right after the roads, so usedReaders are the maps they
	// came from, with their street lookup readers open.
	@Nullable
	private GeocodingResult findAddress(@NonNull List<GeocodingResult> roads, @NonNull LatLon latLon) {
		ResultMatcher<GeocodingResult> cancel = new ResultMatcher<GeocodingResult>() {
			@Override
			public boolean publish(GeocodingResult object) {
				return false;
			}

			@Override
			public boolean isCancelled() {
				return !hasAnyRequest(latLon);
			}
		};
		List<BinaryMapIndexReader> readers = new ArrayList<>();
		for (BinaryMapReaderResource resource : usedReaders) {
			if (!resource.isClosed()) {
				BinaryMapIndexReader reader = resource.getReader(BinaryMapReaderResourceType.STREET_LOOKUP);
				if (reader != null) {
					readers.add(reader);
				}
			}
		}
		List<GeocodingResult> addresses;
		try {
			addresses = new GeocodingUtilities().findAddresses(readers, roads, cancel);
		} catch (IOException | RuntimeException e) {
			LOG.error("Exception happened during reverse geocoding", e);
			return null;
		}
		if (cancel.isCancelled()) {
			return null;
		}
		return addresses.isEmpty() ? new GeocodingResult() : addresses.get(0);
	}

	// the roads near the point, nearest first, passed to the callback on the lookup thread; null when a
	// newer request of the same kind replaced this one
	public void findRoads(@NonNull Location loc, @NonNull RoadsLookup kind, boolean cancelPreviousSearch,
	                      @Nullable ApplicationMode appMode, @NonNull Consumer<List<GeocodingResult>> callback) {
		AtomicInteger requestNumber = roadsRequests[kind.ordinal()];
		int request = requestNumber.incrementAndGet();
		lookupThread.submit(() -> {
			if (cancelPreviousSearch && request != requestNumber.get()) {
				callback.accept(null);
				return;
			}
			List<GeocodingResult> roads = searchRoads(loc.getLatitude(), loc.getLongitude(), kind, appMode);
			callback.accept(roads == null ? new ArrayList<>() : roads);
		});
	}

	@Nullable
	private List<GeocodingResult> searchRoads(double lat, double lon, @NonNull RoadsLookup kind,
	                                          @Nullable ApplicationMode appMode) {
		List<BinaryMapReaderResource> checkReaders = checkReaders(lat, lon, usedReaders, kind.addressData);
		if (appMode == null) {
			appMode = app.getSettings().getApplicationMode();
		}
		if (ctx == null || ctxAppMode != appMode || checkReaders != usedReaders) {
			initCtx(checkReaders, appMode);
			if (ctx == null) {
				return null;
			}
		}
		try {
			return new GeocodingUtilities().reverseGeocodingSearch(kind.addressData ? defCtx : ctx, lat, lon,
					kind.allowEmptyNames);
		} catch (Exception e) {
			LOG.error("Exception happened during searchRoads", e);
			return null;
		}
	}

	private void initCtx(@NonNull List<BinaryMapReaderResource> checkReaders, @NonNull ApplicationMode appMode) {
		ctxAppMode = appMode;
		String p;
		if (appMode.isDerivedRoutingFrom(ApplicationMode.BICYCLE)) {
			p = GeneralRouterProfile.BICYCLE.name().toLowerCase();
		} else if (appMode.isDerivedRoutingFrom(ApplicationMode.PEDESTRIAN)) {
			p = GeneralRouterProfile.PEDESTRIAN.name().toLowerCase();
		} else if (appMode.isDerivedRoutingFrom(ApplicationMode.CAR)) {
			p = GeneralRouterProfile.CAR.name().toLowerCase();
		} else {
			p = "geocoding";
		}

		BinaryMapIndexReader[] rs = new BinaryMapIndexReader[checkReaders.size()];
		if (rs.length > 0) {
			int i = 0;
			for (BinaryMapReaderResource rep : checkReaders) {
				rs[i++] = rep.getReader(BinaryMapReaderResourceType.STREET_LOOKUP);
			}
			RoutingMemoryLimits memoryLimits = new RoutingMemoryLimits(10, 10);
			RoutingConfiguration cfg = app.getRoutingConfigForMode(appMode).build(p, memoryLimits,
					new HashMap<String, String>());
			cfg.routeCalculationTime = System.currentTimeMillis();
			ctx = new RoutePlannerFrontEnd().buildRoutingContext(cfg, null, rs);
			RoutingConfiguration defCfg = app.getDefaultRoutingConfig().build("geocoding", memoryLimits,
					new HashMap<String, String>());
			defCtx = new RoutePlannerFrontEnd().buildRoutingContext(defCfg, null, rs);
		} else {
			ctx = null;
			defCtx = null;
		}
		usedReaders = checkReaders;
	}

	private List<BinaryMapReaderResource> checkReaders(double lat, double lon,
			List<BinaryMapReaderResource> ur, boolean requireAddressData) {
		List<BinaryMapReaderResource> res = ur;
		for (BinaryMapReaderResource t : ur) {
			if (t.isClosed()) {
				res = new ArrayList<>();
				break;
			}
		}
		int y31 = MapUtils.get31TileNumberY(lat);
		int x31 = MapUtils.get31TileNumberX(lon);
		for (BinaryMapReaderResource r : app.getResourceManager().getFileReaders()) {
			if (!r.isClosed()) {
				BinaryMapIndexReader shallowReader = r.getShallowReader();
				if (shallowReader != null && shallowReader.containsRouteData(x31, y31, x31, y31, 15)
						&& (!requireAddressData || shallowReader.containsAddressData())) {
					if (!res.contains(r)) {
						res = new ArrayList<>(res);
						res.add(r);
					}
				}
			}
		}
		return res;
	}

	private void publishAddress(@Nullable GeocodingResult object) {
		String result = null;
		if (object != null) {
			OsmandSettings settings = app.getSettings();
			String lang = settings.MAP_PREFERRED_LOCALE.get();
			boolean transliterate = settings.MAP_TRANSLITERATE_NAMES.get();
			String geocodingResult = "";

			if (object.building != null) {
				String bldName = object.building.getName(lang, transliterate);
				if (!Algorithms.isEmpty(object.buildingInterpolation)) {
					bldName = object.buildingInterpolation;
				}
				geocodingResult = object.street.getName(lang, transliterate) + " " + bldName + ", "
						+ object.city.getName(lang, transliterate);
			} else if (object.street != null) {
				geocodingResult = object.street.getName(lang, transliterate) + ", " + object.city.getName(lang, transliterate);
			} else if (object.city != null) {
				geocodingResult = object.city.getName(lang, transliterate);
			} else if (object.point != null) {
				RouteDataObject rd = object.point.getRoad();
				String sname = rd.getName(lang, transliterate);
				if (Algorithms.isEmpty(sname)) {
					sname = "";
				}
				String ref = rd.getRef(lang, transliterate, true);
				if (!Algorithms.isEmpty(ref)) {
					if (!Algorithms.isEmpty(sname)) {
						sname += ", ";
					}
					sname += ref;
				}
				geocodingResult = sname;
			}

			result = geocodingResult;

			double relevantDistance = object.getDistance();
			if (!Algorithms.isEmpty(result) && relevantDistance > 100) {
				result = app.getString(R.string.shared_string_near) + " " + result;
			}
		}

		lastFoundAddress = result;
		searchDone = true;
	}

	private class AddressLookupRequestsAsyncTask extends AsyncTask<AddressLookupRequest, AddressLookupRequest, Void> {

		private final OsmandApplication app;

		public AddressLookupRequestsAsyncTask(OsmandApplication app) {
			this.app = app;
		}

		@Override
		protected Void doInBackground(AddressLookupRequest... addressLookupRequests) {
			for (;;) {
				try {
					while (!lookupLocations.isEmpty()) {
						LatLon latLon;
						synchronized (GeocodingLookupService.this) {
							latLon = lookupLocations.poll();
							currentRequestedLocation = latLon;
							List<AddressLookupRequest> requests = addressLookupRequestsMap.get(latLon);
							if (requests == null || requests.size() == 0) {
								addressLookupRequestsMap.remove(latLon);
								continue;
							}
						}

						// geocode
						long startTime = System.currentTimeMillis();
						searchDone = false;
						while (!geocode(latLon)) {
							try {
								Thread.sleep(50);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}

						long counter = 0;
						while (!searchDone) {
							try {
								Thread.sleep(50);
								counter++;
								// call progress every 500 ms
								if (counter == 10) {
									counter = 0;
									synchronized (GeocodingLookupService.this) {
										List<AddressLookupRequest> requests = addressLookupRequestsMap.get(latLon);
										for (AddressLookupRequest request : requests) {
											if (request.uiProgressCallback != null) {
												app.runInUIThread(request.uiProgressCallback::geocodingInProgress);
											}
										}
									}
								}
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						// time first: a sample between the two sees the time and counts the lookup next time
						lookupsTimeMs.addAndGet(System.currentTimeMillis() - startTime);
						lookups.incrementAndGet();

						synchronized (GeocodingLookupService.this) {
							List<AddressLookupRequest> requests = addressLookupRequestsMap.get(latLon);
							for (AddressLookupRequest request : requests) {
								if (request.uiResultCallback != null) {
									app.runInUIThread(() -> request.uiResultCallback.geocodingDone(lastFoundAddress));
								}
							}
							addressLookupRequestsMap.remove(latLon);
						}
					}

				} catch (Exception e) {
					e.printStackTrace();
				}

				synchronized (GeocodingLookupService.this) {
					currentRequestedLocation = null;
					if (lookupLocations.isEmpty()) {
						break;
					}
				}
			}

			return null;
		}
	}
}
