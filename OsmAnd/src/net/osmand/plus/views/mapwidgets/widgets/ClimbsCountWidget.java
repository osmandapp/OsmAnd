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
			// Opponent starts at the same time as user and rides at speed corresponding to target VAM
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
		updateClimbsCount();
	}

	private void updateClimbsCount() {
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
	}

	private boolean isRouteAvailable() {
		return routingHelper != null && routingHelper.isFollowingMode() && routingHelper.getCurrentGPX() != null;
	}

	private ClimbsResult calculateClimbs(int minElevation, float minGrade) {
		ClimbsResult result = new ClimbsResult();
		result.info.totalClimbs = 0;
		result.info.completedClimbs = 0;
		result.climbs = new ArrayList<>();
		climbs.clear();

		GpxFile gpxFile = routingHelper.getCurrentGPX();
		if (gpxFile == null) {
			return result;
		}

		// Get all points from the route using the public API
		List<WptPt> allPoints = gpxFile.getAllSegmentsPoints();
		if (allPoints == null || allPoints.size() < 2) {
			return result;
		}

		// Find climbs by identifying uphill segments that meet criteria
		// A climb ends when there's more than 50m without ascent AND no new climb starts within 200m
		// Implementation: end climb when there's 200m without ascent (this satisfies both conditions)
		int climbStartIndex = 0;
		boolean inClimb = false;
		double currentClimbGain = 0;
		double currentClimbDistance = 0;
		double netClimbGain = 0; // Net gain from climb start (can go negative temporarily)
		double distanceSinceLastAscent = 0; // Distance traveled without positive elevation gain

		// For building elevation profiles per climb
		List<Double> climbElevationProfile = new ArrayList<>();
		List<Double> climbDistanceProfile = new ArrayList<>();

		for (int i = 1; i < allPoints.size(); i++) {
			WptPt prevPoint = allPoints.get(i - 1);
			WptPt currPoint = allPoints.get(i);

			// Skip if no valid elevation data
			if (Double.isNaN(prevPoint.getEle()) || Double.isNaN(currPoint.getEle())) {
				// End current climb if we were in one
				if (inClimb && netClimbGain >= minElevation) {
					float avgGrade = calculateGrade(netClimbGain, currentClimbDistance);
					if (avgGrade >= minGrade) {
						Climb climb = createClimb(climbStartIndex, i - 1, netClimbGain, currentClimbDistance, avgGrade, climbElevationProfile, climbDistanceProfile);
						result.climbs.add(climb);
						climbs.add(climb);
						result.info.totalClimbs++;
					}
				}
				inClimb = false;
				climbElevationProfile = new ArrayList<>();
				climbDistanceProfile = new ArrayList<>();
				continue;
			}

			double elevationGain = currPoint.getEle() - prevPoint.getEle();
			double segmentDistance = MapUtils.getDistance(prevPoint.getLatitude(), prevPoint.getLongitude(), currPoint.getLatitude(), currPoint.getLongitude());

			// Track distance since last ascent for climb termination
			if (elevationGain > 0) {
				distanceSinceLastAscent = 0;
			} else {
				distanceSinceLastAscent += segmentDistance;
			}

			if (!inClimb) {
				// Not in a climb, check if we should start one
				if (elevationGain > 0) {
					inClimb = true;
					climbStartIndex = i - 1;
					currentClimbGain = elevationGain;
					currentClimbDistance = segmentDistance;
					netClimbGain = elevationGain;
					// Start fresh profiles for this climb
					climbElevationProfile = new ArrayList<>();
					climbDistanceProfile = new ArrayList<>();
					climbElevationProfile.add(prevPoint.getEle());
					climbDistanceProfile.add(0.0);
					climbElevationProfile.add(currPoint.getEle());
					climbDistanceProfile.add(segmentDistance);
				} else {
					// Not in a climb and not climbing - reset profiles
					climbElevationProfile = new ArrayList<>();
					climbDistanceProfile = new ArrayList<>();
				}
			} else {
				// We're in a climb - add to profiles
				climbElevationProfile.add(currPoint.getEle());
				if (climbDistanceProfile.isEmpty()) {
					climbDistanceProfile.add(segmentDistance);
				} else {
					climbDistanceProfile.add(climbDistanceProfile.get(climbDistanceProfile.size() - 1) + segmentDistance);
				}

				if (elevationGain >= 0) {
					// Still climbing or flat
					currentClimbGain += elevationGain;
					currentClimbDistance += segmentDistance;
					netClimbGain += elevationGain;
				} else {
					// Descending - reduce net gain
					netClimbGain += elevationGain;
					currentClimbDistance += segmentDistance;

					// End climb if there's been more than 200m without ascent
					// (this satisfies: >50m without ascent AND no new climb within 200m, since distanceSinceLastAscent resets on ascent)
					if (distanceSinceLastAscent > 200) {
						// End of climb - check if it meets criteria
						double totalGain = currentClimbGain;
						if (totalGain >= minElevation) {
							float avgGrade = calculateGrade(totalGain, currentClimbDistance);
							if (avgGrade >= minGrade) {
								Climb climb = createClimb(climbStartIndex, i - 1, totalGain, currentClimbDistance, avgGrade, climbElevationProfile, climbDistanceProfile);
								result.climbs.add(climb);
								climbs.add(climb);
								result.info.totalClimbs++;
							}
						}
						inClimb = false;
						// Reset distance since last ascent
						distanceSinceLastAscent = 0;
					}
				}
			}
		}

		// Handle the last climb if we're still in one
		if (inClimb && currentClimbGain >= minElevation) {
			float avgGrade = calculateGrade(currentClimbGain, currentClimbDistance);
			if (avgGrade >= minGrade) {
				Climb climb = createClimb(climbStartIndex, allPoints.size() - 1, currentClimbGain, currentClimbDistance, avgGrade, climbElevationProfile, climbDistanceProfile);
				result.climbs.add(climb);
				climbs.add(climb);
				result.info.totalClimbs++;
			}
		}

		// Build cumulative distances for all points
		cumulativeDistances.clear();
		cumulativeDistances.add(0.0);
		for (int i = 1; i < allPoints.size(); i++) {
			WptPt prev = allPoints.get(i - 1);
			WptPt curr = allPoints.get(i);
			if (!Double.isNaN(prev.getEle()) && !Double.isNaN(curr.getEle())) {
				double dist = MapUtils.getDistance(prev.getLatitude(), prev.getLongitude(), curr.getLatitude(), curr.getLongitude());
				cumulativeDistances.add(cumulativeDistances.get(i - 1) + dist);
			} else {
				cumulativeDistances.add(cumulativeDistances.get(i - 1));
			}
		}
		
		// Also update the member variable
		this.cumulativeDistances = new ArrayList<>(cumulativeDistances);

		// Calculate completed climbs based on current location
		RouteCalculationResult route = routingHelper.getRoute();
		if (route != null && !result.climbs.isEmpty()) {
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

			// Also update completedClimbs member variable for overlay logic
			this.completedClimbs = completedCount;

			if (wholeDistance == 0) {
				result.info.completedClimbs = 0;
				this.completedClimbs = 0;
			}
		} else {
			result.info.completedClimbs = 0;
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
		if (!widgetState.shouldShowOverlay()) {
			hideClimbProfileOverlay();
			return;
		}

		// Show overlay if user is on a climb
		if (currentClimbInfo != null && currentClimbInfo.climb != null) {
			showClimbProfileOverlay(currentClimbInfo);
			return;
		}

		// Also show overlay for next upcoming climb (within 500m)
		// This helps users see what's coming up
		RouteCalculationResult route = routingHelper.getRoute();
		if (route != null && !cumulativeDistances.isEmpty()) {
			int routeDistance = route.getDistanceFromStart();
			
			// Find next climb
			Climb nextClimb = findNextClimb(cumulativeDistances, routeDistance);
			if (nextClimb != null) {
				// Create a dummy CurrentClimbInfo for the next climb
				// This will show the climb profile but with 0 progress
				CurrentClimbInfo nextClimbInfo = new CurrentClimbInfo();
				nextClimbInfo.climb = nextClimb;
				nextClimbInfo.currentPointIndex = nextClimb.startIndex;
				nextClimbInfo.currentDistanceInClimb = 0;
				nextClimbInfo.totalClimbDistance = nextClimb.distance;
				
				showClimbProfileOverlay(nextClimbInfo);
				return;
			}
			
			// Also check for recently completed climb (within 200m after end)
			Climb prevClimb = findPreviousClimb(cumulativeDistances, routeDistance);
			if (prevClimb != null) {
				// Create a dummy CurrentClimbInfo for the previous climb
				// This will show the climb profile with 100% progress (completed)
				CurrentClimbInfo prevClimbInfo = new CurrentClimbInfo();
				prevClimbInfo.climb = prevClimb;
				prevClimbInfo.currentPointIndex = prevClimb.endIndex;
				prevClimbInfo.currentDistanceInClimb = prevClimb.distance;
				prevClimbInfo.totalClimbDistance = prevClimb.distance;
				
				showClimbProfileOverlay(prevClimbInfo);
				return;
			}
		}

		// Hide overlay if no climb is active, upcoming, or recently completed
		hideClimbProfileOverlay();
	}

	private void showClimbProfileOverlay(@NonNull CurrentClimbInfo climbInfo) {
		if (climbProfileOverlay != null) {
			// Update existing overlay
			updateOverlayContent(climbInfo);
			return;
		}

		// Create overlay from layout
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
			rootView.addView(climbProfileOverlay);
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
	}

	private void hideClimbProfileOverlay() {
		if (climbProfileOverlay != null) {
			ViewGroup parent = (ViewGroup) climbProfileOverlay.getParent();
			if (parent != null) {
				parent.removeView(climbProfileOverlay);
			}
			climbProfileOverlay = null;
			elevationProfileView = null;
		}
	}

	private void updateOverlayContent(@NonNull CurrentClimbInfo climbInfo) {
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

		// Ensure elevation profile view exists and is added to container
		FrameLayout elevationContainer = climbProfileOverlay.findViewById(R.id.climb_profile_elevation_container);
		if (elevationContainer != null && elevationProfileView == null) {
			elevationProfileView = new ElevationProfileView(mapActivity);
			elevationContainer.addView(elevationProfileView);
		}

		// Update elevation profile view with data
		if (elevationProfileView != null && climb.elevationProfile != null && !climb.elevationProfile.isEmpty()) {
			float currentVAM = calculateVAM(climbInfo);
			int vamTarget = getVamTarget();
			elevationProfileView.setData(climb.elevationProfile, climb.distanceProfile, progress, vamTarget, currentVAM);
		}
		// Update position indicator
		View positionIndicator = climbProfileOverlay.findViewById(R.id.climb_profile_position_indicator);
		View chartContainer = climbProfileOverlay.findViewById(R.id.climb_profile_chart_container);
		if (positionIndicator != null && chartContainer != null) {
			FrameLayout.LayoutParams indicatorParams = (FrameLayout.LayoutParams) positionIndicator.getLayoutParams();
			if (indicatorParams == null) {
				indicatorParams = new FrameLayout.LayoutParams(
						2, // 2dp width
						ViewGroup.LayoutParams.MATCH_PARENT
				);
			}
			indicatorParams.leftMargin = (int) (chartContainer.getWidth() * progress);
			positionIndicator.setLayoutParams(indicatorParams);
		}
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
