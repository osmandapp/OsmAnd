package net.osmand.data.preparation;

import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.Entity;
import net.osmand.osm.Relation;
import net.osmand.osm.Way;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class RelationPropagator {

	private static final Log log = LogFactory.getLog(RelationPropagator.class);
	private final MapRenderingTypes renderingTypes;

	public RelationPropagator(MapRenderingTypes renderingTypes) {
		this.renderingTypes = renderingTypes;
	}	

	public void iterateRelation(Relation r, OsmDbAccessorContext ctx) throws SQLException {
		iterateRelation(r,ctx,null);
	}	

	public void iterateRelation(Relation r, OsmDbAccessorContext ctx, Collection<String> tagKeySet) throws SQLException {

		ctx.loadEntityRelation(r);

		List<Tag> tagsToPropagate = new ArrayList<Tag>();

		if(tagKeySet == null)tagKeySet = r.getTagKeySet();

		for (String tag : tagKeySet) {
			String val = r.getTag(tag);
			boolean exists = renderingTypes.ruleExists(tag,val);
			if(exists)tagsToPropagate.add(new Tag(tag,val));
		}

		Collection<Entity> members = r.getMembers(null);
		for(Iterator iter = members.iterator(); iter.hasNext();) {
			Entity e = (Entity)iter.next();
			if(e instanceof Way) {
				ctx.loadEntityWay((Way)e);
				Iterator it = tagsToPropagate.iterator();
				while(it.hasNext()) {
					Tag t = (Tag)it.next();
					log.info("adding tag " + t.name + ":" + t.value + " to " + e);
					e.putTag(t.name,t.value);
				}
				if(e.isDirty())ctx.saveEntityWay((Way)e);
			}
			if(e instanceof Relation)iterateRelation((Relation)e,ctx,tagKeySet);

		}			

	}

	private class Tag {
		public String name;
		public String value;
		
		public Tag(String name, String value) {
			this.name = name;
			this.value = value;
		}
	}

}
