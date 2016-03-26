package net.osmand.plus.routing;

import net.osmand.Location;
import net.osmand.plus.GPXFile;
import net.osmand.plus.OsmandSettings;

import java.util.List;

public class GPXRouteParamsBuilder {
    boolean calculateOsmAndRoute = false;
    // parameters
    private GPXFile file;
    private boolean reverse;
    private boolean leftSide;
    private boolean passWholeRoute;
    private boolean calculateOsmAndRouteParts;
    private boolean useIntermediatePointsRTE;

    public GPXRouteParamsBuilder(GPXFile file, OsmandSettings settings){
        leftSide = settings.DRIVING_REGION.get().leftHandDriving;
        this.file = file;
    }

    public boolean isReverse() {
        return reverse;
    }
    public boolean isLeftSide() { return leftSide; }

    public boolean isCalculateOsmAndRouteParts() {
        return calculateOsmAndRouteParts;
    }

    public void setCalculateOsmAndRouteParts(boolean calculateOsmAndRouteParts) {
        this.calculateOsmAndRouteParts = calculateOsmAndRouteParts;
    }

    public void setUseIntermediatePointsRTE(boolean useIntermediatePointsRTE) {
        this.useIntermediatePointsRTE = useIntermediatePointsRTE;
    }

    public boolean isUseIntermediatePointsRTE() {
        return useIntermediatePointsRTE;
    }

    public boolean isCalculateOsmAndRoute() {
        return calculateOsmAndRoute;
    }

    public void setCalculateOsmAndRoute(boolean calculateOsmAndRoute) {
        this.calculateOsmAndRoute = calculateOsmAndRoute;
    }

    public void setPassWholeRoute(boolean passWholeRoute){
        this.passWholeRoute = passWholeRoute;
    }

    public boolean isPassWholeRoute() {
        return passWholeRoute;
    }

    public GPXRouteParams build(Location start, OsmandSettings settings) {
        GPXRouteParams res = new GPXRouteParams();
        res.prepareGPXFile(this);
//			if(passWholeRoute && start != null){
//				res.points.add(0, start);
//			}
        return res;
    }


    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }

    public GPXFile getFile() {
        return file;
    }

    public List<Location> getPoints() {
        GPXRouteParams copy = new GPXRouteParams();
        copy.prepareGPXFile(this);
        return copy.getPoints();
    }

}