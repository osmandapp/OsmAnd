package net.osmand.osm;

import java.util.LinkedHashSet;
import java.util.Map;

public class PoiType extends AbstractPoiType {
	
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
	private int order = 90;


	public PoiType(MapPoiTypes poiTypes, PoiCategory category, PoiFilter filter, String keyName) {
		super(keyName, poiTypes);
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
			return parentType.putTypes(acceptedTypes);
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
				'}';
	}
}
