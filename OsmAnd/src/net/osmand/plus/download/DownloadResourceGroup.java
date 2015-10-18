package net.osmand.plus.download;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import net.osmand.OsmAndCollator;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.WorldRegion;

public class DownloadResourceGroup {

	private final DownloadResourceGroupType type;
	private final DownloadResourceGroup parentGroup;
	// ASSERT: individualResources are not empty if and only if groups are empty
	private final List<IndexItem> individualResources;
	private final List<DownloadResourceGroup> groups;
	protected final String id;

	protected WorldRegion region;
	public static final String REGION_MAPS_ID = "maps";

	public enum DownloadResourceGroupType {
//		return ctx.getResources().getString(R.string.index_name_voice);
//		return ctx.getResources().getString(R.string.index_name_tts_voice);
		WORLD, VOICE_REC, VOICE_TTS, WORLD_MAPS, REGION, REGION_MAPS

	}

	public DownloadResourceGroup(DownloadResourceGroup parentGroup, DownloadResourceGroupType type, String id,
			boolean flat) {
		if (flat) {
			this.individualResources = new ArrayList<IndexItem>();
			this.groups = null;
		} else {
			this.individualResources = null;
			this.groups = new ArrayList<DownloadResourceGroup>();
		}
		this.id = id;
		this.type = type;
		this.parentGroup = parentGroup;
	}
	
	public void trimEmptyGroups() {
		if(groups != null) {
			for(DownloadResourceGroup gr : groups) {
				gr.trimEmptyGroups();
			}
			Iterator<DownloadResourceGroup> gr = groups.iterator();
			while(gr.hasNext()) {
				DownloadResourceGroup group = gr.next();
				if(group.isEmpty()) {
					gr.remove();
				}
			}
		}
		
	}
	
	public void addGroup(DownloadResourceGroup g) {
		groups.add(g);
		if (g.individualResources != null) {
			final net.osmand.Collator collator = OsmAndCollator.primaryCollator();
			final OsmandApplication app = getRoot().app;
			final OsmandRegions osmandRegions = app.getRegions();
			Collections.sort(g.individualResources, new Comparator<IndexItem>() {
				@Override
				public int compare(IndexItem lhs, IndexItem rhs) {
					return collator.compare(lhs.getVisibleName(app.getApplicationContext(), osmandRegions),
							rhs.getVisibleName(app.getApplicationContext(), osmandRegions));
				}
			});
		}
	}
	
	public void addItem(IndexItem i) {
		individualResources.add(i);
	}
	
	public boolean isEmpty() {
		return isEmpty(individualResources) && isEmpty(groups);
	}

	private boolean isEmpty(List<?> l) {
		return l == null || l.isEmpty();
	}

	public String getGroupId() {
		return id;
	}

	public boolean flatGroup() {
		return individualResources != null;
	}

	public DownloadResourceGroup getParentGroup() {
		return parentGroup;
	}

	public DownloadResources getRoot() {
		if (this instanceof DownloadResources) {
			return (DownloadResources) this;
		} else if (parentGroup != null) {
			return parentGroup.getRoot();
		}
		return null;
	}

	public DownloadResourceGroupType getType() {
		return type;
	}

	public DownloadResourceGroup getGroupById(String uid) {
		String[] lst = uid.split("\\#");
		return getGroupById(lst, 0);
	}

	private DownloadResourceGroup getGroupById(String[] lst, int subInd) {
		if (lst.length > subInd && lst[subInd].equals(id)) {
			if (lst.length == subInd + 1) {
				return this;
			} else if (groups != null) {
				for (DownloadResourceGroup rg : groups) {
					DownloadResourceGroup r = rg.getGroupById(lst, subInd + 1);
					if (r != null) {
						return r;
					}
				}
			}
		}
		return null;
	}

	public String getUniqueId() {
		if (parentGroup == null) {
			return id;
		}
		return parentGroup.getUniqueId() + "#" + id;
	}

}