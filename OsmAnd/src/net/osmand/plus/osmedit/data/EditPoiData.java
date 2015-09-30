package net.osmand.plus.osmedit.data;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.osmand.data.Amenity;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.OSMSettings;
import net.osmand.util.Algorithms;

public class EditPoiData {
	private Set<TagsChangedListener> mListeners = new HashSet<>();
	private LinkedHashMap<String, String > tagValues = new LinkedHashMap<String, String>();
	private boolean isInEdit = false;
	public final Amenity amenity;
	public static final String POI_TYPE_TAG = "poi_type_tag";
	
	public EditPoiData(Amenity amenity, Node node, Map<String, PoiType> allTranslatedSubTypes) {
		this.amenity = amenity;
		initTags(node, allTranslatedSubTypes);
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

		tryAddTag(OSMSettings.OSMTagKey.ADDR_STREET.getValue(),
				node.getTag(OSMSettings.OSMTagKey.ADDR_STREET));
		tryAddTag(OSMSettings.OSMTagKey.ADDR_HOUSE_NUMBER.getValue(),
				node.getTag(OSMSettings.OSMTagKey.ADDR_HOUSE_NUMBER));
		tryAddTag(OSMSettings.OSMTagKey.PHONE.getValue(),
				amenity.getPhone());
		tryAddTag(OSMSettings.OSMTagKey.WEBSITE.getValue(),
				amenity.getSite());
		for (String tag : node.getTagKeySet()) {
			tryAddTag(tag, node.getTag(tag));
		}
		String subType = amenity.getSubType();
		String key;
		String value;
		if (allTranslatedSubTypes.get(subType) != null) {
			PoiType pt = allTranslatedSubTypes.get(subType);
			key = pt.getOsmTag();
			value = pt.getOsmValue();
		} else {
			key = amenity.getType().getDefaultTag();
			value = subType;
		}
		tagValues.remove(key);
		tagValues.put(POI_TYPE_TAG, value);		
	}


	public Map<String, String> getTagValues() {
		return Collections.unmodifiableMap(tagValues);
	}
	

	public void putTag(String tag, String value) {
		if(isInEdit) {
			throw new IllegalStateException("Can't modify in edit mode");
		}
		try { 
			isInEdit = true;
			tagValues.put(tag, value);
			notifyDatasetChanged();
		} finally {
			isInEdit = false;
		}
	}
	
	public void notifyToUpdateUI() {
		if(isInEdit) {
			throw new IllegalStateException("Can't modify in edit mode");
		}
		try { 
			isInEdit = true;
			notifyDatasetChanged();
		} finally {
			isInEdit = false;
		}		
	}
	
	public void removeTag(String tag) {
		if(isInEdit) {
			throw new IllegalStateException("Can't modify in edit mode");
		}
		try { 
			isInEdit = true;
			tagValues.remove(tag);
			notifyDatasetChanged();
		} finally {
			isInEdit = false;
		}
	}
	
	public boolean isInEdit() {
		return isInEdit;
	}
	
	
	private void notifyDatasetChanged() {
		for (TagsChangedListener listener : mListeners) {
			listener.onTagsChanged();
		}
	}

	public void addListener(TagsChangedListener listener) {
		mListeners.add(listener);
	}

	public void deleteListener(TagsChangedListener listener) {
		mListeners.remove(listener);
	}

	public interface TagsChangedListener {
		
		void onTagsChanged();
		
	}

	

	
}