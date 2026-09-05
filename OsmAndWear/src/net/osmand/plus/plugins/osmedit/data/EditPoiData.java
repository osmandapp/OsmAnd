package net.osmand.plus.plugins.osmedit.data;

import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.Entity;
import net.osmand.plus.OsmandApplication;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static net.osmand.osm.edit.Entity.POI_TYPE_TAG;

public class EditPoiData {
	private static final Log LOG = PlatformUtil.getLog(EditPoiData.class);
	private final Set<TagsChangedListener> mListeners = new HashSet<>();
	private final LinkedHashMap<String, String> tagValues = new LinkedHashMap<>();
	private final LinkedHashMap<String, String> initTagValues = new LinkedHashMap<>();
	private boolean isInEdit;
	private final Entity entity;

	public static final String REMOVE_TAG_VALUE = "DELETE";
	private final Map<String, PoiType> allTranslatedSubTypes;
	private PoiCategory category;
	private PoiType currentPoiType;

	private final Set<String> changedTags = Collections.synchronizedSet(new HashSet<>());
	
	public EditPoiData(Entity entity, OsmandApplication app) {
		allTranslatedSubTypes = app.getPoiTypes().getAllTranslatedNames(true);
		category = app.getPoiTypes().getOtherPoiCategory();
		this.entity = entity;
		initTags(entity);
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
			removeCurrentTypeTag();
			currentPoiType=null;
		}
	}
	
	@Nullable
	public PoiCategory getPoiCategory() {
		return category;
	}
	
	@Nullable
	public PoiType getCurrentPoiType() {
		return currentPoiType;
	}
	
	public PoiType getPoiTypeDefined() {
		return allTranslatedSubTypes.get(getPoiTypeString().toLowerCase());
	}
	
	public String getPoiTypeString() {
		String s = tagValues.get(POI_TYPE_TAG) ;
		return s == null ? "" : s;
	}

	public Entity getEntity() {
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
		if (value != null) {
			tagValues.put(key, value);
		}
	}
	
	private void initTags(Entity entity) {
		checkNotInEdit();
		for (String s : entity.getTagKeySet()) {
			tryAddTag(s, entity.getTag(s));
		}
		Set<String> changedTags = entity.getChangedTags();
		if (!Algorithms.isEmpty(changedTags)) {
			this.changedTags.addAll(changedTags);
		}
		retrieveType();
	}

	public void setupInitPoint() {
		initTagValues.clear();
		initTagValues.putAll(tagValues);
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
			tagValues.remove(Entity.REMOVE_TAG_PREFIX + tag);
			String oldValue = tagValues.get(tag);
			if (oldValue == null || !oldValue.equals(value)) {
				changedTags.add(tag);
			}
			String tagVal = value != null ? value : "";
			tagValues.put(tag, tagVal);
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
			tagValues.put(Entity.REMOVE_TAG_PREFIX + tag, REMOVE_TAG_VALUE);
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

	public boolean hasChanges() {
		return !Algorithms.objectEquals(initTagValues, tagValues);
	}

	public void updateTypeTag(String string, boolean userChanges) {
		checkNotInEdit();
		try {
			String val = string != null ? string : "";
			tagValues.put(POI_TYPE_TAG, val);
			if (userChanges) {
				changedTags.add(POI_TYPE_TAG);
			}
			retrieveType();
			PoiType pt = getPoiTypeDefined();
			String editOsmTag = pt != null ? pt.getEditOsmTag() : null;
			if (editOsmTag != null) {
				removeTypeTagWithPrefix(!tagValues.containsKey(Entity.REMOVE_TAG_PREFIX + editOsmTag));
				currentPoiType = pt;
				String tagVal = pt.getEditOsmValue() != null ? pt.getEditOsmValue() : "";
				tagValues.put(editOsmTag, tagVal);
				if (userChanges) {
					changedTags.add(editOsmTag);
				}
				category = pt.getCategory();
			} else if (currentPoiType != null) {
				removeTypeTagWithPrefix(true);
				category = currentPoiType.getCategory();
			}
			notifyDatasetChanged(POI_TYPE_TAG);
		} finally {
			isInEdit = false;
		}
	}

	private void removeTypeTagWithPrefix(boolean needRemovePrefix) {
		if (currentPoiType != null) {
			if (needRemovePrefix) {
				tagValues.put(Entity.REMOVE_TAG_PREFIX + currentPoiType.getOsmTag2(), REMOVE_TAG_VALUE);
				tagValues.put(Entity.REMOVE_TAG_PREFIX + currentPoiType.getEditOsmTag(), REMOVE_TAG_VALUE);
				tagValues.put(Entity.REMOVE_TAG_PREFIX + currentPoiType.getEditOsmTag2(), REMOVE_TAG_VALUE);
			} else {
				tagValues.remove(Entity.REMOVE_TAG_PREFIX + currentPoiType.getOsmTag2());
				tagValues.remove(Entity.REMOVE_TAG_PREFIX + currentPoiType.getEditOsmTag());
				tagValues.remove(Entity.REMOVE_TAG_PREFIX + currentPoiType.getEditOsmTag2());
			}
			removeCurrentTypeTag();
		}
	}

	private void removeCurrentTypeTag() {
		if (currentPoiType != null) {
			tagValues.remove(currentPoiType.getOsmTag2());
			tagValues.remove(currentPoiType.getEditOsmTag());
			tagValues.remove(currentPoiType.getEditOsmTag2());
			changedTags.removeAll(Arrays.asList(currentPoiType.getEditOsmTag(),currentPoiType.getEditOsmTag2(), currentPoiType.getOsmTag2()));
		}
	}

	public boolean hasEmptyValue() {
		for (Map.Entry<String, String> tag : tagValues.entrySet()) {
			if (tag.getValue().isEmpty() && !POI_TYPE_TAG.equals(tag.getKey())) {
				return true;
			}
		}
		return false;
	}
}