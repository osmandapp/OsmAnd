package net.osmand.plus.download;

import java.util.ArrayList;
import java.util.List;

import net.osmand.plus.WorldRegion;

public class DownloadResourceGroup {

	private final DownloadResourceGroupType type;
	private final DownloadResourceGroup parentGroup;
	// ASSERT: individualResources are not empty if and only if groups are empty
	private final List<IndexItem> individualResources;
	private final List<DownloadResourceGroup> groups;
	protected final String id;

	protected WorldRegion region;

	public enum DownloadResourceGroupType {
		WORLD, VOICE, WORLD_MAPS, REGION

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