package net.osmand.osm;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class PoiType extends AbstractPoiType {

	public static final int DEFAULT_ORDER = 90;
	public static final int DEFAULT_MIN_COUNT = 3;
	public static final int DEFAULT_MAX_PER_MAP = 100;
	
	private PoiCategory category;
	private PoiFilter filter;
	private AbstractPoiType parentType;
	private PoiType referenceType;
	private String osmTag;
	private String osmTag2;
	private String osmValue;
	private String osmValue2;
	
	private String editTag;
	private String editValue;
	private String editTag2;
	private String editValue2;
	private boolean filterOnly;

	private String nameTag;
	private boolean text;
	private boolean nameOnly;
	private boolean relation;
	private int order = DEFAULT_ORDER;
	private boolean topIndex = false;
	private boolean hidden = false;
	private int maxPerMap;
	private int minCount;


	public PoiType(MapPoiTypes poiTypes, PoiCategory category, PoiFilter filter, String keyName, String iconName) {
		super(keyName, poiTypes, iconName);
		this.category = category;
		this.filter = filter;
	}

	public PoiType getReferenceType() {
		return referenceType;
	}
	
	public void setReferenceType(PoiType referenceType) {
		this.referenceType = referenceType;
	}
	
	public boolean isReference() {
		return referenceType != null;
	}

	public String getOsmTag() {
		if(isReference()) {
			return referenceType.getOsmTag();
		}
		if(editTag != null) {
			return editTag;
		}
		if(osmTag != null && osmTag.startsWith("osmand_amenity")) {
			return "amenity";
		}
		return osmTag;
	}
	
	public String getRawOsmTag() {
		if(isReference()) {
			return referenceType.getOsmTag();
		}
		return osmTag;
	}
	
	public void setOsmEditTagValue(String osmTag, String editValue) {
		this.editTag = osmTag;
		this.editValue = editValue;
	}

	public void setOsmEditTagValue2(String osmTag, String editValue) {
		this.editTag2 = osmTag;
		this.editValue2 = editValue;
	}

	public String getEditOsmTag() {
		if (isReference()) {
			return referenceType.getEditOsmTag();
		}
		if (editTag == null) {
			return getOsmTag();
		}
		return editTag;
	}
	
	public String getEditOsmValue() {
		if (isReference()) {
			return referenceType.getEditOsmValue();
		}
		if (editValue == null) {
			return getOsmValue();
		}
		return editValue;
	}

	public String getEditOsmTag2() {
		if (isReference()) {
			return referenceType.getEditOsmTag2();
		}
		return editTag2;
	}

	public String getEditOsmValue2() {
		if (isReference()) {
			return referenceType.getEditOsmValue2();
		}
		return editValue2;
	}

	public void setOsmTag(String osmTag) {
		this.osmTag = osmTag;
	}

	public String getOsmTag2() {
		if(isReference()) {
			return referenceType.getOsmTag2();
		}
		return osmTag2;
	}

	public void setOsmTag2(String osmTag2) {
		this.osmTag2 = osmTag2;
	}

	public String getOsmValue() {
		if(isReference()) {
			return referenceType.getOsmValue();
		}
		return osmValue;
	}

	public void setOsmValue(String osmValue) {
		this.osmValue = osmValue;
	}

	public String getOsmValue2() {
		if(isReference()) {
			return referenceType.getOsmValue2();
		}
		return osmValue2;
	}

	public void setOsmValue2(String osmValue2) {
		this.osmValue2 = osmValue2;
	}

	public boolean isFilterOnly() {
		return filterOnly;
	}

	public void setFilterOnly(boolean filterOnly) {
		this.filterOnly = filterOnly;
	}

	public PoiCategory getCategory() {
		return category;
	}

	public PoiFilter getFilter() {
		return filter;
	}

	public Map<PoiCategory, LinkedHashSet<String>> putTypes(Map<PoiCategory, LinkedHashSet<String>> acceptedTypes) {
		if (isAdditional()) {
			parentType.putTypes(acceptedTypes);
			if (filterOnly) {
				LinkedHashSet<String> set = acceptedTypes.get(category);
				for (PoiType pt : category.getPoiTypes()) {
					List<PoiType> poiAdditionals = pt.getPoiAdditionals();
					if (poiAdditionals == null) {
						continue;
					}
					for (PoiType poiType : poiAdditionals) {
						if (poiType.getKeyName().equals(keyName)) {
							set.add(pt.getKeyName());
						}
					}
				}
			}
			return acceptedTypes;
		}
		PoiType rt = getReferenceType();
		PoiType poiType = rt != null ? rt : this;
		if (!acceptedTypes.containsKey(poiType.category)) {
			acceptedTypes.put(poiType.category, new LinkedHashSet<String>());
		}
		LinkedHashSet<String> set = acceptedTypes.get(poiType.category);
		if(set != null) {
			set.add(poiType.getKeyName());
		}
		return acceptedTypes;
	}

	@Override
	public String getParentTypeName() {
		if (parentType != null) {
			return parentType.getTranslation();
		} else if (category != null) {
			return category.getTranslation();
		} else {
			return "";
		}
	}

	public void setAdditional(AbstractPoiType parentType) {
        this.parentType = parentType;
    }

    public boolean isAdditional(){
        return parentType != null;
    }

    public AbstractPoiType getParentType() {
        return parentType;
    }
    
    public boolean isText() {
    	return text;
    }
    
    public void setText(boolean text) {
    	this.text = text;
    }
    
    public String getNameTag() {
		return nameTag;
	}
    
    public void setNameTag(String nameTag) {
		this.nameTag = nameTag;
	}

	public boolean isNameOnly() {
		return nameOnly;
	}

	public void setNameOnly(boolean nameOnly) {
		this.nameOnly = nameOnly;
	}

	public boolean isRelation() {
		return relation;
	}

	public void setRelation(boolean relation) {
		this.relation = relation;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	public boolean isHidden() {
		return hidden;
	}

	@Override
	public String toString() {
		return "PoiType{" +
				"category=" + category +
				", parentType=" + parentType +
				", referenceType=" + referenceType +
				", osmTag='" + osmTag + '\'' +
				", osmTag2='" + osmTag2 + '\'' +
				", osmValue='" + osmValue + '\'' +
				", osmValue2='" + osmValue2 + '\'' +
				", text=" + text +
				", nameOnly=" + nameOnly +
				", relation=" + relation +
				", order=" + order +
				", hidden=" + hidden +
				'}';
	}

	public boolean isTopIndex() {
		return topIndex;
	}

	public void setTopIndex(boolean topIndex) {
		this.topIndex = topIndex;
	}

	public int getMaxPerMap() {
		return maxPerMap;
	}

	public void setMaxPerMap(int maxPerMap) {
		this.maxPerMap = maxPerMap;
	}

	public int getMinCount() {
		return minCount;
	}

	public void setMinCount(int minCount) {
		this.minCount = minCount;
	}

	public Map<String, String> getOsmTagsValues() {
		Map<String, String> tags = new LinkedHashMap<>();
		String tag1 = getRawOsmTag(), val1 = getOsmValue();
		if (tag1 != null && val1 != null) {
			tags.put(tag1, val1);
		}
		String tag2 = getOsmTag2(), val2 = getOsmValue2();
		if (tag2 != null && val2 != null) {
			tags.put(tag2, val2);
		}
		return tags;
	}
}
