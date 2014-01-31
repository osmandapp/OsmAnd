package net.osmand.router;

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.List;

import net.osmand.binary.RouteDataObject;
import net.osmand.data.QuadPoint;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.util.MapUtils;

public class PrecalculatedRouteDirection {
	
	private int[] pointsX;
	private int[] pointsY;
	private float speed;
	private float[] tms;
	private static final int SHIFT = (1 << (31 - 17));
	private static final int[] SHIFTS = new int[]{1 << (31 - 15), 1 << (31 - 13), 1 << (31 - 12), 
		1 << (31 - 11), 1 << (31 - 7)};
	
	private List<Integer> cachedS = new ArrayList<Integer>();
	
	private long startPoint = 0;
	private long endPoint = 0;
//	private DataTileManager<Integer> indexedPoints = new DataTileManager<Integer>(17);
	QuadTree<Integer> quadTree = new QuadTree<Integer>(new QuadRect(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE),
			8, 0.55f); 
	
	private PrecalculatedRouteDirection(List<RouteSegmentResult> ls, float avgSpeed) {
		this.speed = avgSpeed;
		init(ls);
	}
	
	private PrecalculatedRouteDirection(PrecalculatedRouteDirection parent, int s1, int s2) {
		this.speed = parent.speed;
		tms = new float[s2 - s1 + 1];
		pointsX = new int[s2 - s1 + 1];
		pointsY = new int[s2 - s1 + 1];
		for (int i = s1; i <= s2; i++) {
			pointsX[i - s1] = parent.pointsX[i];
			pointsY[i - s1] = parent.pointsY[i];
//			indexedPoints.registerObjectXY(parent.pointsX.get(i), parent.pointsY.get(i), pointsX.size() - 1);
			quadTree.insert(pointsX.length - 1, parent.pointsX[i], parent.pointsY[i]);
			tms[i - s1] = parent.tms[i] - parent.tms[s2];
		}
	}
	
	public static PrecalculatedRouteDirection build(List<RouteSegmentResult> ls, float cutoffDistance, float avgSpeed){
		int begi = 0;
		float d = cutoffDistance;
		for (; begi < ls.size(); begi++) {
			d -= ls.get(begi).getDistance();
			if (d < 0) {
				break;
			}
		}
		int endi = ls.size();
		d = cutoffDistance;
		for (; endi > 0; endi--) {
			d -= ls.get(endi - 1).getDistance();
			if (d < 0) {
				break;
			}
		}
		if(begi < endi) {
			return new PrecalculatedRouteDirection(ls.subList(begi, endi), avgSpeed);
		}
		return null;
	}
	

	private void init(List<RouteSegmentResult> ls) {
		float totaltm = 0;
		List<Float> times = new ArrayList<Float>();
		TIntArrayList px = new TIntArrayList();
		TIntArrayList py = new TIntArrayList();
		for (RouteSegmentResult s : ls) {
			boolean plus = s.getStartPointIndex() < s.getEndPointIndex();
			int i = s.getStartPointIndex();
			RouteDataObject obj = s.getObject();
			float routeSpd = s.getDistance()/ s.getRoutingTime() ;
			while (true) {
				int iprev = i;
				i = plus? i + 1 : i -1;
				// MapUtils.measuredDist31 vs BinaryRoutePlanner.squareRootDist
				// use measuredDist31 because we use precise s.getDistance() to calculate routeSpd
				float dist = (float) MapUtils.measuredDist31(obj.getPoint31XTile(iprev), obj.getPoint31YTile(iprev), 
						obj.getPoint31XTile(i), obj.getPoint31YTile(i));
				float tm = dist / routeSpd;
				px.add(obj.getPoint31XTile(i));
				py.add(obj.getPoint31YTile(i));
				times.add(tm);
				quadTree.insert(px.size() - 1, obj.getPoint31XTile(i), obj.getPoint31YTile(i));
				// indexedPoints.registerObjectXY();
				totaltm += tm;
				
				if (i == s.getEndPointIndex()) {
					break;
				}
			}
		}
		pointsX = px.toArray();
		pointsY = py.toArray();
		tms = new float[times.size()];
		float totDec = totaltm;
		for(int i = 0; i < times.size(); i++) {
			totDec -= times.get(i);
			tms[i] = totDec;
		}
		
	}

	public float timeEstimate(int sx31, int sy31, int ex31, int ey31) {
		long l1 = calc(sx31, sy31);
		long l2 = calc(ex31, ey31);
		int x31;
		int y31;
		boolean start;
		if(l1 == startPoint || l1 == endPoint) {
			start = l1 == startPoint;
			x31 = ex31;
			y31 = ey31;
		} else if(l2 == startPoint || l2 == endPoint) {
			start = l2 == startPoint;
			x31 = sx31;
			y31 = sy31;
		} else {
			throw new UnsupportedOperationException();
		}
		int ind = getIndex(x31, y31);
		if(ind == -1) {
			return -1;
		}
		if((ind == 0 && start) || 
				(ind == pointsX.length - 1 && !start)) {
			return -1;
		}
		float distToPoint = getDeviationDistance(x31, y31, ind);
		float deviationPenalty = distToPoint / speed;
		if(start) {
			return (tms[0] - tms[ind]) +  deviationPenalty;
		} else {
			return tms[ind] + deviationPenalty;
		}
	}
	
	public float getDeviationDistance(int x31, int y31) {
		int ind = getIndex(x31, y31);
		if(ind == -1) {
			return 0;
		}
		return getDeviationDistance(x31, y31, ind);
	}

	public float getDeviationDistance(int x31, int y31, int ind) {
		float distToPoint = 0; //BinaryRoutePlanner.squareRootDist(x31, y31, pointsX.get(ind), pointsY.get(ind));
		if(ind < pointsX.length - 1 && ind != 0) {
			double nx = BinaryRoutePlanner.squareRootDist(x31, y31, pointsX[ind + 1], pointsY[ind + 1]);
			double pr = BinaryRoutePlanner.squareRootDist(x31, y31, pointsX[ind - 1], pointsY[ind - 1]);
			int nind =  nx > pr ? ind -1 : ind +1;
			QuadPoint proj = MapUtils.getProjectionPoint31(x31, y31, pointsX[ind], pointsY[ind], pointsX[nind], pointsX[nind]);
			distToPoint = (float) BinaryRoutePlanner.squareRootDist(x31, y31, (int)proj.x, (int)proj.y) ;
		}
		return distToPoint;
	}

	public int getIndex(int x31, int y31) {
		int ind = -1;
		cachedS.clear();
//		indexedPoints.getObjects(x31 - SHIFT, y31 - SHIFT, x31 + SHIFT, y31 + SHIFT, cachedS);
		quadTree.queryInBox(new QuadRect(x31 - SHIFT, y31 - SHIFT, x31 + SHIFT, y31 + SHIFT), cachedS);
		if (cachedS.size() == 0) {
			for (int k = 0; k < SHIFTS.length; k++) {
				quadTree.queryInBox(new QuadRect(x31 - SHIFTS[k], y31 - SHIFTS[k], x31 + SHIFTS[k], y31 + SHIFTS[k]), cachedS);
//				indexedPoints.getObjects(x31 - SHIFTS[k], y31 - SHIFTS[k], x31 + SHIFTS[k], y31 + SHIFTS[k],cachedS);
				if (cachedS.size() != 0) {
					break;
				}
			}
			if (cachedS.size() == 0) {
				return -1;
			}
		}
		double minDist = 0;
		for (int i = 0; i < cachedS.size(); i++) {
			Integer n = cachedS.get(i);
			double ds = BinaryRoutePlanner.squareRootDist(x31, y31, pointsX[n], pointsY[n]);
			if (ds < minDist || i == 0) {
				ind = n;
				minDist = ds;
			}
		}
		return ind;
	}

	private long calc(int x31, int y31) {
		return ((long) x31) << 32l + ((long)y31);
	}
	
	public PrecalculatedRouteDirection adopt(RoutingContext ctx) {
		int ind1 = getIndex(ctx.startX, ctx.startY);
		int ind2 = getIndex(ctx.targetX, ctx.targetY);
		if(ind1 == -1) {
			throw new IllegalArgumentException();
		}
		if(ind2 == -1) {
			throw new IllegalArgumentException();
		}
		PrecalculatedRouteDirection routeDirection = new PrecalculatedRouteDirection(this, ind1, ind2);
		routeDirection.startPoint = calc(ctx.startX, ctx.startY);
//		routeDirection.startX31 = ctx.startX;
//		routeDirection.startY31 = ctx.startY;
		routeDirection.endPoint = calc(ctx.targetX, ctx.targetX);
//		routeDirection.endX31 = ctx.targetX;
//		routeDirection.endY31 = ctx.targetY;
		return routeDirection;
	}


	

}
