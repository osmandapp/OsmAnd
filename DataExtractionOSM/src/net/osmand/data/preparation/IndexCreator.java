package net.osmand.data.preparation;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import net.osmand.Algoritms;
import net.osmand.IProgress;
import net.osmand.binary.BinaryMapIndexWriter;
import net.osmand.data.IndexConstants;
import net.osmand.data.preparation.OsmDbAccessor.OsmDbVisitor;
import net.osmand.impl.ConsoleProgressImplementation;
import net.osmand.osm.Entity;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.Relation;
import net.osmand.osm.Entity.EntityId;
import net.osmand.osm.Entity.EntityType;
import net.osmand.osm.io.IOsmStorageFilter;
import net.osmand.osm.io.OsmBaseStorage;
import net.osmand.swing.DataExtractionSettings;
import net.osmand.swing.Messages;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.bzip2.CBZip2InputStream;
import org.xml.sax.SAXException;

import rtree.RTreeException;

/**
 * http://wiki.openstreetmap.org/wiki/OSM_tags_for_routing#Is_inside.2Foutside
 * http://wiki.openstreetmap.org/wiki/Relations/Proposed/Postal_Addresses
 * http://wiki.openstreetmap.org/wiki/Proposed_features/House_numbers/Karlsruhe_Schema#Tags (node, way)
 * 
 * That data extraction has aim, save runtime memory and generate indexes on the fly. It will be longer than load in memory (needed part)
 * and save into index.
 */
public class IndexCreator {
	private static final Log log = LogFactory.getLog(IndexCreator.class);

	// ONLY derby.jar needed for derby dialect
	private final DBDialect dialect = DBDialect.SQLITE;

	public static final int BATCH_SIZE = 5000;
	public static final int BATCH_SIZE_OSM = 10000;
	public static final String TEMP_NODES_DB = "nodes.tmp.odb";

	public static final int STEP_MAIN = 4;

	private File workingDir = null;

	private boolean indexMap;
	private boolean indexPOI;
	private boolean indexTransport;
	private boolean indexAddress;

	private boolean normalizeStreets = true; // true by default
	private boolean saveAddressWays = true; // true by default
	private int zoomWaySmothness = 2;

	private String regionName;
	private String poiFileName = null;
	private String mapFileName = null;
	private Long lastModifiedDate = null;
	
	

	
	private IndexTransportCreator indexTransportCreator;
	private IndexPoiCreator indexPoiCreator;
	private IndexAddressCreator indexAddressCreator;
	private IndexVectorMapCreator indexMapCreator;
	private OsmDbAccessor accessor;
	// constants to start process from the middle and save temporary results
	private boolean recreateOnlyBinaryFile = false; // false;
	private boolean deleteOsmDB = false;
	private boolean deleteDatabaseIndexes = true;

	private Connection dbConn;
	private File dbFile;

	private File mapFile;
	private RandomAccessFile mapRAFile;
	private Connection mapConnection;
	private String cityAdminLevel = "8";
	

	public IndexCreator(File workingDir) {
		this.workingDir = workingDir;
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
	
	public void setZoomWaySmothness(int zoomWaySmothness) {
		this.zoomWaySmothness = zoomWaySmothness;
	}

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
		return dialect.getDatabaseConnection(fileName, log);
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
	
	public String getCityAdminLevel() {
		return cityAdminLevel;
	}
	
	public void setCityAdminLevel(String cityAdminLevel) {
		this.cityAdminLevel = cityAdminLevel;
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
	
	/* ***** END OF GETTERS/SETTERS ***** */
	public void generateIndexes(File readFile, IProgress progress, IOsmStorageFilter addFilter) throws IOException, SAXException, SQLException{
		generateIndexes(readFile, progress, addFilter, null, null);
	}
	
	private void iterateMainEntity(Entity e, OsmDbAccessorContext ctx) throws SQLException {
		if (indexPOI) {
			indexPoiCreator.iterateEntity(e, ctx);
		}
		if (indexTransport) {
			indexTransportCreator.visitEntityMainStep(e, ctx);
		}
		if (indexMap) {
			indexMapCreator.iterateMainEntity(e, ctx);
		}
		if (indexAddress) {
			indexAddressCreator.iterateMainEntity(e, ctx);
		}
	}

	private OsmDbCreator extractOsmToNodesDB(File readFile, IProgress progress, IOsmStorageFilter addFilter) throws FileNotFoundException,
			IOException, SQLException, SAXException {
		boolean pbfFile = false;
		InputStream stream = new BufferedInputStream(new FileInputStream(readFile), 8192 * 4);
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
		
		storage.getFilters().add(new IOsmStorageFilter() {
			
			@Override
			public boolean acceptEntityToLoad(OsmBaseStorage storage, EntityId entityId, Entity entity) {
				indexAddressCreator.registerCityIfNeeded(entity);
				// accept to allow db creator parse it
				return true;
			}
		});

		// 1. Loading osm file
		OsmDbCreator dbCreator = new OsmDbCreator(this);
		try {
			progress.setGeneralProgress("[35 / 100]"); //$NON-NLS-1$
			progress.startTask(Messages.getString("IndexCreator.LOADING_FILE") + readFile.getAbsolutePath(), -1); //$NON-NLS-1$
			// 1 init database to store temporary data
			dbCreator.initDatabase(dialect, dbConn);
			storage.getFilters().add(dbCreator);
			if (pbfFile) {
				storage.parseOSMPbf(stream, progress, false);
			} else {
				storage.parseOSM(stream, progress, streamFile, false);
			}
			dbCreator.finishLoading();
			dbConn.commit();

			if (log.isInfoEnabled()) {
				log.info("File parsed : " + (System.currentTimeMillis() - st)); //$NON-NLS-1$
			}
			progress.finishTask();
			return dbCreator;
		} finally {
			if (log.isInfoEnabled()) {
				log.info("File indexed : " + (System.currentTimeMillis() - st)); //$NON-NLS-1$
			}
		}
	}
	
	private boolean createPlainOsmDb(IProgress progress, File readFile, IOsmStorageFilter addFilter) throws SQLException, FileNotFoundException, IOException, SAXException{
		// initialize db file
		boolean loadFromExistingFile = dbFile != null && dialect.databaseFileExists(dbFile);
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
		if (!loadFromExistingFile) {
			OsmDbCreator dbCreator = extractOsmToNodesDB(readFile, progress, addFilter);
			if (dbCreator != null) {
				allNodes = dbCreator.getAllNodes();
				allWays = dbCreator.getAllWays();
				allRelations = dbCreator.getAllRelations();
			}
		}
		accessor.initDatabase(dbConn, allNodes, allWays, allRelations);
		return loadFromExistingFile;
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
			indexMapCreator.createDatabaseStructure(mapConnection, dialect, getRTreeMapIndexNonPackFileName());
		}
		if (indexAddress) {
			indexAddressCreator.createDatabaseStructure(mapConnection, dialect);
		}
		if (indexPOI) {
			indexPoiCreator.createDatabaseStructure(new File(workingDir, getPoiFileName()));
		}
		if (indexTransport) {
			indexTransportCreator.createDatabaseStructure(mapConnection, dialect, getRTreeTransportStopsFileName());
		}
	}

	
	public void generateIndexes(File readFile, IProgress progress, IOsmStorageFilter addFilter, MapZooms mapZooms,
			MapRenderingTypes renderingTypes) throws IOException, SAXException, SQLException {
		if (renderingTypes == null) {
			renderingTypes = MapRenderingTypes.getDefault();
		}
		if (mapZooms == null) {
			mapZooms = MapZooms.getDefault();
		}

		// clear previous results and setting variables
		if (readFile != null && regionName == null) {
			int i = readFile.getName().indexOf('.');
			if (i > -1) {
				regionName = Algoritms.capitalizeFirstLetterAndLowercase(readFile.getName().substring(0, i));
			}
		}
		this.indexTransportCreator = new IndexTransportCreator();
		this.indexPoiCreator = new IndexPoiCreator();
		this.indexAddressCreator = new IndexAddressCreator();
		this.indexMapCreator = new IndexVectorMapCreator();
		this.accessor = new OsmDbAccessor();
		

		indexMapCreator.initSettings(mapZooms, renderingTypes, zoomWaySmothness);

		// init address
		String[] normalizeDefaultSuffixes = null;
		String[] normalizeSuffixes = null;
		if (normalizeStreets) {
			normalizeDefaultSuffixes = DataExtractionSettings.getSettings().getDefaultSuffixesToNormalizeStreets();
			normalizeSuffixes = DataExtractionSettings.getSettings().getSuffixesToNormalizeStreets();
		}
		indexAddressCreator.initSettings(normalizeStreets, normalizeDefaultSuffixes, normalizeSuffixes, saveAddressWays, cityAdminLevel);

		// Main generation method
		try {
			// ////////////////////////////////////////////////////////////////////////
			// 1. creating nodes db to fast access for all nodes and simply import all relations, ways, nodes to it
			boolean loadFromExistingFile = createPlainOsmDb(progress, readFile, addFilter);
			
			// do not create temp map file and rtree files
			if (recreateOnlyBinaryFile) {
				mapFile = new File(workingDir, getMapFileName());
				File tempDBMapFile = new File(workingDir, getTempMapDBFileName());
				mapConnection = getDatabaseConnection(tempDBMapFile.getAbsolutePath());
				mapConnection.setAutoCommit(false);
				try {
					if (indexMap) {
						indexMapCreator.createRTreeFiles(getRTreeMapIndexPackFileName());
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
					progress.startTask(Messages.getString("IndexCreator.INDEX_CITIES"), accessor.getAllNodes()); //$NON-NLS-1$
					if (loadFromExistingFile) {
						// load cities names
						accessor.iterateOverEntities(progress, EntityType.NODE,  new OsmDbVisitor() {
							@Override
							public void iterateEntity(Entity e, OsmDbAccessorContext ctx) {
								indexAddressCreator.registerCityIfNeeded(e);
							}
						});
					}
					indexAddressCreator.writeCitiesIntoDb();
				}

				// 3.2 index address relations
				if (indexAddress || indexMap) {
					progress.setGeneralProgress("[30 / 100]"); //$NON-NLS-1$
					progress.startTask(Messages.getString("IndexCreator.PREINDEX_ADRESS_MAP"), accessor.getAllRelations()); //$NON-NLS-1$
					accessor.iterateOverEntities(progress, EntityType.RELATION, new OsmDbVisitor() {
						@Override
						public void iterateEntity(Entity e, OsmDbAccessorContext ctx) throws SQLException {
							if (indexAddress) {
								indexAddressCreator.indexAddressRelation((Relation) e, ctx);
								indexAddressCreator.indexBoundariesRelation((Relation) e, ctx);
							}
							if (indexMap) {
								indexMapCreator.indexMapRelationsAndMultiPolygons(e, ctx);
							}
						}
					});
					if (indexAddress) {
						progress.setGeneralProgress("[40 / 100]"); //$NON-NLS-1$
						progress.startTask(Messages.getString("IndexCreator.PREINDEX_ADRESS_MAP"), accessor.getAllWays()); //$NON-NLS-1$
						accessor.iterateOverEntities(progress, EntityType.WAY, new OsmDbVisitor() {
							@Override
							public void iterateEntity(Entity e, OsmDbAccessorContext ctx) throws SQLException {
								indexAddressCreator.indexBoundariesRelation(e, ctx);
							}
						});

						indexAddressCreator.commitToPutAllCities();
					}

				}

				// 3.3 MAIN iterate over all entities
				if (indexPOI || indexAddress || indexMap) {
					progress.setGeneralProgress("[50 / 100]");
					progress.startTask(Messages.getString("IndexCreator.PROCESS_OSM_NODES"), accessor.getAllNodes());
					accessor.iterateOverEntities(progress, EntityType.NODE, new OsmDbVisitor() {
						@Override
						public void iterateEntity(Entity e, OsmDbAccessorContext ctx) throws SQLException {
							iterateMainEntity(e, ctx);
						}
					});
					progress.setGeneralProgress("[70 / 100]");
					progress.startTask(Messages.getString("IndexCreator.PROCESS_OSM_WAYS"), accessor.getAllWays());
					accessor.iterateOverEntities(progress, EntityType.WAY, new OsmDbVisitor() {
						@Override
						public void iterateEntity(Entity e, OsmDbAccessorContext ctx) throws SQLException {
							iterateMainEntity(e, ctx);
						}
					});
				}
				progress.setGeneralProgress("[85 / 100]");
				progress.startTask(Messages.getString("IndexCreator.PROCESS_OSM_REL"), accessor.getAllRelations());
				accessor.iterateOverEntities(progress, EntityType.RELATION, new OsmDbVisitor() {
					@Override
					public void iterateEntity(Entity e, OsmDbAccessorContext ctx) throws SQLException {
						iterateMainEntity(e, ctx);
					}
				});

				// 3.4 combine all low level ways and simplify them
				if (indexMap) {
					progress.setGeneralProgress("[90 / 100]");
					progress.startTask(Messages.getString("IndexCreator.INDEX_LO_LEVEL_WAYS"), indexMapCreator.getLowLevelWays());
					indexMapCreator.processingLowLevelWays(progress);
				}

				// 3.5 update all postal codes from relations
				if (indexAddress) {
					progress.setGeneralProgress("[90 / 100]");
					progress.startTask(Messages.getString("IndexCreator.REGISTER_PCODES"), -1);
					indexAddressCreator.processingPostcodes();
				}

				// 4. packing map rtree indexes
				if (indexMap) {
					progress.setGeneralProgress("[90 / 100]"); //$NON-NLS-1$
					progress.startTask(Messages.getString("IndexCreator.PACK_RTREE_MAP"), -1); //$NON-NLS-1$
					indexMapCreator.packRtreeFiles(getRTreeMapIndexNonPackFileName(), getRTreeMapIndexPackFileName());
				}

				if (indexTransport) {
					progress.setGeneralProgress("[90 / 100]"); //$NON-NLS-1$
					progress.startTask(Messages.getString("IndexCreator.PACK_RTREE_TRANSP"), -1); //$NON-NLS-1$
					indexTransportCreator.packRTree(getRTreeTransportStopsFileName(), getRTreeTransportStopsPackFileName());
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
					indexMapCreator.writeBinaryMapIndex(writer, regionName);
				}

				if (indexAddress) {
					progress.setGeneralProgress("[95 of 100]");
					progress.startTask("Writing address index to binary file...", -1);
					indexAddressCreator.writeBinaryAddressIndex(writer, regionName, progress);
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
				accessor.closeReadingConnection();

				indexPoiCreator.commitAndClosePoiFile(lastModifiedDate);
				indexAddressCreator.closeAllPreparedStatements();
				indexTransportCreator.commitAndCloseFiles(getRTreeTransportStopsFileName(), getRTreeTransportStopsPackFileName(),
						deleteDatabaseIndexes);
				indexMapCreator.commitAndCloseFiles(getRTreeMapIndexNonPackFileName(), getRTreeMapIndexPackFileName(),
						deleteDatabaseIndexes);

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


	public static void main(String[] args) throws IOException, SAXException, SQLException {
		
		long time = System.currentTimeMillis();
		IndexCreator creator = new IndexCreator(new File("/home/victor/projects/OsmAnd/data/osm-gen/")); //$NON-NLS-1$
		creator.setIndexMap(true);
//		creator.setIndexAddress(true);
//		creator.setIndexPOI(true);
//		creator.setIndexTransport(true);
		// for NL
//		creator.setCityAdminLevel("10");

		creator.recreateOnlyBinaryFile = false;
		creator.deleteDatabaseIndexes = true;
//		creator.deleteOsmDB = true;
				
		creator.setZoomWaySmothness(2);
		MapRenderingTypes rt = new MapRenderingTypes("/home/victor/projects/OsmAnd/data/testdata/roads_rendering_types.xml");
		MapZooms zooms = MapZooms.parseZooms("15-");
		creator.setNodesDBFile(new File("/home/victor/projects/OsmAnd/data/osm-gen/nodes.tmp.odb"));
		creator.generateIndexes(new File("/home/victor/projects/OsmAnd/data/belarus-osm/belarus.osm.pbf"),
				new ConsoleProgressImplementation(1), null, zooms, rt);
		
//		creator.setNodesDBFile(new File("/home/victor/projects/OsmAnd/data/osm-gen/nodes3.tmp.odb"));
//		creator.generateIndexes(new File("/home/victor/projects/OsmAnd/data/osm-maps/stadion-dynamo.osm"),
//				new ConsoleProgressImplementation(1), null, MapZooms.getDefault(), null);
		
		
		
		
		
		// world generation
//		MapZooms mapZooms = new MapZooms();
//		MapZoomPair pair1 = new MapZooms.MapZoomPair(1, 3);
//		MapZoomPair pair2 = new MapZooms.MapZoomPair(4, 5);
//		MapZoomPair pair3 = new MapZooms.MapZoomPair(6, 7);
//		
//		mapZooms.setLevels(Arrays.asList(pair1, pair2, pair3));
//		// for coastline
//		creator.setZoomWaySmothness(2);
//		creator.generateIndexes(new File(
//				"/home/victor/projects/OsmAnd/download/basemap/basemap_1.osm"
//				), 
//				new ConsoleProgressImplementation(1), null, mapZooms, null);
		
		

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