package net.osmand.router;

import java.util.ArrayList;
import java.util.List;

import gnu.trove.list.array.TIntArrayList;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPointDouble;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.util.MapUtils;

public class PrecalculatedRouteDirection {
	
	private int[] pointsX;
	private int[] pointsY;
	private float minSpeed;
	private float maxSpeed;
	private float[] tms;
	private boolean followNext;
	private static final int SHIFT = (1 << (31 - 17));
	private static final int[] SHIFTS = new int[]{1 << (31 - 15), 1 << (31 - 13), 1 << (31 - 12), 
		1 << (31 - 11), 1 << (31 - 7)};
	
	private List<Integer> cachedS = new ArrayList<Integer>();
	
	private long startPoint = 0;
	private long endPoint = 0;
//	private DataTileManager<Integer> indexedPoints = new DataTileManager<Integer>(17);
	QuadTree<Integer> quadTree = new QuadTree<Integer>(new QuadRect(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE),
			8, 0.55f);
	private float startFinishTime;
	private float endFinishTime; 
	
	public PrecalculatedRouteDirection(TIntArrayList px, TIntArrayList py, List<Float> speedSegments, float maxSpeed) {
		this.maxSpeed = maxSpeed;
		init(px, py, speedSegments);
	}
	
	private PrecalculatedRouteDirection(List<RouteSegmentResult> ls, float maxSpeed) {
		this.maxSpeed = maxSpeed;
		init(ls);
	}
	
	private PrecalculatedRouteDirection(LatLon[] ls, float maxSpeed) {
		this.maxSpeed = maxSpeed;
		init(ls);
	}
	
	private PrecalculatedRouteDirection(PrecalculatedRouteDirection parent, int s1, int s2) {
		this.minSpeed = parent.minSpeed;
		this.maxSpeed = parent.maxSpeed;
		boolean inverse = false;
		if (s1 > s2) {
			int tmp = s1;
			s1 = s2;
			s2 = tmp;
			inverse = true;
		}
		tms = new float[s2 - s1 + 1];
		pointsX = new int[s2 - s1 + 1];
		pointsY = new int[s2 - s1 + 1];
		for (int i = s1; i <= s2; i++) {
			int shiftInd = i - s1;
			pointsX[shiftInd] = parent.pointsX[i];
			pointsY[shiftInd] = parent.pointsY[i];
//			indexedPoints.registerObjectXY(parent.pointsX.get(i), parent.pointsY.get(i), pointsX.size() - 1);
			quadTree.insert(shiftInd, parent.pointsX[i], parent.pointsY[i]);
			tms[shiftInd] = parent.tms[i] - parent.tms[inverse ? s1 : s2];
		}
	}
	
	public static PrecalculatedRouteDirection build(List<RouteSegmentResult> ls, float cutoffDistance, float maxSpeed){
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
			return new PrecalculatedRouteDirection(ls.subList(begi, endi), maxSpeed);
		}
		return null;
	}
	
	public static PrecalculatedRouteDirection build(LatLon[] ls, float maxSpeed){
		return new PrecalculatedRouteDirection(ls, maxSpeed);
	}
	

	private void init(List<RouteSegmentResult> ls) {
		TIntArrayList px = new TIntArrayList();
		TIntArrayList py = new TIntArrayList();
		List<Float> speedSegments = new ArrayList<Float>();
		for (RouteSegmentResult s : ls) {
			boolean plus = s.getStartPointIndex() < s.getEndPointIndex();
			int i = s.getStartPointIndex();
			RouteDataObject obj = s.getObject();
			float routeSpd = (s.getRoutingTime() == 0 || s.getDistance() == 0) ? maxSpeed : 
				(s.getDistance() / s.getRoutingTime());
			while (true) {
				i = plus? i + 1 : i -1;
				px.add(obj.getPoint31XTile(i));
				py.add(obj.getPoint31YTile(i));
				speedSegments.add(routeSpd);
				if (i == s.getEndPointIndex()) {
					break;
				}
			}
		}
		init(px, py, speedSegments);
	}
	
	private void init(LatLon[] ls) {
		TIntArrayList px = new TIntArrayList();
		TIntArrayList py = new TIntArrayList();
		List<Float> speedSegments = new ArrayList<Float>();
		for (LatLon s : ls) {
			float routeSpd = maxSpeed; // (s.getDistance() / s.getRoutingTime())
			px.add(MapUtils.get31TileNumberX(s.getLongitude()));
			py.add(MapUtils.get31TileNumberY(s.getLatitude()));
			speedSegments.add(routeSpd);
		}
		init(px, py, speedSegments);
	}

	private void init(TIntArrayList px, TIntArrayList py, List<Float> speedSegments) {
		float totaltm = 0;
		List<Float> times = new ArrayList<Float>();
		for (int i = 0; i < px.size(); i++) {
			// MapUtils.measuredDist31 vs BinaryRoutePlanner.squareRootDist
			// use measuredDist31 because we use precise s.getDistance() to calculate routeSpd
			int ip = i == 0 ? 0 : i - 1;
			float dist = (float) MapUtils.measuredDist31(px.get(ip), py.get(ip), px.get(i), py.get(i));
			float tm = dist / speedSegments.get(i);// routeSpd;
			times.add(tm);
			quadTree.insert(i, px.get(i), py.get(i));
			// indexedPoints.registerObjectXY();
			totaltm += tm;
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
		if (l1 == startPoint || l1 == endPoint) {
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
		float deviationPenalty = distToPoint / minSpeed;
		float finishTime = (start? startFinishTime : endFinishTime);
		if(start) {
			return (tms[0] - tms[ind]) +  deviationPenalty + finishTime;
		} else {
			return tms[ind] + deviationPenalty + finishTime;
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
			QuadPointDouble proj = MapUtils.getProjectionPoint31(x31, y31, pointsX[ind], pointsY[ind], pointsX[nind], pointsX[nind]);
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

	public void setFollowNext(boolean followNext) {
		this.followNext = followNext;
	}

	public boolean isFollowNext() {
		return followNext;
	}

	public PrecalculatedRouteDirection adopt(RoutingContext ctx) {
		int ind1 = getIndex(ctx.startX, ctx.startY);
		int ind2 = getIndex(ctx.targetX, ctx.targetY);
		minSpeed = ctx.getRouter().getDefaultSpeed();
		maxSpeed = ctx.getRouter().getMaxSpeed();
		if(ind1 == -1) {
			return null;
		}
		if(ind2 == -1) {
			return null;
		}
		PrecalculatedRouteDirection routeDirection = new PrecalculatedRouteDirection(this, ind1, ind2);
		routeDirection.startPoint = calc(ctx.startX, ctx.startY);
		routeDirection.startFinishTime = (float) BinaryRoutePlanner.squareRootDist(pointsX[ind1], pointsY[ind1], ctx.startX, ctx.startY) / maxSpeed;
		routeDirection.endPoint = calc(ctx.targetX, ctx.targetY);
		routeDirection.endFinishTime = (float) BinaryRoutePlanner.squareRootDist(pointsX[ind2], pointsY[ind2], ctx.targetX, ctx.targetY) / maxSpeed;
		routeDirection.followNext = followNext;
		return routeDirection;
	}

	public void updatePreciseStartEnd(int sx, int sy, int ex, int ey) {
		if (sx > 0 && sy > 0) {
			int ind = getIndex(sx, sy);
			if (ind != -1) {
				startPoint = calc(sx, sy);
				startFinishTime = (float) BinaryRoutePlanner.squareRootDist(pointsX[ind], pointsY[ind], sx, sy) / maxSpeed;
			}
		}
		if (ex > 0 && ey > 0) {
			int ind = getIndex(ex, ey);
			if (ind != -1) {
				endPoint = calc(ex, ey);
				endFinishTime = (float) BinaryRoutePlanner.squareRootDist(pointsX[ind], pointsY[ind], ex, ey) / maxSpeed;
			}
		}
	}
}
