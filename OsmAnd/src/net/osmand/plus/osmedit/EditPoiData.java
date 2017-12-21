package net.osmand.plus.osmedit;

import net.osmand.PlatformUtil;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.Node;
import net.osmand.plus.OsmandApplication;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.Arrays;
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
	
	public static final String POI_TYPE_TAG = "poi_type_tag";
	public static final String REMOVE_TAG_PREFIX = "----";
	public static final String REMOVE_TAG_VALUE = "DELETE";
	private boolean hasChangesBeenMade = false;
	private Map<String, PoiType> allTranslatedSubTypes;
	private PoiCategory category;

	private Set<String> changedTags = new HashSet<>();
	
	public EditPoiData(Node node, OsmandApplication app) {
		allTranslatedSubTypes = app.getPoiTypes().getAllTranslatedNames(true);
		category = app.getPoiTypes().getOtherPoiCategory();
		entity = node;
		initTags(node);
		updateTypeTag(getPoiTypeString(), false);
	}
	
	public Map<String, PoiType> getAllTranslatedSubTypes() {
		return allTranslatedSubTypes;
	}
	
	public void updateType(PoiCategory type) {
		if(type != null && type != category) {
			category = type;
			tagValues.put(POI_TYPE_TAG, "");
			changedTags.add(POI_TYPE_TAG);
		}
	}
	
	
	public PoiCategory getPoiCategory() {
		return category;
	}
	
	public PoiType getPoiTypeDefined() {
		return allTranslatedSubTypes.get(getPoiTypeString().toLowerCase());
	}
	
	public String getPoiTypeString() {
		String s = tagValues.get(POI_TYPE_TAG) ;
		return s == null ? "" : s;
	}

	public Node getEntity() {
		return entity;
	}
	
	public String getTag(String key) {
		return tagValues.get(key);
	}
	
	public void updateTags(Map<String, String> mp) {
		checkNotInEdit();
		this.tagValues.clear();
		this.tagValues.putAll(mp);
		changedTags.clear();
		retrieveType();
	}
	
	private void tryAddTag(String key, String value) {
		if (!Algorithms.isEmpty(value)) {
			tagValues.put(key, value);
		}
	}
	
	private void initTags(Node node) {
		checkNotInEdit();
		for (String s : node.getTagKeySet()) {
			tryAddTag(s, node.getTag(s));
		}
		retrieveType();
	}

	private void retrieveType() {
		String tp = tagValues.get(POI_TYPE_TAG);
		if(tp != null) {
			PoiType pt = allTranslatedSubTypes.get(tp);
			if (pt != null) {
				category = pt.getCategory();
			}
		}
	}

	public Map<String, String> getTagValues() {
		return Collections.unmodifiableMap(tagValues);
	}


	public void putTag(String tag, String value) {
		checkNotInEdit();
		try {
			isInEdit = true;
			tagValues.remove(REMOVE_TAG_PREFIX+tag);
			String oldValue = tagValues.get(tag);
			if (oldValue == null || !oldValue.equals(value)) {
				changedTags.add(tag);
			}
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
			tagValues.put(REMOVE_TAG_PREFIX+tag, REMOVE_TAG_VALUE);
			tagValues.remove(tag);
			changedTags.remove(tag);
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

	public Set<String> getChangedTags() {
		return changedTags;
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

	public void updateTypeTag(String string, boolean userChanges) {
		tagValues.put(POI_TYPE_TAG, string);
		if (userChanges) {
			changedTags.add(POI_TYPE_TAG);
		}
		retrieveType();
		PoiType pt = getPoiTypeDefined();
		if(pt != null) {
			tagValues.put(REMOVE_TAG_PREFIX+pt.getOsmTag(), REMOVE_TAG_VALUE);
			tagValues.put(REMOVE_TAG_PREFIX+pt.getOsmTag2(), REMOVE_TAG_VALUE);
			tagValues.remove(pt.getOsmTag());
			tagValues.remove(pt.getOsmTag2());
			changedTags.removeAll(Arrays.asList(pt.getOsmTag(), pt.getOsmTag2()));
			category = pt.getCategory();
		}
		notifyDatasetChanged(POI_TYPE_TAG);
	}
}