package net.osmand.data.preparation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import net.osmand.IProgress;
import net.osmand.osm.Entity;
import net.osmand.osm.Node;
import net.osmand.osm.Relation;
import net.osmand.osm.Way;
import net.osmand.osm.Entity.EntityId;
import net.osmand.osm.Entity.EntityType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;

import com.anvisics.jleveldb.ArraySerializer;
import com.anvisics.jleveldb.ArraySerializer.EntityValueTokenizer;
import com.anvisics.jleveldb.ext.DBAccessor;
import com.anvisics.jleveldb.ext.DBIterator;
import com.anvisics.jleveldb.ext.ReadOptions;

public class OsmDbAccessor implements OsmDbAccessorContext {
	private static final Log log = LogFactory.getLog(OsmDbAccessor.class);
	
	private PreparedStatement pselectNode;
	private PreparedStatement pselectWay;
	private PreparedStatement pselectRelation;
	private PreparedStatement pselectTags;
	private int allRelations ;
	private int allWays;
	private int allNodes;
	private Connection dbConn;
	private DBDialect dialect;
	
	// leveldb
	private ReadOptions randomAccessOptions;
	
	private DBAccessor accessor;
	
	
	public interface OsmDbVisitor {
		public void iterateEntity(Entity e, OsmDbAccessorContext ctx) throws SQLException;
	}
	
	public OsmDbAccessor(){
	}
	
	public void initDatabase(Object dbConnection, DBDialect dialect, int allNodes, int allWays, int allRelations) throws SQLException {
		
		this.dialect = dialect;
		this.allNodes = allNodes;
		this.allWays = allWays;
		this.allRelations = allRelations;
		if(this.dialect == DBDialect.NOSQL){
			accessor = (DBAccessor) dbConnection;
			randomAccessOptions = new ReadOptions();
		} else {
			this.dbConn = (Connection) dbConnection;
			
			pselectNode = dbConn.prepareStatement("select n.latitude, n.longitude, t.skeys, t.value from node n left join tags t on n.id = t.id and t.type = 0 where n.id = ?"); //$NON-NLS-1$
			pselectWay = dbConn.prepareStatement("select w.node, w.ord, t.skeys, t.value, n.latitude, n.longitude " + //$NON-NLS-1$
					"from ways w left join tags t on w.id = t.id and t.type = 1 and w.ord = 0 inner join node n on w.node = n.id " + //$NON-NLS-1$
					"where w.id = ? order by w.ord"); //$NON-NLS-1$
			pselectRelation = dbConn.prepareStatement("select r.member, r.type, r.role, r.ord, t.skeys, t.value " + //$NON-NLS-1$
					"from relations r left join tags t on r.id = t.id and t.type = 2 and r.ord = 0 " + //$NON-NLS-1$
					"where r.id = ? order by r.ord"); //$NON-NLS-1$
			pselectTags = dbConn.prepareStatement("select skeys, value from tags where id = ? and type = ?"); //$NON-NLS-1$
		}
	}
	
	public int getAllNodes() {
		return allNodes;
	}
	
	public int getAllRelations() {
		return allRelations;
	}
	
	public int getAllWays() {
		return allWays;
	}
	
	public void loadEntityData(Entity e, boolean loadTags) throws SQLException {
		if (e instanceof Node || (e instanceof Way && !((Way) e).getNodes().isEmpty())) {
			// do not load tags for nodes inside way
			return;
		}
		if(dialect == DBDialect.NOSQL){
			loadEntityDataNoSQL(e, loadTags);
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
	
	
	

	public int iterateOverEntities(IProgress progress, EntityType type, OsmDbVisitor visitor) throws SQLException {
		if(dialect == DBDialect.NOSQL){
			return iterateOverEntitiesNoSQL(progress, type, visitor);
		}
		Statement statement = dbConn.createStatement();
		String select;
		int count = 0;

		// stat.executeUpdate("create table tags (id "+longType+", type smallint, skeys varchar(1024), value varchar(1024))");
		// stat.executeUpdate("create table ways (id "+longType+", node "+longType+", ord smallint)");
//		stat.executeUpdate("create table relations (id "+longType+", member "+longType+", type smallint, role varchar(1024), ord smallint)");
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
				if (progress != null) {
					progress.progress(1);
				}
				if (prevEntity != null) {
					count++;
					visitor.iterateEntity(prevEntity, this);
				}
				prevEntity = e;
			}
			prevId = curId;
		}
		if (prevEntity != null) {
			count++;
			visitor.iterateEntity(prevEntity, this);
		}
		rs.close();
		if(EntityType.NODE == type){
			allNodes = count;
		} else if(EntityType.WAY == type){
			allWays = count;
		} else if(EntityType.RELATION == type){
			allRelations = count;
		}
		return count;
	}

	
	private void loadEntityDataNoSQL(Entity e, boolean loadTags) {
		Collection<EntityId> ids = e instanceof Relation ? ((Relation) e).getMemberIds() : ((Way) e).getEntityIds();
		Map<EntityId, Entity> map = new LinkedHashMap<EntityId, Entity>();
		for (EntityId i : ids) {
			char pr = i.getType() == EntityType.NODE ? '0' : (i.getType() == EntityType.WAY ? '1' : '2');
			String key = pr + "" + i.getId();
			String value = accessor.get(randomAccessOptions, key);
			if (value != null && value.length() > 0) {
				try {
					Entity es = loadEntityNoSqlFromValue(randomAccessOptions, key, value, loadTags, false);
					map.put(i, es);
				} catch (JSONException e1) {
					log.warn(key + " - " + e1.getMessage() + " " + value + "("+value.length()+"]", e1);
				}
			}
		}
		e.initializeLinks(map);
	}
	
	private void assertToken(int expected, int actual, String value){
		if(expected != actual){
			System.err.println("Expected token " + expected + " != " + actual +" actual for : " + value);
		}
	}
	
	private Entity loadEntityNoSqlFromValue(ReadOptions opts,
			String key, String value, boolean loadTags, boolean skipIfEmptyTags) throws JSONException{
		if(value == null){
			return null;
		}
		Entity e = null;
		long id = Long.parseLong(key.substring(1));
		ArraySerializer.EntityValueTokenizer tokenizer = new EntityValueTokenizer();
		tokenizer.tokenize(value);
		assertToken(ArraySerializer.START_ARRAY, tokenizer.next(), value);
		
		int next = tokenizer.next();
		if(next == ArraySerializer.ELEMENT && skipIfEmptyTags){
			return null;
		} 
		
		// create e 
		if (key.charAt(0) == '0') {
			e = new Node(0, 0, id);
		} else if (key.charAt(0) == '1') {
			e = new Way(id);
		} else if (key.charAt(0) == '2') {
			e = new Relation(id);
		}
		
		// let's read tags
		if(next == ArraySerializer.START_ARRAY){
			int n = tokenizer.next();
			while(n == ArraySerializer.ELEMENT){
				String tagKey = tokenizer.value();
				assertToken(ArraySerializer.ELEMENT, tokenizer.next(), value);
				String tagValue = tokenizer.value();
				e.putTag(tagKey, tagValue);
				n = tokenizer.next();
			}
		}
		
		if (key.charAt(0) == '0') {
			try {
				assertToken(ArraySerializer.ELEMENT, tokenizer.next(), value);
				double lat = Double.parseDouble(tokenizer.value());
				assertToken(ArraySerializer.ELEMENT, tokenizer.next(), value);
				double lon = Double.parseDouble(tokenizer.value());
				((Node)e).setLatitude(lat);
				((Node)e).setLongitude(lon);
			} catch (java.lang.NumberFormatException ex) {
				log.warn("Cannot parse lat/log for Node with key:" + key + " value:" + value);
				e = null;
			}
		} else if (key.charAt(0) == '1') {
			assertToken(ArraySerializer.START_ARRAY, tokenizer.next(), value);
			int n = tokenizer.next();
			while(n == ArraySerializer.ELEMENT){
				String pointId = "0"+ tokenizer.value();
				String pointVal = this.accessor.get(opts, pointId);
				Node node = (Node) loadEntityNoSqlFromValue(opts, pointId, pointVal, false, false);
				if(node != null){
					((Way) e).addNode(node);
				}
				n = tokenizer.next();
			}
		} else if (key.charAt(0) == '2') {
			assertToken(ArraySerializer.START_ARRAY, tokenizer.next(), value);
			int n = tokenizer.next();
			while(n == ArraySerializer.ELEMENT){
				String mkey = tokenizer.value();
				EntityType t = null;
				long mid = Long.parseLong(mkey.substring(1));
				if(mkey.charAt(0) == '0'){
					t = EntityType.NODE;
				} else if(mkey.charAt(0) == '1'){
					t = EntityType.WAY;
				} else if(mkey.charAt(0) == '2'){
					t = EntityType.RELATION;
				}
				assertToken(ArraySerializer.ELEMENT, tokenizer.next(), value);
				String role = tokenizer.value();
				((Relation) e).addMember(mid, t, role);
				n = tokenizer.next();
			}
		}
		
		return e;
	}

	private int iterateOverEntitiesNoSQL(IProgress progress, EntityType type, OsmDbVisitor visitor) throws SQLException {
		ReadOptions opts = new ReadOptions();
		DBIterator iterator = accessor.newIterator(opts);
		String prefix = "0";
		int count = 0;
		if (type == EntityType.WAY) {
			prefix = "1";
		} else if (type == EntityType.RELATION) {
			prefix = "2";
		}
		
		iterator.seek(prefix);
		
		while(iterator.valid()){
			String key = iterator.key();
			if(!key.startsWith(prefix)){
				break;
			}
			String value = iterator.value();
			try {
				Entity e = null;
				if (type == EntityType.NODE) {
					e = loadEntityNoSqlFromValue(opts, key, value, true, true);
				} else if (type == EntityType.WAY) {
					e = loadEntityNoSqlFromValue(opts, key, value, true, false);
				} else {
					e = loadEntityNoSqlFromValue(opts, key, value, true, false);
				}
				
				if(e != null){
					count++;
					if (progress != null) {
						progress.progress(1);
					}
					visitor.iterateEntity(e, this);
				}
			} catch (JSONException e) {
				log.warn(key + " - " + e.getMessage() + " " + value + "("+value.length()+"]", e);
			}
			iterator.next();
		}
		iterator.delete();
		return count;
	}

	public void closeReadingConnection() throws SQLException {
		if (dialect != DBDialect.NOSQL) {
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
		}
		
	}

}
