package net.osmand.data.preparation;

import gnu.trove.list.array.TIntArrayList;

import java.io.File;
import java.io.IOException;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.osmand.Algoritms;
import net.osmand.IProgress;
import net.osmand.binary.BinaryMapPoiReaderAdapter;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.data.IndexConstants;
import net.osmand.impl.ConsoleProgressImplementation;
import net.osmand.osm.Entity;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.MapUtils;
import net.osmand.osm.OSMSettings.OSMTagKey;
import net.sf.junidecode.Junidecode;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class IndexPoiCreator extends AbstractIndexPartCreator {

	private static final Log log = LogFactory.getLog(IndexPoiCreator.class);

	private Connection poiConnection;
	private File poiIndexFile;
	private PreparedStatement poiPreparedStatement;
	private static final int ZOOM_TO_SAVE_END = 16;
	private static final int ZOOM_TO_SAVE_START = 6;
	private static final int ZOOM_TO_WRITE_CATEGORIES_START = 12;
	private static final int ZOOM_TO_WRITE_CATEGORIES_END = 16;
	private static final int CHARACTERS_TO_BUILD = 4;
	private boolean useInMemoryCreator = true; 
	

	private List<Amenity> tempAmenityList = new ArrayList<Amenity>();

	private final MapRenderingTypes renderingTypes;

	public IndexPoiCreator(MapRenderingTypes renderingTypes) {
		this.renderingTypes = renderingTypes;
	}

	public void iterateEntity(Entity e, OsmDbAccessorContext ctx) throws SQLException {
		tempAmenityList.clear();
		tempAmenityList = Amenity.parseAmenities(renderingTypes, e, tempAmenityList);
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
	
	public void removePoiFile(){
		Algoritms.removeAllFiles(poiIndexFile);
	}

	public void checkEntity(Entity e) {
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
		poiPreparedStatement.setString(11, amenity.getDescription());
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
				" (id bigint, x int, y int, name_en varchar(1024), name varchar(1024), "
				+ "type varchar(1024), subtype varchar(1024), opening_hours varchar(1024), phone varchar(1024), site varchar(1024), description varchar(4096), "
				+ "primary key(id, type, subtype))");
		stat.executeUpdate("create index poi_loc on poi (x, y, type, subtype)");
		stat.executeUpdate("create index poi_id on poi (id, type, subtype)");
		stat.execute("PRAGMA user_version = " + IndexConstants.POI_TABLE_VERSION); //$NON-NLS-1$
		stat.close();

		// create prepared statment
		poiPreparedStatement = poiConnection
				.prepareStatement("INSERT INTO " + IndexConstants.POI_TABLE + "(id, x, y, name_en, name, type, subtype, opening_hours, site, phone, description) " + //$NON-NLS-1$//$NON-NLS-2$
						"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		pStatements.put(poiPreparedStatement, 0);

		poiConnection.setAutoCommit(false);
	}

	private void buildTypeIds(String category, String subcategory, Map<String, Map<String, Integer>> categories,
			Map<String, Integer> catIndexes, TIntArrayList types) {
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
				types.add((subcatInd << BinaryMapPoiReaderAdapter.SHIFT_BITS_CATEGORY) | catInd);
			}
		} else {
			subcategory = subcategory.trim();
			Integer subcatInd = map.get(subcategory);
			if (subcatInd == null) {
				throw new IllegalArgumentException("Unknown subcategory " + subcategory + " category " + category);
			}
			types.add((subcatInd << BinaryMapPoiReaderAdapter.SHIFT_BITS_CATEGORY) | catInd);
		}
	}
	
	public void writeBinaryPoiIndex(BinaryMapIndexWriter writer, String regionName, IProgress progress) throws SQLException, IOException {
		if (poiPreparedStatement != null) {
			closePreparedStatements(poiPreparedStatement);
		}
		poiConnection.commit();
		Collator collator = Collator.getInstance();
		collator.setStrength(Collator.PRIMARY);
		
		Map<String, Set<PoiTileBox>> namesIndex = new TreeMap<String, Set<PoiTileBox>>();
		
		// 0. process all entities
		ResultSet rs;
		if(useInMemoryCreator) {
			rs = poiConnection.createStatement().executeQuery("SELECT x,y,name,name_en,type,subtype,id,opening_hours,site, phone, description from poi");
		} else {
			rs = poiConnection.createStatement().executeQuery("SELECT x,y,name,name_en,type,subtype from poi");
		}
		int zoomToStart = ZOOM_TO_SAVE_START;
		Tree<PoiTileBox> rootZoomsTree = new Tree<PoiTileBox>();
		rootZoomsTree.setNode(new PoiTileBox());
		int minX = Integer.MAX_VALUE;
		int maxX = 0;
		int minY = Integer.MAX_VALUE;
		int maxY = 0;
		int count = 0;
		ConsoleProgressImplementation console = new ConsoleProgressImplementation();
		console.startWork(1000000);
		while (rs.next()) {
			int x = rs.getInt(1);
			int y = rs.getInt(2);
			minX = Math.min(x, minX);
			maxX = Math.max(x, maxX);
			minY = Math.min(y, minY);
			maxY = Math.max(y, maxY);
			if(count++ > 10000){
				count = 0;
				console.progress(10000);
			}

			String name = rs.getString(3);
			String nameEn = rs.getString(4);
			String type = rs.getString(5);
			String subtype = rs.getString(6);

			Tree<PoiTileBox> prevTree = rootZoomsTree;
			rootZoomsTree.getNode().addCategory(type, subtype);
			for (int i = zoomToStart; i <= ZOOM_TO_SAVE_END; i++) {
				int xs = x >> (31 - i);
				int ys = y >> (31 - i);
				Tree<PoiTileBox> subtree = null;
				for (Tree<PoiTileBox> sub : prevTree.getSubtrees()) {
					if (sub.getNode().x == xs && sub.getNode().y == ys && sub.getNode().zoom == i) {
						subtree = sub;
						break;
					}
				}
				if (subtree == null) {
					subtree = new Tree<PoiTileBox>();
					PoiTileBox poiBox = new PoiTileBox();
					subtree.setNode(poiBox);
					poiBox.x = xs;
					poiBox.y = ys;
					poiBox.zoom = i;

					prevTree.addSubTree(subtree);
				}
				subtree.getNode().addCategory(type, subtype);

				prevTree = subtree;
			}
			addNamePrefix(name, nameEn, prevTree.getNode(), namesIndex);
			
			if (useInMemoryCreator) {
				if (prevTree.getNode().poiData == null) {
					prevTree.getNode().poiData = new ArrayList<PoiData>();
				}
				PoiData poiData = new PoiData();
				poiData.x = x;
				poiData.y = y;
				poiData.name = name;
				poiData.nameEn = nameEn;
				poiData.type = type;
				poiData.subtype = subtype;
				poiData.id = rs.getLong(7); 
				poiData.openingHours = rs.getString(8);
				poiData.site = rs.getString(9);
				poiData.phone = rs.getString(10);
				poiData.description = rs.getString(11);
				prevTree.getNode().poiData.add(poiData);
				
			}
		}
		log.info("Poi processing finishied");
		// Finish process all entities
		
		// 1. write header
		int right31 = maxX;
		int left31 = minX;
		int bottom31 = maxY;
		int top31 = minY;
		long startFpPoiIndex = writer.startWritePOIIndex(regionName, left31, right31, bottom31, top31);

		// 2. write categories table
		Map<String, Map<String, Integer>> categories = rootZoomsTree.node.categories;
		Map<String, Integer> catIndexes = writer.writePOICategoriesTable(categories);
		
		// 2.5 write names table
		Map<PoiTileBox, List<BinaryFileReference>> fpToWriteSeeks = writer.writePoiNameIndex(namesIndex, startFpPoiIndex);

		// 3. write boxes
		log.info("Poi box processing finishied");
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

		// 3.2 write tree using stack
		for (Tree<PoiTileBox> subs : rootZoomsTree.getSubtrees()) {
			writePoiBoxes(writer, subs, startFpPoiIndex, fpToWriteSeeks, categories, catIndexes);
		}

		// 4. write poi data
		// not so effictive probably better to load in memory one time
		PreparedStatement prepareStatement = poiConnection
				.prepareStatement("SELECT id, x, y, name_en, name, type, subtype, opening_hours, site, phone from poi "
						+ "where x >= ? AND x < ? AND y >= ? AND y < ?");
		TIntArrayList types = new TIntArrayList();
		for (Map.Entry<PoiTileBox, List<BinaryFileReference>> entry : fpToWriteSeeks.entrySet()) {
			int z = entry.getKey().zoom;
			int x = entry.getKey().x;
			int y = entry.getKey().y;
			writer.startWritePoiData(z, x, y, entry.getValue());

			if(useInMemoryCreator){
				List<PoiData> poiData = entry.getKey().poiData;
				
				for(PoiData poi : poiData){
					int x31 = poi.x;
					int y31 = poi.y;
					String type = poi.type;
					String subtype = poi.subtype;
					types.clear();
					buildTypeIds(type, subtype, categories, catIndexes, types);
					int x24shift = (x31 >> 7) - (x << (24 - z));
					int y24shift = (y31 >> 7) - (y << (24 - z));
					writer.writePoiDataAtom(poi.id, x24shift, y24shift, poi.nameEn, poi.name, types, poi.openingHours, poi.site, poi.phone, poi.description);	
				}
				
			} else {
				prepareStatement.setInt(1, x << (31 - z));
				prepareStatement.setInt(2, (x + 1) << (31 - z));
				prepareStatement.setInt(3, y << (31 - z));
				prepareStatement.setInt(4, (y + 1) << (31 - z));
				ResultSet rset = prepareStatement.executeQuery();
				while (rset.next()) {
					long id = rset.getLong(1);
					int x31 = rset.getInt(2);
					int y31 = rset.getInt(3);
					int x24shift = (x31 >> 7) - (x << (24 - z));
					int y24shift = (y31 >> 7) - (y << (24 - z));
					String nameEn = rset.getString(4);
					String name = rset.getString(5);
					String type = rset.getString(6);
					String subtype = rset.getString(7);

					types.clear();
					buildTypeIds(type, subtype, categories, catIndexes, types);

					String openingHours = rset.getString(8);
					String site = rset.getString(9);
					String phone = rset.getString(10);
					String description =  rset.getString(11);

					writer.writePoiDataAtom(id, x24shift, y24shift, nameEn, name, types, openingHours, site, phone, description);
				}
				rset.close();
			}
			writer.endWritePoiData();
		}

		prepareStatement.close();

		writer.endWritePOIIndex();

	}
	
	public void addNamePrefix(String name, String nameEn, PoiTileBox data, Map<String, Set<PoiTileBox>> poiData) {
		if(Algoritms.isEmpty(nameEn)){
			nameEn = Junidecode.unidecode(name);
		}
		parsePrefix(name, data, poiData);
		parsePrefix(nameEn, data, poiData);
	}

	private void parsePrefix(String name, PoiTileBox data, Map<String, Set<PoiTileBox>> poiData) {
		int prev = -1;
		for (int i = 0; i <= name.length(); i++) {
			if (i == name.length() || (!Character.isLetter(name.charAt(i)) && !Character.isDigit(name.charAt(i)) && name.charAt(i) != '\'')) {
				if (prev != -1) {
					String substr = name.substring(prev, i);
					if (substr.length() > CHARACTERS_TO_BUILD) {
						substr = substr.substring(0, CHARACTERS_TO_BUILD);
					}
					String val = substr.toLowerCase();
					if(!poiData.containsKey(val)){
						poiData.put(val, new LinkedHashSet<PoiTileBox>());
					}
					poiData.get(val).add(data);
					prev = -1;
				}
			} else {
				if(prev == -1){
					prev = i;
				}
			}
		}
		
	}

	private void writePoiBoxes(BinaryMapIndexWriter writer, Tree<PoiTileBox> tree, 
			long startFpPoiIndex, Map<PoiTileBox, List<BinaryFileReference>> fpToWriteSeeks,
			Map<String, Map<String, Integer>> categories, Map<String, Integer> catIndexes) throws IOException, SQLException {
		int x = tree.getNode().x;
		int y = tree.getNode().y;
		int zoom = tree.getNode().zoom;
		boolean end = zoom == ZOOM_TO_SAVE_END;
		BinaryFileReference fileRef = writer.startWritePoiBox(zoom, x, y, startFpPoiIndex, end);
		if(fileRef != null){
			if(!fpToWriteSeeks.containsKey(tree.getNode())) {
				fpToWriteSeeks.put(tree.getNode(), new ArrayList<BinaryFileReference>());
			}
			fpToWriteSeeks.get(tree.getNode()).add(fileRef);
		}
		if(zoom >= ZOOM_TO_WRITE_CATEGORIES_START && zoom <= ZOOM_TO_WRITE_CATEGORIES_END){
			TIntArrayList types = new TIntArrayList();
			for(Map.Entry<String, Map<String, Integer>> cats : tree.getNode().categories.entrySet()) {
				for(String subcat : cats.getValue().keySet()){
					String cat = cats.getKey();
					buildTypeIds(cat, subcat, categories, catIndexes, types);
				}
			}
			writer.writePOICategories(types);
		}
		
		if (!end) {
			for (Tree<PoiTileBox> subTree : tree.getSubtrees()) {
				writePoiBoxes(writer, subTree, startFpPoiIndex, fpToWriteSeeks, categories, catIndexes);
			}
		}
		writer.endWritePoiBox();
	}
	
	private static class PoiData {
		int x;
		int y;
		String name;
		String nameEn;
		String type;
		String subtype;
		long id;
		String openingHours;
		String phone;
		String site;
		String description;
	}
	
	public static class PoiTileBox {
		int x;
		int y;
		int zoom;
		Map<String, Map<String, Integer>> categories = new LinkedHashMap<String, Map<String, Integer>>();
		List<PoiData> poiData = null;
		
		public int getX() {
			return x;
		}
		
		public int getY() {
			return y;
		}
		public int getZoom() {
			return zoom;
		}
		
		private void addCategory(String cat, String subCat){
			if(!categories.containsKey(cat)){
				categories.put(cat, new TreeMap<String, Integer>());
			}
			if (subCat.contains(";") || subCat.contains(",")) {
				String[] split = subCat.split(",|;");
				for (String sub : split) {
					categories.get(cat).put(sub.trim(), 0);
				}
			} else {
				categories.get(cat).put(subCat.trim(), 0);
			}
			categories.get(cat).put(subCat, 0);
		}

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
				if (subtrees != null) {
					for (Tree<T> t : subtrees) {
						sum += t.getSubTreesOnLevel(level - 1);
					}
				}
				return sum;
			}
		}

	}


}
