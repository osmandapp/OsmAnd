package net.osmand.data;

import gnu.trove.map.hash.TLongObjectHashMap;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.Way;
import net.osmand.util.MapUtils;
import org.apache.commons.logging.Log;

import java.util.*;

/**
 * The idea of multipolygon:
 * - we treat each outer way as closed polygon
 * - multipolygon is always closed!
 * - each way we try to assign to existing way and form
 * so a more complex polygon
 * - number of outer ways, is number of polygons
 *
 * @author Pavol Zibrita
 */
public class MultipolygonBuilder {

	/* package */ List<Way> outerWays = new ArrayList<Way>();
	/* package */ List<Way> innerWays = new ArrayList<Way>();

	long id;

	/**
	 * Create a multipolygon with initialized outer and inner ways
	 *
	 * @param outers a list of outer ways
	 * @param inners a list of inner ways
	 */
	public MultipolygonBuilder(List<Way> outers, List<Way> inners) {
		this();
		outerWays.addAll(outers);
		innerWays.addAll(inners);
	}

	public MultipolygonBuilder() {
		id = -1L;
	}

	public void setId(long newId) {
		id = newId;
	}

	public long getId() {
		return id;
	}

	public MultipolygonBuilder addInnerWay(Way w) {
		innerWays.add(w);
		return this;
	}

	public List<Way> getOuterWays() {
		return outerWays;
	}

	public List<Way> getInnerWays() {
		return innerWays;
	}

	public MultipolygonBuilder addOuterWay(Way w) {
		outerWays.add(w);
		return this;
	}

	/**
	 * Split this multipolygon in several separate multipolygons with one outer ring each
	 *
	 * @param log the stream to log problems to, if log = null, nothing will be logged
	 * @return a list with multipolygons which have exactly one outer ring
	 */
	public List<Multipolygon> splitPerOuterRing(Log log) {
		SortedSet<Ring> inners = new TreeSet<Ring>(combineToRings(innerWays));
		ArrayList<Ring> outers = combineToRings(outerWays);
		Collections.sort(outers, new Comparator<Ring>() {

			@Override
			public int compare(Ring o1, Ring o2) {
				return -Integer.compare(o1.getBorder().size(), o2.getBorder().size());
			}
		});
		ArrayList<Multipolygon> multipolygons = new ArrayList<Multipolygon>();
		// loop; start with the smallest outer ring
		for (Ring outer : outers) {
			ArrayList<Ring> innersInsideOuter = new ArrayList<Ring>();
			Iterator<Ring> innerIt = inners.iterator();
			while (innerIt.hasNext()) {
				Ring inner = innerIt.next();
				if (inner.isIn(outer)) {
					innersInsideOuter.add(inner);
					innerIt.remove();
				}
			}
			multipolygons.add(new Multipolygon(outer, innersInsideOuter, id, true));
		}

		if (inners.size() != 0 && log != null) {
			log.warn("Multipolygon " + getId() + " has a mismatch in outer and inner rings");
		}

		return multipolygons;
	}

	public Multipolygon build() {
		return new Multipolygon(combineToRings(outerWays), combineToRings(innerWays), id);
	}

	public ArrayList<Ring> combineToRings(List<Way> ways) {
		// make a list of multiLines (connecter pieces of way)
		TLongObjectHashMap<List<Way>> multilineStartPoint = new TLongObjectHashMap<List<Way>>();
		TLongObjectHashMap<List<Way>> multilineEndPoint = new TLongObjectHashMap<List<Way>>();
		for (Way toAdd : ways) {
			if (toAdd.getNodeIds().size() < 2) {
				continue;
			}
			// iterate over the multiLines, and add the way to the correct one
			Way changedWay = toAdd;
			Way newWay;
			do {
				newWay = merge(multilineStartPoint, getLastId(changedWay), changedWay, 
						multilineEndPoint, getFirstId(changedWay));
				if(newWay == null) {
					newWay = merge(multilineEndPoint, getFirstId(changedWay), changedWay, 
							multilineStartPoint, getLastId(changedWay));
				}
				if(newWay == null) {
					newWay = merge(multilineStartPoint, getFirstId(changedWay), changedWay, 
							multilineEndPoint, getLastId(changedWay));
				}
				if(newWay == null) {
					newWay = merge(multilineEndPoint, getLastId(changedWay), changedWay, 
							multilineStartPoint, getFirstId(changedWay));
				}
				if(newWay != null) {
					changedWay = newWay;
				}
			} while (newWay != null);
			
			addToMap(multilineStartPoint, getFirstId(changedWay), changedWay);
			addToMap(multilineEndPoint, getLastId(changedWay), changedWay);

		}
		
		List<Way> multiLines = new ArrayList<Way>();
		for(List<Way> lst : multilineStartPoint.valueCollection()) {
			multiLines.addAll(lst);
		}
		ArrayList<Ring> result = new ArrayList<Ring>();
		for (Way multiLine : multiLines) {
			Ring r = new Ring(multiLine);
			result.add(r);
		}
		return result;
	}

	private Way merge(TLongObjectHashMap<List<Way>> endMap, long stNodeId, Way changedWay,
			TLongObjectHashMap<List<Way>> startMap, long endNodeId) {
		List<Way> lst = endMap.get(stNodeId);
		if(lst != null && lst.size() > 0) {
			Way candToMerge = lst.get(0);
			Way newWay = combineTwoWaysIfHasPoints(candToMerge, changedWay);
			List<Way> otherLst = startMap.get(
					getLastId(candToMerge) == stNodeId ? getFirstId(candToMerge) : getLastId(candToMerge));
			boolean removed1 = lst.remove(candToMerge) ;
			boolean removed2 = otherLst != null && otherLst.remove(candToMerge);
			if(newWay == null || !removed1 || !removed2) {
				throw new UnsupportedOperationException("Can't merge way: " + changedWay.getId() + " " + stNodeId + " -> " + endNodeId);
			}
			return newWay;
		}
		
		return null;
	}

	private void addToMap(TLongObjectHashMap<List<Way>> mp, long id, Way changedWay) {
		List<Way> lst = mp.get(id);
		if(lst == null) {
			lst = new ArrayList<>();
			mp.put(id, lst);
		}
		lst.add(changedWay);
	}
	

	private long getId(Node n) {
		if(n == null ) {
			return - nextRandId();
		}
		long l = MapUtils.get31TileNumberY(n.getLatitude());
		l = (l << 31) | MapUtils.get31TileNumberX(n.getLongitude());
		return l;
	}
	
	/**
	 * make a new Way with the nodes from two other ways
	 *
	 * @param w1 the first way
	 * @param w2 the second way
	 * @return null if it is not possible
	 */
	private Way combineTwoWaysIfHasPoints(Way w1, Way w2) {
		boolean combine = true;
		boolean firstReverse = false;
		boolean secondReverse = false;
		long w1f = getFirstId(w1);
		long w2f = getFirstId(w2);
		long w1l = getLastId(w1);
		long w2l = getLastId(w2);
		if (w1f == w2f) {
			firstReverse = true;
			secondReverse = false;
		} else if (w1l == w2f) {
			firstReverse = false;
			secondReverse = false;
		} else if (w1l  == w2l) {
			firstReverse = false;
			secondReverse = true;
		} else if (w1f == w2l) {
			firstReverse = true;
			secondReverse = true;
		} else {
			combine = false;
		}
		if (combine) {
			Way newWay = new Way(nextRandId());
			boolean nodePresent = w1.getNodes() != null || w1.getNodes().size() != 0;
			int w1size = nodePresent ? w1.getNodes().size() : w1.getNodeIds().size();
			for (int i = 0; i < w1size; i++) {
				int ind = firstReverse ? (w1size - 1 - i) : i;
				if (nodePresent) {
					newWay.addNode(w1.getNodes().get(ind));
				} else {
					newWay.addNode(w1.getNodeIds().get(ind));
				}
			}
			int w2size = nodePresent ? w2.getNodes().size() : w2.getNodeIds().size();
			for (int i = 1; i < w2size; i++) {
				int ind = secondReverse ? (w2size - 1 - i) : i;
				if (nodePresent) {
					newWay.addNode(w2.getNodes().get(ind));
				} else {
					newWay.addNode(w2.getNodeIds().get(ind));
				}
			}
			return newWay;
		}
		return null;

	}

	private long getLastId(Way w1) {
		return w1.getLastNodeId() > 0 ? w1.getLastNodeId(): getId(w1.getLastNode());
	}

	private long getFirstId(Way w1) {
		return w1.getFirstNodeId() > 0 ? w1.getFirstNodeId(): getId(w1.getFirstNode());
	}

	

	private static long initialValue = -1000;
	private final static long randomInterval = 5000;

	/**
	 * get a random long number
	 *
	 * @return
	 */
	private static long nextRandId() {
		// exclude duplicates in one session (!) and be quazirandom every run
		long val = initialValue - Math.round(Math.random() * randomInterval);
		initialValue = val;
		return val;
	}

}
