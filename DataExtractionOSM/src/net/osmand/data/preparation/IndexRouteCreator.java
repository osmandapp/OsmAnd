package net.osmand.data.preparation;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import net.osmand.Algoritms;
import net.osmand.binary.OsmandOdb.IdTable;
import net.osmand.binary.OsmandOdb.OsmAndRoutingIndex.RouteDataBlock;
import net.osmand.binary.OsmandOdb.RestrictionData;
import net.osmand.binary.OsmandOdb.RestrictionData.Builder;
import net.osmand.binary.OsmandOdb.RouteData;
import net.osmand.data.preparation.BinaryMapIndexWriter.RoutePointToWrite;
import net.osmand.osm.Entity;
import net.osmand.osm.Entity.EntityId;
import net.osmand.osm.Entity.EntityType;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.MapRoutingTypes;
import net.osmand.osm.MapRoutingTypes.MapRouteType;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Node;
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

public class IndexRouteCreator extends AbstractIndexPartCreator {
	
	private Connection mapConnection;
	private final Log logMapDataWarn;
	private final static boolean WRITE_POINT_ID = false;
	private final static boolean WRITE_TEXT_TAGS = true;
	private RTree routeTree = null;
	private MapRoutingTypes routeTypes;
	
	
	private TLongObjectHashMap<TLongArrayList> highwayRestrictions = new TLongObjectHashMap<TLongArrayList>();

	// local purpose to speed up processing cache allocation
	TIntArrayList outTypes = new TIntArrayList();
	TLongObjectHashMap<TIntArrayList> pointTypes = new TLongObjectHashMap<TIntArrayList>();
	Map<MapRoutingTypes.MapRouteType, String> names = new HashMap<MapRoutingTypes.MapRouteType, String>(); 

	private PreparedStatement mapRouteInsertStat;


	public IndexRouteCreator(Log logMapDataWarn) {
		this.logMapDataWarn = logMapDataWarn;
		this.routeTypes = new MapRoutingTypes();
	}

	public void indexRelations(Entity e, OsmDbAccessorContext ctx) throws SQLException {
		indexHighwayRestrictions(e, ctx);
	}


	public void iterateMainEntity(Entity es, OsmDbAccessorContext ctx) throws SQLException {
		if (es instanceof Way) {
			Way e = (Way) es;
			boolean encoded = routeTypes.encodeEntity(e, outTypes, names);
			if (encoded) {
				// Load point with  tags!
				ctx.loadEntityWay(e);
				routeTypes.encodePointTypes(e, pointTypes);
				boolean init = false;
				int minX = Integer.MAX_VALUE;
				int maxX = 0;
				int minY = Integer.MAX_VALUE;
				int maxY = 0;

				ByteArrayOutputStream bcoordinates = new ByteArrayOutputStream();
				ByteArrayOutputStream bpointIds = new ByteArrayOutputStream();
				ByteArrayOutputStream bpointTypes = new ByteArrayOutputStream();
				ByteArrayOutputStream btypes = new ByteArrayOutputStream();

				try {
					for (int j = 0; j < outTypes.size(); j++) {
						Algoritms.writeSmallInt(btypes, outTypes.get(j));
					}

					for (Node n : e.getNodes()) {
						if (n != null) {
							// write id
							Algoritms.writeLongInt(bpointIds, n.getId());
							// write point type
							TIntArrayList types = pointTypes.get(n.getId());
							if (types != null) {
								for (int j = 0; j < types.size(); j++) {
									Algoritms.writeSmallInt(bpointTypes, types.get(j));
								}
							}
							Algoritms.writeSmallInt(bpointTypes, 0);
							// write coordinates
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

				} catch (IOException est) {
					throw new IllegalStateException(est);
				}
				if (init) {
					// conn.prepareStatement("insert into route_objects(id, types, pointTypes, pointIds, pointCoordinates, name) values(?, ?, ?, ?, ?, ?, ?)");
					mapRouteInsertStat.setLong(1, e.getId());
					mapRouteInsertStat.setBytes(2, btypes.toByteArray());
					mapRouteInsertStat.setBytes(3, bpointTypes.toByteArray());
					mapRouteInsertStat.setBytes(4, bpointIds.toByteArray());
					mapRouteInsertStat.setBytes(5, bcoordinates.toByteArray());
					if(WRITE_TEXT_TAGS) {
						mapRouteInsertStat.setString(6, encodeNames(names));
					} else {
						mapRouteInsertStat.setString(6, "");
					}

					addBatch(mapRouteInsertStat, false);
					try {
						routeTree.insert(new LeafElement(new Rect(minX, minY, maxX, maxY), e.getId()));
					} catch (RTreeInsertException e1) {
						throw new IllegalArgumentException(e1);
					} catch (IllegalValueException e1) {
						throw new IllegalArgumentException(e1);
					}
				}

			}
		}
		
	}

	private static final char SPECIAL_CHAR = ((char) 0x60000);

	protected String encodeNames(Map<MapRouteType, String> tempNames) {
		StringBuilder b = new StringBuilder();
		for (Map.Entry<MapRouteType, String> e : tempNames.entrySet()) {
			if (e.getValue() != null) {
				b.append(SPECIAL_CHAR).append((char)e.getKey().getInternalId()).append(e.getValue());
			}
		}
		return b.toString();
	}

	protected void decodeNames(String name, Map<MapRouteType, String> tempNames) {
		int i = name.indexOf(SPECIAL_CHAR);
		while (i != -1) {
			int n = name.indexOf(SPECIAL_CHAR, i + 2);
			int ch = (short) name.charAt(i + 1);
			MapRouteType rt = routeTypes.getTypeByInternalId(ch);
			if (n == -1) {
				tempNames.put(rt, name.substring(i + 2));
			} else {
				tempNames.put(rt, name.substring(i + 2, n));
			}
			i = n;
		}
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
		this.mapConnection = mapConnection;
		Statement stat = mapConnection.createStatement();
		stat.executeUpdate(CREATETABLE);
		stat.executeUpdate(CREATE_IND);
		stat.close();
		mapRouteInsertStat = createStatementRouteObjInsert(mapConnection);
		try {
			routeTree = new RTree(rtreeMapIndexNonPackFileName);
		} catch (RTreeException e) {
			throw new IOException(e);
		}
		pStatements.put(mapRouteInsertStat, 0);
	}
	
	private static final String CREATETABLE = "create table route_objects (id bigint primary key, "
			+ "types binary, pointTypes binary, pointIds binary, pointCoordinates binary, name varchar(4096))";
	private static final String CREATE_IND = "create index route_objects_ind on route_objects (id)";
	private static final String SELECT_STAT = "SELECT types, pointTypes, pointIds, pointCoordinates, name FROM route_objects WHERE id = ?";
	private static final String INSERT_STAT = "insert into route_objects(id, types, pointTypes, pointIds, pointCoordinates, name) values(?, ?, ?, ?, ?, ?)";


	private PreparedStatement createStatementRouteObjInsert(Connection conn) throws SQLException {
		return conn.prepareStatement(INSERT_STAT);
	}

	public void commitAndCloseFiles(String rTreeMapIndexNonPackFileName, String rTreeMapIndexPackFileName, boolean deleteDatabaseIndexes)
			throws IOException, SQLException {

		// delete map rtree files
		if (routeTree != null) {
			RandomAccessFile file = routeTree.getFileHdr().getFile();
			file.close();
			if (rTreeMapIndexNonPackFileName != null) {
				File f = new File(rTreeMapIndexNonPackFileName);
				if (f.exists() && deleteDatabaseIndexes) {
					f.delete();
				}
			}
			if (rTreeMapIndexPackFileName != null) {
				File f = new File(rTreeMapIndexPackFileName);
				if (f.exists() && deleteDatabaseIndexes) {
					f.delete();
				}
			}
		}
		closeAllPreparedStatements();
	}


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
					ctx.loadEntityRelation((Relation) e);
					Collection<EntityId> fromL = ((Relation) e).getMemberIds("from"); //$NON-NLS-1$
					Collection<EntityId> toL = ((Relation) e).getMemberIds("to"); //$NON-NLS-1$
					if (!fromL.isEmpty() && !toL.isEmpty()) {
						EntityId from = fromL.iterator().next();
						EntityId to = toL.iterator().next();
						if (from.getType() == EntityType.WAY) {
							if (!highwayRestrictions.containsKey(from.getId())) {
								highwayRestrictions.put(from.getId(), new TLongArrayList());
							}
							highwayRestrictions.get(from.getId()).add((to.getId() << 3) | (long) type);
						}
					}
				}
			}
		}
	}

	public void createRTreeFiles(String rTreeRouteIndexPackFileName) throws RTreeException {
		routeTree = new RTree(rTreeRouteIndexPackFileName);
	}

	public void packRtreeFiles(String rTreeRouteIndexNonPackFileName, String rTreeRouteIndexPackFileName) throws IOException {
		routeTree = packRtreeFile(routeTree, rTreeRouteIndexNonPackFileName, rTreeRouteIndexPackFileName);
		
	}
	
	public void writeBinaryRouteIndex(BinaryMapIndexWriter writer, String regionName) throws IOException, SQLException {
		closePreparedStatements(mapRouteInsertStat);
		mapConnection.commit();
		try {
			writer.startWriteRouteIndex(regionName);
			// write map encoding rules

			writer.writeRouteEncodingRules(routeTypes.getEncodingRuleTypes());

			PreparedStatement selectData = mapConnection.prepareStatement(SELECT_STAT);
			// write map levels and map index
			TLongObjectHashMap<BinaryFileReference> treeHeader = new TLongObjectHashMap<BinaryFileReference>();
			long rootIndex = routeTree.getFileHdr().getRootIndex();
			rtree.Node root = routeTree.getReadNode(rootIndex);
			Rect rootBounds = calcBounds(root);
			if (rootBounds != null) {
				writeBinaryRouteTree(root, rootBounds, routeTree, writer, treeHeader);
				writeBinaryMapBlock(root, rootBounds, routeTree, writer, selectData, treeHeader, new LinkedHashMap<String, Integer>(),
						new LinkedHashMap<MapRouteType, String>());
			}

			selectData.close();

			writer.endWriteRouteIndex();
			writer.flush();
		} catch (RTreeException e) {
			throw new IllegalStateException(e);
		}
	}
	
	
	private int registerId(TLongArrayList ids, long id) {
		for (int i = 0; i < ids.size(); i++) {
			if (ids.getQuick(i) == id) {
				return i;
			}
		}
		ids.add(id);
		return ids.size() - 1;
	}
	
	public void writeBinaryMapBlock(rtree.Node parent, Rect parentBounds, RTree r, BinaryMapIndexWriter writer, PreparedStatement selectData,
			TLongObjectHashMap<BinaryFileReference> bounds, Map<String, Integer> tempStringTable, Map<MapRouteType, String> tempNames)
					throws IOException, RTreeException, SQLException {
		Element[] e = parent.getAllElements();

		RouteDataBlock.Builder dataBlock = null;
		BinaryFileReference ref = bounds.get(parent.getNodeIndex());
		TLongArrayList wayMapIds = new TLongArrayList();
		TLongArrayList pointMapIds = new TLongArrayList();
		for (int i = 0; i < parent.getTotalElements(); i++) {
			if (e[i].getElementType() == rtree.Node.LEAF_NODE) {
				long id = ((LeafElement) e[i]).getPtr();
				// IndexRouteCreator.SELECT_STAT;
				// "SELECT types, pointTypes, pointIds, pointCoordinates, name FROM route_objects WHERE id = ?"
				selectData.setLong(1, id);

				ResultSet rs = selectData.executeQuery();
				if (rs.next()) {
					if (dataBlock == null) {
						dataBlock = RouteDataBlock.newBuilder();
						tempStringTable.clear();
						wayMapIds.clear();
						pointMapIds.clear();
					}
					int cid = registerId(wayMapIds, id);
					tempNames.clear();
					decodeNames(rs.getString(5), tempNames);
					byte[] types = rs.getBytes(1);
					int[] typeUse = new int[types.length / 2];
					for (int j = 0; j < types.length; j += 2) {
						int ids = Algoritms.parseSmallIntFromBytes(types, j);
						typeUse[j / 2] = routeTypes.getTypeByInternalId(ids).getTargetId();
					}
					byte[] pointTypes = rs.getBytes(2);
					byte[] pointIds = rs.getBytes(3);
					byte[] pointCoordinates = rs.getBytes(4);
					int typeInd = 0;
					RoutePointToWrite[] points = new RoutePointToWrite[pointCoordinates.length / 8];
					TLongArrayList restrictions = highwayRestrictions.get(id);
					if(restrictions != null){
						for(int li = 0; li<restrictions.size(); li++){
							Builder restriction = RestrictionData.newBuilder();
							restriction.setFrom(cid);
							int toId = registerId(wayMapIds, restrictions.get(li) >> 3);
							restriction.setTo(toId);
							restriction.setType((int) (restrictions.get(li) & 0x7));
							dataBlock.addRestrictions(restriction.build());
						}
					}
					for (int j = 0; j < points.length; j++) {
						points[j] = new RoutePointToWrite();
						points[j].x = Algoritms.parseIntFromBytes(pointCoordinates, j * 8);
						points[j].y = Algoritms.parseIntFromBytes(pointCoordinates, j * 8 + 4);
						if(WRITE_POINT_ID) {
							points[j].id = registerId(pointMapIds, Algoritms.parseLongFromBytes(pointIds, j * 8));
						}
						int type = 0;
						do {
							type = Algoritms.parseSmallIntFromBytes(pointTypes, typeInd);
							typeInd += 2;
							if (type != 0) {
								points[j].types.add(routeTypes.getTypeByInternalId(type).getTargetId());
							}
						} while (type != 0);
					}

					RouteData routeData = writer.writeRouteData(cid, parentBounds.getMinX(), parentBounds.getMinY(), typeUse, points,
							tempNames, tempStringTable, dataBlock, true, WRITE_POINT_ID);
					if (routeData != null) {
						dataBlock.addDataObjects(routeData);
					}
				} else {
					logMapDataWarn.error("Something goes wrong with id = " + id); //$NON-NLS-1$
				}
			}
		}
		if (dataBlock != null) {
			IdTable.Builder idTable = IdTable.newBuilder();
			long prev = 0;
			for (int i = 0; i < wayMapIds.size(); i++) {
				idTable.addRouteId(wayMapIds.getQuick(i) - prev);
				prev = wayMapIds.getQuick(i);
			}
			if (WRITE_POINT_ID) {
				prev = 0;
				for (int i = 0; i < pointMapIds.size(); i++) {
					prev = pointMapIds.getQuick(i);
				}
			}
			dataBlock.setIdTable(idTable.build());
			writer.writeRouteDataBlock(dataBlock, tempStringTable, ref);
		}
		for (int i = 0; i < parent.getTotalElements(); i++) {
			if (e[i].getElementType() != rtree.Node.LEAF_NODE) {
				long ptr = ((NonLeafElement) e[i]).getPtr();
				rtree.Node ns = r.getReadNode(ptr);
				writeBinaryMapBlock(ns, e[i].getRect(), r, writer, selectData, bounds, tempStringTable, tempNames);
			}
		}
	}

	public void writeBinaryRouteTree(rtree.Node parent, Rect re, RTree r, BinaryMapIndexWriter writer, TLongObjectHashMap<BinaryFileReference> bounds)
			throws IOException, RTreeException {
		Element[] e = parent.getAllElements();
		boolean containsLeaf = false;
		for (int i = 0; i < parent.getTotalElements(); i++) {
			if (e[i].getElementType() == rtree.Node.LEAF_NODE) {
				containsLeaf = true;
			}
		}
		BinaryFileReference ref = writer.startRouteTreeElement(re.getMinX(), re.getMaxX(), re.getMinY(), re.getMaxY(), containsLeaf);
		if (ref != null) {
			bounds.put(parent.getNodeIndex(), ref);
		}
		for (int i = 0; i < parent.getTotalElements(); i++) {
			if (e[i].getElementType() != rtree.Node.LEAF_NODE) {
				rtree.Node chNode = r.getReadNode(((NonLeafElement) e[i]).getPtr());
				writeBinaryRouteTree(chNode, e[i].getRect(), r, writer, bounds);
			}
		}
		writer.endRouteTreeElement();
	} 
}
