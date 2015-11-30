package net.osmand.osm;

import java.util.LinkedHashSet;
import java.util.Map;

public class PoiType extends AbstractPoiType {
	
	private PoiCategory category;
	private AbstractPoiType parentType;
	private PoiType referenceType;
	private String osmTag;
	private String osmTag2;
	private String osmValue;
	private String osmValue2;
	
	
	private String nameTag;
	private boolean text;
	private boolean nameOnly;
	private boolean relation;
	private int order = 90;


	public PoiType(MapPoiTypes poiTypes, PoiCategory category, String name) {
		super(name, poiTypes);
		this.category = category;
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
		return osmTag;
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

	public PoiCategory getCategory() {
		return category;
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
