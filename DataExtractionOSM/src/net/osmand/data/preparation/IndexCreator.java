package net.osmand.data.preparation;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.osmand.Algoritms;
import net.osmand.IProgress;
import net.osmand.binary.BinaryMapIndexWriter;
import net.osmand.data.Amenity;
import net.osmand.data.Boundary;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.DataTileManager;
import net.osmand.data.Street;
import net.osmand.data.TransportRoute;
import net.osmand.data.City.CityType;
import net.osmand.data.index.DataIndexReader;
import net.osmand.data.index.DataIndexWriter;
import net.osmand.data.index.IndexConstants;
import net.osmand.impl.ConsoleProgressImplementation;
import net.osmand.osm.Entity;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Node;
import net.osmand.osm.Relation;
import net.osmand.osm.Way;
import net.osmand.osm.Entity.EntityId;
import net.osmand.osm.Entity.EntityType;
import net.osmand.osm.OSMSettings.OSMTagKey;
import net.osmand.osm.io.IOsmStorageFilter;
import net.osmand.osm.io.OsmBaseStorage;
import net.osmand.swing.DataExtractionSettings;
import net.osmand.swing.Messages;
import net.sf.junidecode.Junidecode;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.bzip2.CBZip2InputStream;
import org.xml.sax.SAXException;

import rtree.Element;
import rtree.IllegalValueException;
import rtree.LeafElement;
import rtree.NonLeafElement;
import rtree.Pack;
import rtree.RTree;
import rtree.RTreeException;
import rtree.Rect;

/**
 * http://wiki.openstreetmap.org/wiki/OSM_tags_for_routing#Is_inside.2Foutside
 * http://wiki.openstreetmap.org/wiki/Relations/Proposed/Postal_Addresses
 * http://wiki.openstreetmap.org/wiki/Proposed_features/House_numbers/Karlsruhe_Schema#Tags (node, way)
 * 
 * That data extraction has aim, save runtime memory and generate indexes on the fly. It will be longer than load in memory (needed part)
 * and save into index.
 */
@SuppressWarnings("unchecked")
public class IndexCreator {
	private static final Log log = LogFactory.getLog(IndexCreator.class);

	// ONLY derby.jar needed for derby dialect
	private final DBDialect dialect = DBDialect.SQLITE;

	public static final int BATCH_SIZE = 5000;
	public static final int BATCH_SIZE_OSM = 10000;
	public static final String TEMP_NODES_DB = "nodes.tmp.odb";

	public static final int STEP_CITY_NODES = 1;
	public static final int STEP_ADDRESS_RELATIONS_AND_MULTYPOLYGONS = 2;
	public static final int STEP_BORDER_CITY_WAYS = 3;
	public static final int STEP_MAIN = 4;

	private File workingDir = null;

	private boolean indexMap;
	private boolean indexPOI;
	private boolean indexTransport;
	private boolean indexAddress;

	private boolean normalizeStreets = true; // true by default
	private boolean saveAddressWays = true; // true by default

	private String regionName;
	private String poiFileName = null;
	private String mapFileName = null;
	private Long lastModifiedDate = null;

	private PreparedStatement pselectNode;
	private PreparedStatement pselectWay;
	private PreparedStatement pselectRelation;
	private PreparedStatement pselectTags;

	
	private IndexTransportCreator indexTransportCreator;
	// constants to start process from the middle and save temporary results
	private boolean recreateOnlyBinaryFile = false; // false;
	private boolean deleteOsmDB = false;
	private boolean deleteDatabaseIndexes = true;

	private Connection dbConn;
	private File dbFile;

	Map<PreparedStatement, Integer> pStatements = new LinkedHashMap<PreparedStatement, Integer>();

	private Connection poiConnection;
	private File poiIndexFile;
	private PreparedStatement poiPreparedStatement;

	private File mapFile;
	private RandomAccessFile mapRAFile;
	private Connection mapConnection;

	private PreparedStatement mapBinaryStat;
	private PreparedStatement mapLowLevelBinaryStat;
	private int lowLevelWays = -1;

	private PreparedStatement addressCityStat;
	private PreparedStatement addressStreetStat;
	private PreparedStatement addressBuildingStat;
	private PreparedStatement addressStreetNodeStat;

	private RTree[] mapTree = null;
	private MapZooms mapZooms = MapZooms.getDefault();  
	private MapRenderingTypes renderingTypes = MapRenderingTypes.getDefault();
	private String cityAdminLevel = "8";
	

	// MEMORY map : save it in memory while that is allowed
	private Map<Long, Set<Integer>>[] multiPolygonsWays;
	private Map<Long, String> multiPolygonsNames = new LinkedHashMap<Long, String>();
	private Map<Long, List<Long>> highwayRestrictions = new LinkedHashMap<Long, List<Long>>();

	// MEMORY address : choose what to use ?
	private boolean loadInMemory = true;
	private PreparedStatement addressSearchStreetStat;
	private PreparedStatement addressSearchBuildingStat;
	private PreparedStatement addressSearchStreetNodeStat;

	private Map<String, Long> addressStreetLocalMap = new LinkedHashMap<String, Long>();
	private Set<Long> addressBuildingLocalSet = new LinkedHashSet<Long>();
	private Set<Long> addressStreetNodeLocalSet = new LinkedHashSet<Long>();

	// MEMORY address : address structure
	// load it in memory
	private Map<EntityId, City> cities = new LinkedHashMap<EntityId, City>();
	private DataTileManager<City> cityVillageManager = new DataTileManager<City>(13);
	private DataTileManager<City> cityManager = new DataTileManager<City>(10);
	private List<Relation> postalCodeRelations = new ArrayList<Relation>();
	private Map<City, Boundary> citiBoundaries = new LinkedHashMap<City, Boundary>();
	private Set<Long> visitedBoundaryWays = new HashSet<Long>();

	private String[] normalizeDefaultSuffixes;
	private String[] normalizeSuffixes;

	// local purpose
	List<Integer> typeUse = new ArrayList<Integer>(8);
	List<Long> restrictionsUse = new ArrayList<Long>(8);



	public IndexCreator(File workingDir) {
		this.workingDir = workingDir;
		this.indexTransportCreator = new IndexTransportCreator(this); 
	}

	public void setIndexAddress(boolean indexAddress) {
		this.indexAddress = indexAddress;
	}

	public void setIndexMap(boolean indexMap) {
		this.indexMap = indexMap;
	}

	public void setIndexPOI(boolean indexPOI) {
		this.indexPOI = indexPOI;
	}

	public void setIndexTransport(boolean indexTransport) {
		this.indexTransport = indexTransport;
	}

	public void setSaveAddressWays(boolean saveAddressWays) {
		this.saveAddressWays = saveAddressWays;
	}

	public void setNormalizeStreets(boolean normalizeStreets) {
		this.normalizeStreets = normalizeStreets;
	}

/*	protected static int defineLevel(int minZoom) {
		int level = 0;
		if (minZoom < 15) {
			for (int i = 1; i < MAP_ZOOMS.length; i++) {
				if (minZoom <= MAP_ZOOMS[i]) {
					level = MAP_ZOOMS.length - 1 - i;
					break;
				}
			}
		}
		return level;
	}
*/

	public String getRegionName() {
		if (regionName == null) {
			return "Region"; //$NON-NLS-1$
		}
		return regionName;
	}

	public void setRegionName(String regionName) {
		this.regionName = regionName;
	}

	private Connection getDatabaseConnection(String fileName) throws SQLException {
		return getDatabaseConnection(fileName, false);
	}


	private Connection getDatabaseConnection(String fileName, boolean forceSqLite) throws SQLException {
		if (DBDialect.SQLITE == dialect || forceSqLite) {
			try {
				Class.forName("org.sqlite.JDBC");
			} catch (ClassNotFoundException e) {
				log.error("Illegal configuration", e);
				throw new IllegalStateException(e);
			}
			Connection connection = DriverManager.getConnection("jdbc:sqlite:" + fileName);
			Statement statement = connection.createStatement();
			statement.executeUpdate("PRAGMA synchronous = 0");
			statement.close();
			return connection;
		} else if (DBDialect.DERBY == dialect) {
			try {
				Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
			} catch (ClassNotFoundException e) {
				log.error("Illegal configuration", e);
				throw new IllegalStateException(e);
			}
			Connection conn = DriverManager.getConnection("jdbc:derby:" + fileName + ";create=true");
			conn.setAutoCommit(false);
			return conn;
		} else if (DBDialect.H2 == dialect) {
			try {
				Class.forName("org.h2.Driver");
			} catch (ClassNotFoundException e) {
				log.error("Illegal configuration", e);
				throw new IllegalStateException(e);
			}

			return DriverManager.getConnection("jdbc:h2:file:" + fileName);
		} else {
			throw new UnsupportedOperationException();
		}

	}

	public void loadEntityData(Entity e, boolean loadTags) throws SQLException {
		if (e instanceof Node || (e instanceof Way && !((Way) e).getNodes().isEmpty())) {
			// do not load tags for nodes inside way
			return;
		}
		Map<EntityId, Entity> map = new LinkedHashMap<EntityId, Entity>();
		if (e instanceof Relation && ((Relation) e).getMemberIds().isEmpty()) {
			pselectRelation.setLong(1, e.getId());
			if (pselectRelation.execute()) {
				ResultSet rs = pselectRelation.getResultSet();
				boolean first = true;
				while (rs.next()) {
					int ord = rs.getInt(4);
					if (ord > 0 || first) {
						first = false;
						((Relation) e).addMember(rs.getLong(1), EntityType.values()[rs.getInt(2)], rs.getString(3));
					}
				}
				rs.close();
			}
		} else if (e instanceof Way && ((Way) e).getEntityIds().isEmpty()) {
			pselectWay.setLong(1, e.getId());
			if (pselectWay.execute()) {
				ResultSet rs = pselectWay.getResultSet();
				boolean first = true;
				while (rs.next()) {
					int ord = rs.getInt(2);
					if (ord > 0 || first) {
						first = false;
						((Way) e).addNode(new Node(rs.getDouble(5), rs.getDouble(6), rs.getLong(1)));
					}
				}
				rs.close();
			}
		}
		Collection<EntityId> ids = e instanceof Relation ? ((Relation) e).getMemberIds() : ((Way) e).getEntityIds();

		for (EntityId i : ids) {
			// pselectNode = dbConn.prepareStatement("select n.latitude, n.longitude, t.skeys, t.value from node n left join tags t on n.id = t.id and t.type = 0 where n.id = ?");
			if (i.getType() == EntityType.NODE) {
				pselectNode.setLong(1, i.getId());
				if (pselectNode.execute()) {
					ResultSet rs = pselectNode.getResultSet();
					Node n = null;
					while (rs.next()) {
						if (n == null) {
							n = new Node(rs.getDouble(1), rs.getDouble(2), i.getId());
						}
						if (rs.getObject(3) != null) {
							n.putTag(rs.getString(3), rs.getString(4));
						}
					}
					map.put(i, n);
					rs.close();
				}
			} else if (i.getType() == EntityType.WAY) {
				// pselectWay = dbConn.prepareStatement("select w.node, w.ord, t.skeys, t.value, n.latitude, n.longitude " +
				// "from ways w left join tags t on w.id = t.id and t.type = 1 and w.ord = 0 inner join node n on w.node = n.id " +
				// "where w.id = ? order by w.ord");
				pselectWay.setLong(1, i.getId());
				if (pselectWay.execute()) {
					ResultSet rs = pselectWay.getResultSet();
					Way way = new Way(i.getId());
					map.put(i, way);
					boolean first = true;
					while (rs.next()) {
						int ord = rs.getInt(2);
						if (ord > 0 || first) {
							first = false;
							way.addNode(new Node(rs.getDouble(5), rs.getDouble(6), rs.getLong(1)));
						}
						if (ord == 0 && rs.getObject(3) != null) {
							way.putTag(rs.getString(3), rs.getString(4));
						}
					}
					rs.close();
				}
			} else if (i.getType() == EntityType.RELATION) {
				pselectRelation.setLong(1, i.getId());
				// pselectRelation = dbConn.prepareStatement("select r.member, r.type, r.role, r.ord, t.skeys, t.value" +
				// "from relations r left join tags t on r.id = t.id and t.type = 2 and r.ord = 0 " +
				// "where r.id = ? order by r.ord");
				if (pselectRelation.execute()) {
					ResultSet rs = pselectRelation.getResultSet();
					Relation rel = new Relation(i.getId());
					map.put(i, rel);
					boolean first = true;
					while (rs.next()) {
						int ord = rs.getInt(4);
						if (ord > 0 || first) {
							first = false;
							rel.addMember(rs.getLong(1), EntityType.values()[rs.getInt(2)], rs.getString(3));
						}
						if (ord == 0 && rs.getObject(5) != null) {
							rel.putTag(rs.getString(5), rs.getString(6));
						}
					}
					// do not load relation members recursively ? It is not needed for transport, address, poi before
					rs.close();
				}
			}
		}

		e.initializeLinks(map);
	}

	public void setPoiFileName(String poiFileName) {
		this.poiFileName = poiFileName;
	}

	public void setNodesDBFile(File file) {
		dbFile = file;
	}

	public void setMapFileName(String mapFileName) {
		this.mapFileName = mapFileName;
	}

	public String getMapFileName() {
		if (mapFileName == null) {
			return getRegionName() + IndexConstants.BINARY_MAP_INDEX_EXT;
		}
		return mapFileName;
	}

	public String getTempMapDBFileName() {
		return getMapFileName() + ".tmp"; //$NON-NLS-1$
	}

	public Long getLastModifiedDate() {
		return lastModifiedDate;
	}

	public void setLastModifiedDate(Long lastModifiedDate) {
		this.lastModifiedDate = lastModifiedDate;
	}

	public String getPoiFileName() {
		if (poiFileName == null) {
			return IndexConstants.POI_INDEX_DIR + getRegionName() + IndexConstants.POI_INDEX_EXT;
		}
		return poiFileName;
	}

	public int iterateOverEntities(IProgress progress, EntityType type, int allCount, int step) throws SQLException {
		Statement statement = dbConn.createStatement();
		String select;
		int count = 0;

		// stat.executeUpdate("create table tags (id "+longType+", type smallint, skeys varchar(255), value varchar(255))");
		// stat.executeUpdate("create table ways (id "+longType+", node "+longType+", ord smallint)");
//		stat.executeUpdate("create table relations (id "+longType+", member "+longType+", type smallint, role varchar(255), ord smallint)");
		if (type == EntityType.NODE) {
			// filter out all nodes without tags
			select = "select n.id, n.latitude, n.longitude, t.skeys, t.value from node n inner join tags t on n.id = t.id and t.type = 0 order by n.id"; //$NON-NLS-1$
		} else if (type == EntityType.WAY) {
			select = "select w.id, w.node, w.ord, t.skeys, t.value, n.latitude, n.longitude " + //$NON-NLS-1$
					"from ways w left join tags t on w.id = t.id and t.type = 1 and w.ord = 0 inner join node n on w.node = n.id " + //$NON-NLS-1$
					"order by w.id, w.ord"; //$NON-NLS-1$
		} else {
			select = "select r.id, t.skeys, t.value  from relations r inner join tags t on t.id = r.id and t.type = 2 and r.ord = 0"; //$NON-NLS-1$
		}

		ResultSet rs = statement.executeQuery(select);
		Entity prevEntity = null;

		long prevId = -1;
		while (rs.next()) {
			long curId = rs.getLong(1);
			boolean newEntity = curId != prevId;
			Entity e = prevEntity;
			if (type == EntityType.NODE) {
				if (newEntity) {
					e = new Node(rs.getDouble(2), rs.getDouble(3), curId);
				}
				e.putTag(rs.getString(4), rs.getString(5));
			} else if (type == EntityType.WAY) {
				if (newEntity) {
					e = new Way(curId);
				}
				int ord = rs.getInt(3);
				if (ord == 0 && rs.getObject(4) != null) {
					e.putTag(rs.getString(4), rs.getString(5));
				}
				if (newEntity || ord > 0) {
					((Way) e).addNode(new Node(rs.getDouble(6), rs.getDouble(7), rs.getLong(2)));
				}
			} else {
				if (newEntity) {
					e = new Relation(curId);
				}
				e.putTag(rs.getString(2), rs.getString(3));
			}
			if (newEntity) {
				count++;
				if (progress != null) {
					progress.progress(1);
				}
				if (prevEntity != null) {
					iterateEntity(prevEntity, step);
				}
				prevEntity = e;
			}
			prevId = curId;
		}
		if (prevEntity != null) {
			count++;
			iterateEntity(prevEntity, step);
		}
		rs.close();
		return count;
	}

	protected void loadEntityTags(EntityType type, Entity e) throws SQLException {
		pselectTags.setLong(1, e.getId());
		pselectTags.setByte(2, (byte) type.ordinal());
		ResultSet rsTags = pselectTags.executeQuery();
		while (rsTags.next()) {
			e.putTag(rsTags.getString(1), rsTags.getString(2));
		}
		rsTags.close();
	}

	
	
	public String getCityAdminLevel() {
		return cityAdminLevel;
	}
	
	public void setCityAdminLevel(String cityAdminLevel) {
		this.cityAdminLevel = cityAdminLevel;
	}
	
	public void indexBoundariesRelation(Entity e) throws SQLException {
		String adminLevel = e.getTag("admin_level");
		Boundary boundary = null;
		if (cityAdminLevel.equals(adminLevel)) {
			if (e instanceof Relation) {
				Relation i = (Relation) e;
				loadEntityData(i, true);
				boundary = new Boundary();
				if (i.getTag(OSMTagKey.NAME) != null) {
					boundary.setName(i.getTag(OSMTagKey.NAME));
				}
				boundary.setBoundaryId(i.getId());
				Map<Entity, String> entities = i.getMemberEntities();
				for (Entity es : entities.keySet()) {
					if (es instanceof Way) {
						boolean inner = "inner".equals(entities.get(es)); //$NON-NLS-1$
						if (inner) {
							boundary.getInnerWays().add((Way) es);
						} else {
							String wName = es.getTag(OSMTagKey.NAME);
							// if name are not equal keep the way for further check (it could be different suburb)
							if (Algoritms.objectEquals(wName, boundary.getName()) || wName == null) {
								visitedBoundaryWays.add(es.getId());
							}
							boundary.getOuterWays().add((Way) es);
						}
					}
				}
			} else if (e instanceof Way) {
				if (!visitedBoundaryWays.contains(e.getId())) {
					boundary = new Boundary();
					if (e.getTag(OSMTagKey.NAME) != null) {
						boundary.setName(e.getTag(OSMTagKey.NAME));
					}
					boundary.setBoundaryId(e.getId());
					boundary.getOuterWays().add((Way) e);

				}

			}
		}
		
		if (boundary != null && boundary.getCenterPoint() != null) {
			LatLon point = boundary.getCenterPoint();
			boolean cityFound = false;
			boolean containsCityInside = false;
			for (City c : cityManager.getClosestObjects(point.getLatitude(), point.getLongitude(), 3)) {
				if (boundary.containsPoint(c.getLocation())) {
					if (boundary.getName() == null || boundary.getName().equalsIgnoreCase(c.getName())) {
						citiBoundaries.put(c, boundary);
						cityFound = true;
						containsCityInside = true;
					}
				}
			}
			// TODO mark all suburbs inside city as is_in tag (!) or use another solution
			if (!cityFound) {
				for (City c : cityVillageManager.getClosestObjects(point.getLatitude(), point.getLongitude(), 3)) {
					if (boundary.containsPoint(c.getLocation())) {
						if (boundary.getName() == null || boundary.getName().equalsIgnoreCase(c.getName())) {
							citiBoundaries.put(c, boundary);
							cityFound = true;
						}
					}
				}
			}
			if (!cityFound && boundary.getName() != null) {
				// / create new city for named boundary very rare case that's why do not proper generate id
				// however it could be a problem
				City nCity = new City(containsCityInside ? CityType.CITY : CityType.SUBURB);
				nCity.setLocation(point.getLatitude(), point.getLongitude());
				nCity.setId(-boundary.getBoundaryId());
				nCity.setName(boundary.getName());
				citiBoundaries.put(nCity, boundary);
				cityVillageManager.registerObject(point.getLatitude(), point.getLongitude(), nCity);

				DataIndexWriter.writeCity(addressCityStat, pStatements, nCity, BATCH_SIZE);
				// commit to put all cities
				if (pStatements.get(addressCityStat) > 0) {
					addressCityStat.executeBatch();
					pStatements.put(addressCityStat, 0);
				}
			}
		}
	}

	public void indexAddressRelation(Relation i) throws SQLException {
		String type = i.getTag(OSMTagKey.ADDRESS_TYPE);
		boolean house = "house".equals(type); //$NON-NLS-1$
		boolean street = "a6".equals(type); //$NON-NLS-1$
		if (house || street) {
			// try to find appropriate city/street
			City c = null;
			// load with member ways with their nodes and tags !
			loadEntityData(i, true);

			Collection<Entity> members = i.getMembers("is_in"); //$NON-NLS-1$
			Relation a3 = null;
			Relation a6 = null;
			if (!members.isEmpty()) {
				if (street) {
					a6 = i;
				}
				Entity in = members.iterator().next();
				loadEntityData(in, true);
				if (in instanceof Relation) {
					// go one level up for house
					if (house) {
						a6 = (Relation) in;
						members = ((Relation) in).getMembers("is_in"); //$NON-NLS-1$
						if (!members.isEmpty()) {
							in = members.iterator().next();
							loadEntityData(in, true);
							if (in instanceof Relation) {
								a3 = (Relation) in;
							}
						}

					} else {
						a3 = (Relation) in;
					}
				}
			}

			if (a3 != null) {
				Collection<EntityId> memberIds = a3.getMemberIds("label"); //$NON-NLS-1$
				if (!memberIds.isEmpty()) {
					c = cities.get(memberIds.iterator().next());
				}
			}
			if (c != null && a6 != null) {
				String name = a6.getTag(OSMTagKey.NAME);

				if (name != null) {
					LatLon location = c.getLocation();
					for (Entity e : i.getMembers(null)) {
						if (e instanceof Way) {
							LatLon l = ((Way) e).getLatLon();
							if (l != null) {
								location = l;
								break;
							}
						}
					}

					Long streetId = getStreetInCity(c, name, location, (a6.getId() << 2) | 2);
					if (streetId == null) {
						return;
					}
					if (street) {
						for (Map.Entry<Entity, String> r : i.getMemberEntities().entrySet()) {
							if ("street".equals(r.getValue())) { //$NON-NLS-1$
								if (r.getKey() instanceof Way && saveAddressWays) {
									DataIndexWriter.writeStreetWayNodes(addressStreetNodeStat, 
											pStatements, streetId, (Way) r.getKey(), BATCH_SIZE);
									if (loadInMemory) {
										addressStreetNodeLocalSet.add(r.getKey().getId());
									}
								}
							} else if ("house".equals(r.getValue())) { //$NON-NLS-1$
								// will be registered further in other case
								if (!(r.getKey() instanceof Relation)) {
									String hno = r.getKey().getTag(OSMTagKey.ADDR_HOUSE_NUMBER);
									if (hno != null) {
										Building building = new Building(r.getKey());
										building.setName(hno);
										DataIndexWriter.writeBuilding(addressBuildingStat, pStatements, 
												streetId, building, BATCH_SIZE);
										if (loadInMemory) {
											addressBuildingLocalSet.add(r.getKey().getId());
										}
									}
								}
							}
						}
					} else {
						String hno = i.getTag(OSMTagKey.ADDRESS_HOUSE);
						if (hno == null) {
							hno = i.getTag(OSMTagKey.ADDR_HOUSE_NUMBER);
						}
						if (hno == null) {
							hno = i.getTag(OSMTagKey.NAME);
						}
						members = i.getMembers("border"); //$NON-NLS-1$
						if (!members.isEmpty()) {
							Entity border = members.iterator().next();
							if (border != null) {
								EntityId id = EntityId.valueOf(border);
								// special check that address do not contain twice in a3 - border and separate a6
								if (!a6.getMemberIds().contains(id)) {
									Building building = new Building(border);
									if (building.getLocation() != null) {
										building.setName(hno);
										DataIndexWriter.writeBuilding(addressBuildingStat, pStatements, streetId, building, BATCH_SIZE);
										if (loadInMemory) {
											addressBuildingLocalSet.add(id.getId());
										}
									} else {
										log.error("Strange border " + id + " location couldn't be found");
									}
								}
							}
						} else {
							log.info("For relation " + i.getId() + " border not found"); //$NON-NLS-1$ //$NON-NLS-2$
						}

					}
				}
			}
		}
	}

	public City getClosestCity(LatLon point) {
		if (point == null) {
			return null;
		}
		
		for (City c : cityManager.getClosestObjects(point.getLatitude(), point.getLongitude(), 3)) {
			Boundary boundary = citiBoundaries.get(c);
			if(boundary != null){
				if(boundary.containsPoint(point)){
					return c;
				}
			}
		}
		for (City c : cityVillageManager.getClosestObjects(point.getLatitude(), point.getLongitude(), 3)) {
			Boundary boundary = citiBoundaries.get(c);
			if(boundary != null){
				if(boundary.containsPoint(point)){
					return c;
				}
			}
		}
		City closest = null;
		double relDist = Double.POSITIVE_INFINITY;
		for (City c : cityManager.getClosestObjects(point.getLatitude(), point.getLongitude(), 3)) {
			double rel = MapUtils.getDistance(c.getLocation(), point) / c.getType().getRadius();
			if (rel < relDist) {
				closest = c;
				relDist = rel;
				if (relDist < 0.2d) {
					break;
				}
			}
		}
		if (relDist < 0.2d) {
			return closest;
		}
		for (City c : cityVillageManager.getClosestObjects(point.getLatitude(), point.getLongitude(), 3)) {
			double rel = MapUtils.getDistance(c.getLocation(), point) / c.getType().getRadius();
			if (rel < relDist) {
				closest = c;
				relDist = rel;
				if (relDist < 0.2d) {
					break;
				}
			}
		}
		return closest;
	}

	public String normalizeStreetName(String name) {
		name = name.trim();
		if (normalizeStreets) {
			String newName = name;
			boolean processed = newName.length() != name.length();
			for (String ch : normalizeDefaultSuffixes) {
				int ind = checkSuffix(newName, ch);
				if (ind != -1) {
					newName = cutSuffix(newName, ind, ch.length());
					processed = true;
					break;
				}
			}

			if (!processed) {
				for (String ch : normalizeSuffixes) {
					int ind = checkSuffix(newName, ch);
					if (ind != -1) {
						newName = putSuffixToEnd(newName, ind, ch.length());
						processed = true;
						break;
					}
				}
			}
			if (processed) {
				return newName;
			}
		}
		return name;
	}

	private int checkSuffix(String name, String suffix) {
		int i = -1;
		boolean searchAgain = false;
		do {
			i = name.indexOf(suffix, i);
			searchAgain = false;
			if (i > 0) {
				if (Character.isLetterOrDigit(name.charAt(i - 1))) {
					i++;
					searchAgain = true;
				}
			}
		} while (searchAgain);
		return i;
	}

	private String cutSuffix(String name, int ind, int suffixLength) {
		String newName = name.substring(0, ind);
		if (name.length() > ind + suffixLength + 1) {
			newName += name.substring(ind + suffixLength + 1);
		}
		return newName.trim();
	}

	private String putSuffixToEnd(String name, int ind, int suffixLength) {
		if (name.length() <= ind + suffixLength) {
			return name;

		}
		String newName;
		if (ind > 0) {
			newName = name.substring(0, ind);
			newName += name.substring(ind + suffixLength);
			newName += name.substring(ind - 1, ind + suffixLength);
		} else {
			newName = name.substring(suffixLength + 1) + name.charAt(suffixLength) + name.substring(0, suffixLength);
		}

		return newName.trim();
	}

	public Long getStreetInCity(City city, String name, LatLon location, long initId) throws SQLException {
		if (name == null || city == null) {
			return null;
		}
		Long foundId = null;

		name = normalizeStreetName(name);
		if (loadInMemory) {
			foundId = addressStreetLocalMap.get(name + "_" + city.getId()); //$NON-NLS-1$
		} else {
			addressSearchStreetStat.setLong(1, city.getId());
			addressSearchStreetStat.setString(2, name);
			ResultSet rs = addressSearchStreetStat.executeQuery();
			if (rs.next()) {
				foundId = rs.getLong(1);
			}
			rs.close();
		}

		if (foundId == null) {
			DataIndexWriter.insertStreetData(addressStreetStat, initId, name, Junidecode.unidecode(name), 
					location.getLatitude(), location.getLongitude(), city.getId());
			if (loadInMemory) {
				DataIndexWriter.addBatch(pStatements, addressStreetStat, BATCH_SIZE);
				addressStreetLocalMap.put(name + "_" + city.getId(), initId); //$NON-NLS-1$
			} else {
				addressStreetStat.execute();
				// commit immediately to search after
				mapConnection.commit();
			}
			foundId = initId;
		}
		return foundId;
	}

	private List<Amenity> tempAmenityList = new ArrayList<Amenity>();

	public void checkEntity(Entity e){
		String name = e.getTag(OSMTagKey.NAME);
		if (name == null){
			String msg = "";
			Collection<String> keys = e.getTagKeySet();
			int cnt = 0;
			for (Iterator iter = keys.iterator(); iter.hasNext();) {
				String key = (String) iter.next();
				if (key.startsWith("name:") && key.length() <= 8) {
					// ignore specialties like name:botanical
					if (cnt == 0)
						msg += "Entity misses default name tag, but it has localized name tag(s):\n";
					msg += key + "=" + e.getTag(key) + "\n";
					cnt++;
				}
			}
			if (cnt > 0) {
				msg += "Consider adding the name tag at " + e.getOsmUrl();
				log.warn(msg);
			}
		}
	}

	private void iterateEntity(Entity e, int step) throws SQLException {
		if (step == STEP_MAIN) {
			if (indexPOI) {
				tempAmenityList.clear();
				tempAmenityList = Amenity.parseAmenities(e, tempAmenityList);
				if (!tempAmenityList.isEmpty() && poiPreparedStatement != null) {
					// load data for way (location etc...)
					loadEntityData(e, false);
					for (Amenity a : tempAmenityList) {
						checkEntity(e);
						a.setEntity(e);
						if (a.getLocation() != null) {
							// do not convert english name
							// convertEnglishName(a);
							DataIndexWriter.insertAmenityIntoPoi(poiPreparedStatement, pStatements, a, BATCH_SIZE);
						}
					}
				}
			}
			if (indexTransport) {
				if (e instanceof Relation && e.getTag(OSMTagKey.ROUTE) != null) {
					loadEntityData(e, true);
					TransportRoute route = indexTransportCreator.indexTransportRoute((Relation) e);
					if (route != null) {
						indexTransportCreator.insertTransportIntoIndex(route, pStatements);
					}
				}
			}

			if (indexMap && (e instanceof Way || e instanceof Node)) {
				// manipulate what kind of way to load
				loadEntityData(e, false);
				boolean inverse = "-1".equals(e.getTag(OSMTagKey.ONEWAY)); //$NON-NLS-1$
				for (int i = 0; i < mapZooms.size(); i++) {
					writeBinaryEntityToMapDatabase(e, e.getId(), i == 0 ? inverse : false, i);
				}

			}

			if (indexAddress) {
				// index not only buildings but also nodes that belongs to addr:interpolation ways
				if (e.getTag(OSMTagKey.ADDR_HOUSE_NUMBER) != null && e.getTag(OSMTagKey.ADDR_STREET) != null) {
					// TODO e.getTag(OSMTagKey.ADDR_CITY) could be used to find city however many cities could have same name!
					// check that building is not registered already
					boolean exist = false;
					if (loadInMemory) {
						exist = addressBuildingLocalSet.contains(e.getId());
					} else {
						addressSearchBuildingStat.setLong(1, e.getId());
						ResultSet rs = addressSearchBuildingStat.executeQuery();
						exist = rs.next();
						rs.close();

					}
					if (!exist) {
						loadEntityData(e, false);
						LatLon l = e.getLatLon();
						City city = getClosestCity(l);
						Long idStreet = getStreetInCity(city, e.getTag(OSMTagKey.ADDR_STREET), l, (e.getId() << 2));
						if (idStreet != null) {
							Building building = new Building(e);
							building.setName(e.getTag(OSMTagKey.ADDR_HOUSE_NUMBER));
							DataIndexWriter.writeBuilding(addressBuildingStat, pStatements, idStreet, building, BATCH_SIZE);
						}
					}
				} else if (e instanceof Way /* && OSMSettings.wayForCar(e.getTag(OSMTagKey.HIGHWAY)) */
						&& e.getTag(OSMTagKey.HIGHWAY) != null && e.getTag(OSMTagKey.NAME) != null) {
					// suppose that streets with names are ways for car
					// Ignore all ways that have house numbers and highway type
					boolean exist = false;

					// if we saved address ways we could checked that we registered before
					if (saveAddressWays) {
						if (loadInMemory) {
							exist = addressStreetNodeLocalSet.contains(e.getId());
						} else {
							addressSearchStreetNodeStat.setLong(1, e.getId());
							ResultSet rs = addressSearchStreetNodeStat.executeQuery();
							exist = rs.next();
							rs.close();
						}
					}

					// check that street way is not registered already
					if (!exist) {
						loadEntityData(e, false);
						LatLon l = e.getLatLon();
						City city = getClosestCity(l);
						Long idStreet = getStreetInCity(city, e.getTag(OSMTagKey.NAME), l, (e.getId() << 2) | 1);
						if (idStreet != null && saveAddressWays) {
							DataIndexWriter.writeStreetWayNodes(addressStreetNodeStat, pStatements, idStreet, (Way) e, BATCH_SIZE);
						}
					}
				}
				if (e instanceof Relation) {
					if (e.getTag(OSMTagKey.POSTAL_CODE) != null) {
						loadEntityData(e, false);
						postalCodeRelations.add((Relation) e);
					}
				}
			}
		} else if(step == STEP_BORDER_CITY_WAYS) {
			if (indexAddress) {
				if (e instanceof Way && "administrative".equals(e.getTag(OSMTagKey.BOUNDARY))) { //$NON-NLS-1$
					indexBoundariesRelation(e);
				}
			}
		} else if (step == STEP_ADDRESS_RELATIONS_AND_MULTYPOLYGONS) {
			if (indexAddress) {
				if (e instanceof Relation && "address".equals(e.getTag(OSMTagKey.TYPE))) { //$NON-NLS-1$
					indexAddressRelation((Relation) e);
				}
				
				if (e instanceof Relation && "administrative".equals(e.getTag(OSMTagKey.BOUNDARY))) { //$NON-NLS-1$
					indexBoundariesRelation((Relation) e);
				}
			}
			if (indexMap && e instanceof Relation && "restriction".equals(e.getTag(OSMTagKey.TYPE))) { //$NON-NLS-1$
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
						loadEntityData(e, true);
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
			if (indexMap && e instanceof Relation && "multipolygon".equals(e.getTag(OSMTagKey.TYPE))) { //$NON-NLS-1$
				loadEntityData(e, true);
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
				if(!outerFound){
					log.warn("Probably map bug: Multipoligon id=" + e.getId() + " contains only inner ways : "); //$NON-NLS-1$ //$NON-NLS-2$
					return;
				}

				int mtType = findMultiPolygonType(e, 0);
				if (mtType != 0) {

					String name = renderingTypes.getEntityName(e);
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
						 // log.warn("In multipolygon  " + e.getId() + " there are incompleted ways : " + incompletedRings);
						return;
						// completedRings.addAll(incompletedRings);
					}

					// skip completed rings that are not one type
					for (List<Way> l : completedRings) {
						boolean innerType = "inner".equals(entities.get(l.get(0))); //$NON-NLS-1$
						for (Way way : l) {
							boolean inner = "inner".equals(entities.get(way)); //$NON-NLS-1$
							if (innerType != inner) {
								log.warn("Probably map bug: Multipoligon contains outer and inner ways.\n" +  //$NON-NLS-1$
										"Way:" + way.getId() + " is strange part of completed ring. InnerType:" + innerType + " way inner: " + inner + " way inner string:" + entities.get(way)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
								return;
							}
						}
					}

					for (List<Way> l : completedRings) {
						boolean innerType = "inner".equals(entities.get(l.get(0))); //$NON-NLS-1$
						boolean clockwise = isClockwiseWay(l);
						// clockwise - outer (like coastline), anticlockwise - inner
						boolean inverse = clockwise != !innerType;
						for (Way way : l) {
							boolean inner = "inner".equals(entities.get(way)); //$NON-NLS-1$
							if (!inner && name != null) {
								multiPolygonsNames.put(way.getId(), name);
							}
							putMultipolygonType(multiPolygonsWays[0], way.getId(), mtType, inverse);
							for (int i = 1; i < multiPolygonsWays.length; i++) {
								int type = findMultiPolygonType(e, i);
								if (type != 0) {
									putMultipolygonType(multiPolygonsWays[i], way.getId(), type, inverse);
								}
							}
						}
					}

				}
			}
		} else if (step == STEP_CITY_NODES) {
			registerCityIfNeeded(e);
		}
	}

	public void combineMultiPolygons(Way w, List<List<Way>> completedRings, List<List<Way>> incompletedRings) {
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

	private void putMultipolygonType(Map<Long, Set<Integer>> multiPolygonsWays, long baseId, int mtType, boolean inverse) {
		if (mtType == 0) {
			return;
		}
		if (!multiPolygonsWays.containsKey(baseId)) {
			multiPolygonsWays.put(baseId, new LinkedHashSet<Integer>());
		}
		if (inverse) {
			multiPolygonsWays.get(baseId).add(mtType | (1 << 15));
		} else {
			multiPolygonsWays.get(baseId).add(mtType);
		}
	}

	private int findMultiPolygonType(Entity e, int level) {
		int t = renderingTypes.encodeEntityWithType(e, mapZooms.getLevel(level).getMaxZoom(), true, typeUse);
		int mtType = 0;
		if (t != 0) {
			if ((t & 3) == MapRenderingTypes.MULTY_POLYGON_TYPE) {
				mtType = t;
			} else {
				for (Integer i : typeUse) {
					if ((i & 3) == MapRenderingTypes.MULTY_POLYGON_TYPE) {
						mtType = i;
						break;
					}
				}
			}
		}
		return mtType;
	}

	public Point2D.Float getIntersectionPoint(Line2D.Float line1, Line2D.Float line2) {
		if (!line1.intersectsLine(line2))
			return null;
		double px = line1.getX1(), py = line1.getY1(), rx = line1.getX2() - px, ry = line1.getY2() - py;
		double qx = line2.getX1(), qy = line2.getY1(), sx = line2.getX2() - qx, sy = line2.getY2() - qy;

		double det = sx * ry - sy * rx;
		if (det == 0) {
			return null;
		} else {
			double z = (sx * (qy - py) + sy * (px - qx)) / det;
			if (z <= 0 || z >= 1)
				return null; // intersection at end point!
			return new Point2D.Float((float) (px + z * rx), (float) (py + z * ry));
		}
	} // end intersection line-line

	private boolean isClockwiseWay(List<Way> ways) {
		if (ways.isEmpty()) {
			return false;
		}
		List<Node> nodes;
		if (ways.size() == 1) {
			nodes = ways.get(0).getNodes();
		} else {
			nodes = new ArrayList<Node>();
			boolean first = true;
			for (Way e : ways) {
				if (first) {
					first = false;
					nodes.addAll(e.getNodes());
				} else {
					nodes.addAll(e.getNodes().subList(1, e.getNodes().size()));
				}
			}
		}
		if (nodes.isEmpty()) {
			return false;
		}
		double angle = 0;
		double prevAng = 0;
		double firstAng = 0;
		double selfIntersection = 0;
		boolean open = nodes.get(nodes.size() - 1).getId() != nodes.get(0).getId();

		for (int i = 1; open ? i < nodes.size() : i <= nodes.size(); i++) {// nodes.get(i).getId()
			double ang;
			if (i < nodes.size()) {
				ang = Math.atan2(nodes.get(i).getLatitude() - nodes.get(i - 1).getLatitude(), 
						nodes.get(i).getLongitude() - nodes.get(i - 1).getLongitude());
				// find self intersection
				Line2D.Float l = new Line2D.Float((float) nodes.get(i).getLongitude(), (float) nodes.get(i).getLatitude(), 
						(float) nodes.get(i - 1).getLongitude(), (float) nodes.get(i -1).getLatitude());
				for (int j = i - 2; j > i - 7; j--) {
					if (j < 1) {
						break;
					}
					Line2D.Float l2 = new Line2D.Float((float) nodes.get(j).getLongitude(), (float) nodes.get(j).getLatitude(),
							(float) nodes.get(j - 1).getLongitude(), (float) nodes.get(j - 1).getLatitude());
					java.awt.geom.Point2D.Float point = getIntersectionPoint(l, l2);
					if (point != null) {
						double dang = Math.atan2(nodes.get(j).getLatitude() - nodes.get(j - 1).getLatitude(), 
								nodes.get(j).getLongitude() - nodes.get(j - 1).getLongitude());
						if (adjustDirection(ang - dang) < 0) {
							selfIntersection += 2 * Math.PI;
						} else {
							selfIntersection -= 2 * Math.PI;
						}
					}

				}
			} else {
				ang = firstAng;
			}
			if (i > 1) {
				angle += adjustDirection(ang - prevAng);
				prevAng = ang;
			} else {
				prevAng = ang;
				firstAng = ang;
			}

		}
		return (angle - selfIntersection) < 0;
	}

	private double adjustDirection(double ang) {
		if (ang < -Math.PI) {
			ang += 2 * Math.PI;
		} else if (ang > Math.PI) {
			ang -= 2 * Math.PI;
		}
		return ang;
	}

	public void registerCityIfNeeded(Entity e) {
		if (e instanceof Node && e.getTag(OSMTagKey.PLACE) != null) {
			City city = new City((Node) e);
			if (city.getType() != null && !Algoritms.isEmpty(city.getName())) {
				if (city.getType() == CityType.CITY || city.getType() == CityType.TOWN) {
					cityManager.registerObject(((Node) e).getLatitude(), ((Node) e).getLongitude(), city);
				} else {
					cityVillageManager.registerObject(((Node) e).getLatitude(), ((Node) e).getLongitude(), city);
				}
				cities.put(city.getEntityId(), city);
			}
		}
	}

	private void writeBinaryEntityToMapDatabase(Entity e, long baseId, boolean inverse, int level) throws SQLException {
		int type = renderingTypes.encodeEntityWithType(e, mapZooms.getLevel(level).getMaxZoom(), false, typeUse);
		Map<Long, Set<Integer>> multiPolygonsWays = this.multiPolygonsWays[level];
		boolean hasMulti = e instanceof Way && multiPolygonsWays.containsKey(e.getId());
		if (hasMulti) {
			Set<Integer> set = multiPolygonsWays.get(e.getId());
			boolean first = true;
			for (Integer i : set) {
				if (first && type == 0) {
					type = i;
					first = false;
				} else {
					// do not compare direction
					int k = i & 0x7fff;
					int ks = k | MapRenderingTypes.POLYGON_TYPE;
					// turn of polygon type 3 ^ (suppose polygon = multipolygon)
					if (ks == type) {
						type = i;
					} else {
						int ind = typeUse.indexOf(ks);
						if (ind == -1) {
							typeUse.add(i);
						} else {
							typeUse.set(ind, i);
						}
					}
				}
			}
		}

		if (type == 0) {
			return;
		}

		restrictionsUse.clear();
		// try to find restrictions only for max zoom level
		if (level == 0 && highwayRestrictions.containsKey(baseId)) {
			restrictionsUse.addAll(highwayRestrictions.get(baseId));
		}

		boolean point = (type & 3) == MapRenderingTypes.POINT_TYPE;
		RTree rtree = null;
		int zoom;
		long id = (baseId << 3) | ((level & 3) << 1);
		rtree = mapTree[level];
		zoom = mapZooms.getLevel(level).getMaxZoom() - 1;
		boolean skip = false;

		String eName = renderingTypes.getEntityName(e);
		if (eName == null) {
			eName = multiPolygonsNames.get(baseId);
		}
		int highwayAttributes = 0;
		if (e.getTag(OSMTagKey.HIGHWAY) != null) {
			highwayAttributes = MapRenderingTypes.getHighwayAttributes(e);
		}
		if (e instanceof Way) {
			id |= 1;
			// simplify route
			if (level > 0) {
				e = simplifyWay((Way) e, id, hasMulti, zoom, eName, type, level);
				skip = e == null;
			}

		}
		if (!skip) {
			DataIndexWriter.insertBinaryMapRenderObjectIndex(pStatements, mapBinaryStat, rtree, e, eName, id, type, typeUse,
					highwayAttributes, restrictionsUse, inverse, point, true);
		}
	}

	protected long encodeTypesToOneLong(int mainType) {
		long i = 0;
		int ind = 0;
		int sh = 0;
		if(typeUse.size() > 3){
			log.error("Types for low index way more than 4"); //$NON-NLS-1$
		}
		i |= (mainType << sh);
		if (typeUse.size() > ind) {
			sh += 16;
			i |= ((long)typeUse.get(ind++) << sh );
			if (typeUse.size() > ind) {
				sh += 16;
				i |= ((long)typeUse.get(ind++) << sh );
				if (typeUse.size() > ind) {
					sh += 16;
					i |= ((long)typeUse.get(ind++) << sh);
				}
			}
		}
		return i;
	}

	protected int decodeTypesFromOneLong(long i) {
		typeUse.clear();
		int mask = (1 << 16) - 1;
		int k = (int) (i & mask);
		int r = 0;
		if (k > 0) {
			r = k;
			i >>= 16;
			k = (int) (i & mask);
			if (k > 0) {
				typeUse.add(k);
				i >>= 16;
				k = (int) (i & mask);
				if (k > 0) {
					typeUse.add(k);
					i >>= 16;
					k = (int) (i & mask);
					if (k > 0) {
						typeUse.add(k);
						i >>= 16;
					}
				}
			}
		}
		return r;
	}
	

	protected Way simplifyWay(Way originalE, long id, boolean hasMulti, int zoom, String name, int type, int level) throws SQLException {
		List<Node> nodes = originalE.getNodes();
		Way way = new Way(id);
		for (String t : originalE.getTagKeySet()) {
			way.putTag(t, originalE.getTag(t));
		}
		boolean cycle = originalE.getNodeIds().get(0).longValue() == originalE.getNodeIds().get(nodes.size() - 1).longValue();
		long longType = encodeTypesToOneLong(type);

		boolean skip = checkForSmallAreas(nodes, zoom, 3, 3);
		if (skip && cycle/* || !hasMulti)*/) {
			return null;
		}

		simplifyDouglasPeucker(nodes, zoom + 8, 3, way);
		if (way.getNodes().size() < 2) {
			return null;
		}
		if (cycle) {
			// nothing to do
			return way;
		} else {
			lowLevelWays ++;
			DataIndexWriter.insertLowLevelMapBinaryObject(pStatements, mapLowLevelBinaryStat, level, longType, id, way.getNodes(), name);
			return null;
		}

	}
	
	private void loadNodes(byte[] nodes, List<Float> toPut){
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
	
	private void processingLowLevelWays(IProgress progress) throws SQLException {
		restrictionsUse.clear();
		mapLowLevelBinaryStat.executeBatch();
		mapLowLevelBinaryStat.close();
		pStatements.remove(mapLowLevelBinaryStat);
		mapLowLevelBinaryStat = null;
		mapConnection.commit();
		
		PreparedStatement startStat = mapConnection.prepareStatement("SELECT id, start_node, end_node, nodes FROM " 
				+ IndexConstants.LOW_LEVEL_MAP_TABLE + " WHERE start_node = ? AND type=? AND level = ? AND name=?");
		PreparedStatement endStat = mapConnection.prepareStatement("SELECT id, start_node, end_node, nodes FROM " 
				+ IndexConstants.LOW_LEVEL_MAP_TABLE + " WHERE end_node = ? AND type=? AND level = ? AND name=?");
		Statement selectStatement = mapConnection.createStatement();
		ResultSet rs = selectStatement.executeQuery("SELECT id, start_node, end_node, name, nodes, type, level FROM " + IndexConstants.LOW_LEVEL_MAP_TABLE);
		Set<Long> visitedWays = new LinkedHashSet<Long>();
		ArrayList<Float> list = new ArrayList<Float>(100);
		while(rs.next()){
			if(lowLevelWays != -1){
				progress.progress(1);
			}
			long id = rs.getLong(1);
			if(visitedWays.contains(id)){
				continue;
			}
			
			visitedWays.add(id);
			int level = rs.getInt(7);
			int zoom = mapZooms.getLevel(level).getMaxZoom();
			
			long startNode = rs.getLong(2);
			long endNode = rs.getLong(3);
			
			String name = rs.getString(4);
			long ltype = rs.getLong(6);
			loadNodes(rs.getBytes(5), list);
			ArrayList<Float> wayNodes = new ArrayList<Float>(list);
			
			
			// combine startPoint with EndPoint
			boolean combined = true;
			while (combined) {
				combined = false;
				endStat.setLong(1, startNode);
				endStat.setLong(2, ltype);
				endStat.setShort(3, (short) level);
				endStat.setString(4, name);
				ResultSet fs = endStat.executeQuery();
				while (fs.next()) {
					if (!visitedWays.contains(fs.getLong(1))) {
						combined = true;
						long lid = fs.getLong(1);
						startNode = fs.getLong(2);
						visitedWays.add(lid);
						loadNodes(fs.getBytes(4), list);
						ArrayList li = new ArrayList<Float>(list);
						// remove first lat/lon point
						wayNodes.remove(0);
						wayNodes.remove(0);
						li.addAll(wayNodes);
						wayNodes = li;
					}
				}
				fs.close();
			}
			
			// combined end point
			combined = true;
			while (combined) {
				combined = false;
				startStat.setLong(1, endNode);
				startStat.setLong(2, ltype);
				startStat.setShort(3, (short) level);
				startStat.setString(4, name);
				ResultSet fs = startStat.executeQuery();
				while (fs.next()) {
					if (!visitedWays.contains(fs.getLong(1))) {
						combined = true;
						long lid = fs.getLong(1);
						endNode = fs.getLong(3);
						visitedWays.add(lid);
						loadNodes(fs.getBytes(4), list);
						for (int i = 2; i < list.size(); i++) {
							wayNodes.add(list.get(i));
						}
					}
				}
				fs.close();
			}
			List<Node> wNodes = new ArrayList<Node>();
			for (int i = 0; i < wayNodes.size(); i += 2) {
				wNodes.add(new Node(wayNodes.get(i), wayNodes.get(i + 1), i == 0 ? startNode : endNode));
			}
			boolean skip = false;
			boolean cycle = startNode == endNode;
			boolean hasMulti = multiPolygonsWays[level].containsKey(id >> 3);
			if(cycle || !hasMulti){
				skip = checkForSmallAreas(wNodes, zoom - 1, 1, 4);
			}
			
			if (!skip) {
				Way newWs = new Way(id);
				simplifyDouglasPeucker(wNodes, zoom - 1 + 8, 3, newWs);
				
				int type = decodeTypesFromOneLong(ltype);
				DataIndexWriter.insertBinaryMapRenderObjectIndex(pStatements, mapBinaryStat, mapTree[level], newWs, name, 
						id, type, typeUse, 0, restrictionsUse, false, false, false);
			}
			
		}
		
	}
	
	
	private boolean checkForSmallAreas(List<Node> nodes, int zoom, int minz, int maxz) {
		int minX = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxY = Integer.MIN_VALUE;
		int c = 0;
		for (int i = 0; i < nodes.size(); i++) {
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
		return ((maxX - minX) <= minz && (maxY - minY) <= maxz) || 
				((maxX - minX) <= maxz && (maxY - minY) <= minz);

	}
	
	private void simplifyDouglasPeucker(List<Node> n, int zoom, int epsilon, Way w){
		ArrayList<Integer> l = new ArrayList<Integer>();
		int first = 0;
		while(first < n.size()){
			if(n.get(first) != null){
				break;
			}
			first++;
		}
		int last = n.size() - 1;
		while (last >= 0) {
			if (n.get(last) != null) {
				break;
			}
			last--;
		}
		if(last - first < 1){
			return;
		}
		boolean cycle = n.get(first).getId() == n.get(last).getId();
		simplifyDouglasPeucker(n, zoom, epsilon, l, first, cycle ? last - 1: last);
		w.addNode(n.get(first));
		for (int i = 0; i < l.size(); i++) {
			w.addNode(n.get(l.get(i)));
		}
		if (cycle) {
			w.addNode(n.get(first));
		}
	}
	
	private void simplifyDouglasPeucker(List<Node> n, int zoom, int epsilon, List<Integer> ints, int start, int end){
		double dmax = -1;
		int index = -1;
		for (int i = start + 1; i <= end - 1; i++) {
			if(n.get(i) == null){
				continue;
			}
			double d = orthogonalDistance(zoom, n.get(start), n.get(end), n.get(i));// calculate distance from line
			if (d > dmax) {
				dmax = d;
				index = i;
			}
		}
		if(dmax >= epsilon){
			simplifyDouglasPeucker(n, zoom, epsilon, ints, start, index);
			simplifyDouglasPeucker(n, zoom, epsilon, ints, index, end);
		} else {
			ints.add(end);
		}
	}

	private double orthogonalDistance(int zoom, Node nodeLineStart, Node nodeLineEnd, Node node) {
		double x1 = MapUtils.getTileNumberX(zoom, nodeLineStart.getLongitude());
		double y1 = MapUtils.getTileNumberY(zoom, nodeLineStart.getLatitude());
		double x2 = MapUtils.getTileNumberX(zoom, nodeLineEnd.getLongitude());
		double y2 = MapUtils.getTileNumberY(zoom, nodeLineEnd.getLatitude());
		double x = MapUtils.getTileNumberX(zoom, node.getLongitude());
		double y = MapUtils.getTileNumberY(zoom, node.getLatitude());
		double A = x - x1;
		double B = y - y1;
		double C = x2 - x1;
		double D = y2 - y1;
		return Math.abs(A * D - C * B) / Math.sqrt(C * C + D * D);
	}

	public boolean nodeIsLastSubTree(RTree tree, long ptr) throws RTreeException {
		rtree.Node parent = tree.getReadNode(ptr);
		Element[] e = parent.getAllElements();
		for (int i = 0; i < parent.getTotalElements(); i++) {
			if (e[i].getElementType() != rtree.Node.LEAF_NODE) {
				return false;
			}
		}
		return true;

	}

	public void writeBinaryAddressIndex(BinaryMapIndexWriter writer, IProgress progress) throws IOException, SQLException {
		boolean readWayNodes = saveAddressWays;

		writer.startWriteAddressIndex(getRegionName());
		DataIndexReader reader = new DataIndexReader();
		List<City> cities = reader.readCities(mapConnection);
		List<Street> streets = new ArrayList<Street>();
		Collections.sort(cities, new Comparator<City>() {

			@Override
			public int compare(City o1, City o2) {
				if (o1.getType() != o2.getType()) {
					return (o1.getType().ordinal() - o2.getType().ordinal());
				}
				return Collator.getInstance().compare(o1.getName(), o2.getName());
			}
		});
		PreparedStatement streetstat = reader.getStreetsBuildingPreparedStatement(mapConnection);
		PreparedStatement waynodesStat = null;
		if (readWayNodes) {
			waynodesStat = reader.getStreetsWayNodesPreparedStatement(mapConnection);
		}

		int j = 0;
		for (; j < cities.size(); j++) {
			City c = cities.get(j);
			if (c.getType() != CityType.CITY && c.getType() != CityType.TOWN) {
				break;
			}
		}
		progress.startTask(Messages.getString("IndexCreator.SERIALIZING_ADRESS"), j + ((cities.size() - j) / 100 + 1)); //$NON-NLS-1$

		Map<String, Set<Street>> postcodes = new TreeMap<String, Set<Street>>();
		boolean writeCities = true;
		
		// collect suburbs with is in value
		List<City> suburbs = new ArrayList<City>();
		for(City s : cities){
			if(s.getType() == CityType.SUBURB && s.getIsInValue() != null){
				suburbs.add(s);
			}
		}

		// write cities and after villages
		writer.startCityIndexes(false);
		for (int i = 0; i < cities.size(); i++) {
			City c = cities.get(i);
			List<City> listSuburbs = null;
			for (City suburb : suburbs) {
				if (suburb.getIsInValue().contains(c.getName().toLowerCase())) {
					if(listSuburbs == null){
						listSuburbs = new ArrayList<City>();
					}
					listSuburbs.add(suburb);
				}
			}
			if (writeCities) {
				progress.progress(1);
			} else if ((cities.size() - i) % 100 == 0) {
				progress.progress(1);
			}
			if (writeCities && c.getType() != CityType.CITY && c.getType() != CityType.TOWN) {
				writer.endCityIndexes(false);
				writer.startCityIndexes(true);
				writeCities = false;
			}

			streets.clear();
			Map<Street, List<Node>> streetNodes = null;
			if (readWayNodes) {
				streetNodes = new LinkedHashMap<Street, List<Node>>();
			}
			long time = System.currentTimeMillis();
			reader.readStreetsBuildings(streetstat, c, streets, waynodesStat, streetNodes, listSuburbs);
			long f = System.currentTimeMillis() - time;
			writer.writeCityIndex(c, streets, streetNodes);
			int bCount = 0;
			for (Street s : streets) {
				bCount++;
				for (Building b : s.getBuildings()) {
					bCount++;
					if (b.getPostcode() != null) {
						if (!postcodes.containsKey(b.getPostcode())) {
							postcodes.put(b.getPostcode(), new LinkedHashSet<Street>(3));
						}
						postcodes.get(b.getPostcode()).add(s);
					}
				}
			}
			if (f > 500) {
				System.out.println("! " + c.getName() + " ! " + f + " " + bCount + " streets " + streets.size()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
		}
		writer.endCityIndexes(!writeCities);

		// write postcodes
		writer.startPostcodes();
		for (String s : postcodes.keySet()) {
			writer.writePostcode(s, postcodes.get(s));
		}
		writer.endPostcodes();

		progress.finishTask();

		writer.endWriteAddressIndex();
		writer.flush();
		streetstat.close();
		if (readWayNodes) {
			waynodesStat.close();
		}

	}

	public void writeBinaryMapIndex(BinaryMapIndexWriter writer) throws IOException, SQLException {
		try {
			PreparedStatement selectData = mapConnection.prepareStatement("SELECT nodes, types, name, highway, restrictions FROM binary_map_objects WHERE id = ?"); //$NON-NLS-1$

			writer.startWriteMapIndex(regionName);

			for (int i = 0; i < mapZooms.size(); i++) {
				RTree rtree = mapTree[i];
				long rootIndex = rtree.getFileHdr().getRootIndex();
				rtree.Node root = rtree.getReadNode(rootIndex);
				Rect rootBounds = calcBounds(root);
				if (rootBounds != null) {
					boolean last = nodeIsLastSubTree(rtree, rootIndex);
					writer.startWriteMapLevelIndex(mapZooms.getLevel(i).getMinZoom(), mapZooms.getLevel(i).getMaxZoom(), rootBounds
							.getMinX(), rootBounds.getMaxX(), rootBounds.getMinY(), rootBounds.getMaxY());
					if (last) {
						writer.startMapTreeElement(rootBounds.getMinX(), rootBounds.getMaxX(), rootBounds.getMinY(), rootBounds.getMaxY());

					}
					writeBinaryMapTree(root, rtree, writer, selectData);
					if (last) {
						writer.endWriteMapTreeElement();
					}

					writer.endWriteMapLevelIndex();
				}
			}
			selectData.close();
			writer.writeMapEncodingRules(renderingTypes.getEncodingRuleTypes());
			writer.endWriteMapIndex();
			writer.flush();
		} catch (RTreeException e) {
			throw new IllegalStateException(e);
		}
	}

	public void writeBinaryMapTree(rtree.Node parent, RTree r, BinaryMapIndexWriter writer, PreparedStatement selectData) throws IOException, RTreeException, SQLException {
		Element[] e = parent.getAllElements();

		for (int i = 0; i < parent.getTotalElements(); i++) {
			Rect re = e[i].getRect();
			if (e[i].getElementType() == rtree.Node.LEAF_NODE) {
				long id = ((LeafElement) e[i]).getPtr();
				selectData.setLong(1, id);
				ResultSet rs = selectData.executeQuery();
				if (rs.next()) {
					 // mapConnection.prepareStatement("SELECT nodes, types, name, highway, restrictions FROM binary_map_objects WHERE id = ?");
					 writer.writeMapData(id, rs.getBytes(1), 
							 rs.getBytes(2), rs.getString(3),  
							 rs.getInt(4),  rs.getBytes(5));
				} else {
					log.error("Something goes wrong with id = " + id); //$NON-NLS-1$
				}
			} else {
				long ptr = ((NonLeafElement) e[i]).getPtr();
				rtree.Node ns = r.getReadNode(ptr);

				writer.startMapTreeElement(re.getMinX(), re.getMaxX(), re.getMinY(), re.getMaxY());

				writeBinaryMapTree(ns, r, writer, selectData);
				writer.endWriteMapTreeElement();
			}
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

	public String getRTreeMapIndexNonPackFileName() {
		return mapFile.getAbsolutePath() + ".rtree"; //$NON-NLS-1$
	}

	public String getRTreeTransportStopsFileName() {
		return mapFile.getAbsolutePath() + ".trans"; //$NON-NLS-1$
	}

	public String getRTreeTransportStopsPackFileName() {
		return mapFile.getAbsolutePath() + ".ptrans"; //$NON-NLS-1$
	}

	public String getRTreeMapIndexPackFileName() {
		return mapFile.getAbsolutePath() + ".prtree"; //$NON-NLS-1$
	}

	public void generateIndexes(File readFile, IProgress progress, IOsmStorageFilter addFilter) throws IOException, SAXException, SQLException{
		generateIndexes(readFile, progress, addFilter, null, null);
	}
	
	public void generateIndexes(File readFile, IProgress progress, IOsmStorageFilter addFilter, MapZooms mapZooms, MapRenderingTypes renderingTypes) 
	throws IOException, SAXException,
			SQLException {
		if(renderingTypes != null){
			this.renderingTypes = renderingTypes;
		}

		if(mapZooms != null){
			this.mapZooms = mapZooms;
		} else {
			mapZooms = this.mapZooms;
		}
		multiPolygonsWays = new Map[mapZooms.size()];
		for (int i = 0; i < multiPolygonsWays.length; i++) {
			multiPolygonsWays[i] = new LinkedHashMap<Long, Set<Integer>>();
		}
		
		// clear previous results and setting variables
		if (readFile != null && regionName == null) {
			int i = readFile.getName().indexOf('.');
			if (i > -1) {
				regionName = Algoritms.capitalizeFirstLetterAndLowercase(readFile.getName().substring(0, i));
			}
		}

		cities.clear();
		cityManager.clear();
		lowLevelWays = -1;
		postalCodeRelations.clear();
		if (normalizeStreets) {
			normalizeDefaultSuffixes = DataExtractionSettings.getSettings().getDefaultSuffixesToNormalizeStreets();
			normalizeSuffixes = DataExtractionSettings.getSettings().getSuffixesToNormalizeStreets();
		}

		// Main generation method
		try {
			// ////////////////////////////////////////////////////////////////////////
			// 1. creating nodes db to fast access for all nodes and simply import all relations, ways, nodes to it
			boolean loadFromPath = dbFile == null || !dialect.databaseFileExists(dbFile);
			if (dbFile == null) {
				dbFile = new File(workingDir, TEMP_NODES_DB);
				// to save space
				if (dialect.databaseFileExists(dbFile)) {
					dialect.removeDatabase(dbFile);
				}
			}
			dbConn = getDatabaseConnection(dbFile.getAbsolutePath());

			int allRelations = 100000;
			int allWays = 1000000;
			int allNodes = 10000000;
			if (loadFromPath) {
				progress.setGeneralProgress("[35 / 100]"); //$NON-NLS-1$
				progress.startTask(Messages.getString("IndexCreator.LOADING_FILE") + readFile.getAbsolutePath(), -1); //$NON-NLS-1$

				OsmDbCreator filter = extractOsmToNodesDB(readFile, progress, addFilter);
				if (filter != null) {
					allNodes = filter.getAllNodes();
					allWays = filter.getAllWays();
					allRelations = filter.getAllRelations();
				}
			}

			pselectNode = dbConn.prepareStatement("select n.latitude, n.longitude, t.skeys, t.value from node n left join tags t on n.id = t.id and t.type = 0 where n.id = ?"); //$NON-NLS-1$
			pselectWay = dbConn.prepareStatement("select w.node, w.ord, t.skeys, t.value, n.latitude, n.longitude " + //$NON-NLS-1$
					"from ways w left join tags t on w.id = t.id and t.type = 1 and w.ord = 0 inner join node n on w.node = n.id " + //$NON-NLS-1$
					"where w.id = ? order by w.ord"); //$NON-NLS-1$
			pselectRelation = dbConn.prepareStatement("select r.member, r.type, r.role, r.ord, t.skeys, t.value " + //$NON-NLS-1$
					"from relations r left join tags t on r.id = t.id and t.type = 2 and r.ord = 0 " + //$NON-NLS-1$
					"where r.id = ? order by r.ord"); //$NON-NLS-1$
			pselectTags = dbConn.prepareStatement("select skeys, value from tags where id = ? and type = ?"); //$NON-NLS-1$

			// do not create temp map file and rtree files
			if (recreateOnlyBinaryFile) {
				mapFile = new File(workingDir, getMapFileName());
				File tempDBMapFile = new File(workingDir, getTempMapDBFileName());
				mapConnection = getDatabaseConnection(tempDBMapFile.getAbsolutePath());
				mapConnection.setAutoCommit(false);
				try {
					if (indexMap) {
						mapTree = new RTree[mapZooms.size()];
						for (int i = 0; i < mapZooms.size(); i++) {
							mapTree[i] = new RTree(getRTreeMapIndexPackFileName() + i);
						}

					}
					if (indexTransport) {
						indexTransportCreator.createRTreeFile(getRTreeTransportStopsPackFileName());
					}
				} catch (RTreeException e) {
					log.error("Error flushing", e); //$NON-NLS-1$
					throw new IOException(e);
				}
			} else {

				// 2. Create index connections and index structure
				createDatabaseIndexesStructure();

				// 3. Processing all entries
				// 3.1 write all cities

				if (indexAddress) {
					progress.setGeneralProgress("[40 / 100]"); //$NON-NLS-1$
					progress.startTask(Messages.getString("IndexCreator.INDEX_CITIES"), allNodes); //$NON-NLS-1$
					if (!loadFromPath) {
						allNodes = iterateOverEntities(progress, EntityType.NODE, allNodes, STEP_CITY_NODES);
					}

					for (City c : cities.values()) {
						DataIndexWriter.writeCity(addressCityStat, pStatements, c, BATCH_SIZE);
					}
					// commit to put all cities
					if (pStatements.get(addressCityStat) > 0) {
						addressCityStat.executeBatch();
						pStatements.put(addressCityStat, 0);
						mapConnection.commit();
					}

				}

				// 3.2 index address relations
				if (indexAddress || indexMap) {
					progress.setGeneralProgress("[30 / 100]"); //$NON-NLS-1$
					progress.startTask(Messages.getString("IndexCreator.PREINDEX_ADRESS_MAP"), allRelations); //$NON-NLS-1$
					allRelations = iterateOverEntities(progress, EntityType.RELATION, allRelations,
							STEP_ADDRESS_RELATIONS_AND_MULTYPOLYGONS);
					if (indexAddress) {
						progress.setGeneralProgress("[40 / 100]"); //$NON-NLS-1$
						progress.startTask(Messages.getString("IndexCreator.PREINDEX_ADRESS_MAP"), allWays); //$NON-NLS-1$
						allWays = iterateOverEntities(progress, EntityType.WAY, allWays, STEP_BORDER_CITY_WAYS);
					}
					
					// commit to put all cities
					if (indexAddress) {
						if (pStatements.get(addressBuildingStat) > 0) {
							addressBuildingStat.executeBatch();
							pStatements.put(addressBuildingStat, 0);
						}
						if (pStatements.get(addressStreetNodeStat) > 0) {
							addressStreetNodeStat.executeBatch();
							pStatements.put(addressStreetNodeStat, 0);
						}
						mapConnection.commit();
					}

				}

				// 3.3 MAIN iterate over all entities
				if (indexPOI || indexAddress || indexMap) {
					progress.setGeneralProgress("[50 / 100]");
					progress.startTask(Messages.getString("IndexCreator.PROCESS_OSM_NODES"), allNodes);
					iterateOverEntities(progress, EntityType.NODE, allNodes, STEP_MAIN);
					progress.setGeneralProgress("[70 / 100]");
					progress.startTask(Messages.getString("IndexCreator.PROCESS_OSM_WAYS"), allWays);
					iterateOverEntities(progress, EntityType.WAY, allWays, STEP_MAIN);
				}
				progress.setGeneralProgress("[85 / 100]");
				progress.startTask(Messages.getString("IndexCreator.PROCESS_OSM_REL"), allRelations);
				iterateOverEntities(progress, EntityType.RELATION, allRelations, STEP_MAIN);
				
				// 3.4 combine all low level ways and simplify them
				if(indexMap){
					progress.setGeneralProgress("[90 / 100]");
					progress.startTask(Messages.getString("IndexCreator.INDEX_LO_LEVEL_WAYS"), lowLevelWays);
					processingLowLevelWays(progress);
				}

				// 3.5 update all postal codes from relations
				if (indexAddress && !postalCodeRelations.isEmpty()) {
					progress.setGeneralProgress("[90 / 100]");
					progress.startTask(Messages.getString("IndexCreator.REGISTER_PCODES"), -1);
					if (pStatements.get(addressBuildingStat) > 0) {
						addressBuildingStat.executeBatch();
						pStatements.put(addressBuildingStat, 0);
						mapConnection.commit();
					}
					processingPostcodes();
				}

				// 4. packing map rtree indexes
				if (indexMap) {
					progress.setGeneralProgress("[90 / 100]"); //$NON-NLS-1$
					progress.startTask(Messages.getString("IndexCreator.PACK_RTREE_MAP"), -1); //$NON-NLS-1$
					for (int i = 0; i < mapZooms.size(); i++) {
						mapTree[i] = packRtreeFile(mapTree[i], getRTreeMapIndexNonPackFileName() + i, getRTreeMapIndexPackFileName() + i);
					}
				}

				if (indexTransport) {
					progress.setGeneralProgress("[90 / 100]"); //$NON-NLS-1$
					progress.startTask(Messages.getString("IndexCreator.PACK_RTREE_TRANSP"), -1); //$NON-NLS-1$
					indexTransportCreator.transportStopsTree = packRtreeFile(indexTransportCreator.transportStopsTree, getRTreeTransportStopsFileName(), getRTreeTransportStopsPackFileName());
				}
			}

			// 5. Writing binary file
			if (indexMap || indexAddress || indexTransport) {
				if (mapFile.exists()) {
					mapFile.delete();
				}
				mapRAFile = new RandomAccessFile(mapFile, "rw");
				BinaryMapIndexWriter writer = new BinaryMapIndexWriter(mapRAFile);
				if (indexMap) {
					progress.setGeneralProgress("[95 of 100]");
					progress.startTask("Writing map index to binary file...", -1);
					closePreparedStatements(mapBinaryStat, mapLowLevelBinaryStat);
					mapConnection.commit();
					writeBinaryMapIndex(writer);
				}

				if (indexAddress) {
					progress.setGeneralProgress("[95 of 100]");
					progress.startTask("Writing address index to binary file...", -1);
					closePreparedStatements(addressCityStat, addressStreetStat, addressStreetNodeStat, addressBuildingStat);
					mapConnection.commit();
					writeBinaryAddressIndex(writer, progress);
				}
				
				
				if (indexTransport) {
					progress.setGeneralProgress("[95 of 100]");
					progress.startTask("Writing transport index to binary file...", -1);
					indexTransportCreator.writeBinaryTransportIndex(writer, regionName, mapConnection);
				}
				progress.finishTask();
				writer.close();
				mapRAFile.close();
				log.info("Finish writing binary file"); //$NON-NLS-1$
			}
		} catch (RuntimeException e) {
			log.error("Log exception", e); //$NON-NLS-1$
			throw e;
		} catch (SQLException e) {
			log.error("Log exception", e); //$NON-NLS-1$
			throw e;
		} catch (IOException e) {
			log.error("Log exception", e); //$NON-NLS-1$
			throw e;
		} catch (SAXException e) {
			log.error("Log exception", e); //$NON-NLS-1$
			throw e;
		} finally {
			try {
				if (pselectNode != null) {
					pselectNode.close();
				}
				if (pselectWay != null) {
					pselectWay.close();
				}
				if (pselectRelation != null) {
					pselectRelation.close();
				}
				if (pselectTags != null) {
					pselectTags.close();
				}
				for (PreparedStatement p : pStatements.keySet()) {
					if (pStatements.get(p) > 0) {
						p.executeBatch();
					}
					p.close();
				}

				if (poiConnection != null) {
					poiConnection.commit();
					poiConnection.close();
					poiConnection = null;
					if (lastModifiedDate != null) {
						poiIndexFile.setLastModified(lastModifiedDate);
					}
				}

				if (mapConnection != null) {
					mapConnection.commit();
					mapConnection.close();
					mapConnection = null;
					File tempDBFile = new File(workingDir, getTempMapDBFileName());
					if (dialect.databaseFileExists(tempDBFile) && deleteDatabaseIndexes) {
						// do not delete it for now
						dialect.removeDatabase(tempDBFile);
					}
				}

				// delete map rtree files
				if (mapTree != null) {
					for (int i = 0; i < mapTree.length; i++) {
						if (mapTree[i] != null) {
							RandomAccessFile file = mapTree[i].getFileHdr().getFile();
							file.close();
						}

					}
					for (int i = 0; i < mapTree.length; i++) {
						File f = new File(getRTreeMapIndexNonPackFileName() + i);
						if (f.exists() && deleteDatabaseIndexes) {
							f.delete();
						}
						f = new File(getRTreeMapIndexPackFileName() + i);
						if (f.exists() && deleteDatabaseIndexes) {
							f.delete();
						}
					}
				}

				// delete transport rtree files
				if (indexTransportCreator.transportStopsTree != null) {
					indexTransportCreator.transportStopsTree.getFileHdr().getFile().close();
					File f = new File(getRTreeTransportStopsFileName());
					if (f.exists() && deleteDatabaseIndexes) {
						f.delete();
					}
					f = new File(getRTreeTransportStopsPackFileName());
					if (f.exists() && deleteDatabaseIndexes) {
						f.delete();
					}
				}

				// do not delete first db connection
				if (dbConn != null) {
					if (DBDialect.H2 == dialect) {
						dbConn.createStatement().execute("SHUTDOWN COMPACT"); //$NON-NLS-1$
					}
					dbConn.close();
				}
				if (deleteOsmDB) {
					if (DBDialect.DERBY == dialect) {
						try {
							DriverManager.getConnection("jdbc:derby:;shutdown=true"); //$NON-NLS-1$
						} catch (SQLException e) {
							// ignore exception
						}
					}
					dialect.removeDatabase(dbFile);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}


	private void processingPostcodes() throws SQLException {
		PreparedStatement pstat = DataIndexWriter.getUpdateBuildingPostcodePreparedStatement(mapConnection);;
		pStatements.put(pstat, 0);
		for (Relation r : postalCodeRelations) {
			String tag = r.getTag(OSMTagKey.POSTAL_CODE);
			for (EntityId l : r.getMemberIds()) {
				pstat.setString(1, tag);
				pstat.setLong(2, l.getId());
				DataIndexWriter.addBatch(pStatements, pstat, BATCH_SIZE);
			}
		}
		if (pStatements.get(pstat) > 0) {
			pstat.executeBatch();
		}
		pStatements.remove(pstat);
	}

	public RTree packRtreeFile(RTree tree, String nonPackFileName, String packFileName) throws IOException {
		try {
			assert rtree.Node.MAX < 50 : "It is better for search performance"; //$NON-NLS-1$
			tree.flush();
			File file = new File(packFileName);
			if (file.exists()) {
				file.delete();
			}
			long rootIndex = tree.getFileHdr().getRootIndex();
			if (!nodeIsLastSubTree(tree, rootIndex)) {
				// there is a bug for small files in packing method
				new Pack().packTree(tree, packFileName);
				tree.getFileHdr().getFile().close();
				file = new File(nonPackFileName);
				file.delete();

				return new RTree(packFileName);
			}
		} catch (RTreeException e) {
			log.error("Error flushing", e); //$NON-NLS-1$
			throw new IOException(e);
		}
		return tree;
	}


	private OsmDbCreator extractOsmToNodesDB(File readFile, IProgress progress, IOsmStorageFilter addFilter) throws FileNotFoundException,
			IOException, SQLException, SAXException {
		boolean pbfFile = false;
		InputStream stream = new BufferedInputStream(new FileInputStream(readFile), 8192*4);;
		InputStream streamFile = stream;
		long st = System.currentTimeMillis();
		if (readFile.getName().endsWith(".bz2")) { //$NON-NLS-1$
			if (stream.read() != 'B' || stream.read() != 'Z') {
				throw new RuntimeException("The source stream must start with the characters BZ if it is to be read as a BZip2 stream."); //$NON-NLS-1$
			} else {
				stream = new CBZip2InputStream(stream);
			}
		} else if (readFile.getName().endsWith(".pbf")) { //$NON-NLS-1$
			pbfFile = true;
		}

		OsmBaseStorage storage = new OsmBaseStorage();
		storage.setSupressWarnings(DataExtractionSettings.getSettings().isSupressWarningsForDuplicatedId());
		if (addFilter != null) {
			storage.getFilters().add(addFilter);
		}

		// 1. Loading osm file
		OsmDbCreator filter = new OsmDbCreator(this);
		try {
			// 1 init database to store temporary data
			filter.initDatabase(dialect, dbConn);
			storage.getFilters().add(filter);
			if (pbfFile) {
				storage.parseOSMPbf(stream, progress, false);
			} else {
				storage.parseOSM(stream, progress, streamFile, false);
			}
			filter.finishLoading();
			dbConn.commit();

			if (log.isInfoEnabled()) {
				log.info("File parsed : " + (System.currentTimeMillis() - st)); //$NON-NLS-1$
			}
			progress.finishTask();
			return filter;
		} finally {
			if (log.isInfoEnabled()) {
				log.info("File indexed : " + (System.currentTimeMillis() - st)); //$NON-NLS-1$
			}
		}
	}

	private void createDatabaseIndexesStructure() throws SQLException, IOException {
		// 2.1 create temporary sqlite database to put temporary results to it
		if (indexMap || indexAddress || indexTransport) {
			mapFile = new File(workingDir, getMapFileName());
			// to save space
			mapFile.getParentFile().mkdirs();
			File tempDBMapFile = new File(workingDir, getTempMapDBFileName());
			dialect.removeDatabase(tempDBMapFile);
			mapConnection = getDatabaseConnection(tempDBMapFile.getAbsolutePath());
			mapConnection.setAutoCommit(false);
		}

		// 2.2 create rtree map
		if (indexMap) {
			DataIndexWriter.createMapIndexStructure(mapConnection);
			mapBinaryStat = DataIndexWriter.createStatementMapBinaryInsert(mapConnection);
			mapLowLevelBinaryStat = DataIndexWriter.createStatementLowLevelMapBinaryInsert(mapConnection);
			try {
				mapTree = new RTree[mapZooms.size()];
				for (int i = 0; i < mapZooms.size(); i++) {
					File file = new File(getRTreeMapIndexNonPackFileName() + i);
					if (file.exists()) {
						file.delete();
					}
					mapTree[i] = new RTree(getRTreeMapIndexNonPackFileName() + i);
					// very slow
					// mapTree[i].getFileHdr().setBufferPolicy(true);
				}
			} catch (RTreeException e) {
				throw new IOException(e);
			}
			pStatements.put(mapBinaryStat, 0);
			pStatements.put(mapLowLevelBinaryStat, 0);
		}

		if (indexAddress) {
			DataIndexWriter.createAddressIndexStructure(mapConnection, dialect);
			addressCityStat = DataIndexWriter.getCityInsertPreparedStatement(mapConnection);
			addressStreetStat = DataIndexWriter.getStreetInsertPreparedStatement(mapConnection);
			addressBuildingStat = DataIndexWriter.getBuildingInsertPreparedStatement(mapConnection);
			addressStreetNodeStat = DataIndexWriter.getStreetNodeInsertPreparedStatement(mapConnection);
			addressSearchStreetStat = DataIndexWriter.getSearchStreetPreparedStatement(mapConnection);
			addressSearchBuildingStat = DataIndexWriter.getSearchBuildingPreparedStatement(mapConnection);
			addressSearchStreetNodeStat = DataIndexWriter.getStreeNodeSearchPreparedStatement(mapConnection);

			pStatements.put(addressCityStat, 0);
			pStatements.put(addressStreetStat, 0);
			pStatements.put(addressStreetNodeStat, 0);
			pStatements.put(addressBuildingStat, 0);
			// put search statements to close them after all
			pStatements.put(addressSearchBuildingStat, 0);
			pStatements.put(addressSearchStreetNodeStat, 0);
			pStatements.put(addressSearchStreetStat, 0);

		}

		if (indexPOI) {
			poiIndexFile = new File(workingDir, getPoiFileName());
			// to save space
			if (poiIndexFile.exists()) {
				Algoritms.removeAllFiles(poiIndexFile);
			}
			poiIndexFile.getParentFile().mkdirs();
			// creating nodes db to fast access for all nodes
			poiConnection = getDatabaseConnection(poiIndexFile.getAbsolutePath(), true);
			DataIndexWriter.createPoiIndexStructure(poiConnection, dialect);
			poiPreparedStatement = DataIndexWriter.createStatementAmenityInsert(poiConnection);
			pStatements.put(poiPreparedStatement, 0);
			poiConnection.setAutoCommit(false);
		}

		if (indexTransport) {
			indexTransportCreator.createTransportIndexStructure(mapConnection, dialect, getRTreeTransportStopsFileName(), pStatements);
		}
	}

	protected void closePreparedStatements(PreparedStatement... preparedStatements) throws SQLException {
		for (PreparedStatement p : preparedStatements) {
			if (p != null) {
				p.executeBatch();
				p.close();
				pStatements.remove(p);
			}
		}
	}

	public static void main(String[] args) throws IOException, SAXException, SQLException {
		
		
		long time = System.currentTimeMillis();
		IndexCreator creator = new IndexCreator(new File("/home/victor/projects/OsmAnd/data/osm-gen/")); //$NON-NLS-1$
		creator.setIndexMap(true);
		creator.setIndexAddress(true);
		creator.setIndexPOI(true);
		creator.setIndexTransport(true);
		// for NL
//		creator.setCityAdminLevel("10");

		creator.recreateOnlyBinaryFile = false;
		creator.deleteDatabaseIndexes = true;
				
//		creator.generateIndexes(new File("/home/victor/projects/OsmAnd/download/384/spain.osm.pbf"), 
//				new ConsoleProgressImplementation(1), null, MapZooms.getDefault(), null);
		creator.generateIndexes(new File("/home/victor/projects/OsmAnd/data/osm-maps/minsk_around.osm"), 
				new ConsoleProgressImplementation(1), null, MapZooms.getDefault(), null);
		
		
		
//		creator.setNodesDBFile(new File("e:/Information/OSM maps/osmand/minsk.tmp.odb"));
//		creator.generateIndexes(new File("e:/Information/OSM maps/belarus osm/minsk.osm"), new ConsoleProgressImplementation(3), null, MapZooms.getDefault(), null);
		


		System.out.println("WHOLE GENERATION TIME :  " + (System.currentTimeMillis() - time)); //$NON-NLS-1$
		 System.out.println("COORDINATES_SIZE " + BinaryMapIndexWriter.COORDINATES_SIZE + " count " + BinaryMapIndexWriter.COORDINATES_COUNT); //$NON-NLS-1$ //$NON-NLS-2$
		System.out.println("TYPES_SIZE " + BinaryMapIndexWriter.TYPES_SIZE); //$NON-NLS-1$
		System.out.println("ID_SIZE " + BinaryMapIndexWriter.ID_SIZE); //$NON-NLS-1$
		 System.out.println("- COORD_TYPES_ID SIZE " + (BinaryMapIndexWriter.COORDINATES_SIZE + BinaryMapIndexWriter.TYPES_SIZE + BinaryMapIndexWriter.ID_SIZE)); //$NON-NLS-1$
		System.out.println("- MAP_DATA_SIZE " + BinaryMapIndexWriter.MAP_DATA_SIZE); //$NON-NLS-1$
		System.out.println("- STRING_TABLE_SIZE " + BinaryMapIndexWriter.STRING_TABLE_SIZE); //$NON-NLS-1$
		System.out.println("-- MAP_DATA_AND_STRINGS SIZE " + (BinaryMapIndexWriter.MAP_DATA_SIZE + BinaryMapIndexWriter.STRING_TABLE_SIZE)); //$NON-NLS-1$

	}
}