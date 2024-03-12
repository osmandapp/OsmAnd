package net.osmand.osm.edit;

import java.util.*;

import net.osmand.data.LatLon;
import net.osmand.data.Multipolygon;
import net.osmand.data.MultipolygonBuilder;
import net.osmand.data.Ring;
import net.osmand.osm.edit.Relation.RelationMember;
import net.osmand.util.Algorithms;
import net.osmand.util.MapAlgorithms;
import net.osmand.util.MapUtils;

public class OsmMapUtils {
	
	private static final double POLY_CENTER_PRECISION= 1e-6;
	private static final int LOOP_LIMITATION = 10000000;

	public static double getDistance(Node e1, Node e2) {
		return MapUtils.getDistance(e1.getLatitude(), e1.getLongitude(), e2.getLatitude(), e2.getLongitude());
	}

	public static double getDistance(Node e1, double latitude, double longitude) {
		return MapUtils.getDistance(e1.getLatitude(), e1.getLongitude(), latitude, longitude);
	}

	public static double getDistance(Node e1, LatLon point) {
		return MapUtils.getDistance(e1.getLatitude(), e1.getLongitude(), point.getLatitude(), point.getLongitude());
	}

	public static boolean isMultipolygon(Map<String, String> tags) {
		return "multipolygon".equals(tags.get(OSMSettings.OSMTagKey.TYPE.getValue())) ||
				"protected_area".equals(tags.get(OSMSettings.OSMTagKey.BOUNDARY.getValue())) ||
				"low_emission_zone".equals(tags.get(OSMSettings.OSMTagKey.BOUNDARY.getValue())) ||
				"national_park".equals(tags.get(OSMSettings.OSMTagKey.BOUNDARY.getValue())) ||
				"danger_area".equals(tags.get(OSMSettings.OSMTagKey.MILITARY.getValue()));
	}

	public static LatLon getCenter(Entity e) {
		if (e instanceof Node) {
			return ((Node) e).getLatLon();
		} else if (e instanceof Way) {
			return getWeightCenterForWay(((Way) e));
		} else if (e instanceof Relation) {
			List<LatLon> list = new ArrayList<LatLon>();
			if (isMultipolygon(e.getTags())) {
				MultipolygonBuilder original = new MultipolygonBuilder();
				original.setId(e.getId());

				// fill the multipolygon with all ways from the Relation
				for (RelationMember es : ((Relation) e).getMembers()) {
					if (es.getEntity() instanceof Way) {
						boolean inner = "inner".equals(es.getRole()); //$NON-NLS-1$
						if (inner) {
							original.addInnerWay((Way) es.getEntity());
						} else if("outer".equals(es.getRole())){
							original.addOuterWay((Way) es.getEntity());
						}
					}
				}
				
				List<Multipolygon> multipolygons = original.splitPerOuterRing(null);
				if (!Algorithms.isEmpty(multipolygons)){
					Multipolygon m = multipolygons.get(0);
					List<Node> out = m.getOuterRings().get(0).getBorder();
					List<List<Node>> inner = new ArrayList<List<Node>>();
					if(!Algorithms.isEmpty(out)) {
						for (Ring r : m.getInnerRings()) {
							inner.add(r.getBorder());
						}
					}
					if (!Algorithms.isEmpty(out)) {
						return getComplexPolyCenter(out, inner);
					}
				}
			}
			
			for (RelationMember fe : ((Relation) e).getMembers()) {
				LatLon c = null;
				// skip relations to avoid circular dependencies
				if (!(fe.getEntity() instanceof Relation) && fe.getEntity() != null) {
					c = getCenter(fe.getEntity());
				}
				if (c != null) {
					list.add(c);
				}
			}
			return getWeightCenter(list);
		}
		return null;
	}
	
	public static LatLon getComplexPolyCenter(Collection<Node> outer, List<List<Node>> inner) {
		if (outer.size() > 3 && outer.size() <= 5 && inner == null) {
			List<Node> sub = new ArrayList<>(outer);
			return getWeightCenterForNodes(sub.subList(0, sub.size()-1));
		}
		
		final List<List<LatLon>> rings = new ArrayList<>();
		List<LatLon> outerRing = new ArrayList<>();
		
		for (Node n : outer) {
			outerRing.add(new LatLon(n.getLatitude(), n.getLongitude()));	
		}
		rings.add(outerRing);
		if (!Algorithms.isEmpty(inner)) {
			for (List<Node> ring: inner) {
				if (!Algorithms.isEmpty(ring)) {
					List <LatLon> ringll = new ArrayList<LatLon>();
					for (Node n : ring) {
						ringll.add(n.getLatLon());
					} 
					rings.add(ringll);
				}
			}
		}
		return getPolylabelPoint(rings);
	}

	public static LatLon getWeightCenter(Collection<LatLon> nodes) {
		if (nodes.isEmpty()) {
			return null;
		}
		double longitude = 0;
		double latitude = 0;
		for (LatLon n : nodes) {
			longitude += n.getLongitude();
			latitude += n.getLatitude();
		}
		return new LatLon(latitude / nodes.size(), longitude / nodes.size());
	}

	public static LatLon getWeightCenterForNodes(Collection<Node> nodes ) {
		if (nodes.isEmpty()) {
			return null;
		}
		double longitude = 0;
		double latitude = 0;
		int count = 0;
		for (Node n : nodes) {
			if (n != null) {
				count++;
				longitude += n.getLongitude();
				latitude += n.getLatitude();
			}
		}
		if (count == 0) {
			return null;
		}
		return new LatLon(latitude / count, longitude / count);
	}
	
	public static LatLon getWeightCenterForWay(Way w) {
		List<Node> nodes = w.getNodes();
		if (nodes.isEmpty()) {
			return null;
		}
		boolean area = w.getFirstNodeId() == w.getLastNodeId();
		// double check for area (could be negative all)
		if (area) {
			Node fn = w.getFirstNode();
			Node ln = w.getLastNode();
			if (fn != null && fn != null && MapUtils.getDistance(fn.getLatLon(), ln.getLatLon()) < 50) {
				area = true;
			} else {
				area = false;
			}
		}
		LatLon ll = area ? getComplexPolyCenter(nodes, null) : getWeightCenterForNodes(nodes);
		if (ll == null) {
			return null;
		}
		double flat = ll.getLatitude();
		double flon = ll.getLongitude();
		if (!area || !MapAlgorithms.containsPoint(nodes, ll.getLatitude(), ll.getLongitude())) {
			double minDistance = Double.MAX_VALUE;
			for (Node n : nodes) {
				if (n != null) {
					double d = MapUtils.getDistance(n.getLatitude(), n.getLongitude(), ll.getLatitude(), ll.getLongitude());
					if (d < minDistance) {
						flat = n.getLatitude();
						flon = n.getLongitude();
						minDistance = d;
					}
				}
			}
		}
		return new LatLon(flat, flon);

	}
	

	public static LatLon getMathWeightCenterForNodes(Collection<Node> nodes) {
		if (nodes.isEmpty()) {
			return null;
		}
		double longitude = 0;
		double latitude = 0;
		double sumDist = 0;
		Node prev = null;
		for (Node n : nodes) {
			if (n != null) {
				if (prev == null) {
					prev = n;
				} else {
					double dist = getDistance(prev, n);
					sumDist += dist;
					longitude += (prev.getLongitude() + n.getLongitude()) * dist / 2;
					latitude += (n.getLatitude() + n.getLatitude()) * dist / 2;
					prev = n;
				}
			}
		}
		if (sumDist == 0) {
			if (prev == null) {
				return null;
			}
			return prev.getLatLon();
		}
		return new LatLon(latitude / sumDist, longitude / sumDist);
	}

	public static void sortListOfEntities(List<? extends Entity> list, final double lat, final double lon) {
		Collections.sort(list, new Comparator<Entity>() {
			@Override
			public int compare(Entity o1, Entity o2) {
				return Double.compare(MapUtils.getDistance(o1.getLatLon(), lat, lon), MapUtils.getDistance(o2.getLatLon(), lat, lon));
			}
		});
	}

	public static void addIdsToList(Collection<? extends Entity> source, List<Long> ids) {
		for (Entity e : source) {
			ids.add(e.getId());
		}
	}

    public static boolean ccw(Node A, Node B, Node C) {
        return (C.getLatitude()-A.getLatitude()) * (B.getLongitude()-A.getLongitude()) > (B.getLatitude()-A.getLatitude()) *
                (C.getLongitude()-A.getLongitude());
    }

    // Return true if line segments AB and CD intersect
    public static boolean intersect2Segments(Node A, Node B, Node C, Node D) {
        return ccw(A, C, D) != ccw(B, C, D) && ccw(A, B, C) != ccw(A, B, D);
    }

	public static boolean[] simplifyDouglasPeucker(List<Node> n, int zoom, int epsilon, List<Node> result, boolean avoidNooses) {
		if (zoom > 31) {
			zoom = 31;
		}
		boolean[] kept = new boolean[n.size()];
		int first = 0;
		int nsize = n.size();
		while (first < nsize) {
			if (n.get(first) != null) {
				break;
			}
			first++;
		}
		int last = nsize - 1;
		while (last >= 0) {
			if (n.get(last) != null) {
				break;
			}
			last--;
		}
		if (last - first < 1) {
			return kept;
		}
		// check for possible cycle
		boolean checkCycle = true;
		boolean cycle = false;
		while (checkCycle && last > first) {
			checkCycle = false;

			double x1 = MapUtils.getTileNumberX(zoom, n.get(first).getLongitude());
			double y1 = MapUtils.getTileNumberY(zoom, n.get(first).getLatitude());
			double x2 = MapUtils.getTileNumberX(zoom, n.get(last).getLongitude());
			double y2 = MapUtils.getTileNumberY(zoom, n.get(last).getLatitude());
			if (Math.abs(x1 - x2) + Math.abs(y1 - y2) < 0.001) {
				last--;
				cycle = true;
				checkCycle = true;
			}
		}
		if (last - first < 1) {
			return kept;
		}
		simplifyDouglasPeucker(n, zoom, epsilon, kept, first, last, avoidNooses);
		result.add(n.get(first));
		for (int i = 0; i < kept.length; i++) {
			if(kept[i]) {
				result.add(n.get(i));
			}
		}
		if (cycle) {
			result.add(n.get(first));
		}
		kept[first] = true;
		
		return kept;
	}

	private static void simplifyDouglasPeucker(List<Node> n, int zoom, int epsilon, boolean[] kept,
                                               int start, int end, boolean avoidNooses) {
		double dmax = -1;
		int index = -1;
		for (int i = start + 1; i <= end - 1; i++) {
			if (n.get(i) == null) {
				continue;
			}
			double d = orthogonalDistance(zoom, n.get(start), n.get(end), n.get(i));// calculate distance from line
			if (d > dmax) {
				dmax = d;
				index = i;
			}
		}
        boolean nooseFound = false;
        if(avoidNooses && index >= 0) {
            Node st = n.get(start);
            Node e = n.get(end);
            for(int i = 0; i < n.size() - 1; i++) {
                if(i == start - 1) {
                    i = end;
                    continue;
                }
                Node np = n.get(i);
                Node np2 = n.get(i + 1);
                if(np == null || np2 == null) {
                    continue;
                }
                if (OsmMapUtils.intersect2Segments(st, e, np, np2)) {
                    nooseFound = true;
                    break;
                }
            }
        }
		if (dmax >= epsilon || nooseFound ) {
			simplifyDouglasPeucker(n, zoom, epsilon, kept, start, index, avoidNooses);
			simplifyDouglasPeucker(n, zoom, epsilon, kept, index, end, avoidNooses);
		} else {
			kept[end] = true;
		}
	}

	public static void simplifyDouglasPeucker(List<Node> nodes, int start, int end, List<Node> survivedNodes, double epsilon) {
		double dmax = Double.NEGATIVE_INFINITY;
		int index = -1;

		Node startPt = nodes.get(start);
		Node endPt = nodes.get(end);

		for (int i = start + 1; i < end; i++) {
			Node pt = nodes.get(i);
			double d = MapUtils.getOrthogonalDistance(pt.getLatitude(), pt.getLongitude(),
					startPt.getLatitude(), startPt.getLongitude(), endPt.getLatitude(), endPt.getLongitude());
			if (d > dmax) {
				dmax = d;
				index = i;
			}
		}
		if (dmax > epsilon) {
			simplifyDouglasPeucker(nodes, start, index, survivedNodes, epsilon);
			simplifyDouglasPeucker(nodes, index, end, survivedNodes, epsilon);
		} else {
			survivedNodes.add(nodes.get(end));
		}
	}

	private static double orthogonalDistance(int zoom, Node nodeLineStart, Node nodeLineEnd, Node node) {
		LatLon p = MapUtils.getProjection(node.getLatitude(), node.getLongitude(), nodeLineStart.getLatitude(),
				nodeLineStart.getLongitude(), nodeLineEnd.getLatitude(), nodeLineEnd.getLongitude());

		double x1 = MapUtils.getTileNumberX(zoom, p.getLongitude());
		double y1 = MapUtils.getTileNumberY(zoom, p.getLatitude());
		double x2 = MapUtils.getTileNumberX(zoom, node.getLongitude());
		double y2 = MapUtils.getTileNumberY(zoom, node.getLatitude());
		double C = x2 - x1;
		double D = y2 - y1;
		return Math.sqrt(C * C + D * D);
	}

	public static boolean isClockwiseWay(Way w) {
		return isClockwiseWay(Collections.singletonList(w));
	}

	public static boolean isClockwiseWay(List<Way> ways) {
		if (ways.isEmpty()) {
			return true;
		}
		LatLon latLon = ways.get(0).getLatLon();
		double lat = latLon.getLatitude();
		double lon = 180;
		double firstLon = -360;
		boolean firstDirectionUp = false;
		double previousLon = -360;

		double clockwiseSum = 0;

		Node prev = null;
		boolean firstWay = true;
		for (Way w : ways) {
			List<Node> ns = w.getNodes();
			int startInd = 0;
			int nssize = ns.size();
			if (firstWay && nssize > 0) {
				prev = ns.get(0);
				startInd = 1;
				firstWay = false;
			}
			for (int i = startInd; i < nssize; i++) {
				Node next = ns.get(i);
				double rlon = ray_intersect_lon(prev, next, lat, lon);
				if (rlon != -360d) {
					boolean skipSameSide = (prev.getLatitude() <= lat) == (next.getLatitude() <= lat);
					if (skipSameSide) {
						continue;
					}
					boolean directionUp = prev.getLatitude() <= lat;
					if (firstLon == -360) {
						firstDirectionUp = directionUp;
						firstLon = rlon;
					} else {
						boolean clockwise = (!directionUp) == (previousLon < rlon);
						if (clockwise) {
							clockwiseSum += Math.abs(previousLon - rlon);
						} else {
							clockwiseSum -= Math.abs(previousLon - rlon);
						}
					}
					previousLon = rlon;
				}
				prev = next;
			}
		}

		if (firstLon != -360) {
			boolean clockwise = (!firstDirectionUp) == (previousLon < firstLon);
			if (clockwise) {
				clockwiseSum += Math.abs(previousLon - firstLon);
			} else {
				clockwiseSum -= Math.abs(previousLon - firstLon);
			}
		}

		return clockwiseSum >= 0;
	}

	// try to intersect from left to right
	public static double ray_intersect_lon(Node node, Node node2, double latitude, double longitude) {
		// a node below
		Node a = node.getLatitude() < node2.getLatitude() ? node : node2;
		// b node above
		Node b = a == node2 ? node : node2;
		if (latitude == a.getLatitude() || latitude == b.getLatitude()) {
			latitude += 0.00000001d;
		}
		if (latitude < a.getLatitude() || latitude > b.getLatitude()) {
			return -360d;
		} else {
			if (longitude < Math.min(a.getLongitude(), b.getLongitude())) {
				return -360d;
			} else {
				if (a.getLongitude() == b.getLongitude() && longitude == a.getLongitude()) {
					// the node on the boundary !!!
					return longitude;
				}
				// that tested on all cases (left/right)
				double lon = b.getLongitude() - (b.getLatitude() - latitude) * (b.getLongitude() - a.getLongitude())
						/ (b.getLatitude() - a.getLatitude());
				if (lon <= longitude) {
					return lon;
				} else {
					return -360d;
				}
			}
		}
	}

    /**
     * Get the area in pixels
     * @param nodes
     * @return
     */
    public static double polygonAreaPixels(List<Node> nodes, int zoom) {
        double area = 0.;
        double mult = 1 / MapUtils.getPowZoom((double)Math.max(31 - (zoom + 8), 0));
        int j = nodes.size() - 1;
        for (int i = 0; i < nodes.size(); i++) {
            Node x = nodes.get(i);
            Node y = nodes.get(j);
            if(x != null && y != null) {
            area += (MapUtils.get31TileNumberX(y.getLongitude()) + (double)MapUtils.get31TileNumberX(x.getLongitude()))*
                    (MapUtils.get31TileNumberY(y.getLatitude()) - (double)MapUtils.get31TileNumberY(x.getLatitude()));
            }
            j = i;
        }
        return Math.abs(area) * mult * mult * .5;
    }

	/**
	 * Get the area (in m²) of a closed way, represented as a list of nodes
	 * 
	 * @param nodes
	 *            the list of nodes
	 * @return the area of it
	 */
	public static double getArea(List<Node> nodes) {
		// x = longitude
		// y = latitude
		// calculate the reference point (lower left corner of the bbox)
		// start with an arbitrary value, bigger than any lat or lon
		double refX = 500, refY = 500;
		for (Node n : nodes) {
			if (n.getLatitude() < refY)
				refY = n.getLatitude();
			if (n.getLongitude() < refX)
				refX = n.getLongitude();
		}

		List<Double> xVal = new ArrayList<Double>();
		List<Double> yVal = new ArrayList<Double>();

		for (Node n : nodes) {
			// distance from bottom line to x coordinate of node
			double xDist = MapUtils.getDistance(refY, refX, refY, n.getLongitude());
			// distance from left line to y coordinate of node
			double yDist = MapUtils.getDistance(refY, refX, n.getLatitude(), refX);

			xVal.add(xDist);
			yVal.add(yDist);
		}

		double area = 0;

		for (int i = 1; i < xVal.size(); i++) {
			area += xVal.get(i - 1) * yVal.get(i) - xVal.get(i) * yVal.get(i - 1);
		}

		return Math.abs(area) / 2;
	}
	
	/**
	 * Calculate "visual" center point of polygons (based on Mapbox' polylabel algorithm)
	 * @param rings - list of lists of nodes
	 * @return coordinates of calculated center
	 */
	
    public static LatLon getPolylabelPoint(List<List<LatLon>> rings) {
        // find the bounding box of the outer ring
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        List<LatLon> outerRingCoordinates = rings.get(0);
        for (LatLon p: outerRingCoordinates) {
            double lat = p.getLatitude();
            double lon = p.getLongitude();

            minX = StrictMath.min(minX, lon);
            minY = StrictMath.min(minY, lat);
            maxX = StrictMath.max(maxX, lon);
            maxY = StrictMath.max(maxY, lat);
        }

        double width = maxX - minX;
        double height = maxY - minY;
        double cellSize = Math.min(width, height);
        double h = cellSize / 2;

        if (cellSize == 0) return new LatLon(minX, minY);

        // a priority queue of cells in order of their "potential" (max distance to polygon)
        PriorityQueue<Cell> cellQueue = new PriorityQueue<>(new CellComparator());

        // cover polygon with initial cells
        for (double x = minX; x < maxX; x += cellSize) {
            for (double y = minY; y < maxY; y += cellSize) {
                cellQueue.add(new Cell(x + h, y + h, h, rings));
            }
        }
        
        // take centroid as the first best guess
        Cell bestCell = getCentroidCell(rings);
        if(bestCell == null) {
        	 return new LatLon(minX, minY);
        }

        // special case for rectangular polygons
        Cell bboxCell = new Cell(minX + width / 2, minY + height / 2, 0, rings);
        if (bboxCell.d > bestCell.d) bestCell = bboxCell;

        int count = 0;
		while (!cellQueue.isEmpty()) {
			if (count > LOOP_LIMITATION) {
				System.err.println("Error loop limitation: " + LOOP_LIMITATION);
				break;
			}
            // pick the most promising cell from the queue
            Cell cell = cellQueue.poll();

            // update the best cell if we found a better one
            if (cell.d > bestCell.d) {
                bestCell = cell;
            }

            // do not drill down further if there's no chance of a better solution
//            System.out.println(String.format("check for precision: cell.max - bestCell.d = %f Precision: %f", cell.max, precision));
            if (cell.max - bestCell.d <= POLY_CENTER_PRECISION) continue;

            // split the cell into four cells
            h = cell.h / 2;
            cellQueue.add(new Cell(cell.x - h, cell.y - h, h, rings));
            cellQueue.add(new Cell(cell.x + h, cell.y - h, h, rings));
            cellQueue.add(new Cell(cell.x - h, cell.y + h, h, rings));
            cellQueue.add(new Cell(cell.x + h, cell.y + h, h, rings));
            count++;
        }
//        System.out.println(String.format("Best lat/lon: %f, %f", bestCell.y, bestCell.x));
        return new LatLon(bestCell.y, bestCell.x);
    }

    // get polygon centroid
    private static Cell getCentroidCell(List<List<LatLon>> rings) {
        double area = 0;
        double x = 0;
        double y = 0;

        List<LatLon> points = rings.get(0);
        for (int i = 0, len = points.size(), j = len - 1; i < len; j = i++) {
            LatLon a = points.get(i);
            LatLon b = points.get(j);
            double aLon = a.getLongitude();
            double aLat = a.getLatitude();
            double bLon = b.getLongitude();
            double bLat = b.getLatitude();

            double f = aLon * bLat - bLon * aLat;
            x += (aLon + bLon) * f;
            y += (aLat + bLat) * f;
            area += f * 3;
        }

		if (area == 0) {
			if (points.size() == 0) {
				return null;
			}
			LatLon p = points.get(0);
			return new Cell(p.getLatitude(), p.getLongitude(), 0, rings);
		}

        return new Cell(x / area, y / area, 0, rings);
    }

    private static class CellComparator implements Comparator<Cell> {
        @Override
        public int compare(Cell o1, Cell o2) {
            return Double.compare(o2.max, o1.max);
        }
    }

    private static class Cell {
        private final double x; // cell center x (lon)
        private final double y; // cell center y (lat)
        private final double h; // half the cell size
        private final double d; // distance from cell center to polygon
        private final double max; // max distance to polygon within a cell

        private Cell(double x, double y, double h, List<List<LatLon>> rings) {
            this.x = x;
            this.y = y;
            this.h = h;
            this.d = pointToPolygonDist(x, y, rings);
            this.max = this.d + this.h * Math.sqrt(2);
        }

        // signed distance from point to polygon outline (negative if point is outside)
        private double pointToPolygonDist(double x, double y, List<List<LatLon>> rings) {
            boolean inside = false;
            double minDistSq = Double.MAX_VALUE;

            for (List<LatLon> ring: rings) {
                for (int i = 0, len = ring.size(), j = len - 1; i < len; j = i++) {
                    LatLon a = ring.get(i);
                    LatLon b = ring.get(j);
                    double aLon = a.getLongitude();
                    double aLat = a.getLatitude();
                    double bLon = b.getLongitude();
                    double bLat = b.getLatitude();

                    if ((aLat > y != bLat > y) && (x < (bLon - aLon) * (y - aLat) / (bLat - aLat) + aLon)) {
                    	inside = !inside;
                    }
                    	
                    minDistSq = Math.min(minDistSq, getSegmentDistanceSqared(x, y, a, b));
                }
            }
            return (inside ? 1 : -1) * Math.sqrt(minDistSq);
        }

        // get squared distance from a point to a segment of polygon
        private double getSegmentDistanceSqared(double px, double py, LatLon a, LatLon b) {
            double x = a.getLongitude();
            double y = a.getLatitude();
            double dx = b.getLongitude() - x;
            double dy = b.getLatitude() - y;

            if (dx != 0 || dy != 0) {
                double t = ((px - x) * dx + (py - y) * dy) / (dx * dx + dy * dy);

                if (t > 1) {
                    x = b.getLongitude();
                    y = b.getLatitude();
                } else if (t > 0) {
                    x += dx * t;
                    y += dy * t;
                }
            }

            dx = px - x;
            dy = py - y;

            return dx * dx + dy * dy;
        }
    }
}
