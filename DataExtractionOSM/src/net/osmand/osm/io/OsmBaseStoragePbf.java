package net.osmand.osm.io;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import net.osmand.IProgress;
import net.osmand.osm.Entity;
import net.osmand.osm.Entity.EntityId;
import net.osmand.osm.Entity.EntityType;
import net.osmand.osm.EntityInfo;
import net.osmand.osm.Node;
import net.osmand.osm.Relation;
import net.osmand.osm.Way;
import crosby.binary.BinaryParser;
import crosby.binary.Osmformat.DenseNodes;
import crosby.binary.Osmformat.HeaderBlock;
import crosby.binary.Osmformat.Info;
import crosby.binary.Osmformat.Relation.MemberType;
import crosby.binary.file.BlockInputStream;

public class OsmBaseStoragePbf extends OsmBaseStorage {
	
	public synchronized void parseOSMPbf(final InputStream stream, final IProgress progress, final boolean entityInfo) throws IOException {
		BinaryParser parser = new BinaryParser() {
			public void updateProgress(int count) {
				progressEntity += count;
				if (progress != null && progressEntity > moduleProgress && !progress.isIndeterminate()) {
					try {
						progressEntity = 0;
						progress.remaining(stream.available());
					} catch (IOException e) {
						progress.startWork(-1);
					}
				}
			}

			public void registerEntity(EntityType type, Entity e, EntityInfo info) {
				EntityId entityId = new EntityId(type, e.getId());
				if (acceptEntityToLoad(entityId, e)) {
					Entity oldEntity = entities.put(entityId, e);
					if (info != null) {
						OsmBaseStoragePbf.this.entityInfo.put(entityId, info);
					}
					if (!supressWarnings && oldEntity != null) {
						throw new UnsupportedOperationException("Entity with id=" + oldEntity.getId() + " is duplicated in osm map"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			}

			@Override
			protected void parse(HeaderBlock header) {
			}

			private DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); //$NON-NLS-1$

			@Override
			protected void parseDense(DenseNodes n) {
				EntityInfo info = null;
				long changeset = 0;
				long timestamp = 0;
				int uid = 0;
				int user = 0;
				long id = 0;
				long lat = 0;
				long lon = 0;
				int keyInd = 0;
				boolean tagsEmpty = n.getKeysValsCount() == 0;
				for (int i = 0; i < n.getIdCount(); i++) {
					id += n.getId(i);
					lat += n.getLat(i);
					lon += n.getLon(i);
					Node node = new Node(parseLat(lat), parseLon(lon), id);

					if (entityInfo && n.getDenseinfo() != null) {
						info = new EntityInfo();
						changeset += n.getDenseinfo().getChangeset(i);
						timestamp += n.getDenseinfo().getTimestamp(i);
						uid += n.getDenseinfo().getUid(i);
						user += n.getDenseinfo().getUserSid(i);
						info.setChangeset((changeset) + ""); //$NON-NLS-1$
						info.setTimestamp(format.format(new Date(date_granularity * (timestamp))));
						info.setUser(getStringById(user));
						info.setUid(uid + ""); //$NON-NLS-1$
						info.setVersion(n.getDenseinfo().getVersion(i) + ""); //$NON-NLS-1$
						info.setVisible("true"); //$NON-NLS-1$
					}
					if (!tagsEmpty) {
						while (n.getKeysVals(keyInd) != 0) {
							String key = getStringById(n.getKeysVals(keyInd));
							String val = getStringById(n.getKeysVals(keyInd + 1));
							node.putTag(key, val);
							keyInd += 2;
						}
						keyInd++;
					}
					registerEntity(EntityType.NODE, node, info);
				}
				updateProgress(n.getIdCount());
			}

			protected EntityInfo parseEntityInfo(Info i) {
				EntityInfo info = new EntityInfo();
				info.setChangeset(i.getChangeset() + ""); //$NON-NLS-1$
				info.setTimestamp(format.format(getDate(i)));
				info.setUser(getStringById(i.getUserSid()));
				info.setUid(i.getUid() + ""); //$NON-NLS-1$
				info.setVersion(i.getVersion() + ""); //$NON-NLS-1$
				info.setVisible("true"); //$NON-NLS-1$
				return info;
			}

			@Override
			protected void parseNodes(List<crosby.binary.Osmformat.Node> n) {
				EntityInfo info = null;
				int nsize = n.size();
				for (int i = 0; i < nsize; i++) {
					crosby.binary.Osmformat.Node nod = n.get(i);
					Node e = new Node(parseLat(nod.getLat()), parseLon(nod.getLon()), nod.getId());
					for (int j = 0; j < nod.getKeysCount(); j++) {
						String key = getStringById(nod.getKeys(j));
						String val = getStringById(nod.getVals(j));
						e.putTag(key, val);
					}
					if (entityInfo) {
						info = parseEntityInfo(nod.getInfo());
					}
					registerEntity(EntityType.NODE, e, info);
				}
				updateProgress(nsize);
			}

			@Override
			protected void parseRelations(List<crosby.binary.Osmformat.Relation> r) {
				EntityInfo info = null;
				int rsize = r.size();
				for (int i = 0; i < rsize; i++) {
					crosby.binary.Osmformat.Relation rel = r.get(i);
					Relation e = new Relation(rel.getId());
					long id = 0;
					for (int j = 0; j < rel.getMemidsCount(); j++) {
						id += rel.getMemids(j);
						String role = getStringById(rel.getRolesSid(j));
						MemberType t = rel.getTypes(j);
						EntityType ts = EntityType.NODE;
						switch (t) {
						case NODE:
							ts = EntityType.NODE;
							break;
						case WAY:
							ts = EntityType.WAY;
							break;
						case RELATION:
							ts = EntityType.RELATION;
							break;
						}
						e.addMember(id, ts, role);
					}
					for (int j = 0; j < rel.getKeysCount(); j++) {
						String key = getStringById(rel.getKeys(j));
						String val = getStringById(rel.getVals(j));
						e.putTag(key, val);
					}
					if (entityInfo) {
						info = parseEntityInfo(rel.getInfo());
					}
					registerEntity(EntityType.RELATION, e, info);
				}
				updateProgress(rsize);
			}

			@Override
			protected void parseWays(List<crosby.binary.Osmformat.Way> w) {
				EntityInfo info = null;
				int wsize = w.size();
				for (int i = 0; i < wsize; i++) {
					crosby.binary.Osmformat.Way way = w.get(i);
					Way e = new Way(way.getId());
					long id = 0;
					for (int j = 0; j < way.getRefsCount(); j++) {
						id += way.getRefs(j);
						e.addNode(id);
					}
					for (int j = 0; j < way.getKeysCount(); j++) {
						String key = getStringById(way.getKeys(j));
						String val = getStringById(way.getVals(j));
						e.putTag(key, val);
					}
					if (entityInfo) {
						info = parseEntityInfo(way.getInfo());
					}
					registerEntity(EntityType.WAY, e, info);
				}
				updateProgress(wsize);
			}

			@Override
			public void complete() {
			}

		};
		
		this.progressEntity = 0;
		this.entities.clear();
		this.entityInfo.clear();
		if(progress != null){
			progress.startWork(stream.available());
		}
		
		BlockInputStream bis = new BlockInputStream(stream, parser);
		bis.process();
		
		if(progress != null){
			progress.finishTask();
		}
		completeReading();
	}
}
