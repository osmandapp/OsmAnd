package net.osmand.router;

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.osmand.binary.RouteDataObject;
import net.osmand.data.DataTileManager;

public class PrecalculatedRouteDirection {
	
	private TIntArrayList pointsX = new TIntArrayList();
	private TIntArrayList pointsY = new TIntArrayList();
	private float avgSpeed;
	private float[] tms;
	private static final int SHIFT = (1 << (31 - 18));
	private static final int[] SHIFTS = new int[]{(1 << (31 - 17)), (1 << (31 - 15)), (1 << (31 - 13)), (1 << (31 - 12)), 
		(1 << (31 - 11))};
	
	private List<Integer> cachedS = new ArrayList<Integer>();
	private float[] ct1 = new float[2];
	private float[] ct2 = new float[2];
	
	private Map<Long, Integer> prereg = new TreeMap<Long, Integer>();
	private DataTileManager<Integer> indexedPoints = new DataTileManager<Integer>(17);
	
	private PrecalculatedRouteDirection(List<RouteSegmentResult> ls, float avgSpeed) {
		this.avgSpeed = avgSpeed;
		init(ls);
	}
	
	private PrecalculatedRouteDirection(PrecalculatedRouteDirection parent, int s1, int s2) {
		this.avgSpeed = parent.avgSpeed;
		tms = new float[s2 - s1 + 1];
		for (int i = s1; i <= s2; i++) {
			pointsX.add(parent.pointsX.get(i));
			pointsY.add(parent.pointsY.get(i));
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
		int endi = ls.size() - 1;
		d = cutoffDistance;
		for (; endi >= 0; endi--) {
			d -= ls.get(endi).getDistance();
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
		for (RouteSegmentResult s : ls) {
			boolean plus = s.getStartPointIndex() < s.getEndPointIndex();
			int i = s.getStartPointIndex();
			RouteDataObject obj = s.getObject();
			float spd = s.getSegmentSpeed();
			while (true) {
				int iprev = i;
				i = plus? i + 1 : i -1;
				float tm = (float) (BinaryRoutePlanner.squareRootDist(obj.getPoint31XTile(iprev), obj.getPoint31YTile(iprev), 
						obj.getPoint31XTile(i), obj.getPoint31YTile(i)) / spd);
				pointsX.add(obj.getPoint31XTile(i));
				pointsY.add(obj.getPoint31YTile(i));
				times.add(tm);
				indexedPoints.registerObjectXY(obj.getPoint31XTile(i), obj.getPoint31YTile(i), pointsX.size() - 1);
				totaltm += tm;
				if (i == s.getEndPointIndex()) {
					break;
				}
			}
		}
		tms = new float[times.size()];
		float totDec = totaltm;
		for(int i = 0; i < times.size(); i++) {
			totDec -= times.get(i);
			tms[i] = totDec;
		}
		
	}

	public float timeEstimate(int sx31, int sy31, int ex31, int ey31) {
		getIndex(sx31, sy31, ct1);
		getIndex(ex31, ey31, ct2);
		return Math.abs(ct1[0] - ct2[0]) + ct1[1] + ct2[1];
	}

	private int getIndex(int x31, int y31, float[] ct) {
		long l = ((long) x31) << 32l + ((long)y31);
		Integer lt = prereg.get(l);
		int ind = 0;
		if(lt != null) {
			ind = lt;
		} else {
			cachedS.clear();
			indexedPoints.getObjects(x31 - SHIFT, y31 - SHIFT, x31 + SHIFT, y31 + SHIFT, cachedS);
			if (cachedS.size() == 0) {
				for (int k = 0; k < SHIFTS.length; k++) {
					indexedPoints.getObjects(x31 - SHIFTS[k], y31 - SHIFTS[k], x31 + SHIFTS[k], y31 + SHIFTS[k],
							cachedS);
					if (cachedS.size() != 0) {
						break;
					}
				}
				if (cachedS.size() == 0) {
					throw new IllegalStateException();
				}
			}
			double minDist = 0;
			for (int i = 0; i < cachedS.size(); i++) {
				Integer n = cachedS.get(i);
				double ds = BinaryRoutePlanner.squareRootDist(x31, y31, pointsX.get(n), pointsY.get(n));
				if (ds < minDist || i == 0) {
					ind = n;
					minDist = ds;
				}
			}
		}
		double ds = BinaryRoutePlanner.squareRootDist(x31, y31, pointsX.get(ind), pointsY.get(ind));
		ct[0] = tms[ind];
		ct[1] = (float) (ds / avgSpeed);
		return ind;
	}
	
	public PrecalculatedRouteDirection adopt(RoutingContext ctx) {
		int ind1 = getIndex(ctx.startX, ctx.startY, ct1);
		int ind2 = getIndex(ctx.startX, ctx.startY, ct2);
		if (ind1 < ind2) {
			PrecalculatedRouteDirection routeDirection = new PrecalculatedRouteDirection(this, ind1, ind2);
			routeDirection.preRegisterPoint(ctx.startX, ctx.startY);
			routeDirection.preRegisterPoint(ctx.targetX, ctx.targetY);
			return routeDirection;
		}
		return null;
	}

	private void preRegisterPoint(int x31, int y31) {
		int ind = getIndex(x31, y31, ct1);
		long l = ((long) x31) << 32l + ((long)y31);
		if(ind == -1){
			throw new IllegalStateException();
		}
		prereg.put(l, ind);
	}

	

}
