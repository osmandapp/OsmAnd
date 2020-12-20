package net.osmand.plus.routing;

import androidx.annotation.NonNull;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.router.RouteCalculationProgress;

import java.util.List;

import static net.osmand.plus.notifications.OsmandNotification.NotificationType.NAVIGATION;

class RouteRecalculationThreadHelper {

	private static final int RECALCULATE_THRESHOLD_COUNT_CAUSING_FULL_RECALCULATE = 3;
	private static final int RECALCULATE_THRESHOLD_CAUSING_FULL_RECALCULATE_INTERVAL = 2 * 60 * 1000;

	private final OsmandApplication app;
	private final RoutingHelper routingHelper;

	private Thread currentRunningJob;
	private long lastTimeEvaluatedRoute = 0;
	private String lastRouteCalcError;
	private String lastRouteCalcErrorShort;
	private long recalculateCountInInterval = 0;
	private int evalWaitInterval = 0;
	private boolean waitingNextJob;

	private RouteCalculationProgressCallback progressRoute;

	RouteRecalculationThreadHelper(@NonNull RoutingHelper routingHelper) {
		this.routingHelper = routingHelper;
		this.app = routingHelper.getApplication();
	}

	String getLastRouteCalcError() {
		return lastRouteCalcError;
	}

	String getLastRouteCalcErrorShort() {
		return lastRouteCalcErrorShort;
	}

	void setProgressBar(RouteCalculationProgressCallback progressRoute) {
		this.progressRoute = progressRoute;
	}

	boolean isRouteBeingCalculated() {
		return currentRunningJob instanceof RouteRecalculationThread || waitingNextJob;
	}

	void resetEvalWaitInterval() {
		evalWaitInterval = 0;
	}

	void stopCalculation() {
		Thread job = currentRunningJob;
		if (job instanceof RouteRecalculationThread) {
			((RouteRecalculationThread) job).stopCalculation();
		}
	}

	void stopCalculationIfParamsChanged() {
		Thread job = currentRunningJob;
		if (job instanceof RouteRecalculationThread) {
			RouteRecalculationThread thread = (RouteRecalculationThread) job;
			if (!thread.isParamsChanged()) {
				thread.stopCalculation();
			}
			if (isFollowingMode()) {
				getVoiceRouter().announceBackOnRoute();
			}
		}
	}

	private OsmandSettings getSettings() {
		return routingHelper.getSettings();
	}

	private ApplicationMode getAppMode() {
		return routingHelper.getAppMode();
	}

	private boolean isFollowingMode() {
		return routingHelper.isFollowingMode();
	}

	private VoiceRouter getVoiceRouter() {
		return routingHelper.getVoiceRouter();
	}

	private Location getLastFixedLocation() {
		return routingHelper.getLastFixedLocation();
	}

	private boolean isDeviatedFromRoute() {
		return routingHelper.isDeviatedFromRoute();
	}

	private Location getLastProjection() {
		return routingHelper.getLastProjection();
	}

	private void setNewRoute(RouteCalculationResult prevRoute, final RouteCalculationResult res, Location start) {
		final boolean newRoute = !prevRoute.isCalculated();
		if (isFollowingMode()) {
			Location lastFixedLocation = getLastFixedLocation();
			if (lastFixedLocation != null) {
				start = lastFixedLocation;
			}
			// try remove false route-recalculated prompts by checking direction to second route node
			boolean wrongMovementDirection = false;
			List<Location> routeNodes = res.getImmutableAllLocations();
			if (routeNodes != null && !routeNodes.isEmpty()) {
				int newCurrentRoute = RoutingHelperUtils.lookAheadFindMinOrthogonalDistance(start, routeNodes, res.currentRoute, 15);
				if (newCurrentRoute + 1 < routeNodes.size()) {
					// This check is valid for Online/GPX services (offline routing is aware of route direction)
					wrongMovementDirection = RoutingHelperUtils.checkWrongMovementDirection(start, routeNodes.get(newCurrentRoute + 1));
					// set/reset evalWaitInterval only if new route is in forward direction
					if (wrongMovementDirection) {
						evalWaitInterval = 3000;
					} else {
						evalWaitInterval = Math.max(3000, evalWaitInterval * 3 / 2);
						evalWaitInterval = Math.min(evalWaitInterval, 120000);
					}

				}
			}


			// trigger voice prompt only if new route is in forward direction
			// If route is in wrong direction after one more setLocation it will be recalculated
			if (!wrongMovementDirection || newRoute) {
				getVoiceRouter().newRouteIsCalculated(newRoute);
			}
		}
		app.getWaypointHelper().setNewRoute(res);
		routingHelper.newRouteCalculated(newRoute, res);
	}

	void startRouteCalculationThread(RouteCalculationParams params, boolean paramsChanged, boolean updateProgress) {
		synchronized (routingHelper) {
			final Thread prevRunningJob = currentRunningJob;
			getSettings().LAST_ROUTE_APPLICATION_MODE.set(getAppMode());
			RouteRecalculationThread newThread = new RouteRecalculationThread("Calculating route", params, paramsChanged);
			currentRunningJob = newThread;
			startProgress(params);
			if (updateProgress) {
				updateProgress(params);
			}
			if (prevRunningJob != null) {
				newThread.setWaitPrevJob(prevRunningJob);
			}
			currentRunningJob.start();
		}
	}

	public void recalculateRouteInBackground(final Location start, final LatLon end, final List<LatLon> intermediates,
											 final RouteProvider.GPXRouteParamsBuilder gpxRoute, final RouteCalculationResult previousRoute, boolean paramsChanged, boolean onlyStartPointChanged) {
		if (start == null || end == null) {
			return;
		}
		// do not evaluate very often
		if ((currentRunningJob == null && System.currentTimeMillis() - lastTimeEvaluatedRoute > evalWaitInterval)
				|| paramsChanged || !onlyStartPointChanged) {
			if (System.currentTimeMillis() - lastTimeEvaluatedRoute < RECALCULATE_THRESHOLD_CAUSING_FULL_RECALCULATE_INTERVAL) {
				recalculateCountInInterval++;
			}
			ApplicationMode mode = getAppMode();
			final RouteCalculationParams params = new RouteCalculationParams();
			params.start = start;
			params.end = end;
			params.intermediates = intermediates;
			params.gpxRoute = gpxRoute == null ? null : gpxRoute.build(app);
			params.onlyStartPointChanged = onlyStartPointChanged;
			if (recalculateCountInInterval < RECALCULATE_THRESHOLD_COUNT_CAUSING_FULL_RECALCULATE
					|| (gpxRoute != null && gpxRoute.isPassWholeRoute() && isDeviatedFromRoute())) {
				params.previousToRecalculate = previousRoute;
			} else {
				recalculateCountInInterval = 0;
			}
			params.leftSide = getSettings().DRIVING_REGION.get().leftHandDriving;
			params.fast = getSettings().FAST_ROUTE_MODE.getModeValue(mode);
			params.mode = mode;
			params.ctx = app;
			boolean updateProgress = false;
			if (params.mode.getRouteService() == RouteProvider.RouteService.OSMAND) {
				params.calculationProgress = new RouteCalculationProgress();
				updateProgress = true;
			} else {
				params.resultListener = new RouteCalculationParams.RouteCalculationResultListener() {
					@Override
					public void onRouteCalculated(RouteCalculationResult route) {
						app.runInUIThread(new Runnable() {

							@Override
							public void run() {
								finishProgress(params);
							}
						});
					}
				};
			}
			if (getLastProjection() != null) {
				params.currentLocation = getLastFixedLocation();
			}
			startRouteCalculationThread(params, paramsChanged, updateProgress);
		}
	}

	void startProgress(final RouteCalculationParams params) {
		if (params.calculationProgressCallback != null) {
			params.calculationProgressCallback.start();
		} else if (progressRoute != null) {
			progressRoute.start();
		}
	}

	void updateProgress(final RouteCalculationParams params) {
		final RouteCalculationProgressCallback progressRoute;
		if (params.calculationProgressCallback != null) {
			progressRoute = params.calculationProgressCallback;
		} else {
			progressRoute = this.progressRoute;
		}
		if (progressRoute != null) {
			app.runInUIThread(new Runnable() {

				@Override
				public void run() {
					RouteCalculationProgress calculationProgress = params.calculationProgress;
					if (isRouteBeingCalculated()) {
						Thread t = currentRunningJob;
						if (t instanceof RouteRecalculationThread && ((RouteRecalculationThread) t).params != params) {
							// different calculation started
							return;
						} else {
							progressRoute.updateProgress((int) calculationProgress.getLinearProgress());
							if (calculationProgress.requestPrivateAccessRouting) {
								progressRoute.requestPrivateAccessRouting();
							}
							updateProgress(params);
						}
					} else {
						if (calculationProgress.requestPrivateAccessRouting) {
							progressRoute.requestPrivateAccessRouting();
						}
						progressRoute.finish();
					}
				}
			}, 300);
		}
	}

	void finishProgress(RouteCalculationParams params) {
		final RouteCalculationProgressCallback progressRoute;
		if (params.calculationProgressCallback != null) {
			progressRoute = params.calculationProgressCallback;
		} else {
			progressRoute = this.progressRoute;
		}
		if (progressRoute != null) {
			progressRoute.finish();
		}
	}

	private void showMessage(final String msg) {
		app.runInUIThread(new Runnable() {
			@Override
			public void run() {
				app.showToastMessage(msg);
			}
		});
	}

	class RouteRecalculationThread extends Thread {

		private final RouteCalculationParams params;
		private final boolean paramsChanged;
		private Thread prevRunningJob;

		public RouteRecalculationThread(String name, RouteCalculationParams params, boolean paramsChanged) {
			super(name);
			this.params = params;
			this.paramsChanged = paramsChanged;
			if (params.calculationProgress == null) {
				params.calculationProgress = new RouteCalculationProgress();
			}
		}

		public boolean isParamsChanged() {
			return paramsChanged;
		}

		public void stopCalculation() {
			params.calculationProgress.isCancelled = true;
		}


		@Override
		public void run() {
			synchronized (routingHelper) {
				routingHelper.resetRouteWasFinished();
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
				synchronized (routingHelper) {
					if (params.calculationProgress.isCancelled) {
						return;
					}
					currentRunningJob = this;
					waitingNextJob = false;
				}
			}
			lastRouteCalcError = null;
			lastRouteCalcErrorShort = null;
			RouteProvider provider = routingHelper.getProvider();
			OsmandSettings settings = getSettings();
			RouteCalculationResult res = provider.calculateRouteImpl(params);
			if (params.calculationProgress.isCancelled) {
				synchronized (routingHelper) {
					currentRunningJob = null;
				}
				return;
			}
			final boolean onlineSourceWithoutInternet = !res.isCalculated() &&
					params.mode.getRouteService().isOnline() && !settings.isInternetConnectionAvailable();
			if (onlineSourceWithoutInternet && settings.GPX_ROUTE_CALC_OSMAND_PARTS.get()) {
				if (params.previousToRecalculate != null && params.previousToRecalculate.isCalculated()) {
					res = provider.recalculatePartOfflineRoute(res, params);
				}
			}
			RouteCalculationResult prev = routingHelper.getRoute();
			synchronized (routingHelper) {
				if (res.isCalculated()) {
					if (!params.inSnapToRoadMode && !params.inPublicTransportMode) {
						routingHelper.setRoute(res);
						routingHelper.updateOriginalRoute();
					}
					if (params.resultListener != null) {
						params.resultListener.onRouteCalculated(res);
					}
				} else {
					evalWaitInterval = Math.max(3000, evalWaitInterval * 3 / 2); // for Issue #3899
					evalWaitInterval = Math.min(evalWaitInterval, 120000);
				}
				currentRunningJob = null;
			}
			if (res.isCalculated()) {
				if (!params.inSnapToRoadMode && !params.inPublicTransportMode) {
					setNewRoute(prev, res, params.start);
				}
			} else if (onlineSourceWithoutInternet) {
				lastRouteCalcError = app.getString(R.string.error_calculating_route)
						+ ":\n" + app.getString(R.string.internet_connection_required_for_online_route);
				lastRouteCalcErrorShort = app.getString(R.string.error_calculating_route);
				showMessage(lastRouteCalcError); //$NON-NLS-1$
			} else {
				if (res.getErrorMessage() != null) {
					lastRouteCalcError = app.getString(R.string.error_calculating_route) + ":\n" + res.getErrorMessage();
					lastRouteCalcErrorShort = app.getString(R.string.error_calculating_route);
					showMessage(lastRouteCalcError); //$NON-NLS-1$
				} else {
					lastRouteCalcError = app.getString(R.string.empty_route_calculated);
					lastRouteCalcErrorShort = app.getString(R.string.empty_route_calculated);
					showMessage(lastRouteCalcError);
				}
			}
			app.getNotificationHelper().refreshNotification(NAVIGATION);
			lastTimeEvaluatedRoute = System.currentTimeMillis();
		}

		public void setWaitPrevJob(Thread prevRunningJob) {
			this.prevRunningJob = prevRunningJob;
		}
	}
}
