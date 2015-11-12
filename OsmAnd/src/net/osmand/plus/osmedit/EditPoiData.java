package net.osmand.plus.osmedit;

import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.Node;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class EditPoiData {
	private static final Log LOG = PlatformUtil.getLog(EditPoiData.class);
	private Set<TagsChangedListener> mListeners = new HashSet<>();
	private LinkedHashMap<String, String > tagValues = new LinkedHashMap<String, String>();
	private boolean isInEdit = false;
	private Node entity;
	public final Amenity amenity;
	public static final String POI_TYPE_TAG = "poi_type_tag";
	private boolean hasChangesBeenMade = false;

	public EditPoiData(Amenity amenity, Node node, Map<String, PoiType> allTranslatedSubTypes) {
		this.amenity = amenity;
		entity = node;
		initTags(node, allTranslatedSubTypes);
	}
	

	public Node getEntity() {
		return entity;
	}
	
	public String getTag(String key) {
		return tagValues.get(key);
	}
	
	public void updateTags(Map<String, String> mp) {
		this.tagValues.clear();
		this.tagValues.putAll(mp);
	}
	
	private void tryAddTag(String key, String value) {
		if (!Algorithms.isEmpty(value)) {
			tagValues.put(key, value);
		}
	}
	
	private void initTags(Node node, Map<String, PoiType> allTranslatedSubTypes) {
		checkNotInEdit();
		for (String s : node.getTagKeySet()) {
			tryAddTag(s, node.getTag(s));
		}
		String subType = amenity.getSubType();
		String value ="";
		PoiType pt = amenity.getType().getPoiTypeByKeyName(subType);
		if (pt != null) {
			value = pt.getTranslation();
		}
		tagValues.put(POI_TYPE_TAG, value);		
	}


	public Map<String, String> getTagValues() {
		return Collections.unmodifiableMap(tagValues);
	}
	

	public void putTag(String tag, String value) {
		checkNotInEdit();
		try { 
			isInEdit = true;
			tagValues.put(tag, value);
			notifyDatasetChanged(tag);
		} finally {
			isInEdit = false;
		}
	}


	private void checkNotInEdit() {
		if(isInEdit) {
			throw new IllegalStateException("Can't modify in edit mode");
		}
	}
	
	public void notifyToUpdateUI() {
		checkNotInEdit();
		try { 
			isInEdit = true;
			notifyDatasetChanged(null);
		} finally {
			isInEdit = false;
		}		
	}
	
	public void removeTag(String tag) {
		checkNotInEdit();
		try { 
			isInEdit = true;
			tagValues.remove(tag);
			notifyDatasetChanged(tag);
		} finally {
			isInEdit = false;
		}
	}

	public void setIsInEdit(boolean isInEdit) {
		this.isInEdit = isInEdit;
	}

	public boolean isInEdit() {
		return isInEdit;
	}
	
	
	private void notifyDatasetChanged(String tag) {
		if (mListeners.size() > 0) {
			hasChangesBeenMade = true;
		}
		for (TagsChangedListener listener : mListeners) {
			listener.onTagsChanged(tag);
		}
	}

	public void addListener(TagsChangedListener listener) {
		mListeners.add(listener);
	}

	public void deleteListener(TagsChangedListener listener) {
		mListeners.remove(listener);
	}

	public interface TagsChangedListener {
		
		void onTagsChanged(String tag);
		
	}

	public boolean hasChangesBeenMade() {
		return hasChangesBeenMade;
	}
	
}