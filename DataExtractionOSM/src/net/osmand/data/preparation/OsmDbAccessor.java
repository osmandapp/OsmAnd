package net.osmand.data.preparation;


import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import net.osmand.IProgress;
import net.osmand.osm.Entity;
import net.osmand.osm.Entity.EntityId;
import net.osmand.osm.Entity.EntityType;
import net.osmand.osm.Node;
import net.osmand.osm.Relation;
import net.osmand.osm.Way;


public class OsmDbAccessor implements OsmDbAccessorContext {
	
	//private static final Log log = LogFactory.getLog(OsmDbAccessor.class);
	
	private PreparedStatement pselectNode;
	private PreparedStatement pselectWay;
	private PreparedStatement pselectRelation;
	private int allRelations;
	private int allWays;
	private int allNodes;
	private int allBoundaries;
	private boolean realCounts = false;
	private Connection dbConn;
	private DBDialect dialect;
	
	private PreparedStatement iterateNodes;
	private PreparedStatement iterateWays;
	private PreparedStatement iterateRelations;
	private PreparedStatement iterateWayBoundaries;

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
			throw new UnsupportedOperationException();
		} else {
			this.dbConn = (Connection) dbConnection;
			
			pselectNode = dbConn.prepareStatement("select n.latitude, n.longitude, n.tags from node n where n.id = ?"); //$NON-NLS-1$
			pselectWay = dbConn.prepareStatement("select w.node, w.ord, w.tags, n.latitude, n.longitude, n.tags " + //$NON-NLS-1$
					"from ways w left join node n on w.node = n.id where w.id = ? order by w.ord"); //$NON-NLS-1$
			pselectRelation = dbConn.prepareStatement("select r.member, r.type, r.role, r.ord, r.tags " + //$NON-NLS-1$
					"from relations r where r.id = ? order by r.ord"); //$NON-NLS-1$
		
			iterateNodes = dbConn.prepareStatement("select n.id, n.latitude, n.longitude, n.tags from node n where length(n.tags) > 0"); //$NON-NLS-1$
			iterateWays  = dbConn.prepareStatement("select w.id, w.node, w.ord, w.tags, n.latitude, n.longitude, n.tags " + //$NON-NLS-1$
					"from ways w left join node n on w.node = n.id order by w.id, w.ord"); //$NON-NLS-1$
			iterateWayBoundaries  = dbConn.prepareStatement("select w.id, w.node, w.ord, w.tags, n.latitude, n.longitude, n.tags " + //$NON-NLS-1$
					"from ways w left join node n on w.node = n.id  where w.boundary > 0 order by w.id, w.ord"); //$NON-NLS-1$
			iterateRelations = dbConn.prepareStatement("select r.id, r.tags from relations r where length(r.tags) > 0"); //$NON-NLS-1$
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
	
	
	@Override
	public void loadEntityWay(Way e) throws SQLException {
		if (e.getEntityIds().isEmpty()) {
			pselectWay.setLong(1, e.getId());
			if (pselectWay.execute()) {
				ResultSet rs = pselectWay.getResultSet();
				while (rs.next()) {
					int ord = rs.getInt(2);
					if (ord == 0) {
						readTags(e, rs.getBytes(3));
					}
					if (rs.getObject(5) != null) {
						Node n = new Node(rs.getDouble(4), rs.getDouble(5), rs.getLong(1));
						((Way) e).addNode(n);
						readTags(n, rs.getBytes(6));
					} else {
						((Way) e).addNode(rs.getLong(1));
					}
				}
				rs.close();
			}
		}
	}
	
	@Override
	public void loadEntityRelation(Relation e) throws SQLException {
		loadEntityRelation(e, 1);
	}
	
	public void loadEntityRelation(Relation e, int level) throws SQLException {
		if (e.isDataLoaded()) { //data was already loaded, nothing to do
			return;
		}
		Map<EntityId, Entity> map = new LinkedHashMap<EntityId, Entity>();
		if (e.getMemberIds().isEmpty()) {
			pselectRelation.setLong(1, e.getId());
			if (pselectRelation.execute()) {
				ResultSet rs = pselectRelation.getResultSet();
				while (rs.next()) {
					int ord = rs.getInt(4);
					if (ord == 0 ) {
						readTags(e, rs.getBytes(5));
					}
					e.addMember(rs.getLong(1), EntityType.values()[rs.getInt(2)], rs.getString(3));
				}
				rs.close();
			}
		}
		Collection<EntityId> ids = e.getMemberIds() ;
		if (level > 0) {
			for (EntityId i : ids) {
				if (i.getType() == EntityType.NODE) {
					pselectNode.setLong(1, i.getId());
					if (pselectNode.execute()) {
						ResultSet rs = pselectNode.getResultSet();
						Node n = null;
						while (rs.next()) {
							if (n == null) {
								n = new Node(rs.getDouble(1), rs.getDouble(2), i.getId());
								readTags(n, rs.getBytes(3));
							}
						}
						map.put(i, n);
						rs.close();
					}
				} else if (i.getType() == EntityType.WAY) {
					Way way = new Way(i.getId());
					loadEntityWay(way);
					map.put(i, way);
				} else if (i.getType() == EntityType.RELATION) {
					Relation rel = new Relation(i.getId());
					loadEntityRelation(rel, level - 1);
					map.put(i, rel);
				}
			}

			e.initializeLinks(map);
			e.entityDataLoaded();
		}
	}
	
	public void readTags(Entity e, byte[] tags){
		if (tags != null) {
			try {
				int prev = 0;
				List<String> vs = new ArrayList<String>();
				for (int i = 0; i < tags.length; i++) {
					if (tags[i] == 0) {
						vs.add(new String(tags, prev, i - prev, "UTF-8"));
						prev = i + 1;
					}
				}
				for(int i=0; i<vs.size(); i+=2) {
					e.putTag(vs.get(i), vs.get(i+1));
				}
			} catch (UnsupportedEncodingException e1) {
				throw new RuntimeException(e1);
			}
		}
	}
	
	
	public int iterateOverEntities(IProgress progress, EntityType type, OsmDbVisitor visitor) throws SQLException, InterruptedException {
		Statement statement = dbConn.createStatement();
		PreparedStatement select;
		int count = 0;
		
		computeRealCounts(statement);
		statement.close();
		
		BlockingQueue<Entity> toProcess = new ArrayBlockingQueue<Entity>(100000);
		AbstractProducer entityProducer = null;
		if (type == EntityType.NODE) {
			// filter out all nodes without tags
			select = iterateNodes;
			count = allNodes;
		} else if (type == EntityType.WAY) {
			select = iterateWays;
			count = allWays;
		} else if (type == EntityType.WAY_BOUNDARY) {
			select = iterateWayBoundaries;
			count = allBoundaries;
		} else {
			select = iterateRelations;
			count = allRelations;
		}
		entityProducer = new EntityProducer(toProcess, type, select);
		progress.startWork(count);
		
		//produce
		entityProducer.start();
		try {
			// wait a little before starting taking entities from queue
			Thread.sleep(150);
		} catch (InterruptedException e) {
		}
		
		Entity entityToProcess = null;
		Entity endEntity = entityProducer.getEndingEntity();
		while ((entityToProcess = toProcess.take())  != endEntity) {
			if (progress != null) {
				progress.progress(1);
			}
			visitor.iterateEntity(entityToProcess, this);
		}
		return count;
	}

	
	public void computeRealCounts(Statement statement) throws SQLException {
		if (!realCounts) {
			realCounts = true;
			// filter out all nodes without tags
			allNodes = statement.executeQuery("select count(distinct n.id) from node n where length(n.tags) > 0").getInt(1); //$NON-NLS-1$
			allWays = statement.executeQuery("select count(*) from ways w where w.ord = 0").getInt(1); //$NON-NLS-1$
			allRelations = statement.executeQuery("select count(distinct r.id) from relations r").getInt(1); //$NON-NLS-1$
			allBoundaries = statement.executeQuery("select count(*) from ways w where w.ord = 0 and w.boundary > 0").getInt(1); //$NON-NLS-1$
		}
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
			if (iterateNodes != null) {
				iterateNodes.close();
			}
			if (iterateRelations != null) {
				iterateRelations.close();
			}
			if (iterateWays != null) {
				iterateWays.close();
			}
			if (iterateWayBoundaries != null) {
				iterateWayBoundaries.close();
			}
		}
		
	}

	public class AbstractProducer extends Thread {
		private final Entity endingEntity = new Node(0,0,0);
		
		public Entity getEndingEntity() {
			return endingEntity;
		}
	}
	
	
	public class EntityProducer extends AbstractProducer {

		private final BlockingQueue<Entity> toProcess;
		private final PreparedStatement select;
		private final EntityType type;
		private final boolean putEndingEntity;

		public EntityProducer(BlockingQueue<Entity> toProcess, EntityType type, PreparedStatement select) {
			this(toProcess,type,select,true);
		}
		
		public EntityProducer(BlockingQueue<Entity> toProcess, EntityType type, PreparedStatement select, boolean putEndingEntity) {
			this.toProcess = toProcess;
			this.type = type;
			this.select = select;
			this.putEndingEntity = putEndingEntity;
			setDaemon(true);
			setName("EntityProducer");
		}

		@Override
		public void run() {
			ResultSet rs;
			try {
				select.execute();
				rs = select.getResultSet();
				// rs.setFetchSize(1000); !! not working for SQLite would case troubles probably
				Entity prevEntity = null;
				long prevId = Long.MIN_VALUE;
				while (rs.next()) {
					long curId = rs.getLong(1);
					boolean newEntity = curId != prevId;
					Entity e = prevEntity;
					if (type == EntityType.NODE) {
						e = new Node(rs.getDouble(2), rs.getDouble(3), curId);
						readTags(e, rs.getBytes(4));
					} else if (type == EntityType.WAY || type == EntityType.WAY_BOUNDARY) {
						if (newEntity) {
							e = new Way(curId);
						}
						int ord = rs.getInt(3);
						if (ord == 0) {
							readTags(e, rs.getBytes(4));
						}
						if (rs.getObject(6) == null) {
							((Way) e).addNode(rs.getLong(2));
						} else {
							Node n = new Node(rs.getDouble(5), rs.getDouble(6), rs.getLong(2));
							readTags(n, rs.getBytes(7));
							((Way) e).addNode(n);
						}
					} else {
						e = new Relation(curId);
						readTags(e, rs.getBytes(2));
					}
					if (newEntity) {
						if (prevEntity != null) {
							toProcess.put(prevEntity);
						}
						prevEntity = e;
					}
					prevId = curId;
				}
				if (prevEntity != null) {
					toProcess.put(prevEntity);
				}
				rs.close();
			} catch (SQLException e1) {
				e1.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				if (putEndingEntity) {
					try {
						toProcess.put(getEndingEntity());
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
	}


}
