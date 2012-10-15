package net.osmand.router;

import java.util.List;

import net.osmand.data.DataTileManager;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;

public class BaseRouteResult {

	private final List<RouteSegmentResult> baseResult;
	private DataTileManager<RouteSegmentResult> indexedData = new DataTileManager<RouteSegmentResult>(15);

	public BaseRouteResult(List<RouteSegmentResult> baseResult) {
		this.baseResult = baseResult;
		indexData();
	}
	
	
	private void indexData() {
		for(RouteSegmentResult r : baseResult){
//			r.getObject().getPoint31XTile(i);
			
//			indexedData.evaluateTile(latitude, longitude)
		}
		
	}


	public float getOrthogonalDistance(RouteSegment r){
		float dist = 0;
		int x = r.getRoad().getPoint31XTile(r.getSegmentStart());
		int y = r.getRoad().getPoint31YTile(r.getSegmentStart());
		return dist;
	}
}
