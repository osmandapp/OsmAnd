package net.osmand.data.preparation;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;
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
import net.osmand.osm.Entity;
import net.osmand.osm.Entity.EntityId;
import net.osmand.osm.Entity.EntityType;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.MapRenderingTypes.MapRulType;
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

	private RTree mapTree = null;
	private MapRoutingTypes routeTypes;
	private Map<Long, List<Long>> highwayRestrictions = new LinkedHashMap<Long, List<Long>>();

	// local purpose to speed up processing cache allocation
	TIntArrayList outTypes = new TIntArrayList();
	TLongObjectHashMap<TIntArrayList> pointTypes = new TLongObjectHashMap<TIntArrayList>();

	private PreparedStatement mapRouteStat;


	public IndexRouteCreator(Log logMapDataWarn) {
		this.logMapDataWarn = logMapDataWarn;
		this.routeTypes = new MapRoutingTypes();
	}

	public void indexRelations(Entity e, OsmDbAccessorContext ctx) throws SQLException {
		indexHighwayRestrictions(e, ctx);
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


	

	public void iterateMainEntity(Entity e, OsmDbAccessorContext ctx) throws SQLException {
		if (e instanceof Way) {
			// manipulate what kind of way to load
			ctx.loadEntityData(e);
			boolean encoded = routeTypes.encodeEntity((Way) e, outTypes, pointTypes);
			if (encoded) {
				
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
		createRouteIndexStructure(mapConnection);
		this.mapConnection = mapConnection;
		mapRouteStat = createStatementRouteObjInsert(mapConnection);
		try {
			mapTree = new RTree(rtreeMapIndexNonPackFileName);
		} catch (RTreeException e) {
			throw new IOException(e);
		}
		pStatements.put(mapRouteStat, 0);
	}

	private void createRouteIndexStructure(Connection conn) throws SQLException {
		Statement stat = conn.createStatement();
		stat.executeUpdate("create table route_objects (id bigint primary key, "
				+ "types binary, pointTypes binary, pointIds binary, pointCoordinates binary)");
		stat.executeUpdate("create index route_objects_ind on binary_map_objects (id)");
		stat.close();
	}

	private PreparedStatement createStatementRouteObjInsert(Connection conn) throws SQLException {
		return conn
				.prepareStatement("insert into route_objects(id, area, coordinates, innerPolygons, types, additionalTypes, name) values(?, ?, ?, ?, ?, ?, ?)");
	}

	private void insertBinaryMapRenderObjectIndex(RTree mapTree, Collection<Node> nodes, List<List<Node>> innerWays,
			Map<MapRouteType, String> names, long id, boolean area, TIntArrayList types, TIntArrayList addTypes, boolean commit)
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
			mapRouteStat.setLong(1, id);
			mapRouteStat.setBoolean(2, area);
			mapRouteStat.setBytes(3, bcoordinates.toByteArray());
			mapRouteStat.setBytes(4, binnercoord.toByteArray());
			mapRouteStat.setBytes(5, btypes.toByteArray());
			mapRouteStat.setBytes(6, badditionalTypes.toByteArray());
			mapRouteStat.setString(7, encodeNames(names));

			addBatch(mapRouteStat, commit);
			try {
				mapTree.insert(new LeafElement(new Rect(minX, minY, maxX, maxY), id));
			} catch (RTreeInsertException e1) {
				throw new IllegalArgumentException(e1);
			} catch (IllegalValueException e1) {
				throw new IllegalArgumentException(e1);
			}
		}
	}


	public void commitAndCloseFiles(String rTreeMapIndexNonPackFileName, String rTreeMapIndexPackFileName, boolean deleteDatabaseIndexes)
			throws IOException, SQLException {

		// delete map rtree files
		if (mapTree != null) {
			RandomAccessFile file = mapTree.getFileHdr().getFile();
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
	
	/* FIXME
	public void writeBinaryMapIndex(BinaryMapIndexWriter writer, String regionName) throws IOException, SQLException {
		closePreparedStatements(mapBinaryStat, mapLowLevelBinaryStat);
		mapConnection.commit();
		try {
			writer.startWriteMapIndex(regionName);
			// write map encoding rules
			
			writer.writeMapEncodingRules(routeTypes.getEncodingRuleTypes());

			PreparedStatement selectData = mapConnection
					.prepareStatement("SELECT area, coordinates, innerPolygons, types, additionalTypes, name FROM binary_map_objects WHERE id = ?");

			// write map levels and map index
			TLongObjectHashMap<BinaryFileReference> treeHeader = new TLongObjectHashMap<BinaryFileReference>();
			RTree rtree = mapTree;
			long rootIndex = rtree.getFileHdr().getRootIndex();
			rtree.Node root = rtree.getReadNode(rootIndex);
			Rect rootBounds = calcBounds(root);
			if (rootBounds != null) {
				writer.startWriteMapLevelIndex(mapZooms.getLevel(i).getMinZoom(), mapZooms.getLevel(i).getMaxZoom(), rootBounds.getMinX(),
						rootBounds.getMaxX(), rootBounds.getMinY(), rootBounds.getMaxY());
				writeBinaryMapTree(root, rootBounds, rtree, writer, treeHeader);

				writeBinaryMapBlock(root, rootBounds, rtree, writer, selectData, treeHeader, new LinkedHashMap<String, Integer>(),
						new LinkedHashMap<MapRenderingTypes.MapRulType, String>());

				writer.endWriteMapLevelIndex();
			}

			selectData.close();

			writer.endWriteMapIndex();
			writer.flush();
		} catch (RTreeException e) {
			throw new IllegalStateException(e);
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
					int[] typeUse = new int[types.length / 2];
					for (int j = 0; j < types.length; j += 2) {
						int ids = Algoritms.parseSmallIntFromBytes(types, j);
						typeUse[j / 2] = routeTypes.getTypeByInternalId(ids).getTargetId();
					}
					byte[] addTypes = rs.getBytes(5);
					int[] addtypeUse = null ;
					if (addTypes != null) {
						addtypeUse = new int[addTypes.length / 2];
						for (int j = 0; j < addTypes.length; j += 2) {
							int ids = Algoritms.parseSmallIntFromBytes(addTypes, j);
							addtypeUse[j / 2] = routeTypes.getTypeByInternalId(ids).getTargetId();
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
	*/
}
