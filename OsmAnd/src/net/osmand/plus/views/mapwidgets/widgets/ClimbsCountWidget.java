package net.osmand.plus.views.mapwidgets.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import net.osmand.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgetstates.ClimbsCountWidgetState;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public class ClimbsCountWidget extends SimpleWidget {

	private static final float DESCENT_TOLERANCE = 3.0f; // percentage - allow descents up to this slope within a climb

	// Custom view for drawing elevation profile
	private static class ElevationProfileView extends View {
		private List<Double> elevations;
		private List<Double> distances;
		private double progress;
		private int vamTarget;
		private double currentVAM;
		private Paint redPaint;
		private Paint orangePaint;
		private Paint linePaint;
		private Paint opponentPaint;

		public ElevationProfileView(Context context) {
			super(context);
			init();
		}

		public ElevationProfileView(Context context, AttributeSet attrs) {
			super(context, attrs);
			init();
		}

		public ElevationProfileView(Context context, AttributeSet attrs, int defStyleAttr) {
			super(context, attrs, defStyleAttr);
			init();
		}

		private void init() {
			redPaint = new Paint();
			redPaint.setColor(0xFFFF0000); // Solid red for progress line
			redPaint.setStyle(Paint.Style.STROKE);
			redPaint.setStrokeWidth(3);
			redPaint.setAntiAlias(true);

			orangePaint = new Paint();
			orangePaint.setColor(0xFFA500); // Solid orange (kept for potential future use)
			orangePaint.setStyle(Paint.Style.STROKE);

			linePaint = new Paint();
			linePaint.setColor(0xFF4B0082); // Dark purple
			linePaint.setStyle(Paint.Style.STROKE);
			linePaint.setStrokeWidth(5);
			linePaint.setAntiAlias(true);

			opponentPaint = new Paint();
			opponentPaint.setColor(0xFF8A2BE2); // Blue Violet for opponent
			opponentPaint.setStyle(Paint.Style.STROKE);
			opponentPaint.setStrokeWidth(3);
			opponentPaint.setAntiAlias(true);
			opponentPaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{10, 5}, 0));
		}

		public void setData(List<Double> elevations, List<Double> distances, double progress, int vamTarget, double currentVAM) {
			this.elevations = elevations;
			this.distances = distances;
			this.progress = Math.max(0, Math.min(1, progress));
			this.vamTarget = vamTarget;
			this.currentVAM = currentVAM;
			invalidate();
		}

		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);

			if (elevations == null || elevations.size() < 2) {
				return;
			}

			int width = getWidth();
			int height = getHeight();

			// Find min and max elevation
			double minEle = Double.MAX_VALUE;
			double maxEle = Double.MIN_VALUE;
			for (Double ele : elevations) {
				if (ele != null && !Double.isNaN(ele)) {
					minEle = Math.min(minEle, ele);
					maxEle = Math.max(maxEle, ele);
				}
			}

			// Add padding
			double elevationRange = maxEle - minEle;
			if (elevationRange == 0) {
				elevationRange = 1;
			}
			minEle -= elevationRange * 0.1;
			maxEle += elevationRange * 0.1;
			elevationRange = maxEle - minEle;

			// Calculate cutoff point
			int cutoffX = (int) (width * progress);

			// Create path for elevation profile line only (no fill)
			Path path = new Path();

			// Calculate points for the profile
			float[] points = new float[elevations.size() * 2];
			for (int i = 0; i < elevations.size(); i++) {
				// Calculate x position based on distance
				float x = distances != null && i < distances.size()
						? (float) (width * distances.get(i) / distances.get(distances.size() - 1))
						: (float) (width * i / (elevations.size() - 1));

				// Calculate y position (inverted because Android canvas has y=0 at top)
				float y = (float) (height - (elevations.get(i) - minEle) / elevationRange * height);

				points[i * 2] = x;
				points[i * 2 + 1] = y;
			}

			// Draw the profile as a line (only contour, no fill)
			path.moveTo(points[0], points[1]);
			for (int i = 2; i < points.length; i += 2) {
				path.lineTo(points[i], points[i + 1]);
			}

			// Draw the profile line
			canvas.drawPath(path, linePaint);

			// Draw virtual opponent line at position based on VAM target
			// If user VAM > target VAM, user is faster, opponent is behind (ratio < progress)
			// If user VAM < target VAM, user is slower, opponent is ahead (ratio > progress)
			// opponentRatio = (targetVAM / currentVAM) * progress
			if (vamTarget > 0 && currentVAM > 0) {
				double opponentRatio = ((double) vamTarget / currentVAM) * progress;
				opponentRatio = Math.max(0, Math.min(1, opponentRatio));
				int opponentX = (int) (width * opponentRatio);
				
				// Draw vertical dashed line for opponent position
				canvas.drawLine(opponentX, 0, opponentX, height, opponentPaint);
			}

			// Draw the cutoff point as a vertical line (progress indicator)
			if (cutoffX > 0 && cutoffX < width) {
				canvas.drawLine(cutoffX, 0, cutoffX, height, redPaint);
			}
		}
	}

	private final ClimbsCountWidgetState widgetState;
	private final RoutingHelper routingHelper;
	private final OsmandApplication app;

	private int totalClimbs = 0;
	private int completedClimbs = 0;
	private List<Climb> climbs = new ArrayList<>();
	private List<Double> cumulativeDistances = new ArrayList<>();
	private CurrentClimbInfo currentClimbInfo;
	private View climbProfileOverlay;
	private ElevationProfileView elevationProfileView;
	private final Handler handler = new Handler(Looper.getMainLooper());

	public ClimbsCountWidget(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType,
	                        @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, widgetType, customId, widgetsPanel);
		this.app = mapActivity.getApp();
		this.widgetState = new ClimbsCountWidgetState(app, customId);
		this.routingHelper = app.getRoutingHelper();
	}

	@Override
	protected void setupView(@NonNull View view) {
		super.setupView(view);
		setIcons(widgetType);
		setText(NO_VALUE, null);
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		try {
			updateClimbsCount();
		} catch (Exception e) {
			// Prevent crash from propagating
		}
	}

	private void updateClimbsCount() {
		try {
			if (!isRouteAvailable()) {
				setText(NO_VALUE, null);
				hideClimbProfileOverlay();
				return;
			}

			int minElevation = widgetState.getMinClimbElevation();
			float minGrade = widgetState.getMinClimbGrade();

			ClimbsResult result = calculateClimbs(minElevation, minGrade);
			totalClimbs = result.info.totalClimbs;
			completedClimbs = result.info.completedClimbs;

			if (totalClimbs > 0) {
				String text = completedClimbs + "/" + totalClimbs;
				setText(text, null);
			} else {
				setText("0", null);
			}

			// Update climb profile overlay
			updateClimbProfileOverlay();
		} catch (Exception e) {
			// Prevent crash from propagating
		}
	}

	private boolean isRouteAvailable() {
		// Detect climbs for navigation routes (A to B), both during preview and active navigation
		// Only for calculated routes, not for GPX tracks
		return routingHelper != null && routingHelper.getRoute() != null && routingHelper.getRoute().isCalculated();
	}

	private ClimbsResult calculateClimbs(int minElevation, float minGrade) {
		ClimbsResult result = new ClimbsResult();
		result.info.totalClimbs = 0;
		result.info.completedClimbs = 0;
		result.climbs = new ArrayList<>();
		climbs.clear();

		// Only use points from the calculated navigation route
		RouteCalculationResult route = routingHelper.getRoute();
		if (route == null) {
			return result;
		}

		List<Location> locations = route.getImmutableAllLocations();
		if (locations == null || locations.size() < 2) {
			return result;
		}

		// Convert Location objects to WptPt
		List<WptPt> allPoints = new ArrayList<>();
		for (Location loc : locations) {
			WptPt point = new WptPt();
			point.setLat(loc.getLatitude());
			point.setLon(loc.getLongitude());
			point.setEle(loc.getAltitude());
			allPoints.add(point);
		}

		// Build cumulative distances for all points
		cumulativeDistances.clear();
		cumulativeDistances.add(0.0);
		for (int i = 1; i < allPoints.size(); i++) {
			WptPt prev = allPoints.get(i - 1);
			WptPt curr = allPoints.get(i);
			double dist = MapUtils.getDistance(prev.getLatitude(), prev.getLongitude(), curr.getLatitude(), curr.getLongitude());
			cumulativeDistances.add(cumulativeDistances.get(i - 1) + dist);
		}
		this.cumulativeDistances = new ArrayList<>(cumulativeDistances);

		// ROLLING WINDOW ALGORITHM (2026-11-15 v3):
		// Based on user suggestion: use a rolling window of 200m to calculate average grade
		//
		// Steps:
		// 1. Create fixed-size windows of 200m along the route
		// 2. For each window, calculate average grade
		// 3. A window is a "climb window" if its average grade >= minClimbGrade
		// 4. Group consecutive climb windows into a single climb
		// 5. For each climb, calculate total elevation gain and average grade
		// 6. Only count climbs where total elevation gain >= minClimbElevation AND average grade >= minClimbGrade
		
		final double WINDOW_SIZE = 200.0; // meters
		
		// Create windows of WINDOW_SIZE (200m)
		List<Boolean> isClimbWindow = new ArrayList<>();
		List<Double> windowElevationGains = new ArrayList<>();
		List<Double> windowDistances = new ArrayList<>();
		List<Integer> windowStartIndices = new ArrayList<>();
		List<Integer> windowEndIndices = new ArrayList<>();

		// Create windows
		double totalDistance = cumulativeDistances.get(cumulativeDistances.size() - 1);
		int numWindows = (int) Math.ceil(totalDistance / WINDOW_SIZE);
		
		for (int w = 0; w < numWindows; w++) {
			double windowStartDist = w * WINDOW_SIZE;
			double windowEndDist = Math.min((w + 1) * WINDOW_SIZE, totalDistance);
			
			// Find the indices for this window
			int startIdx = 0;
			int endIdx = allPoints.size() - 1;
			
			// Find first location >= windowStartDist
			for (int i = 0; i < cumulativeDistances.size(); i++) {
				if (cumulativeDistances.get(i) >= windowStartDist) {
					startIdx = i;
					break;
				}
			}
			
			// Find last location <= windowEndDist
			for (int i = cumulativeDistances.size() - 1; i >= 0; i--) {
				if (cumulativeDistances.get(i) <= windowEndDist) {
					endIdx = i;
					break;
				}
			}
			
			// If we have valid indices, calculate the elevation gain for this window
			if (startIdx < endIdx && startIdx >= 0 && endIdx < allPoints.size()) {
				double positiveElevationGain = 0;
				
				// Calculate elevation gain within this window
				for (int i = startIdx + 1; i <= endIdx; i++) {
					WptPt prevPoint = allPoints.get(i - 1);
					WptPt currPoint = allPoints.get(i);
					
					if (Double.isNaN(prevPoint.getEle()) || Double.isNaN(currPoint.getEle())) {
						continue;
					}
					
					double elevationGain = currPoint.getEle() - prevPoint.getEle();
					if (elevationGain > 0) {
						positiveElevationGain += elevationGain;
					}
				}
				
				double windowActualDistance = cumulativeDistances.get(endIdx) - cumulativeDistances.get(startIdx);
				
				// Calculate average grade for this window
				float avgGrade = windowActualDistance > 0 ? 
					(float) ((positiveElevationGain / windowActualDistance) * 100) : 0f;
				
				// Check if this is a climb window
				boolean climbWindow = avgGrade >= minGrade && positiveElevationGain > 0;
				
				isClimbWindow.add(climbWindow);
				windowElevationGains.add(positiveElevationGain);
				windowDistances.add(windowActualDistance);
				windowStartIndices.add(startIdx);
				windowEndIndices.add(endIdx);
			} else {
				// Empty window, add default values
				isClimbWindow.add(false);
				windowElevationGains.add(0.0);
				windowDistances.add(0.0);
				windowStartIndices.add(startIdx);
				windowEndIndices.add(endIdx);
			}
		}

		// Now group consecutive climb windows into climbs
		// For building elevation profiles, we need to collect all the elevation points
		int climbStartWindow = -1;
		boolean inClimb = false;
		double currentClimbElevationGain = 0;
		double currentClimbDistance = 0;
		int currentClimbStartIndex = -1;
		int currentClimbEndIndex = -1;
		List<Double> currentClimbElevationProfile = new ArrayList<>();

		for (int w = 0; w < isClimbWindow.size(); w++) {
			if (!inClimb) {
				if (isClimbWindow.get(w)) {
					inClimb = true;
					climbStartWindow = w;
					currentClimbElevationGain = windowElevationGains.get(w);
					currentClimbDistance = windowDistances.get(w);
					currentClimbStartIndex = windowStartIndices.get(w);
					currentClimbEndIndex = windowEndIndices.get(w);
					
					// Start elevation profile - collect all points in this window
					currentClimbElevationProfile = new ArrayList<>();
					for (int i = currentClimbStartIndex; i <= currentClimbEndIndex; i++) {
						currentClimbElevationProfile.add(allPoints.get(i).getEle());
					}
				}
			} else {
				if (isClimbWindow.get(w)) {
					// Continue the climb
					currentClimbElevationGain += windowElevationGains.get(w);
					currentClimbDistance += windowDistances.get(w);
					currentClimbEndIndex = windowEndIndices.get(w);
					
					// Add points from this window to elevation profile
					int startIdx = windowStartIndices.get(w);
					int endIdx = windowEndIndices.get(w);
					// Skip the first point if it's the same as the last point from previous window
					if (startIdx > currentClimbElevationProfile.size() - 1) {
						for (int i = startIdx; i <= endIdx; i++) {
							currentClimbElevationProfile.add(allPoints.get(i).getEle());
						}
					} else {
						for (int i = startIdx + 1; i <= endIdx; i++) {
							currentClimbElevationProfile.add(allPoints.get(i).getEle());
						}
					}
				} else {
					// End of climb
					if (currentClimbElevationGain >= minElevation) {
						float avgGrade = currentClimbDistance > 0 ? 
							(float) ((currentClimbElevationGain / currentClimbDistance) * 100) : 0f;
						
						// Create distance profile based on cumulative distances
						List<Double> climbDistanceProfile = new ArrayList<>();
						double climbStartDist = cumulativeDistances.get(currentClimbStartIndex);
						for (int i = currentClimbStartIndex; i <= currentClimbEndIndex; i++) {
							climbDistanceProfile.add(cumulativeDistances.get(i) - climbStartDist);
						}
						
						if (avgGrade >= minGrade) {
							Climb climb = createClimb(currentClimbStartIndex, currentClimbEndIndex, 
									currentClimbElevationGain, currentClimbDistance, avgGrade, 
									currentClimbElevationProfile, climbDistanceProfile);
							result.climbs.add(climb);
							climbs.add(climb);
							result.info.totalClimbs++;
						}
					}
					// Reset
					inClimb = false;
					currentClimbElevationGain = 0;
					currentClimbDistance = 0;
					currentClimbElevationProfile = new ArrayList<>();
				}
			}
		}

		// Handle the last climb if we're still in one
		if (inClimb && currentClimbElevationGain >= minElevation) {
			float avgGrade = currentClimbDistance > 0 ? 
				(float) ((currentClimbElevationGain / currentClimbDistance) * 100) : 0f;
			
			// Create distance profile for the last climb
			List<Double> climbDistanceProfile = new ArrayList<>();
			double climbStartDist = cumulativeDistances.get(currentClimbStartIndex);
			for (int i = currentClimbStartIndex; i <= currentClimbEndIndex; i++) {
				climbDistanceProfile.add(cumulativeDistances.get(i) - climbStartDist);
			}
			
			if (avgGrade >= minGrade) {
				Climb climb = createClimb(currentClimbStartIndex, currentClimbEndIndex, 
						currentClimbElevationGain, currentClimbDistance, avgGrade, 
						currentClimbElevationProfile, climbDistanceProfile);
				result.climbs.add(climb);
				climbs.add(climb);
				result.info.totalClimbs++;
			}
		}

		// Calculate completed climbs based on current location
		if (!result.climbs.isEmpty()) {
			int routeDistance = route.getDistanceFromStart();
			int wholeDistance = route.getWholeDistance();

			// Find which climb the user is currently on
			updateCurrentClimbInfo(result.climbs, allPoints, routeDistance, cumulativeDistances);

			// Count completed climbs based on actual position
			// A climb is completed if the user has passed its end point
			int completedCount = 0;
			for (Climb climb : result.climbs) {
				if (climb.endIndex < cumulativeDistances.size()) {
					double climbEndDist = cumulativeDistances.get(climb.endIndex);
					if (routeDistance >= climbEndDist) {
						completedCount++;
					}
				}
			}
			result.info.completedClimbs = completedCount;
			this.completedClimbs = completedCount;

			if (wholeDistance == 0) {
				result.info.completedClimbs = 0;
				this.completedClimbs = 0;
			}
		} else {
			currentClimbInfo = null;
			this.completedClimbs = 0;
		}

		return result;
	}

	private Climb createClimb(int startIndex, int endIndex, double elevationGain, double distance, float averageGrade,
				List<Double> elevationProfile, List<Double> distanceProfile) {
		Climb climb = new Climb();
		climb.startIndex = startIndex;
		climb.endIndex = endIndex;
		climb.elevationGain = elevationGain;
		climb.distance = distance;
		climb.averageGrade = averageGrade;
		climb.elevationProfile = new ArrayList<>(elevationProfile);
		climb.distanceProfile = new ArrayList<>(distanceProfile);
		return climb;
	}

	private void updateCurrentClimbInfo(List<Climb> climbs, List<WptPt> allPoints, int currentDistanceFromStart, List<Double> cumulativeDistances) {
		if (climbs.isEmpty() || allPoints.isEmpty() || cumulativeDistances.isEmpty()) {
			currentClimbInfo = null;
			return;
		}

		// Find current climb based on current distance
		for (Climb climb : climbs) {
			if (climb.startIndex < cumulativeDistances.size() && climb.endIndex < cumulativeDistances.size()) {
				double climbStartDist = cumulativeDistances.get(climb.startIndex);
				double climbEndDist = cumulativeDistances.get(climb.endIndex);

				if (currentDistanceFromStart >= climbStartDist && currentDistanceFromStart <= climbEndDist) {
					// User is on this climb
					CurrentClimbInfo info = new CurrentClimbInfo();
					info.climb = climb;

					// Find the current point index
					for (int i = climb.startIndex; i <= climb.endIndex && i < cumulativeDistances.size(); i++) {
						if (currentDistanceFromStart <= cumulativeDistances.get(i)) {
							info.currentPointIndex = i;
							if (i > 0) {
								info.currentDistanceInClimb = currentDistanceFromStart - cumulativeDistances.get(climb.startIndex);
								info.totalClimbDistance = climbEndDist - climbStartDist;
							} else {
								info.currentDistanceInClimb = 0;
								info.totalClimbDistance = climb.distance;
							}
							break;
						}
					}
					currentClimbInfo = info;
					return;
				}
			}
		}

		// User is not on any climb
		currentClimbInfo = null;
	}

	@Nullable
	private Climb findNextClimb(@NonNull List<Double> cumulativeDistances, int currentDistanceFromStart) {
		// Look for the next climb that starts after current position
		Climb nextClimb = null;
		double minDistanceToNext = Double.MAX_VALUE;
		
		for (Climb climb : climbs) {
			if (climb.startIndex < cumulativeDistances.size()) {
				double climbStartDist = cumulativeDistances.get(climb.startIndex);
				
				// If climb starts after current position
				if (climbStartDist > currentDistanceFromStart) {
					double distanceToClimb = climbStartDist - currentDistanceFromStart;
					
					// Consider climbs within 500m
					if (distanceToClimb < 500 && distanceToClimb < minDistanceToNext) {
						minDistanceToNext = distanceToClimb;
						nextClimb = climb;
					}
				}
			}
		}

		return nextClimb;
	}

	@Nullable
	private Climb findPreviousClimb(@NonNull List<Double> cumulativeDistances, int currentDistanceFromStart) {
		// Look for the most recent climb that ended before current position
		Climb prevClimb = null;
		double minDistanceAfterEnd = Double.MAX_VALUE;
		
		for (Climb climb : climbs) {
			if (climb.endIndex < cumulativeDistances.size()) {
				double climbEndDist = cumulativeDistances.get(climb.endIndex);
				
				// If climb ended before current position
				if (climbEndDist < currentDistanceFromStart) {
					double distanceAfterClimb = currentDistanceFromStart - climbEndDist;
					
					// Consider climbs that were completed within last 200m
					if (distanceAfterClimb < 200 && distanceAfterClimb < minDistanceAfterEnd) {
						minDistanceAfterEnd = distanceAfterClimb;
						prevClimb = climb;
					}
				}
			}
		}

		return prevClimb;
	}

	// Result class to hold both info and climbs
	private static class ClimbsResult {
		ClimbsInfo info = new ClimbsInfo();
		List<Climb> climbs;
	}

	private float calculateGrade(double elevationGain, double distance) {
		if (distance <= 0) {
			return 0f;
		}
		return (float) ((elevationGain / distance) * 100);
	}

	private float calculateCurrentGrade(@NonNull CurrentClimbInfo climbInfo) {
		// Calculate the current grade based on the last segment
		if (climbInfo.climb == null || climbInfo.climb.elevationProfile == null ||
		    climbInfo.climb.elevationProfile.size() < 2) {
			return climbInfo.climb != null ? climbInfo.climb.averageGrade : 0f;
		}

		// Get the current segment based on progress
		List<Double> elevations = climbInfo.climb.elevationProfile;
		List<Double> distances = climbInfo.climb.distanceProfile;

		// Calculate which segment we're on based on progress
		double progressRatio = climbInfo.totalClimbDistance > 0 ?
			(climbInfo.currentDistanceInClimb / climbInfo.totalClimbDistance) : 0;

		int segmentIndex = (int) (progressRatio * (elevations.size() - 1));
		segmentIndex = Math.min(segmentIndex, elevations.size() - 2);
		segmentIndex = Math.max(segmentIndex, 0);

		// Calculate elevation gain and distance for this segment
		double elevationGain = elevations.get(segmentIndex + 1) - elevations.get(segmentIndex);
		double segmentDistance = 0;

		if (distances != null && distances.size() > segmentIndex + 1) {
			segmentDistance = distances.get(segmentIndex + 1) - distances.get(segmentIndex);
		} else {
			// Estimate distance based on elevation profile size
			segmentDistance = climbInfo.climb.distance / (elevations.size() - 1);
		}

		return calculateGrade(elevationGain, segmentDistance);
	}

	private float calculateVAM(@NonNull CurrentClimbInfo climbInfo) {
		// VAM = Vertical Ascent Speed in vm/h (vertical meters per hour)
		// Accurate calculation from current speed and current grade:
		// VAM = currentSpeed * sin(atan(currentGrade/100)) * 3600
		// This calculates the actual vertical component of the current speed

		if (climbInfo.climb == null) {
			return 0f;
		}

		// Get current grade at current position
		float currentGrade = calculateCurrentGrade(climbInfo);
		
		// Get current speed in m/s from location
		float currentSpeed = 0;
		if (app != null && app.getLocationProvider() != null) {
			Location lastLocation = app.getLocationProvider().getLastKnownLocation();
			if (lastLocation != null) {
				currentSpeed = lastLocation.getSpeed();
			}
		}

		// If no speed available, use average speed
		if (currentSpeed <= 0 && app != null) {
			currentSpeed = app.getAverageSpeedComputer().getAverageSpeed(0, 10000, false);
		}

		// If still no speed, cannot calculate VAM
		if (currentSpeed <= 0) {
			return 0f;
		}

		// Calculate VAM from speed and grade
		// For a grade of G%, the slope angle theta has: tan(theta) = G/100
		// The vertical component of speed is: speed * sin(theta)
		// We use: sin(atan(x)) = x / sqrt(1 + x^2)
		// So: sin(atan(currentGrade/100)) = (currentGrade/100) / sqrt(1 + (currentGrade/100)^2)
		
		double gradeFactor = currentGrade / 100.0;
		double sinGrade = gradeFactor / Math.sqrt(1.0 + gradeFactor * gradeFactor);
		
		// VAM (vm/h) = currentSpeed (m/s) * sin(grade) * 3600 (s/h)
		return currentSpeed * (float) sinGrade * 3600f;
	}

	private void updateClimbProfileOverlay() {
		// All overlay state changes should happen on UI thread
		handler.post(() -> {
			try {
				if (!widgetState.shouldShowOverlay()) {
					hideClimbProfileOverlay();
					return;
				}

				// Only show overlay when user is ACTIVELY on a climb
				// Don't show for next/previous climbs to avoid showing overlay for entire route
				if (currentClimbInfo != null && currentClimbInfo.climb != null) {
					showClimbProfileOverlay(currentClimbInfo);
					return;
				}

				// Hide overlay if user is not on a climb
				hideClimbProfileOverlay();
			} catch (Exception e) {
				// Catch any errors in overlay state management
			}
		});
	}

	private void showClimbProfileOverlay(@NonNull CurrentClimbInfo climbInfo) {
		// Ensure all view operations happen on UI thread
		handler.post(() -> {
			try {
				// Reuse existing overlay if it already exists
				if (climbProfileOverlay != null) {
					updateOverlayContent(climbInfo);
					return;
				}

				// Create new overlay from layout
				climbProfileOverlay = mapActivity.getLayoutInflater().inflate(R.layout.climb_profile_overlay, null);

				// Configure overlay
				FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.WRAP_CONTENT
				);
				params.gravity = Gravity.BOTTOM;
				params.setMargins(0, 0, 0, 0);

				climbProfileOverlay.setLayoutParams(params);

				// Add to map activity
				ViewGroup rootView = mapActivity.findViewById(android.R.id.content);
				if (rootView != null) {
					// Check if already has parent
					ViewGroup existingParent = (ViewGroup) climbProfileOverlay.getParent();
					if (existingParent != null) {
						existingParent.removeView(climbProfileOverlay);
					}
					rootView.addView(climbProfileOverlay);
				} else {
					// Can't add overlay without root view
					climbProfileOverlay = null;
					return;
				}

				// Add elevation profile view programmatically
				FrameLayout elevationContainer = climbProfileOverlay.findViewById(R.id.climb_profile_elevation_container);
				if (elevationContainer != null) {
					elevationProfileView = new ElevationProfileView(mapActivity);
					elevationContainer.addView(elevationProfileView,
						new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
				}

				// Update content
				updateOverlayContent(climbInfo);
			} catch (Exception e) {
				// If anything goes wrong, clean up on UI thread
				handler.post(() -> {
					try {
						hideClimbProfileOverlay();
					} catch (Exception ex) {}
				});
				climbProfileOverlay = null;
				elevationProfileView = null;
			}
		});
	}

	private void hideClimbProfileOverlay() {
		// Ensure removal happens on UI thread
		handler.post(() -> {
			try {
				if (climbProfileOverlay != null) {
					ViewGroup parent = (ViewGroup) climbProfileOverlay.getParent();
					if (parent != null) {
						try {
							parent.removeView(climbProfileOverlay);
						} catch (Exception e) {
							// Ignore - view might already be removed
						}
					}
					climbProfileOverlay = null;
					elevationProfileView = null;
				}
			} catch (Exception e) {
				climbProfileOverlay = null;
				elevationProfileView = null;
			}
		});
	}

	private void updateOverlayContent(@NonNull CurrentClimbInfo climbInfo) {
		// Ensure content update happens on UI thread
		handler.post(() -> {
			try {
				if (climbProfileOverlay == null) {
					return;
				}

				Climb climb = climbInfo.climb;

			// Calculate progress percentage
			double progress = climbInfo.totalClimbDistance > 0 ? (climbInfo.currentDistanceInClimb / climbInfo.totalClimbDistance) : 0;

			// Update average grade (gemiddelde stijgingspercentage)
			TextView avgGradeText = climbProfileOverlay.findViewById(R.id.climb_profile_average_grade);
			if (avgGradeText != null) {
				avgGradeText.setText(String.format("%.1f%%", climb.averageGrade));
			}

			// Update current grade (huidige stijgingspercentage)
			TextView currentGradeText = climbProfileOverlay.findViewById(R.id.climb_profile_current_grade);
			if (currentGradeText != null) {
				float currentGrade = calculateCurrentGrade(climbInfo);
				currentGradeText.setText(String.format("%.1f%%", currentGrade));
			}

			// Update VAM (Vertical Ascent per Minute)
			TextView vamText = climbProfileOverlay.findViewById(R.id.climb_profile_vam);
			if (vamText != null) {
				float vam = calculateVAM(climbInfo);
				vamText.setText(String.format("%.0f vm/h", vam));
			}

			// Update elevation gain
			TextView elevationText = climbProfileOverlay.findViewById(R.id.climb_profile_elevation_gain);
			if (elevationText != null) {
				elevationText.setText("+" + (int) climb.elevationGain + "m");
			}

			// Ensure elevation profile view exists and is added to container with proper layout params
			FrameLayout elevationContainer = climbProfileOverlay.findViewById(R.id.climb_profile_elevation_container);
			if (elevationContainer != null && elevationProfileView == null) {
				try {
					elevationProfileView = new ElevationProfileView(mapActivity);
					elevationContainer.addView(elevationProfileView,
						new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
				} catch (Exception e) {
					// Ignore
				}
			}

			// Update elevation profile view with data
			if (elevationProfileView != null && climb.elevationProfile != null && !climb.elevationProfile.isEmpty()) {
				try {
					float currentVAM = calculateVAM(climbInfo);
					int vamTarget = getVamTarget();
					elevationProfileView.setData(climb.elevationProfile, climb.distanceProfile, progress, vamTarget, currentVAM);
				} catch (Exception e) {
					// Ignore
				}
			}
				// Hide the XML position indicator - we use the red line drawn in ElevationProfileView instead
				View positionIndicator = climbProfileOverlay.findViewById(R.id.climb_profile_position_indicator);
				if (positionIndicator != null) {
					try {
						positionIndicator.setVisibility(View.GONE);
					} catch (Exception e) {
						// Ignore
					}
				}
			} catch (Exception e) {
				// Catch any errors in content update
			}
		});
	}

	@Override
	public boolean isMetricSystemDepended() {
		return false;
	}

	@Override
	public boolean isAltitudeMetricDepended() {
		return true; // Elevation is displayed in meters
	}

	@NonNull
	public ClimbsCountWidgetState getWidgetState() {
		return widgetState;
	}

	@NonNull
	public CommonPreference<Integer> getMinClimbElevationPref() {
		return widgetState.getMinClimbElevationPref();
	}

	@NonNull
	public CommonPreference<Float> getMinClimbGradePref() {
		return widgetState.getMinClimbGradePref();
	}

	public int getVamTarget() {
		return widgetState.getVamTarget();
	}

	// Inner class to hold climb count info
	private static class ClimbsInfo {
		int totalClimbs;
		int completedClimbs;
	}

	// Class to hold climb profile data
	private static class Climb {
		int startIndex;
		int endIndex;
		double elevationGain;
		double distance;
		float averageGrade;
		List<Double> elevationProfile = new ArrayList<>();
		List<Double> distanceProfile = new ArrayList<>();
	}

	// Class to hold current climb tracking info
	private static class CurrentClimbInfo {
		Climb climb;
		int currentPointIndex;
		double currentDistanceInClimb;
		double totalClimbDistance;
	}
}
