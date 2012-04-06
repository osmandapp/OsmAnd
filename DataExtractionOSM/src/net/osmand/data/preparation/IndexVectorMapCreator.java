package net.osmand.data.preparation;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.procedure.TObjectProcedure;
import gnu.trove.set.hash.TLongHashSet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.Algoritms;
import net.osmand.IProgress;
import net.osmand.binary.OsmandOdb.MapData;
import net.osmand.binary.OsmandOdb.MapDataBlock;
import net.osmand.data.Boundary;
import net.osmand.data.MapAlgorithms;
import net.osmand.data.WayBoundary;
import net.osmand.osm.Entity;
import net.osmand.osm.Entity.EntityId;
import net.osmand.osm.Entity.EntityType;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.MapRenderingTypes.MapRulType;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Node;
import net.osmand.osm.WayChain;
import net.osmand.osm.OSMSettings.OSMTagKey;
import net.osmand.osm.Relation;
import net.osmand.osm.Way;

import org.apache.commons.logging.Log;

import rtree.Element;
import rtree.IllegalValueException;
import rtree.LeafElement;
import rtree.NonLeafElement;
import rtree.RTree;
import rtree.RTreeException;
import rtree.RTreeInsertException;
import rtree.Rect;

public class IndexVectorMapCreator extends AbstractIndexPartCreator {
	
	

	// map zoom levels <= 2^MAP_LEVELS
	private static final int MAP_LEVELS_POWER = 3;
	private static final int MAP_LEVELS_MAX = 1 << MAP_LEVELS_POWER;
	private MapRenderingTypes renderingTypes;
	private MapZooms mapZooms;
	
	private boolean COASTLINE_PROCESS = true;

	Map<Long, TIntArrayList> multiPolygonsWays = new LinkedHashMap<Long, TIntArrayList>();
	
	
	
	private Map<Long, List<Long>> highwayRestrictions = new LinkedHashMap<Long, List<Long>>();

	// local purpose to speed up processing cache allocation
	TIntArrayList typeUse = new TIntArrayList(8);
	List<MapRulType> tempNameUse = new ArrayList<MapRenderingTypes.MapRulType>();
	Map<MapRulType, String> namesUse = new LinkedHashMap<MapRenderingTypes.MapRulType, String>();
	TIntArrayList addtypeUse = new TIntArrayList(8);
	List<Long> restrictionsUse = new ArrayList<Long>(8);

	private CoastlineProcessor coastlineProcessor;
	private PreparedStatement mapBinaryStat;
	private PreparedStatement mapLowLevelBinaryStat;
	private int lowLevelWays = -1;
	private RTree[] mapTree = null;
	private Connection mapConnection;

	private int zoomWaySmothness = 0;
	private final Log logMapDataWarn;
	
	private static long notUsedId = - 1 << 40; // million million  

	public IndexVectorMapCreator(Log logMapDataWarn, MapZooms mapZooms, MapRenderingTypes renderingTypes, int zoomWaySmothness) {
		this.logMapDataWarn = logMapDataWarn;
		this.mapZooms = mapZooms;
		this.zoomWaySmothness = zoomWaySmothness;
		this.renderingTypes = renderingTypes;
		this.coastlineProcessor = new CoastlineProcessor(logMapDataWarn, mapZooms, renderingTypes, zoomWaySmothness);
		lowLevelWays = -1;
	}

	public void indexMapRelationsAndMultiPolygons(Entity e, OsmDbAccessorContext ctx) throws SQLException {
		indexMultiPolygon(e, ctx);
	}

	private void indexMultiPolygon(Entity e, OsmDbAccessorContext ctx) throws SQLException {
		if (e instanceof Relation && "multipolygon".equals(e.getTag(OSMTagKey.TYPE))) { //$NON-NLS-1$
			ctx.loadEntityData(e);
			Map<Entity, String> entities = ((Relation) e).getMemberEntities();

			boolean outerFound = false;
			for (Entity es : entities.keySet()) {
				if (es instanceof Way) {
					boolean inner = "inner".equals(entities.get(es)); //$NON-NLS-1$
					if (!inner) {
						outerFound = true;
						for (String t : es.getTagKeySet()) {
							e.putTag(t, es.getTag(t));
						}
						break;
					}
				}
			}
			if (!outerFound) {
				logMapDataWarn.warn("Probably map bug: Multipoligon id=" + e.getId() + " contains only inner ways : "); //$NON-NLS-1$ //$NON-NLS-2$
				return;
			}

			renderingTypes.encodeEntityWithType(e, mapZooms.getLevel(0).getMaxZoom(), typeUse, addtypeUse, namesUse, tempNameUse);
			if (typeUse.size() > 0) {
				List<List<Way>> completedRings = new ArrayList<List<Way>>();
				List<List<Way>> incompletedRings = new ArrayList<List<Way>>();
				for (Entity es : entities.keySet()) {
					if (es instanceof Way) {
						if (!((Way) es).getNodeIds().isEmpty()) {
							combineMultiPolygons((Way) es, completedRings, incompletedRings);
						}
					}
				}
				// skip incompleted rings and do not add whole relation ?
				if (!incompletedRings.isEmpty()) {
					logMapDataWarn.warn("In multipolygon  " + e.getId() + " there are incompleted ways : " + incompletedRings);
					return;
					// completedRings.addAll(incompletedRings);
				}

				// skip completed rings that are not one type
				for (List<Way> l : completedRings) {
					boolean innerType = "inner".equals(entities.get(l.get(0))); //$NON-NLS-1$
					for (Way way : l) {
						boolean inner = "inner".equals(entities.get(way)); //$NON-NLS-1$
						if (innerType != inner) {
							logMapDataWarn
									.warn("Probably map bug: Multipoligon contains outer and inner ways.\n" + //$NON-NLS-1$
											"Way:"
											+ way.getId()
											+ " is strange part of completed ring. InnerType:" + innerType + " way inner: " + inner + " way inner string:" + entities.get(way)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
							return;
						}
					}
				}

				// That check is not strictly needed on preproccessing step because client can handle it
				Node nodeOut = checkOuterWaysEncloseInnerWays(completedRings, entities);
				if (nodeOut != null) {
					logMapDataWarn.warn("Map bug: Multipoligon contains 'inner' way point outside of 'outer' border.\n" + //$NON-NLS-1$
							"Multipolygon id : " + e.getId() + ", inner node out id : " + nodeOut.getId()); //$NON-NLS-1$
				}

				List<Node> outerWaySrc = new ArrayList<Node>();
				List<List<Node>> innerWays = new ArrayList<List<Node>>();

				TIntArrayList typeToSave = new TIntArrayList(typeUse);
				long baseId = 0;
				for (List<Way> l : completedRings) {
					boolean innerType = "inner".equals(entities.get(l.get(0))); //$NON-NLS-1$
					if (!innerType && !outerWaySrc.isEmpty()) {
						logMapDataWarn.warn("Map bug: Multipoligon contains many 'outer' borders.\n" + //$NON-NLS-1$
								"Multipolygon id : " + e.getId() + ", outer way id : " + l.get(0).getId()); //$NON-NLS-1$
						return;
					}
					List<Node> toCollect;
					if (innerType) {
						toCollect = new ArrayList<Node>();
						innerWays.add(toCollect);
					} else {
						toCollect = outerWaySrc;
					}

					for (Way way : l) {
						toCollect.addAll(way.getNodes());
						if (!innerType) {
							TIntArrayList out = multiPolygonsWays.put(way.getId(), typeToSave);
							if(out == null){
								baseId = -way.getId();
							}
						}
					}
				}
				if(baseId == 0){
					// use base id as well?
					baseId = notUsedId --;
				}
				nextZoom: for (int level = 0; level < mapZooms.size(); level++) {
					renderingTypes.encodeEntityWithType(e, mapZooms.getLevel(level).getMaxZoom(), typeUse, addtypeUse, namesUse,
							tempNameUse);
					if (typeUse.isEmpty()) {
						continue;
					}
					long id = convertBaseIdToGeneratedId(baseId, level);
					// simplify route
					List<Node> outerWay = outerWaySrc;
					int zoomToSimplify = mapZooms.getLevel(level).getMaxZoom() - 1;
					if (zoomToSimplify < 15) {
						outerWay = simplifyCycleWay(outerWay, zoomToSimplify);
						if (outerWay == null) {
							continue nextZoom;
						}
						List<List<Node>> newinnerWays = new ArrayList<List<Node>>();
						for (List<Node> ls : innerWays) {
							ls = simplifyCycleWay(ls, zoomToSimplify);
							if (ls != null) {
								newinnerWays.add(ls);
							}
						}
						innerWays = newinnerWays;
					}
					insertBinaryMapRenderObjectIndex(mapTree[level], outerWay, innerWays, namesUse, id, true, typeUse, addtypeUse, true);
				}
			}
		}
	}

	private Node checkOuterWaysEncloseInnerWays(List<List<Way>> completedRings, Map<Entity, String> entities) {
		List<List<Way>> innerWays = new ArrayList<List<Way>>();
		Boundary outerBoundary = new Boundary(true);
		Node toReturn = null;
		for (List<Way> ring : completedRings) {
			boolean innerType = "inner".equals(entities.get(ring.get(0))); //$NON-NLS-1$
			if (!innerType) {
				outerBoundary.getOuterWays().addAll(ring);
			} else {
				innerWays.add(ring);
			}
		}

		for (List<Way> innerRing : innerWays) {
			ring: for (Way innerWay : innerRing) {
				for (Node node : innerWay.getNodes()) {
					if (!outerBoundary.containsPoint(node.getLatitude(), node.getLongitude())) {
						if (toReturn == null) {
							toReturn = node;
						}
						completedRings.remove(innerRing);
						break ring;
					}
				}
			}
		}
		return toReturn;
	}

	private void combineMultiPolygons(Way w, List<List<Way>> completedRings, List<List<Way>> incompletedRings) {
		long lId = w.getEntityIds().get(w.getEntityIds().size() - 1).getId().longValue();
		long fId = w.getEntityIds().get(0).getId().longValue();
		if (fId == lId) {
			completedRings.add(Collections.singletonList(w));
		} else {
			List<Way> l = new ArrayList<Way>();
			l.add(w);
			boolean add = true;
			for (int k = 0; k < incompletedRings.size();) {
				boolean remove = false;
				List<Way> i = incompletedRings.get(k);
				Way last = i.get(i.size() - 1);
				Way first = i.get(0);
				long lastId = last.getEntityIds().get(last.getEntityIds().size() - 1).getId().longValue();
				long firstId = first.getEntityIds().get(0).getId().longValue();
				if (fId == lastId) {
					i.addAll(l);
					remove = true;
					l = i;
					fId = firstId;
				} else if (lId == firstId) {
					l.addAll(i);
					remove = true;
					lId = lastId;
				}
				if (remove) {
					incompletedRings.remove(k);
				} else {
					k++;
				}
				if (fId == lId) {
					completedRings.add(l);
					add = false;
					break;
				}
			}
			if (add) {
				incompletedRings.add(l);
			}
		}
	}

	protected List<Node> simplifyCycleWay(List<Node> ns, int zoom) throws SQLException {
		if (checkForSmallAreas(ns, zoom + Math.min(zoomWaySmothness / 2, 3), 2, 4)) {
			return null;
		}
		List<Node> res = new ArrayList<Node>();
		// simplification
		MapAlgorithms.simplifyDouglasPeucker(ns, zoom + 8 + zoomWaySmothness, 3, res);
		if (res.size() < 2) {
			return null;
		}
		return res;
	}

	public int getLowLevelWays() {
		return lowLevelWays;
	}

	private void loadNodes(byte[] nodes, List<Float> toPut) {
		toPut.clear();
		for (int i = 0; i < nodes.length;) {
			int lat = Algoritms.parseIntFromBytes(nodes, i);
			i += 4;
			int lon = Algoritms.parseIntFromBytes(nodes, i);
			i += 4;
			toPut.add(Float.intBitsToFloat(lat));
			toPut.add(Float.intBitsToFloat(lon));
		}
	}
	
	private void parseAndSort(TIntArrayList ts, byte[] bs) {
		ts.clear();
		if (bs != null && bs.length > 0) {
			for (int j = 0; j < bs.length; j += 2) {
				ts.add(Algoritms.parseSmallIntFromBytes(bs, j));
			}
		}
		ts.sort();
	}

	public void processingLowLevelWays(IProgress progress) throws SQLException {
		restrictionsUse.clear();
		mapLowLevelBinaryStat.executeBatch();
		mapLowLevelBinaryStat.close();
		pStatements.remove(mapLowLevelBinaryStat);
		mapLowLevelBinaryStat = null;
		mapConnection.commit();

		PreparedStatement startStat = mapConnection.prepareStatement("SELECT id, start_node, end_node, nodes, name, type, addType FROM low_level_map_objects"
				+ " WHERE start_node = ? AND level = ?");
		PreparedStatement endStat = mapConnection.prepareStatement("SELECT id, start_node, end_node, nodes, name, type, addType FROM low_level_map_objects"
				+ " WHERE end_node = ? AND level = ?");
		Statement selectStatement = mapConnection.createStatement();
		ResultSet rs = selectStatement.executeQuery("SELECT id, start_node, end_node, nodes, name, type, addType, level FROM low_level_map_objects");
		TLongHashSet visitedWays = new TLongHashSet();
		ArrayList<Float> list = new ArrayList<Float>(100);
		TIntArrayList temp = new TIntArrayList();
		TIntArrayList tempAdd = new TIntArrayList();
		while (rs.next()) {
			if (lowLevelWays != -1) {
				progress.progress(1);
			}
			long id = rs.getLong(1);
			if (visitedWays.contains(id)) {
				continue;
			}
			visitedWays.add(id);
			
			int level = rs.getInt(8);
			int zoom = mapZooms.getLevel(level).getMaxZoom();

			long startNode = rs.getLong(2);
			long endNode = rs.getLong(3);

			String name = rs.getString(5);
			parseAndSort(typeUse, rs.getBytes(6));
			parseAndSort(addtypeUse, rs.getBytes(7));
			
			loadNodes(rs.getBytes(4), list);
			ArrayList<Float> wayNodes = new ArrayList<Float>(list);

			// combine startPoint with EndPoint
			boolean combined = true;
			while (combined) {
				combined = false;
				endStat.setLong(1, startNode);
				endStat.setShort(2, (short) level);
				ResultSet fs = endStat.executeQuery();
				// search by exact name
				while (fs.next() && !combined) {
					if (!visitedWays.contains(fs.getLong(1))) {
						parseAndSort(temp, rs.getBytes(6));
						parseAndSort(tempAdd, rs.getBytes(7));
						if(temp.equals(typeUse) && tempAdd.equals(addtypeUse)){
							combined = true;
							long lid = fs.getLong(1);
							startNode = fs.getLong(2);
							visitedWays.add(lid);
							loadNodes(fs.getBytes(4), list);
							if(!Algoritms.objectEquals(rs.getString(5), name)){
								name = null;
							}
							ArrayList<Float> li = new ArrayList<Float>(list);
							// remove first lat/lon point
							wayNodes.remove(0);
							wayNodes.remove(0);
							li.addAll(wayNodes);
							wayNodes = li;
						}
					}
				}
				fs.close();
			}

			// combined end point
			combined = true;
			while (combined) {
				combined = false;
				startStat.setLong(1, endNode);
				startStat.setShort(2, (short) level);
				ResultSet fs = startStat.executeQuery();
				while (fs.next() && !combined) {
					if (!visitedWays.contains(fs.getLong(1))) {
						parseAndSort(temp, rs.getBytes(6));
						parseAndSort(tempAdd, rs.getBytes(7));
						if(temp.equals(typeUse) && tempAdd.equals(addtypeUse)){
							combined = true;
							long lid = fs.getLong(1);
							if (!Algoritms.objectEquals(rs.getString(5), name)) {
								name = null;
							}
							endNode = fs.getLong(3);
							visitedWays.add(lid);
							loadNodes(fs.getBytes(4), list);
							for (int i = 2; i < list.size(); i++) {
								wayNodes.add(list.get(i));
							}
						}
					}
				}
				fs.close();
			}
			List<Node> wNodes = new ArrayList<Node>();
			int wNsize = wayNodes.size();
			for (int i = 0; i < wNsize; i += 2) {
				wNodes.add(new Node(wayNodes.get(i), wayNodes.get(i + 1), i == 0 ? startNode : endNode));
			}
			boolean skip = false;
			boolean cycle = startNode == endNode;
			if (cycle) {
				skip = checkForSmallAreas(wNodes, zoom  + Math.min(zoomWaySmothness / 2, 3), 3, 4);
			} else {
				// coastline
				if(!typeUse.contains(renderingTypes.getCoastlineRuleType().getInternalId())) {
					skip = checkForSmallAreas(wNodes, zoom  + Math.min(zoomWaySmothness / 2, 3), 2, 8);
				}
			}
			if (!skip) {
				List<Node> res = new ArrayList<Node>();
				MapAlgorithms.simplifyDouglasPeucker(wNodes, zoom - 1 + 8 + zoomWaySmothness, 3, res);
				if (res.size() > 0) {
					namesUse.clear();
					if (name != null && name.length() > 0) {
						namesUse.put(renderingTypes.getNameRuleType(), name);
					}
					insertBinaryMapRenderObjectIndex(mapTree[level], res, null, namesUse, id, false, typeUse, addtypeUse, false);
				}
			}
			
			// end cycle 

		}

	}

	private boolean checkForSmallAreas(List<Node> nodes, int zoom, int minz, int maxz) {
		int minX = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxY = Integer.MIN_VALUE;
		int c = 0;
		int nsize = nodes.size();
		for (int i = 0; i < nsize; i++) {
			if (nodes.get(i) != null) {
				c++;
				int x = (int) (MapUtils.getTileNumberX(zoom, nodes.get(i).getLongitude()) * 256d);
				int y = (int) (MapUtils.getTileNumberY(zoom, nodes.get(i).getLatitude()) * 256d);
				minX = Math.min(minX, x);
				maxX = Math.max(maxX, x);
				minY = Math.min(minY, y);
				maxY = Math.max(maxY, y);
			}
		}
		if (c < 2) {
			return true;
		}
		return ((maxX - minX) <= minz && (maxY - minY) <= maxz) || ((maxX - minX) <= maxz && (maxY - minY) <= minz);

	}

	public void iterateMainEntity(Entity e, OsmDbAccessorContext ctx) throws SQLException {
		if (e instanceof Way || e instanceof Node) {
			// manipulate what kind of way to load
			ctx.loadEntityData(e);
			if (e instanceof Way && "coastline".equals(e.getTag(OSMTagKey.NATURAL)) && COASTLINE_PROCESS) {
				coastlineProcessor.processCoastline((Way) e);
				return;
			}
			for (int level = 0; level < mapZooms.size(); level++) {
				boolean area = renderingTypes.encodeEntityWithType(e, mapZooms.getLevel(level).getMaxZoom(), typeUse, addtypeUse, namesUse,
						tempNameUse);
				if (typeUse.isEmpty()) {
					continue;
				}
				boolean hasMulti = e instanceof Way && multiPolygonsWays.containsKey(e.getId());
				if (hasMulti) {
					TIntArrayList set = multiPolygonsWays.get(e.getId());
					typeUse.removeAll(set);
					continue;
				}
				if (typeUse.isEmpty()) {
					continue;
				}
				long id = convertBaseIdToGeneratedId(e.getId(), level);
				List<Node> res = null;
				if (e instanceof Node) {
					res = Collections.singletonList((Node) e);
				} else {
					id |= 1;

					// simplify route
					int zoomToSimplify = mapZooms.getLevel(level).getMaxZoom() - 1;
					if (zoomToSimplify < 15) {
						boolean cycle = ((Way) e).getFirstNodeId() == ((Way) e).getLastNodeId();
						if (cycle) {
							res = simplifyCycleWay(((Way) e).getNodes(), zoomToSimplify);
						} else {
							String ename = namesUse.get(renderingTypes.getNameRuleType());
							insertLowLevelMapBinaryObject(level, zoomToSimplify, typeUse, addtypeUse, id, ((Way) e).getNodes(), ename);
						}
					} else {
						res = ((Way) e).getNodes();
					}
				}
				if (res != null) {
					insertBinaryMapRenderObjectIndex(mapTree[level], res, null, namesUse, id, area, typeUse, addtypeUse, true);
				}
			}
		}
	}

	

	public void writeBinaryMapIndex(BinaryMapIndexWriter writer, String regionName) throws IOException, SQLException {
		closePreparedStatements(mapBinaryStat, mapLowLevelBinaryStat);
		mapConnection.commit();
		if(COASTLINE_PROCESS) {
			coastlineProcessor.writeCoastlinesFile(writer, regionName);
			return;
		}
		try {
			writer.startWriteMapIndex(regionName);
			// write map encoding rules
			writer.writeMapEncodingRules(renderingTypes.getEncodingRuleTypes());
			
			PreparedStatement selectData = mapConnection
					.prepareStatement("SELECT area, coordinates, innerPolygons, types, additionalTypes, name FROM binary_map_objects WHERE id = ?");

			// write map levels and map index
			TLongObjectHashMap<BinaryFileReference> treeHeader = new TLongObjectHashMap<BinaryFileReference>();
			for (int i = 0; i < mapZooms.size(); i++) {
				RTree rtree = mapTree[i];
				long rootIndex = rtree.getFileHdr().getRootIndex();
				rtree.Node root = rtree.getReadNode(rootIndex);
				Rect rootBounds = calcBounds(root);
				if (rootBounds != null) {
					writer.startWriteMapLevelIndex(mapZooms.getLevel(i).getMinZoom(), mapZooms.getLevel(i).getMaxZoom(),
							rootBounds.getMinX(), rootBounds.getMaxX(), rootBounds.getMinY(), rootBounds.getMaxY());
					writeBinaryMapTree(root, rootBounds, rtree, writer, treeHeader);
					
					writeBinaryMapBlock(root,  rootBounds, rtree, writer, selectData, treeHeader, new LinkedHashMap<String, Integer>(),
								new LinkedHashMap<MapRenderingTypes.MapRulType, String>());

					writer.endWriteMapLevelIndex();
				}
			}


			selectData.close();

			writer.endWriteMapIndex();
			writer.flush();
		} catch (RTreeException e) {
			throw new IllegalStateException(e);
		}
	}

	

	private long convertBaseIdToGeneratedId(long baseId, int level) {
		if (level >= MAP_LEVELS_MAX) {
			throw new IllegalArgumentException("Number of zoom levels " + level + " exceeds allowed maximum : " + MAP_LEVELS_MAX);
		}
		return ((baseId << MAP_LEVELS_POWER) | level) << 1;
	}

	public long convertGeneratedIdToObfWrite(long id) {
		return (id >> (MAP_LEVELS_POWER)) + (id & 1);
	}

	private static final char SPECIAL_CHAR = ((char) 0x60000);

	private String encodeNames(Map<MapRulType, String> tempNames) {
		StringBuilder b = new StringBuilder();
		for (Map.Entry<MapRulType, String> e : tempNames.entrySet()) {
			if (e.getValue() != null) {
				b.append(SPECIAL_CHAR).append((char)e.getKey().getInternalId()).append(e.getValue());
			}
		}
		return b.toString();
	}

	private void decodeNames(String name, Map<MapRulType, String> tempNames) {
		int i = name.indexOf(SPECIAL_CHAR);
		while (i != -1) {
			int n = name.indexOf(SPECIAL_CHAR, i + 2);
			int ch = (short) name.charAt(i + 1);
			MapRulType rt = renderingTypes.getTypeByInternalId(ch);
			if (n == -1) {
				tempNames.put(rt, name.substring(i + 2));
			} else {
				tempNames.put(rt, name.substring(i + 2, n));
			}
			i = n;
		}
	}

	public void writeBinaryMapBlock(rtree.Node parent, Rect parentBounds, RTree r, BinaryMapIndexWriter writer, PreparedStatement selectData,
			TLongObjectHashMap<BinaryFileReference> bounds, Map<String, Integer> tempStringTable, Map<MapRulType, String> tempNames)
			throws IOException, RTreeException, SQLException {
		Element[] e = parent.getAllElements();

		MapDataBlock.Builder dataBlock = null;
		BinaryFileReference ref = bounds.get(parent.getNodeIndex());
		long baseId = 0;
		for (int i = 0; i < parent.getTotalElements(); i++) {
			if (e[i].getElementType() == rtree.Node.LEAF_NODE) {
				long id = ((LeafElement) e[i]).getPtr();
				selectData.setLong(1, id);
				// selectData = mapConnection.prepareStatement("SELECT area, coordinates, innerPolygons, types, additionalTypes, name FROM binary_map_objects WHERE id = ?");
				ResultSet rs = selectData.executeQuery();
				if (rs.next()) {
					long cid = convertGeneratedIdToObfWrite(id);
					if (dataBlock == null) {
						baseId = cid;
						dataBlock = writer.createWriteMapDataBlock(baseId);
						tempStringTable.clear();

					}
					tempNames.clear();
					decodeNames(rs.getString(6), tempNames);
					byte[] types = rs.getBytes(4);
					typeUse.clear();
					for (int j = 0; j < types.length; j += 2) {
						int ids = Algoritms.parseSmallIntFromBytes(types, j);
						typeUse.add(renderingTypes.getTypeByInternalId(ids).getTargetId());
					}
					byte[] addTypes = rs.getBytes(5);
					addtypeUse.clear();
					if (addTypes != null) {
						for (int j = 0; j < addTypes.length; j += 2) {
							int ids = Algoritms.parseSmallIntFromBytes(addTypes, j);
							addtypeUse.add(renderingTypes.getTypeByInternalId(ids).getTargetId());
						}
					}
					
					
					MapData mapData = writer.writeMapData(cid - baseId, parentBounds.getMinX(), parentBounds.getMinY(), rs.getBoolean(1), rs.getBytes(2), rs.getBytes(3),
							typeUse, addtypeUse, tempNames, tempStringTable, dataBlock);
					if(mapData != null) {
						dataBlock.addDataObjects(mapData);
					}
				} else {
					logMapDataWarn.error("Something goes wrong with id = " + id); //$NON-NLS-1$
				}
			}
		}
		if (dataBlock != null) {
			writer.writeMapDataBlock(dataBlock, tempStringTable, ref);
		}
		for (int i = 0; i < parent.getTotalElements(); i++) {
			if (e[i].getElementType() != rtree.Node.LEAF_NODE) {
				long ptr = ((NonLeafElement) e[i]).getPtr();
				rtree.Node ns = r.getReadNode(ptr);
				writeBinaryMapBlock(ns, e[i].getRect(), r, writer, selectData, bounds, tempStringTable, tempNames);
			}
		}
	}

	public void writeBinaryMapTree(rtree.Node parent, Rect re, RTree r, BinaryMapIndexWriter writer, TLongObjectHashMap<BinaryFileReference> bounds)
			throws IOException, RTreeException {
		Element[] e = parent.getAllElements();
		boolean containsLeaf = false;
		for (int i = 0; i < parent.getTotalElements(); i++) {
			if (e[i].getElementType() == rtree.Node.LEAF_NODE) {
				containsLeaf = true;
			}
		}
		BinaryFileReference ref = writer.startMapTreeElement(re.getMinX(), re.getMaxX(), re.getMinY(), re.getMaxY(), containsLeaf);
		if (ref != null) {
			bounds.put(parent.getNodeIndex(), ref);
		}
		for (int i = 0; i < parent.getTotalElements(); i++) {
			if (e[i].getElementType() != rtree.Node.LEAF_NODE) {
				rtree.Node chNode = r.getReadNode(((NonLeafElement) e[i]).getPtr());
				writeBinaryMapTree(chNode, e[i].getRect(), r, writer, bounds);
			}
		}
		writer.endWriteMapTreeElement();
	}

	public Rect calcBounds(rtree.Node n) {
		Rect r = null;
		Element[] e = n.getAllElements();
		for (int i = 0; i < n.getTotalElements(); i++) {
			Rect re = e[i].getRect();
			if (r == null) {
				try {
					r = new Rect(re.getMinX(), re.getMinY(), re.getMaxX(), re.getMaxY());
				} catch (IllegalValueException ex) {
				}
			} else {
				r.expandToInclude(re);
			}
		}
		return r;
	}

	public void createDatabaseStructure(Connection mapConnection, DBDialect dialect, String rtreeMapIndexNonPackFileName)
			throws SQLException, IOException {
		createMapIndexStructure(mapConnection);
		this.mapConnection = mapConnection;
		mapBinaryStat = createStatementMapBinaryInsert(mapConnection);
		mapLowLevelBinaryStat = createStatementLowLevelMapBinaryInsert(mapConnection);
		try {
			mapTree = new RTree[mapZooms.size()];
			for (int i = 0; i < mapZooms.size(); i++) {
				File file = new File(rtreeMapIndexNonPackFileName + i);
				if (file.exists()) {
					file.delete();
				}
				mapTree[i] = new RTree(rtreeMapIndexNonPackFileName + i);
				// very slow
				// mapTree[i].getFileHdr().setBufferPolicy(true);
			}
		} catch (RTreeException e) {
			throw new IOException(e);
		}
		pStatements.put(mapBinaryStat, 0);
		pStatements.put(mapLowLevelBinaryStat, 0);
	}

	private void createMapIndexStructure(Connection conn) throws SQLException {
		Statement stat = conn.createStatement();
		stat.executeUpdate("create table binary_map_objects (id bigint primary key, name varchar(4096), "
				+ "area smallint, types binary, additionalTypes binary, coordinates binary, innerPolygons binary)");
		stat.executeUpdate("create index binary_map_objects_ind on binary_map_objects (id)");

		stat.executeUpdate("create table low_level_map_objects (id bigint primary key, start_node bigint, "
				+ "end_node bigint, name varchar(1024), nodes binary, type binary, addType binary, level smallint)");
		stat.executeUpdate("create index low_level_map_objects_ind on low_level_map_objects (id)");
		stat.executeUpdate("create index low_level_map_objects_ind_st on low_level_map_objects (start_node, type)");
		stat.executeUpdate("create index low_level_map_objects_ind_end on low_level_map_objects (end_node, type)");
		stat.close();
	}

	private PreparedStatement createStatementMapBinaryInsert(Connection conn) throws SQLException {
		return conn
				.prepareStatement("insert into binary_map_objects(id, area, coordinates, innerPolygons, types, additionalTypes, name) values(?, ?, ?, ?, ?, ?, ?)");
	}

	private PreparedStatement createStatementLowLevelMapBinaryInsert(Connection conn) throws SQLException {
		return conn
				.prepareStatement("insert into low_level_map_objects(id, start_node, end_node, name, nodes, type, addType, level) values(?, ?, ?, ?, ?, ?, ?, ?)");
	}

	private void insertLowLevelMapBinaryObject(int level, int zoom, TIntArrayList types, TIntArrayList addTypes, long id, List<Node> in, String name)
			throws SQLException {
		lowLevelWays++;
		List<Node> nodes = new ArrayList<Node>();
		MapAlgorithms.simplifyDouglasPeucker(in, zoom + 8 + zoomWaySmothness, 3, nodes);
		boolean first = true;
		long firstId = -1;
		long lastId = -1;
		ByteArrayOutputStream bNodes = new ByteArrayOutputStream();
		ByteArrayOutputStream bTypes = new ByteArrayOutputStream();
		ByteArrayOutputStream bAddtTypes = new ByteArrayOutputStream();
		try {
			for (Node n : nodes) {
				if (n != null) {
					if (first) {
						firstId = n.getId();
						first = false;
					}
					lastId = n.getId();
					Algoritms.writeInt(bNodes, Float.floatToRawIntBits((float) n.getLatitude()));
					Algoritms.writeInt(bNodes, Float.floatToRawIntBits((float) n.getLongitude()));
				}
			}
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		if (firstId == -1) {
			return;
		}
		for (int j = 0; j < types.size(); j++) {
			try {
				Algoritms.writeSmallInt(bTypes, types.get(j));
			} catch (IOException e) {
			}
		}
		for (int j = 0; j < addTypes.size(); j++) {
			try {
				Algoritms.writeSmallInt(bAddtTypes, addTypes.get(j));
			} catch (IOException e) {
			}
		}
		mapLowLevelBinaryStat.setLong(1, id);
		mapLowLevelBinaryStat.setLong(2, firstId);
		mapLowLevelBinaryStat.setLong(3, lastId);
		mapLowLevelBinaryStat.setString(4, name);
		mapLowLevelBinaryStat.setBytes(5, bNodes.toByteArray());
		mapLowLevelBinaryStat.setBytes(6, bTypes.toByteArray());
		mapLowLevelBinaryStat.setBytes(7, bAddtTypes.toByteArray());
		mapLowLevelBinaryStat.setShort(8, (short) level);

		addBatch(mapLowLevelBinaryStat);
	}

	private void insertBinaryMapRenderObjectIndex(RTree mapTree, Collection<Node> nodes, List<List<Node>> innerWays,
			Map<MapRulType, String> names, long id, boolean area, TIntArrayList types, TIntArrayList addTypes, boolean commit)
			throws SQLException {
		boolean init = false;
		int minX = Integer.MAX_VALUE;
		int maxX = 0;
		int minY = Integer.MAX_VALUE;
		int maxY = 0;

		ByteArrayOutputStream bcoordinates = new ByteArrayOutputStream();
		ByteArrayOutputStream binnercoord = new ByteArrayOutputStream();
		ByteArrayOutputStream btypes = new ByteArrayOutputStream();
		ByteArrayOutputStream badditionalTypes = new ByteArrayOutputStream();

		try {
			for (int j = 0; j < types.size(); j++) {
				Algoritms.writeSmallInt(btypes, types.get(j));
			}
			for (int j = 0; j < addTypes.size(); j++) {
				Algoritms.writeSmallInt(badditionalTypes, addTypes.get(j));
			}

			for (Node n : nodes) {
				if (n != null) {
					int y = MapUtils.get31TileNumberY(n.getLatitude());
					int x = MapUtils.get31TileNumberX(n.getLongitude());
					minX = Math.min(minX, x);
					maxX = Math.max(maxX, x);
					minY = Math.min(minY, y);
					maxY = Math.max(maxY, y);
					init = true;
					Algoritms.writeInt(bcoordinates, x);
					Algoritms.writeInt(bcoordinates, y);
				}
			}

			if (innerWays != null) {
				for (List<Node> ws : innerWays) {
					boolean exist = false;
					if (ws != null) {
						for (Node n : ws) {
							if (n != null) {
								exist = true;
								int y = MapUtils.get31TileNumberY(n.getLatitude());
								int x = MapUtils.get31TileNumberX(n.getLongitude());
								Algoritms.writeInt(binnercoord, x);
								Algoritms.writeInt(binnercoord, y);
							}
						}
					}
					if (exist) {
						Algoritms.writeInt(binnercoord, 0);
						Algoritms.writeInt(binnercoord, 0);
					}
				}
			}
		} catch (IOException es) {
			throw new IllegalStateException(es);
		}
		if (init) {
			// conn.prepareStatement("insert into binary_map_objects(id, area, coordinates, innerPolygons, types, additionalTypes, name) values(?, ?, ?, ?, ?, ?, ?)");
			mapBinaryStat.setLong(1, id);
			mapBinaryStat.setBoolean(2, area);
			mapBinaryStat.setBytes(3, bcoordinates.toByteArray());
			mapBinaryStat.setBytes(4, binnercoord.toByteArray());
			mapBinaryStat.setBytes(5, btypes.toByteArray());
			mapBinaryStat.setBytes(6, badditionalTypes.toByteArray());
			mapBinaryStat.setString(7, encodeNames(names));

			addBatch(mapBinaryStat, commit);
			try {
				mapTree.insert(new LeafElement(new Rect(minX, minY, maxX, maxY), id));
			} catch (RTreeInsertException e1) {
				throw new IllegalArgumentException(e1);
			} catch (IllegalValueException e1) {
				throw new IllegalArgumentException(e1);
			}
		}
	}

	public void createRTreeFiles(String rTreeMapIndexPackFileName) throws RTreeException {
		mapTree = new RTree[mapZooms.size()];
		for (int i = 0; i < mapZooms.size(); i++) {
			mapTree[i] = new RTree(rTreeMapIndexPackFileName + i);
		}

	}

	public void packRtreeFiles(String rTreeMapIndexNonPackFileName, String rTreeMapIndexPackFileName) throws IOException {
		for (int i = 0; i < mapZooms.size(); i++) {
			mapTree[i] = packRtreeFile(mapTree[i], rTreeMapIndexNonPackFileName + i, rTreeMapIndexPackFileName + i);
		}
	}

	public void commitAndCloseFiles(String rTreeMapIndexNonPackFileName, String rTreeMapIndexPackFileName, boolean deleteDatabaseIndexes)
			throws IOException, SQLException {

		// delete map rtree files
		if (mapTree != null) {
			for (int i = 0; i < mapTree.length; i++) {
				if (mapTree[i] != null) {
					RandomAccessFile file = mapTree[i].getFileHdr().getFile();
					file.close();
				}

			}
			for (int i = 0; i < mapTree.length; i++) {
				File f = new File(rTreeMapIndexNonPackFileName + i);
				if (f.exists() && deleteDatabaseIndexes) {
					f.delete();
				}
				f = new File(rTreeMapIndexPackFileName + i);
				if (f.exists() && deleteDatabaseIndexes) {
					f.delete();
				}
			}
		}
		closeAllPreparedStatements();

	}

	public void setZoomWaySmothness(int zoomWaySmothness) {
		this.zoomWaySmothness = zoomWaySmothness;
	}

	public int getZoomWaySmothness() {
		return zoomWaySmothness;
	}

	// TODO restrictions should be moved to different creator (Routing Map creator)
	private void indexHighwayRestrictions(Entity e, OsmDbAccessorContext ctx) throws SQLException {
		if (e instanceof Relation && "restriction".equals(e.getTag(OSMTagKey.TYPE))) { //$NON-NLS-1$
			String val = e.getTag("restriction"); //$NON-NLS-1$
			if (val != null) {
				byte type = -1;
				if ("no_right_turn".equalsIgnoreCase(val)) { //$NON-NLS-1$
					type = MapRenderingTypes.RESTRICTION_NO_RIGHT_TURN;
				} else if ("no_left_turn".equalsIgnoreCase(val)) { //$NON-NLS-1$
					type = MapRenderingTypes.RESTRICTION_NO_LEFT_TURN;
				} else if ("no_u_turn".equalsIgnoreCase(val)) { //$NON-NLS-1$
					type = MapRenderingTypes.RESTRICTION_NO_U_TURN;
				} else if ("no_straight_on".equalsIgnoreCase(val)) { //$NON-NLS-1$
					type = MapRenderingTypes.RESTRICTION_NO_STRAIGHT_ON;
				} else if ("only_right_turn".equalsIgnoreCase(val)) { //$NON-NLS-1$
					type = MapRenderingTypes.RESTRICTION_ONLY_RIGHT_TURN;
				} else if ("only_left_turn".equalsIgnoreCase(val)) { //$NON-NLS-1$
					type = MapRenderingTypes.RESTRICTION_ONLY_LEFT_TURN;
				} else if ("only_straight_on".equalsIgnoreCase(val)) { //$NON-NLS-1$
					type = MapRenderingTypes.RESTRICTION_ONLY_STRAIGHT_ON;
				}
				if (type != -1) {
					ctx.loadEntityData(e);
					Collection<EntityId> fromL = ((Relation) e).getMemberIds("from"); //$NON-NLS-1$
					Collection<EntityId> toL = ((Relation) e).getMemberIds("to"); //$NON-NLS-1$
					if (!fromL.isEmpty() && !toL.isEmpty()) {
						EntityId from = fromL.iterator().next();
						EntityId to = toL.iterator().next();
						if (from.getType() == EntityType.WAY) {
							if (!highwayRestrictions.containsKey(from.getId())) {
								highwayRestrictions.put(from.getId(), new ArrayList<Long>(4));
							}
							highwayRestrictions.get(from.getId()).add((to.getId() << 3) | (long) type);
						}
					}
				}
			}
		}
	}
}
