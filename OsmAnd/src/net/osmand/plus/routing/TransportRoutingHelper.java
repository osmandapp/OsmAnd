package net.osmand.plus.routing;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.NativeLibrary;
import net.osmand.PlatformUtil;
import net.osmand.ValueHolder;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.osm.edit.Node;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.render.NativeOsmandLibrary;
import net.osmand.plus.routing.RouteCalculationParams.RouteCalculationResultListener;
import net.osmand.plus.routing.RouteProvider.RouteService;
import net.osmand.plus.routing.RoutingHelper.RouteCalculationProgressCallback;
import net.osmand.router.GeneralRouter;

import net.osmand.router.RouteCalculationProgress;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.TransportRoutePlanner;
import net.osmand.router.TransportRoutePlanner.TransportRouteResult;
import net.osmand.router.TransportRoutePlanner.TransportRouteResultSegment;
import net.osmand.router.TransportRoutePlanner.TransportRoutingContext;
import net.osmand.router.TransportRoutingConfiguration;
import net.osmand.router.NativeTransportRoutingResult;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static net.osmand.plus.notifications.OsmandNotification.NotificationType.NAVIGATION;

public class TransportRoutingHelper {

	private static final org.apache.commons.logging.Log log = PlatformUtil.getLog(TransportRoutingHelper.class);

	private List<WeakReference<IRouteInformationListener>> listeners = new LinkedList<>();

	private OsmandApplication app;
	private ApplicationMode applicationMode = ApplicationMode.PUBLIC_TRANSPORT;
	private RoutingHelper routingHelper;

	private List<TransportRouteResult> routes;
	private Map<Pair<TransportRouteResultSegment, TransportRouteResultSegment>, RouteCalculationResult> walkingRouteSegments;
	private int currentRoute = -1;

	private LatLon startLocation;
	private LatLon endLocation;

	private Thread currentRunningJob;
	private String lastRouteCalcError;
	private String lastRouteCalcErrorShort;
	private long lastTimeEvaluatedRoute = 0;
	private boolean waitingNextJob;

	private TransportRouteCalculationProgressCallback progressRoute;


	public TransportRoutingHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	public void setRoutingHelper(RoutingHelper routingHelper) {
		this.routingHelper = routingHelper;
	}

	public LatLon getStartLocation() {
		return startLocation;
	}

	public LatLon getEndLocation() {
		return endLocation;
	}

	public int getCurrentRoute() {
		return currentRoute;
	}

	public boolean hasActiveRoute() {
		return routingHelper.isPublicTransportMode() && currentRoute >= 0;
	}

	@Nullable
	public TransportRouteResult getActiveRoute() {
		return routes != null && routes.size() > currentRoute && currentRoute >= 0 ? routes.get(currentRoute) : null;
	}

	@Nullable
	public TransportRouteResult getCurrentRouteResult() {
		if (routes != null && currentRoute != -1 && currentRoute < routes.size()) {
			return routes.get(currentRoute);
		}
		return null;
	}

	public List<TransportRouteResult> getRoutes() {
		return routes;
	}

	@Nullable
	public RouteCalculationResult getWalkingRouteSegment(TransportRouteResultSegment s1, TransportRouteResultSegment s2) {
		if (walkingRouteSegments != null) {
			return walkingRouteSegments.get(new Pair<>(s1, s2));
		}
		return null;
	}

	public int getWalkingTime(@NonNull List<TransportRouteResultSegment> segments) {
		int res = 0;
		Map<Pair<TransportRouteResultSegment, TransportRouteResultSegment>, RouteCalculationResult> walkingRouteSegments = this.walkingRouteSegments;
		if (walkingRouteSegments != null) {
			TransportRouteResultSegment prevSegment = null;
			for (TransportRouteResultSegment segment : segments) {
				RouteCalculationResult walkingRouteSegment = getWalkingRouteSegment(prevSegment, segment);
				if (walkingRouteSegment != null) {
					res += walkingRouteSegment.getRoutingTime();
				}
				prevSegment = segment;
			}
			if (segments.size() > 0) {
				RouteCalculationResult walkingRouteSegment = getWalkingRouteSegment(segments.get(segments.size() - 1), null);
				if (walkingRouteSegment != null) {
					res += walkingRouteSegment.getRoutingTime();
				}
			}
		}
		return res;
	}

	public int getWalkingDistance(@NonNull List<TransportRouteResultSegment> segments) {
		int res = 0;
		Map<Pair<TransportRouteResultSegment, TransportRouteResultSegment>, RouteCalculationResult> walkingRouteSegments = this.walkingRouteSegments;
		if (walkingRouteSegments != null) {
			TransportRouteResultSegment prevSegment = null;
			for (TransportRouteResultSegment segment : segments) {
				RouteCalculationResult walkingRouteSegment = getWalkingRouteSegment(prevSegment, segment);
				if (walkingRouteSegment != null) {
					res += walkingRouteSegment.getWholeDistance();
				}
				prevSegment = segment;
			}
			if (segments.size() > 0) {
				RouteCalculationResult walkingRouteSegment = getWalkingRouteSegment(segments.get(segments.size() - 1), null);
				if (walkingRouteSegment != null) {
					res += walkingRouteSegment.getWholeDistance();
				}
			}
		}
		return res;
	}

	public void setCurrentRoute(int currentRoute) {
		this.currentRoute = currentRoute;
	}

	public void addListener(IRouteInformationListener l){
		listeners.add(new WeakReference<>(l));
	}

	public boolean removeListener(IRouteInformationListener lt){
		Iterator<WeakReference<IRouteInformationListener>> it = listeners.iterator();
		while(it.hasNext()) {
			WeakReference<IRouteInformationListener> ref = it.next();
			IRouteInformationListener l = ref.get();
			if(l == null || lt == l) {
				it.remove();
				return true;
			}
		}
		return false;
	}

	public void recalculateRouteDueToSettingsChange() {
		clearCurrentRoute(endLocation);
		recalculateRouteInBackground(startLocation, endLocation);
	}

	private void recalculateRouteInBackground(LatLon start, LatLon end) {
		if (start == null || end == null) {
			return;
		}
		TransportRouteCalculationParams params = new TransportRouteCalculationParams();
		params.start = start;
		params.end = end;
		params.mode = applicationMode;
		params.type = RouteService.OSMAND;
		params.ctx = app;
		params.calculationProgress = new RouteCalculationProgress();

		float rd = (float) MapUtils.getDistance(start, end);
		params.calculationProgress.totalEstimatedDistance = rd * 1.5f;

		startRouteCalculationThread(params);
	}

	private void startRouteCalculationThread(TransportRouteCalculationParams params) {
		synchronized (this) {
			final Thread prevRunningJob = currentRunningJob;
			app.getSettings().LAST_ROUTE_APPLICATION_MODE.set(routingHelper.getAppMode());
			RouteRecalculationThread newThread =
					new RouteRecalculationThread("Calculating public transport route", params,
							app.getSettings().SAFE_MODE.get() ? null : NativeOsmandLibrary.getLoadedLibrary());
			currentRunningJob = newThread;
			startProgress(params);
			updateProgress(params);
			if (prevRunningJob != null) {
				newThread.setWaitPrevJob(prevRunningJob);
			}
			currentRunningJob.start();
		}
	}

	public void setProgressBar(TransportRouteCalculationProgressCallback progressRoute) {
		this.progressRoute = progressRoute;
	}

	private void startProgress(final TransportRouteCalculationParams params) {
		final TransportRouteCalculationProgressCallback progressRoute = this.progressRoute;
		if (progressRoute != null) {
			progressRoute.start();
		}
		setCurrentRoute(-1);
	}

	private void updateProgress(final TransportRouteCalculationParams params) {
		final TransportRouteCalculationProgressCallback progressRoute = this.progressRoute;
		if (progressRoute != null) {
			app.runInUIThread(new Runnable() {

				@Override
				public void run() {
					RouteCalculationProgress calculationProgress = params.calculationProgress;
					if (isRouteBeingCalculated()) {
						float pr = calculationProgress.getLinearProgress();
						progressRoute.updateProgress((int) pr);
						Thread t = currentRunningJob;
						if (t instanceof RouteRecalculationThread && ((RouteRecalculationThread) t).params != params) {
							// different calculation started
						} else {
							updateProgress(params);
						}
					} else {
						if (routes != null && routes.size() > 0) {
							setCurrentRoute(0);
						}
						progressRoute.finish();
					}
				}
			}, 300);
		}
	}

	public boolean isRouteBeingCalculated() {
		return currentRunningJob instanceof RouteRecalculationThread || waitingNextJob;
	}

	private void setNewRoute(final List<TransportRouteResult> res) {
		app.runInUIThread(new Runnable() {
			@Override
			public void run() {
				ValueHolder<Boolean> showToast = new ValueHolder<>();
				showToast.value = true;
				Iterator<WeakReference<IRouteInformationListener>> it = listeners.iterator();
				while (it.hasNext()) {
					WeakReference<IRouteInformationListener> ref = it.next();
					IRouteInformationListener l = ref.get();
					if (l == null) {
						it.remove();
					} else {
						l.newRouteIsCalculated(true, showToast);
					}
				}
				if (showToast.value && OsmandPlugin.isDevelopment()) {
					String msg = "Public transport routes calculated: " + res.size();
					app.showToastMessage(msg);
				}
			}
		});
	}

	public synchronized void setFinalAndCurrentLocation(LatLon finalLocation, LatLon currentLocation) {
		clearCurrentRoute(finalLocation);
		// to update route
		setCurrentLocation(currentLocation);
	}

	public synchronized void clearCurrentRoute(LatLon newFinalLocation) {
		currentRoute = -1;
		routes = null;
		walkingRouteSegments = null;
		app.getWaypointHelper().setNewRoute(new RouteCalculationResult(""));
		app.runInUIThread(new Runnable() {
			@Override
			public void run() {
				Iterator<WeakReference<IRouteInformationListener>> it = listeners.iterator();
				while (it.hasNext()) {
					WeakReference<IRouteInformationListener> ref = it.next();
					IRouteInformationListener l = ref.get();
					if (l == null) {
						it.remove();
					} else {
						l.routeWasCancelled();
					}
				}
			}
		});
		this.endLocation = newFinalLocation;
		if (currentRunningJob instanceof RouteRecalculationThread) {
			((RouteRecalculationThread) currentRunningJob).stopCalculation();
		}
	}

	private void setCurrentLocation(LatLon currentLocation) {
		if (endLocation == null || currentLocation == null) {
			return;
		}
		startLocation = currentLocation;
		recalculateRouteInBackground(currentLocation, endLocation);
	}

	private void showMessage(final String msg) {
		app.runInUIThread(new Runnable() {
			@Override
			public void run() {
				app.showToastMessage(msg);
			}
		});
	}

	@Nullable
	public QuadRect getTransportRouteRect(@NonNull TransportRouteResult result) {
		TransportRoutingHelper transportRoutingHelper = app.getTransportRoutingHelper();
		QuadRect r = new QuadRect(0, 0, 0, 0);
		TransportRouteResultSegment s1;
		TransportRouteResultSegment s2 = null;
		for (TransportRouteResultSegment segment : result.getSegments()) {
			s1 = segment;
			for (Node n : segment.getNodes()) {
				MapUtils.insetLatLonRect(r, n.getLatitude(), n.getLongitude());
			}
			RouteCalculationResult wrs = s2 == null ? transportRoutingHelper.getWalkingRouteSegment(null, s1) :
					transportRoutingHelper.getWalkingRouteSegment(s1, s2);
			if (wrs != null) {
				for (Location p : wrs.getRouteLocations()) {
					MapUtils.insetLatLonRect(r, p.getLatitude(), p.getLongitude());
				}
			}
			s2 = s1;
		}
		RouteCalculationResult wrs = transportRoutingHelper.getWalkingRouteSegment(s2, null);
		if (wrs != null) {
			for (Location p : wrs.getRouteLocations()) {
				MapUtils.insetLatLonRect(r, p.getLatitude(), p.getLongitude());
			}
		}
		return r.left == 0 && r.right == 0 ? null : r;
	}

	public void setApplicationMode(ApplicationMode applicationMode) {
		this.applicationMode = applicationMode;
	}

	public interface TransportRouteCalculationProgressCallback {

		void start();

		void updateProgress(int progress);

		void finish();
	}

	public static class TransportRouteCalculationParams {

		public LatLon start;
		public LatLon end;

		public OsmandApplication ctx;
		public ApplicationMode mode;
		public RouteService type;
		public Map<String, String> params = new TreeMap<>();
		public RouteCalculationProgress calculationProgress;
		public TransportRouteCalculationResultListener resultListener;

		public interface TransportRouteCalculationResultListener {
			void onRouteCalculated(List<TransportRouteResult> route);
		}
	}

	private class WalkingRouteSegment {
		TransportRouteResultSegment s1;
		TransportRouteResultSegment s2;
		LatLon start;
		boolean startTransportStop;
		LatLon end;
		boolean endTransportStop;

		WalkingRouteSegment(TransportRouteResultSegment s1, TransportRouteResultSegment s2) {
			this.s1 = s1;
			this.s2 = s2;

			start = s1.getEnd().getLocation();
			end = s2.getStart().getLocation();
			startTransportStop = true;
			endTransportStop = true;
		}

		WalkingRouteSegment(LatLon start, TransportRouteResultSegment s) {
			this.start = start;
			this.s2 = s;
			end = s2.getStart().getLocation();
			endTransportStop = true;
		}

		WalkingRouteSegment(TransportRouteResultSegment s, LatLon end) {
			this.s1 = s;
			this.end = end;
			start = s1.getEnd().getLocation();
			startTransportStop = true;
		}
	}

	private class RouteRecalculationThread extends Thread {

		private final TransportRouteCalculationParams params;
		private Thread prevRunningJob;

		private final Queue<WalkingRouteSegment> walkingSegmentsToCalculate = new ConcurrentLinkedQueue<>();
		private Map<Pair<TransportRouteResultSegment, TransportRouteResultSegment>, RouteCalculationResult> walkingRouteSegments = new HashMap<>();
		private boolean walkingSegmentsCalculated;
		private NativeLibrary lib;

		public RouteRecalculationThread(String name, TransportRouteCalculationParams params, NativeLibrary library) {
			super(name);
			this.params = params;
			this.lib = library;
			if (params.calculationProgress == null) {
				params.calculationProgress = new RouteCalculationProgress();
			}
		}

		public void stopCalculation() {
			params.calculationProgress.isCancelled = true;
		}


		/**
		 * TODO Check if native lib available and calculate route there.
		 * @param params
		 * @return
		 * @throws IOException
		 * @throws InterruptedException
		 */
		private List<TransportRouteResult> calculateRouteImpl(TransportRouteCalculationParams params, NativeLibrary library)
				throws IOException, InterruptedException {
			RoutingConfiguration.Builder config = params.ctx.getRoutingConfigForMode(params.mode);
			BinaryMapIndexReader[] files = params.ctx.getResourceManager().getTransportRoutingMapFiles();
			params.params.clear();
			OsmandSettings settings = params.ctx.getSettings();
			for(Map.Entry<String, GeneralRouter.RoutingParameter> e : config.getRouter(params.mode.getRoutingProfile()).getParameters().entrySet()){
				String key = e.getKey();
				GeneralRouter.RoutingParameter pr = e.getValue();
				String vl;
				if(pr.getType() == GeneralRouter.RoutingParameterType.BOOLEAN) {
					OsmandSettings.CommonPreference<Boolean> pref = settings.getCustomRoutingBooleanProperty(key, pr.getDefaultBoolean());
					Boolean bool = pref.getModeValue(params.mode);
					vl = bool ? "true" : null;
				} else {
					vl = settings.getCustomRoutingProperty(key, "").getModeValue(params.mode);
				}
				if(vl != null && vl.length() > 0) {
					params.params.put(key, vl);
				}
			}
			GeneralRouter prouter = config.getRouter(params.mode.getRoutingProfile());
			TransportRoutingConfiguration cfg = new TransportRoutingConfiguration(prouter, params.params);
			TransportRoutePlanner planner = new TransportRoutePlanner();
			TransportRoutingContext ctx = new TransportRoutingContext(cfg, library, files);
			ctx.calculationProgress =  params.calculationProgress;
			if (ctx.library != null) {
				NativeTransportRoutingResult[] nativeRes = library.runNativePTRouting(
						MapUtils.get31TileNumberX(params.start.getLongitude()),
						MapUtils.get31TileNumberY(params.start.getLatitude()),
						MapUtils.get31TileNumberX(params.end.getLongitude()),
						MapUtils.get31TileNumberY(params.end.getLatitude()),
						cfg, ctx.calculationProgress);
				List<TransportRouteResult> res = TransportRoutePlanner.convertToTransportRoutingResult(nativeRes, cfg);
				return res;
			} else {
				return planner.buildRoute(ctx, params.start, params.end);
			}
		}

		@Nullable
		private RouteCalculationParams getWalkingRouteParams() {

			ApplicationMode walkingMode = ApplicationMode.PEDESTRIAN;

			final WalkingRouteSegment walkingRouteSegment = walkingSegmentsToCalculate.poll();
			if (walkingRouteSegment == null) {
				return null;
			}

			Location start = new Location("");
			start.setLatitude(walkingRouteSegment.start.getLatitude());
			start.setLongitude(walkingRouteSegment.start.getLongitude());
			LatLon end = new LatLon(walkingRouteSegment.end.getLatitude(), walkingRouteSegment.end.getLongitude());

			final float currentDistanceFromBegin =
					RouteRecalculationThread.this.params.calculationProgress.distanceFromBegin +
							(walkingRouteSegment.s1 != null ? (float) walkingRouteSegment.s1.getTravelDist() : 0);

			final RouteCalculationParams params = new RouteCalculationParams();
			params.inPublicTransportMode = true;
			params.start = start;
			params.end = end;
			params.startTransportStop = walkingRouteSegment.startTransportStop;
			params.targetTransportStop = walkingRouteSegment.endTransportStop;
			RoutingHelper.applyApplicationSettings(params, app.getSettings(), walkingMode);
			params.mode = walkingMode;
			params.ctx = app;
			params.calculationProgress = new RouteCalculationProgress();
			params.calculationProgressCallback = new RouteCalculationProgressCallback() {

				@Override
				public void start() {
				}

				@Override
				public void updateProgress(int progress) {
					float p = Math.max(params.calculationProgress.distanceFromBegin,
							params.calculationProgress.distanceFromEnd);

					RouteRecalculationThread.this.params.calculationProgress.distanceFromBegin =
							Math.max(RouteRecalculationThread.this.params.calculationProgress.distanceFromBegin, currentDistanceFromBegin + p);
				}

				@Override
				public void requestPrivateAccessRouting() {
				}

				@Override
				public void finish() {
					if (walkingSegmentsToCalculate.isEmpty()) {
						walkingSegmentsCalculated = true;
					} else {
						updateProgress(0);
						RouteCalculationParams walkingRouteParams = getWalkingRouteParams();
						if (walkingRouteParams != null) {
							routingHelper.startRouteCalculationThread(walkingRouteParams, true, true);
						}
					}
				}
			};
			params.resultListener = new RouteCalculationResultListener() {
				@Override
				public void onRouteCalculated(RouteCalculationResult route) {
					RouteRecalculationThread.this.walkingRouteSegments.put(new Pair<>(walkingRouteSegment.s1, walkingRouteSegment.s2), route);
				}
			};

			return params;
		}

		private void calculateWalkingRoutes(List<TransportRouteResult> routes) {
			walkingSegmentsCalculated = false;
			walkingSegmentsToCalculate.clear();
			walkingRouteSegments.clear();
			if (routes != null && routes.size() > 0) {
				for (TransportRouteResult r : routes) {
					TransportRouteResultSegment prev = null;
					for (TransportRouteResultSegment s : r.getSegments()) {
						LatLon start = prev != null ? prev.getEnd().getLocation() : params.start;
						LatLon end = s.getStart().getLocation();
						if (start != null && end != null) {
							if (prev == null || MapUtils.getDistance(start, end) > 50) {
								walkingSegmentsToCalculate.add(prev == null ?
										new WalkingRouteSegment(start, s) : new WalkingRouteSegment(prev, s));
							}
						}
						prev = s;
					}
					if (prev != null) {
						walkingSegmentsToCalculate.add(new WalkingRouteSegment(prev, params.end));
					}
				}
				RouteCalculationParams walkingRouteParams = getWalkingRouteParams();
				if (walkingRouteParams != null) {
					routingHelper.startRouteCalculationThread(walkingRouteParams, true, true);
					// wait until all segments calculated
					while (!walkingSegmentsCalculated) {
						try {
							Thread.sleep(50);
						} catch (InterruptedException e) {
							// ignore
						}
						if (params.calculationProgress.isCancelled) {
							walkingSegmentsToCalculate.clear();
							walkingSegmentsCalculated = true;
						}
					}
				}
			}
		}

		@Override
		public void run() {
			synchronized (TransportRoutingHelper.this) {
				currentRunningJob = this;
				waitingNextJob = prevRunningJob != null;
			}
			if (prevRunningJob != null) {
				while (prevRunningJob.isAlive()) {
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						// ignore
					}
				}
				synchronized (TransportRoutingHelper.this) {
					currentRunningJob = this;
					waitingNextJob = false;
				}
			}

			List<TransportRouteResult> res = null;
			String error = null;
			try {
				res = calculateRouteImpl(params, lib);
				if (res != null && !params.calculationProgress.isCancelled) {
					calculateWalkingRoutes(res);
				}
			} catch (Exception e) {
				error = e.getMessage();
				log.error(e);
			}
			if (params.calculationProgress.isCancelled) {
				synchronized (TransportRoutingHelper.this) {
					currentRunningJob = null;
				}
				return;
			}
			synchronized (TransportRoutingHelper.this) {
				routes = res;
				TransportRoutingHelper.this.walkingRouteSegments = walkingRouteSegments;
				if (res != null) {
					if (params.resultListener != null) {
						params.resultListener.onRouteCalculated(res);
					}
				}
				currentRunningJob = null;
			}
			if (res != null) {
				setNewRoute(res);
			} else if (error != null) {
				lastRouteCalcError = app.getString(R.string.error_calculating_route) + ":\n" + error;
				lastRouteCalcErrorShort = app.getString(R.string.error_calculating_route);
				showMessage(lastRouteCalcError);
			} else {
				lastRouteCalcError = app.getString(R.string.empty_route_calculated);
				lastRouteCalcErrorShort = app.getString(R.string.empty_route_calculated);
				showMessage(lastRouteCalcError);
			}
			app.getNotificationHelper().refreshNotification(NAVIGATION);
			lastTimeEvaluatedRoute = System.currentTimeMillis();
		}

		public void setWaitPrevJob(Thread prevRunningJob) {
			this.prevRunningJob = prevRunningJob;
		}
	}
}
