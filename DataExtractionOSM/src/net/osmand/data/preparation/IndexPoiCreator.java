package net.osmand.data.preparation;

import gnu.trove.list.array.TIntArrayList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.osmand.Algoritms;
import net.osmand.IProgress;
import net.osmand.binary.BinaryMapIndexWriter;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.data.IndexConstants;
import net.osmand.impl.ConsoleProgressImplementation;
import net.osmand.osm.Entity;
import net.osmand.osm.MapUtils;
import net.osmand.osm.OSMSettings.OSMTagKey;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class IndexPoiCreator extends AbstractIndexPartCreator {

	private static final Log log = LogFactory.getLog(IndexPoiCreator.class);

	private Connection poiConnection;
	private File poiIndexFile;
	private PreparedStatement poiPreparedStatement;
	private static final int ZOOM_TO_SAVE_END = 15;
	private static final int ZOOM_TO_SAVE_START = 6;
	private static final int SHIFT_BYTES_CATEGORY = 7;

	private List<Amenity> tempAmenityList = new ArrayList<Amenity>();

	public IndexPoiCreator() {
	}

	public void iterateEntity(Entity e, OsmDbAccessorContext ctx) throws SQLException {
		tempAmenityList.clear();
		tempAmenityList = Amenity.parseAmenities(e, tempAmenityList);
		if (!tempAmenityList.isEmpty() && poiPreparedStatement != null) {
			// load data for way (location etc...)
			ctx.loadEntityData(e);
			for (Amenity a : tempAmenityList) {
				// do not add that check because it is too much printing for batch creation
				// by statistic < 1% creates maps manually
				// checkEntity(e);
				a.setEntity(e);
				if (a.getLocation() != null) {
					// do not convert english name
					// convertEnglishName(a);
					insertAmenityIntoPoi(a);
				}
			}
		}
	}

	public void commitAndClosePoiFile(Long lastModifiedDate) throws SQLException {
		closeAllPreparedStatements();
		if (poiConnection != null) {
			poiConnection.commit();
			poiConnection.close();
			poiConnection = null;
			if (lastModifiedDate != null) {
				poiIndexFile.setLastModified(lastModifiedDate);
			}
		}
	}

	private void checkEntity(Entity e) {
		String name = e.getTag(OSMTagKey.NAME);
		if (name == null) {
			String msg = "";
			Collection<String> keys = e.getTagKeySet();
			int cnt = 0;
			for (Iterator<String> iter = keys.iterator(); iter.hasNext();) {
				String key = iter.next();
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

	private void insertAmenityIntoPoi(Amenity amenity) throws SQLException {
		assert IndexConstants.POI_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$

		poiPreparedStatement.setLong(1, amenity.getId());
		poiPreparedStatement.setInt(2, MapUtils.get31TileNumberX(amenity.getLocation().getLongitude()));
		poiPreparedStatement.setInt(3, MapUtils.get31TileNumberY(amenity.getLocation().getLatitude()));
		poiPreparedStatement.setString(4, amenity.getEnName());
		poiPreparedStatement.setString(5, amenity.getName());
		poiPreparedStatement.setString(6, AmenityType.valueToString(amenity.getType()));
		poiPreparedStatement.setString(7, amenity.getSubType());
		poiPreparedStatement.setString(8, amenity.getOpeningHours());
		poiPreparedStatement.setString(9, amenity.getSite());
		poiPreparedStatement.setString(10, amenity.getPhone());
		addBatch(poiPreparedStatement);
	}

	public void createDatabaseStructure(File poiIndexFile) throws SQLException {
		this.poiIndexFile = poiIndexFile;
		// delete previous file to save space
		if (poiIndexFile.exists()) {
			Algoritms.removeAllFiles(poiIndexFile);
		}
		poiIndexFile.getParentFile().mkdirs();
		// creating connection
		poiConnection = (Connection) DBDialect.SQLITE.getDatabaseConnection(poiIndexFile.getAbsolutePath(), log);

		// create database structure
		Statement stat = poiConnection.createStatement();
		stat.executeUpdate("create table " + IndexConstants.POI_TABLE + //$NON-NLS-1$
				"(id bigint, x int, y int, name_en varchar(1024), name varchar(1024), "
				+ "type varchar(1024), subtype varchar(1024), opening_hours varchar(1024), phone varchar(1024), site varchar(1024),"
				+ "primary key(id, type, subtype))");
		stat.executeUpdate("create index poi_loc on poi (x, y, type, subtype)");
		stat.executeUpdate("create index poi_id on poi (id, type, subtype)");
		stat.execute("PRAGMA user_version = " + IndexConstants.POI_TABLE_VERSION); //$NON-NLS-1$
		stat.close();

		// create prepared statment
		poiPreparedStatement = poiConnection
				.prepareStatement("INSERT INTO " + IndexConstants.POI_TABLE + "(id, x, y, name_en, name, type, subtype, opening_hours, site, phone) " + //$NON-NLS-1$//$NON-NLS-2$
						"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		pStatements.put(poiPreparedStatement, 0);

		poiConnection.setAutoCommit(false);
	}

	private void buildTypeIds(String category, String subcategory, Map<String, Map<String, Integer>> categories,
			Map<String, Integer> catIndexes, TIntArrayList types) {
		types.clear();
		Map<String, Integer> map = categories.get(category);
		if (map == null) {
			throw new IllegalArgumentException("Unknown category " + category);
		}
		int catInd = catIndexes.get(category);
		if (subcategory.contains(";") || subcategory.contains(",")) {
			String[] split = subcategory.split(",|;");
			for (String sub : split) {
				sub = sub.trim();
				Integer subcatInd = map.get(sub);
				if (subcatInd == null) {
					throw new IllegalArgumentException("Unknown subcategory " + sub + " category " + category);
				}
				types.add((subcatInd << SHIFT_BYTES_CATEGORY) | catInd);
			}
		} else {
			subcategory = subcategory.trim();
			Integer subcatInd = map.get(subcategory);
			if (subcatInd == null) {
				throw new IllegalArgumentException("Unknown subcategory " + subcategory + " category " + category);
			}
			types.add((subcatInd << SHIFT_BYTES_CATEGORY) | catInd);
		}
	}

	public void writeBinaryPoiIndex(BinaryMapIndexWriter writer, String regionName, IProgress progress) throws SQLException, IOException {
		if (poiPreparedStatement != null) {
			closePreparedStatements(poiPreparedStatement);
		}
		poiConnection.commit();
		Collator collator = Collator.getInstance();
		collator.setStrength(Collator.PRIMARY);
		ResultSet rs = poiConnection.createStatement().executeQuery("SELECT DISTINCT type, subtype FROM poi");
		Map<String, Map<String, Integer>> categories = new LinkedHashMap<String, Map<String, Integer>>();
		while (rs.next()) {
			String category = rs.getString(1);
			String subcategory = rs.getString(2).trim();
			if (!categories.containsKey(category)) {
				categories.put(category, new TreeMap<String, Integer>(collator));
			}
			if (subcategory.contains(";") || subcategory.contains(",")) {
				String[] split = subcategory.split(",|;");
				for (String sub : split) {
					categories.get(category).put(sub.trim(), 0);
				}
			} else {
				categories.get(category).put(subcategory.trim(), 0);
			}
		}
		Statement stat = rs.getStatement();
		rs.close();
		stat.close();

		// 1. write header
		long startFpPoiIndex = writer.startWritePOIIndex(regionName);

		// 2. write categories table
		Map<String, Integer> catIndexes = writer.writePOICategoriesTable(categories);

		// 3. write boxes
		String selectZm = (31 - ZOOM_TO_SAVE_END) + "";
		rs = poiConnection.createStatement().executeQuery("SELECT DISTINCT x>>" + selectZm + ", y>>" + selectZm + " from poi");
		Tree<Long> rootZoomsTree = new Tree<Long>();
		int zoomToStart = ZOOM_TO_SAVE_START;
		while (rs.next()) {
			int x = rs.getInt(1);
			int y = rs.getInt(2);
			Tree<Long> prevTree = rootZoomsTree;
			for (int i = zoomToStart; i <= ZOOM_TO_SAVE_END; i++) {
				int shift = ZOOM_TO_SAVE_END - i;
				long l = (((long) x >> shift) << 31) | ((long) y >> shift);

				Tree<Long> subtree = prevTree.getSubtreeByNode(l);
				if (subtree == null) {
					subtree = new Tree<Long>();
					subtree.setNode(l);
					prevTree.addSubTree(subtree);
				}
				prevTree = subtree;
			}
		}
		int level = 0;
		for (; level < (ZOOM_TO_SAVE_END - zoomToStart); level++) {
			int subtrees = rootZoomsTree.getSubTreesOnLevel(level);
			if (subtrees > 8) {
				level--;
				break;
			}
		}
		if (level > 0) {
			rootZoomsTree.extractChildrenFromLevel(level);
			zoomToStart = zoomToStart + level;
		}

		// write tree using stack
		Map<Long, Long> fpToWriteSeeks = new LinkedHashMap<Long, Long>();
		for (Tree<Long> subs : rootZoomsTree.getSubtrees()) {
			writePoiBoxes(writer, subs, zoomToStart, fpToWriteSeeks);
		}

		stat = rs.getStatement();
		rs.close();
		stat.close();

		// 4. write poi data
		PreparedStatement prepareStatement = poiConnection
				.prepareStatement("SELECT id, x, y, name_en, name, type, subtype, opening_hours, site, phone from poi "
						+ "where x >= ? AND x < ? AND y >= ? AND y < ?");
		TIntArrayList types = new TIntArrayList();
		for (Map.Entry<Long, Long> entry : fpToWriteSeeks.entrySet()) {
			long l = entry.getKey();
			int z = ZOOM_TO_SAVE_END;
			int x = (int) (l >> 31);
			int y = (int) (l & ((1 << 31) - 1));
			writer.startWritePoiData(z, x, y, startFpPoiIndex, entry.getValue());

			prepareStatement.setInt(1, x << (31 - z));
			prepareStatement.setInt(2, (x + 1) << (31 - z));
			prepareStatement.setInt(3, y << (31 - z));
			prepareStatement.setInt(4, (y + 1) << (31 - z));
			rs = prepareStatement.executeQuery();
			while (rs.next()) {
				long id = rs.getLong(1);
				int x31 = rs.getInt(2);
				int y31 = rs.getInt(3);
				int x24shift = (x31 >> 7) - (x << (24 - z));
				int y24shift = (y31 >> 7) - (y << (24 - z));
				String nameEn = rs.getString(4);
				String name = rs.getString(5);
				String type = rs.getString(6);
				String subtype = rs.getString(7);
				buildTypeIds(type, subtype, categories, catIndexes, types);

				String openingHours = rs.getString(8);
				String site = rs.getString(9);
				String phone = rs.getString(10);

				writer.writePoiDataAtom(id, x24shift, y24shift, nameEn, name, types, openingHours, site, phone);

			}
			writer.endWritePoiData();
			rs.close();

		}

		prepareStatement.close();

		writer.endWritePOIIndex();

	}

	private void writePoiBoxes(BinaryMapIndexWriter writer, Tree<Long> tree, int zoom, Map<Long, Long> fpToWriteSeeks) throws IOException {
		long l = tree.getNode();
		int x = (int) (l >> 31);
		int y = (int) (l & ((1 << 31) - 1));
		long fp = writer.startWritePoiBox(zoom, x, y);
		if (zoom < ZOOM_TO_SAVE_END) {
			for (Tree<Long> subTree : tree.getSubtrees()) {
				writePoiBoxes(writer, subTree, zoom + 1, fpToWriteSeeks);
			}
		} else {
			fpToWriteSeeks.put(l, fp);
		}
		writer.endWritePoiBox();
	}

	private static class Tree<T> {

		private T node;
		private List<Tree<T>> subtrees = null;

		public List<Tree<T>> getSubtrees() {
			if (subtrees == null) {
				subtrees = new ArrayList<Tree<T>>();
			}
			return subtrees;
		}

		public void addSubTree(Tree<T> t) {
			getSubtrees().add(t);
		}

		public T getNode() {
			return node;
		}

		public void setNode(T node) {
			this.node = node;
		}

		public void extractChildrenFromLevel(int level) {
			List<Tree<T>> list = new ArrayList<Tree<T>>();
			collectChildrenFromLevel(list, level);
			subtrees = list;
		}

		public void collectChildrenFromLevel(List<Tree<T>> list, int level) {
			if (level == 0) {
				if (subtrees != null) {
					list.addAll(subtrees);
				}
			} else if (subtrees != null) {
				for (Tree<T> sub : subtrees) {
					sub.collectChildrenFromLevel(list, level - 1);
				}

			}

		}

		public int getSubTreesOnLevel(int level) {
			if (level == 0) {
				if (subtrees == null) {
					return 0;
				} else {
					return subtrees.size();
				}
			} else {
				int sum = 0;
				for (Tree<T> t : subtrees) {
					sum += t.getSubTreesOnLevel(level - 1);
				}
				return sum;
			}
		}

		public Tree<T> getSubtreeByNode(T node) {
			if (subtrees == null) {
				return null;
			}
			for (Tree<T> s : subtrees) {
				if (node.equals(s.getNode())) {
					return s;
				}
			}
			return null;
		}
	}

	public static void main(String[] args) throws SQLException, FileNotFoundException, IOException {
		long time = System.currentTimeMillis();
		IndexPoiCreator poiCreator = new IndexPoiCreator();
		poiCreator.poiConnection = (Connection) DBDialect.SQLITE.getDatabaseConnection(
				"/home/victor/projects/OsmAnd/data/osm-gen/POI/Ru-mow.poi.odb", log);
		BinaryMapIndexWriter writer = new BinaryMapIndexWriter(new RandomAccessFile(
				"/home/victor/projects/OsmAnd/data/osm-gen/POI/Test-Ru.poi.obf", "rw"));
		poiCreator.poiConnection.setAutoCommit(false);
		poiCreator.writeBinaryPoiIndex(writer, "Ru-mow", new ConsoleProgressImplementation());
		writer.close();
		System.out.println("TIME " + (System.currentTimeMillis() - time));
	}

}
