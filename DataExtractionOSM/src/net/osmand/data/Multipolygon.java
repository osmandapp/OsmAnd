package net.osmand.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Node;
import net.osmand.osm.Way;

import org.apache.commons.logging.Log;

/**
 * The idea of multipolygon:
 * - we treat each outer way as closed polygon
 * - multipolygon is always closed!
 * - each way we try to assign to existing way and form 
 *   so a more complex polygon
 * - number of outer ways, is number of polygons
 * 
 * @author Pavol Zibrita
 */
public class Multipolygon {
	
	/**
	 * cache with the ways grouped per Ring
	 */
	private SortedSet<Ring> innerRings, outerRings;

	/**
	 * ways added by the user
	 */
	private List<Way> outerWays, innerWays;
	
	/**
	 * an optional id of the multipolygon
	 */
	private long id;
	
	/**
	 * create a multipolygon with these outer and inner rings
	 * the rings have to be well formed or data inconsistency will happen
	 * @param outerRings the collection of outer rings
	 * @param innerRings the collection of inner rings
	 */
	public Multipolygon(SortedSet<Ring> outerRings, SortedSet<Ring> innerRings) {
		this();
		this.outerRings = outerRings;
		this.innerRings = innerRings;
		for (Ring r : outerRings) {
			outerWays.addAll(r.getWays());
		}
		
		for (Ring r : innerRings) {
			innerWays.addAll(r.getWays());
		}
	}

	/**
	 * Create a multipolygon with initialized outer and inner ways
	 * @param outers a list of outer ways
	 * @param inners a list of inner ways
	 */
	public Multipolygon(List<Way> outers, List<Way> inners) {
		this();
		outerWays.addAll(outers);
		innerWays.addAll(inners);
	}
	
	/**
	 * create a new empty multipolygon
	 */
	public Multipolygon(){
		outerWays = new ArrayList<Way> ();
		innerWays = new ArrayList<Way> ();
		id = 0L;
	}
	
	/**
	 * create a new empty multipolygon with specified id
	 * @param id the id to set
	 */
	public Multipolygon(long id){
		this();
		setId(id);
	}
	
	/**
	 * set the id of the multipolygon
	 * @param newId id to set
	 */
	public void setId(long newId) {
		id = newId;
	}
	
	/**
	 * get the id of the multipolygon
	 * @return id
	 */
	public long getId() {
		return id;
	}
	
	/**
	 * check if this multipolygon contains a point
	 * @param point point to check
	 * @return true if this multipolygon is correct and contains the point
	 */
	public boolean containsPoint(LatLon point) {
		
		return containsPoint(point.getLatitude(), point.getLongitude());
		
	}

	/**
	 * check if this multipolygon contains a point
	 * @param latitude lat to check
	 * @param longitude lon to check
	 * @return true if this multipolygon is correct and contains the point
	 */
	public boolean containsPoint(double latitude, double longitude){
		
		
		TreeSet<Ring> outers = new TreeSet<Ring>();
		TreeSet<Ring> inners = new TreeSet<Ring>();
		
			for (Ring outer : getOuterRings()) {
				if (outer.containsPoint(latitude, longitude)) {
					outers.add(outer);
				}
			}
			
			for(Ring inner : getInnerRings()) {
				if (inner.containsPoint(latitude, longitude)) {
					inners.add(inner);
				}
			}
			
			if(outers.size() == 0) return false;
			if(inners.size() == 0) return true;
			
			Ring smallestOuter = outers.first();
			Ring smallestInner = inners.first();
			
			// if the smallest outer is in the smallest inner, the multiPolygon contains the point
			
			return smallestOuter.isIn(smallestInner);
		
	}

	/**
	 * get the Inner Rings
	 * @return the inner rings
	 */
	public SortedSet<Ring> getInnerRings() {
		groupInRings();
		return innerRings;
	}
	
	/**
	 * get the outer rings 
	 * @return outer rings
	 */
	public SortedSet<Ring> getOuterRings() {
		groupInRings();
		return outerRings;
	}

	/**
	 * get the outer ways
	 * @return outerWays or empty list if null
	 */
	private List<Way> getOuterWays() {
		if (outerWays == null) {
			outerWays = new ArrayList<Way>(1);
		}
		return outerWays;
	}

	/**
	 * get the inner ways
	 * @return innerWays or empty list if null
	 */
	private List<Way> getInnerWays() {
		if (innerWays == null) {
			innerWays = new ArrayList<Way>(1);
		}
		return innerWays;
	}

	/**
	 * get the number of outer Rings
	 * @return
	 */
	public int countOuterPolygons() {
		
		groupInRings();
		return zeroSizeIfNull(getOuterRings());
		
		
	}
	
	/**
	 * Check if this multiPolygon has outer ways
	 * @return true if this has outer ways
	 */
	public boolean hasOpenedPolygons() {
	    return zeroSizeIfNull(getOuterWays()) != 0;
	}
	
	/**
	 * chekc if all rings are closed
	 * @return true if all rings are closed by nature, false otherwise
	 */
	public boolean areRingsComplete() {
		SortedSet<Ring> set = getOuterRings();
		for (Ring r : set) {
			if (!r.isClosed()) {
				return false;
			}
		}
		set = getInnerRings();
		for (Ring r : set) {
			if (!r.isClosed()) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * return 0 if the list is null
	 * @param l the list to check
	 * @return the size of the list, or 0 if the list is null
	 */
	private int zeroSizeIfNull(Collection<?> l) {
		return l != null ? l.size() : 0;
	}
	
	/**
	 * Add an inner way to the multiPolygon
	 * @param w the way to add
	 */
	public void addInnerWay(Way w) {
		getInnerWays().add(w);
		innerRings = null;
	}

	/**
	 * Add an outer way to the multiPolygon
	 * @param w the way to add
	 */
	public void addOuterWay(Way w) {
		getOuterWays().add(w);
		outerRings = null;
	}

	/**
	 * Add everything from multipolygon to this
	 * @param multipolygon the MultiPolygon to copy
	 */
	public void copyPolygonsFrom(Multipolygon multipolygon) {
		for (Way inner : multipolygon.getInnerWays()) {
			addInnerWay(inner);
		}
		for (Way outer : multipolygon.getOuterWays()) {
			addOuterWay(outer);
		}
		// reset cache
		outerRings = null;
		innerRings = null;
	}

	/**
	 * Add outer ways to the outer Ring
	 * @param ways the ways to add
	 */
	public void addOuterWays(List<Way> ways) {
		for (Way outer : ways) {
			addOuterWay(outer);
		}
	}

	/**
	 * Get the weighted center of all nodes in this multiPolygon <br />
	 * This only works when the ways have initialized nodes
	 * @return the weighted center
	 */
	public LatLon getCenterPoint() {
		List<Node> points = new ArrayList<Node>();
		for (Way w : getOuterWays()) {
			points.addAll(w.getNodes());
		}
		
		for (Way w : getInnerWays()) {
			points.addAll(w.getNodes());
		}
		
		return MapUtils.getWeightCenterForNodes(points);
	}
	
	/**
	 * check if a cache has been created
	 * @return true if the cache exists
	 */
	public boolean hasCache() {
		return outerRings != null && innerRings != null;
	}
	
	/**
	 * Create the cache <br />
	 * The cache has to be null before it will be created
	 */
	private void groupInRings() {
		if (outerRings == null) {
			outerRings = Ring.combineToRings(getOuterWays());
		}
		if (innerRings == null) {
			innerRings = Ring.combineToRings(getInnerWays());
		}
	}
	
	/**
	 * Split this multipolygon in several separate multipolygons with one outer ring each
	 * @param log the stream to log problems to, if log = null, nothing will be logged
	 * @return a list with multipolygons which have exactly one outer ring
	 */
	public List<Multipolygon> splitPerOuterRing(Log log) {
		
		//make a clone of the inners set
		// this set will be changed through execution of the method
		SortedSet<Ring> inners = new TreeSet<Ring>(getInnerRings());
		
		// get the set of outer rings in a variable. This set will not be changed
		SortedSet<Ring> outers = getOuterRings();
		ArrayList<Multipolygon> multipolygons = new ArrayList<Multipolygon>();
		
		// loop; start with the smallest outer ring
		for (Ring outer : outers) {
			
			// Search the inners inside this outer ring
			SortedSet<Ring> innersInsideOuter = new TreeSet<Ring>();
			for (Ring inner : inners) {
				if (inner.isIn(outer)) {
					innersInsideOuter.add(inner);
				}
			}
			
			// the inners should belong to this outer, so remove them from the list to check
			inners.removeAll(innersInsideOuter);
			
			SortedSet<Ring> thisOuter = new TreeSet<Ring>();
			thisOuter.add(outer);
			
			// create a new multipolygon with this outer and a list of inners
			Multipolygon m = new Multipolygon(thisOuter, innersInsideOuter);
			
			multipolygons.add(m);
		}
		
		if (inners.size() != 0 && log != null)
			log.warn("Multipolygon "+getId() + " has a mismatch in outer and inner rings");
		
		return multipolygons;
	}
	
	/**
	 * This method only works when the multipolygon has exaclt one outer Ring
	 * @return the list of nodes in the outer ring
	 */
	public List<Node> getOuterNodes() {
		return getOuterRings().first().getBorder().getNodes();
	}


}
