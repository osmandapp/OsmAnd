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

import com.anvisics.jleveldb.ext.DBAccessor;
import com.anvisics.jleveldb.ext.ReadOptions;

public class OsmDbAccessor implements OsmDbAccessorContext {
	
	private PreparedStatement pselectNode;
	private PreparedStatement pselectWay;
	private PreparedStatement pselectRelation;
	private PreparedStatement pselectTags;
	private int allRelations ;
	private int allWays;
	private int allNodes;
	private Connection dbConn;
	private DBDialect dialect;
	private DBAccessor accessor;
	private ReadOptions opts;
	
	public interface OsmDbVisitor {
		public void iterateEntity(Entity e, OsmDbAccessorContext ctx) throws SQLException;
	}
	
	public OsmDbAccessor(){
	}
	
	public void initDatabase(Object dbConnection, DBDialect dialect, int allNodes, int allWays, int allRelations) throws SQLException {
		
		this.dialect = dialect;
		if(this.dialect == DBDialect.NOSQL){
			opts = new ReadOptions();
			accessor = (DBAccessor) dbConnection;
		} else {
			this.dbConn = (Connection) dbConnection;
			this.allNodes = allNodes;
			this.allWays = allWays;
			this.allRelations = allRelations;
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
	
	
	private void loadEntityDataNoSQL(Entity e, boolean loadTags) {
		// TODO Auto-generated method stub
		
	}

	public int iterateOverEntities(IProgress progress, EntityType type, OsmDbVisitor visitor) throws SQLException {
		if(dialect == DBDialect.NOSQL){
			iterateOverEntitiesNoSQL(progress, type, visitor);
		}
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


	private void iterateOverEntitiesNoSQL(IProgress progress, EntityType type, OsmDbVisitor visitor) {
		// TODO Auto-generated method stub
		
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
