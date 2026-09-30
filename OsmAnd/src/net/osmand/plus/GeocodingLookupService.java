package net.osmand.plus;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.GeocodingUtilities;
import net.osmand.binary.GeocodingUtilities.GeocodingResult;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.plus.helpers.CurrentPositionHelper.GeocodingRoads;
import net.osmand.plus.resources.BinaryMapReaderResource;
import net.osmand.plus.resources.ResourceManager.BinaryMapReaderResourceType;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
		return app.getLocationProvider()
				.getGeocodingRoads(loc, new ResultMatcher<GeocodingRoads>() {

					// called on the lookup thread of CurrentPositionHelper, or with null when the lookup
					// was replaced by a newer one
					@Override
					public boolean publish(GeocodingRoads roads) {
						GeocodingResult address = roads == null ? null : findAddress(roads, latLon);
						app.runInUIThread(() -> publishAddress(address));
						return true;
					}

					@Override
					public boolean isCancelled() {
						return !hasAnyRequest(latLon);
					}

				});
	}

	// the street, the house and the city of the roads found near the point; null when cancelled
	@Nullable
	private GeocodingResult findAddress(@NonNull GeocodingRoads roads, @NonNull LatLon latLon) {
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
		List<GeocodingResult> complete = new ArrayList<>();
		double minBuildingDistance = 0;
		try {
			GeocodingUtilities utilities = new GeocodingUtilities();
			for (GeocodingResult road : roads.roads) {
				if (cancel.isCancelled()) {
					break;
				}
				BinaryMapIndexReader reader = findStreetReader(road, roads.readers);
				if (reader == null) {
					complete.add(road);
					continue;
				}
				List<GeocodingResult> justified = null;
				try {
					justified = utilities.justifyReverseGeocodingSearch(road, reader, minBuildingDistance, cancel);
				} catch (IOException e) {
					LOG.error("Exception happened during reverse geocoding", e);
				}
				if (justified != null && !justified.isEmpty()) {
					double distance = justified.get(0).getDistance();
					minBuildingDistance = minBuildingDistance == 0 ? distance : Math.min(distance, minBuildingDistance);
					complete.addAll(justified);
				}
			}
			utilities.filterDuplicateRegionResults(complete);
		} catch (RuntimeException e) {
			LOG.error("Exception happened during reverse geocoding", e);
			return null;
		}
		if (cancel.isCancelled()) {
			return null;
		}
		return complete.isEmpty() ? new GeocodingResult() : complete.get(0);
	}

	// the map whose routing section holds the road, among the maps the roads were read from: their
	// street lookup readers are open already, other maps must not be opened here
	@Nullable
	private BinaryMapIndexReader findStreetReader(@NonNull GeocodingResult road,
	                                              @NonNull List<BinaryMapReaderResource> readers) {
		for (BinaryMapReaderResource resource : readers) {
			if (resource.isClosed()) {
				continue;
			}
			BinaryMapIndexReader reader = resource.getReader(BinaryMapReaderResourceType.STREET_LOOKUP);
			if (reader != null) {
				for (RouteRegion region : reader.getRoutingIndexes()) {
					if (road.regionFP == region.getFilePointer() && road.regionLen == region.getLength()) {
						return reader;
					}
				}
			}
		}
		return null;
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
						lookups.incrementAndGet();
						lookupsTimeMs.addAndGet(System.currentTimeMillis() - startTime);

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
