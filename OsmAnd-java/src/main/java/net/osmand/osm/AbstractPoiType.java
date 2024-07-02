package net.osmand.osm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public abstract class AbstractPoiType {

	protected final String keyName;
	protected final MapPoiTypes registry;
	private List<PoiType> poiAdditionals = null;
	private List<PoiType> poiAdditionalsCategorized = null;
	private boolean topVisible;
	private String lang;
	private AbstractPoiType baseLangType;
	private boolean notEditableOsm;
	private String poiAdditionalCategory;
	private List<String> excludedPoiAdditionalCategories;
	private String synonyms;
	private String enTranslation;
	private String translation;

	public AbstractPoiType(String keyName, MapPoiTypes registry) {
		this.keyName = keyName;
		this.registry = registry;
	}

	public void setBaseLangType(AbstractPoiType baseLangType) {
		this.baseLangType = baseLangType;
	}

	public AbstractPoiType getBaseLangType() {
		return baseLangType;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public String getLang() {
		return lang;
	}

	public String getKeyName() {
		return keyName;
	}

	public String getIconKeyName() {
		return getFormattedKeyName();
	}

	public String getFormattedKeyName() {
		return formatKeyName(getKeyName());
	}

	protected String formatKeyName(String kn) {
		if (kn.startsWith("osmand_")) {
			kn = kn.substring("osmand_".length());
		}
		return kn.replace(':', '_');
	}

	public void setTopVisible(boolean topVisible) {
		this.topVisible = topVisible;
	}

	public boolean isTopVisible() {
		return topVisible;
	}

	public boolean isAdditional() {
		return this instanceof PoiType && this.isAdditional();
	}

	public String getTranslation() {
		if (translation == null) {
			translation = registry.getTranslation(this);
		}
		return translation;
	}

	public String getSynonyms() {
		if (synonyms == null) {
			synonyms = registry.getSynonyms(this);
		}
		return synonyms;
	}

	public String getEnTranslation() {
		if (enTranslation == null) {
			enTranslation = registry.getEnTranslation(this);
		}
		return enTranslation;
	}

	public String getPoiAdditionalCategoryTranslation() {
		if (poiAdditionalCategory != null) {
			return registry.getPoiTranslation(poiAdditionalCategory);
		} else {
			return null;
		}
	}

	public void addPoiAdditional(PoiType tp) {
		if (poiAdditionals == null) {
			poiAdditionals = new ArrayList<>();
		}
		poiAdditionals.add(tp);
		if (tp.getPoiAdditionalCategory() != null) {
			if (poiAdditionalsCategorized == null) {
				poiAdditionalsCategorized = new ArrayList<>();
			}
			poiAdditionalsCategorized.add(tp);
		}
	}

	public void addPoiAdditionalsCategorized(List<PoiType> tps) {
		if (poiAdditionals == null) {
			poiAdditionals = new ArrayList<>();
		}
		poiAdditionals.addAll(tps);
		if (poiAdditionalsCategorized == null) {
			poiAdditionalsCategorized = new ArrayList<>();
		}
		poiAdditionalsCategorized.addAll(tps);
	}

	public List<PoiType> getPoiAdditionals() {
		if (poiAdditionals == null) {
			return Collections.emptyList();
		}
		return poiAdditionals;
	}

	public List<PoiType> getPoiAdditionalsCategorized() {
		if (poiAdditionalsCategorized == null) {
			return Collections.emptyList();
		}
		return poiAdditionalsCategorized;
	}

	public boolean isNotEditableOsm() {
		return notEditableOsm;
	}

	public void setNotEditableOsm(boolean notEditableOsm) {
		this.notEditableOsm = notEditableOsm;
	}

	public String getPoiAdditionalCategory() {
		return poiAdditionalCategory;
	}

	public void setPoiAdditionalCategory(String poiAdditionalCategory) {
		this.poiAdditionalCategory = poiAdditionalCategory;
	}

	public List<String> getExcludedPoiAdditionalCategories() {
		return excludedPoiAdditionalCategories;
	}

	public void addExcludedPoiAdditionalCategories(String[] excludedPoiAdditionalCategories) {
		if (this.excludedPoiAdditionalCategories == null) {
			this.excludedPoiAdditionalCategories = new ArrayList<>();
		}
		Collections.addAll(this.excludedPoiAdditionalCategories, excludedPoiAdditionalCategories);
	}

	public abstract Map<PoiCategory, LinkedHashSet<String>> putTypes(Map<PoiCategory, LinkedHashSet<String>> acceptedTypes);

	@Override
	public String toString() {
		return keyName;
	}
}
