package net.osmand.plus.measurementtool.command;

import android.util.Pair;

import androidx.annotation.NonNull;

import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.plus.measurementtool.MeasurementEditingContext;
import net.osmand.plus.measurementtool.MeasurementToolLayer;
import net.osmand.plus.measurementtool.RoadSegmentData;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.router.GpxRouteApproximation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ApplyGpxApproximationCommand extends MeasurementModeCommand {

	private ApplicationMode mode;
	private List<WptPt> points;
	private Map<Pair<WptPt, WptPt>, RoadSegmentData> roadSegmentData;
	private List<GpxRouteApproximation> approximations;
	private List<List<WptPt>> segmentPointsList;
	private final List<List<WptPt>> originalSegmentPointsList;

	public ApplyGpxApproximationCommand(@NonNull MeasurementToolLayer measurementLayer,
	                                    @NonNull List<GpxRouteApproximation> approximations,
	                                    @NonNull List<List<WptPt>> segmentPointsList,
	                                    @NonNull ApplicationMode mode) {
		super(measurementLayer);
		this.approximations = approximations;
		this.segmentPointsList = segmentPointsList;
		this.originalSegmentPointsList = new ArrayList<>(segmentPointsList);
		this.mode = mode;
	}

	public List<WptPt> getPoints() {
		return points;
	}

	public List<List<WptPt>> getOriginalSegmentPointsList() {
		return originalSegmentPointsList;
	}

	@NonNull
	@Override
	public MeasurementCommandType getType() {
		return MeasurementCommandType.APPROXIMATE_POINTS;
	}

	@Override
	public boolean execute() {
		MeasurementEditingContext ctx = getEditingCtx();
		points = new ArrayList<>(ctx.getPoints());
		roadSegmentData = ctx.getRoadSegmentData();
		applyApproximation();
		refreshMap();
		return true;
	}

	@Override
	public boolean update(@NonNull Command command) {
		if (command instanceof ApplyGpxApproximationCommand) {
			ApplyGpxApproximationCommand approxCommand = (ApplyGpxApproximationCommand) command;
			approximations = approxCommand.approximations;
			mode = approxCommand.mode;
			applyApproximation();
			refreshMap();
			return true;
		}
		return false;
	}

	@Override
	public void undo() {
		MeasurementEditingContext ctx = getEditingCtx();
		ctx.resetAppMode();
		ctx.clearSegments();
		ctx.setRoadSegmentData(roadSegmentData);
		ctx.addPoints(points);
		segmentPointsList = new ArrayList<>(originalSegmentPointsList);
		refreshMap();
	}

	@Override
	public void redo() {
		applyApproximation();
		refreshMap();
	}

	public void applyApproximation() {
		MeasurementEditingContext ctx = getEditingCtx();
		ctx.setAppMode(mode);
		for (int i = 0; i < approximations.size(); i++) {
			GpxRouteApproximation approximation = approximations.get(i);
			List<WptPt> segmentPoints = segmentPointsList.get(i);
			List<WptPt> newSegmentPoints = ctx.setPoints(approximation, segmentPoints, mode, false);
			if (newSegmentPoints != null) {
				segmentPointsList.set(i, newSegmentPoints);
			}
		}
	}
}
