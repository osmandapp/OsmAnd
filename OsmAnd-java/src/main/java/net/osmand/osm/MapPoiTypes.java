package net.osmand.osm;

import net.osmand.PlatformUtil;
import net.osmand.StringMatcher;
import net.osmand.data.Amenity;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;


public class MapPoiTypes {
	private static final String OTHER_MAP_CATEGORY = "Other";
	private static MapPoiTypes DEFAULT_INSTANCE = null;
	private static final Log log = PlatformUtil.getLog(MapRenderingTypes.class);
	private String resourceName;
	private List<PoiCategory> categories = new ArrayList<PoiCategory>();
	private Set<String> forbiddenTypes = new HashSet<>();
	private PoiCategory otherCategory;

	public static final String WIKI_LANG = "wiki_lang";
	public static final String WIKI_PLACE = "wiki_place";
	public static final String OSM_WIKI_CATEGORY = "osmwiki";
	public static final String SPEED_CAMERA = "speed_camera";

	public static final String ROUTES = "routes";
	public static final String ROUTE_ARTICLE = "route_article";
	public static final String ROUTE_ARTICLE_POINT = "route_article_point";
	public static final String CATEGORY = "category";
	public static final String ROUTE_TRACK = "route_track";
	public static final String ROUTE_TRACK_POINT = "route_track_point";

	private PoiTranslator poiTranslator = null;
	private boolean init;
	Map<String, PoiType> poiTypesByTag = new LinkedHashMap<String, PoiType>();
	Map<String, String> deprecatedTags = new LinkedHashMap<String, String>();
	Map<String, String> poiAdditionalCategoryIconNames = new LinkedHashMap<String, String>();
	List<PoiType> textPoiAdditionals = new ArrayList<PoiType>();

	public Map<String, PoiType> topIndexPoiAdditional = new LinkedHashMap<String, PoiType>();
	public static final String TOP_INDEX_ADDITIONAL_PREFIX = "top_index_";

	public MapPoiTypes(String fileName) {
		this.resourceName = fileName;
	}

	public interface PoiTranslator {

		String getTranslation(AbstractPoiType type);
		String getTranslation(String keyName);

		String getEnTranslation(AbstractPoiType type);
		String getEnTranslation(String keyName);

		String getSynonyms(AbstractPoiType type);
		String getSynonyms(String keyName);

		String getAllLanguagesTranslationSuffix();
	}

	public static MapPoiTypes getDefaultNoInit() {
		if (DEFAULT_INSTANCE == null) {
			DEFAULT_INSTANCE = new MapPoiTypes(null);
		}
		return DEFAULT_INSTANCE;
	}


	public static void setDefault(MapPoiTypes types) {
		DEFAULT_INSTANCE = types;
		DEFAULT_INSTANCE.init();
	}

	public static MapPoiTypes getDefault() {
		if (DEFAULT_INSTANCE == null) {
			DEFAULT_INSTANCE = new MapPoiTypes(null);
			DEFAULT_INSTANCE.init();
		}
		return DEFAULT_INSTANCE;
	}

	public boolean isInit() {
		return init;
	}

	public boolean isOtherCategory(PoiCategory poiCategory) {
		return Objects.equals(otherCategory, poiCategory);
	}

	public PoiCategory getOtherPoiCategory() {
		return otherCategory;
	}

	public PoiCategory getOtherMapCategory() {
		return getPoiCategoryByName(OTHER_MAP_CATEGORY, true);
	}

	public String getPoiAdditionalCategoryIconName(String category) {
		return poiAdditionalCategoryIconNames.get(category);
	}

	public List<PoiType> getTextPoiAdditionals() {
		return textPoiAdditionals;
	}

	public List<AbstractPoiType> getTopVisibleFilters() {
		List<AbstractPoiType> lf = new ArrayList<AbstractPoiType>();
		for (int i = 0; i < categories.size(); i++) {
			PoiCategory pc = categories.get(i);
			if (pc.isTopVisible()) {
				lf.add(pc);
			}
			for (PoiFilter p : pc.getPoiFilters()) {
				if (p.isTopVisible()) {
					lf.add(p);
				}
			}
			for (PoiType p : pc.getPoiTypes()) {
				if (p.isTopVisible()) {
					lf.add(p);
				}
			}
		}
		sortList(lf);
		return lf;
	}

	public PoiCategory getOsmwiki() {
		for (int i = 0; i < categories.size(); i++) {
			PoiCategory category = categories.get(i);
			if (category.isWiki()) {
				return category;
			}
		}
		return null;
	}

	public PoiCategory getRoutes() {
		for (int i = 0; i < categories.size(); i++) {
			PoiCategory category = categories.get(i);
			if (category.isRoutes()) {
				return category;
			}
		}
		return null;
	}

	public List<String> getAllAvailableWikiLocales() {
		List<String> availableWikiLocales = new ArrayList<>();
		for (PoiType type : getOsmwiki().getPoiTypeByKeyName(WIKI_PLACE).getPoiAdditionals()) {
			String name = type.getKeyName();
			String wikiLang = WIKI_LANG + ":";
			if (name != null && name.startsWith(wikiLang)) {
				String locale = name.substring(wikiLang.length());
				availableWikiLocales.add(locale);
			}
		}
		return availableWikiLocales;
	}

	private void sortList(List<? extends AbstractPoiType> lf) {
		final Collator instance = Collator.getInstance();
		Collections.sort(lf, new Comparator<AbstractPoiType>() {
			@Override
			public int compare(AbstractPoiType object1, AbstractPoiType object2) {
				return instance.compare(object1.getTranslation(), object2.getTranslation());
			}
		});
	}

	public PoiCategory getUserDefinedCategory() {
		return otherCategory;
	}

	public PoiType getPoiTypeByKey(String name) {
		for (int i = 0; i < categories.size(); i++) {
			PoiCategory pc = categories.get(i);
			PoiType pt = pc.getPoiTypeByKeyName(name);
			if (pt != null && !pt.isReference()) {
				return pt;
			}
		}
		return null;
	}

	public PoiType getPoiTypeByKeyInCategory(PoiCategory category, String keyName) {
		if (category != null) {
			return category.getPoiTypeByKeyName(keyName);
		}
		return null;
	}

	public AbstractPoiType getAnyPoiTypeByKey(String name) {
		return getAnyPoiTypeByKey(name, true);
	}

	public AbstractPoiType getAnyPoiTypeByKey(String name, boolean skipAdditional) {
		for (int i = 0; i < categories.size(); i++) {
			PoiCategory pc = categories.get(i);
			if (pc.getKeyName().equals(name)) {
				return pc;
			}
			for (PoiFilter pf : pc.getPoiFilters()) {
				if (pf.getKeyName().equals(name)) {
					return pf;
				}
				// search in poi additional
				if (!skipAdditional) {
					for (PoiType type : pf.getPoiTypes()) {
						if (type.getKeyName().equals(name)) {
							return type;
						}
						AbstractPoiType foundType = findInAdds(type.getPoiAdditionals(), name);
						if (foundType != null) {
							return foundType;
						}
					}
				}
			}
			PoiType pt = pc.getPoiTypeByKeyName(name);
			if (pt != null && !pt.isReference()) {
				return pt;
			}
		}
		return null;
	}
	
	private AbstractPoiType findInAdds(List<PoiType> adds, String name) {
		for (PoiType additional : adds) {
			if (additional.getKeyName().equals(name)) {
				return additional;
			}
			AbstractPoiType foundType = findInAdds(additional.getPoiAdditionals(), name);
			if (foundType != null) {
				return foundType;
			}
		}
		return null;
	}

	public Map<String, PoiType> getAllTranslatedNames(boolean skipNonEditable) {
		Map<String, PoiType> translation = new HashMap<String, PoiType>();
		for (int i = 0; i < categories.size(); i++) {
			PoiCategory pc = categories.get(i);
			if (skipNonEditable && pc.isNotEditableOsm()) {
				continue;
			}
			addPoiTypesTranslation(skipNonEditable, translation, pc);
		}
		return translation;
	}


	private void addPoiTypesTranslation(boolean skipNonEditable, Map<String, PoiType> translation, PoiFilter pf) {
		for (PoiType pt : pf.getPoiTypes()) {
			if (pt.isReference()) {
				continue;
			}
			if (pt.getBaseLangType() != null) {
				continue;
			}
			if (skipNonEditable && pt.isNotEditableOsm()) {
				continue;
			}
			translation.put(pt.getKeyName().replace('_', ' ').toLowerCase(), pt);
			translation.put(pt.getTranslation().toLowerCase(), pt);
		}
	}

	public List<AbstractPoiType> getAllTypesTranslatedNames(StringMatcher matcher) {
		List<AbstractPoiType> tm = new ArrayList<AbstractPoiType>();
		for (int i = 0; i < categories.size(); i++) {
			PoiCategory pc = categories.get(i);
			if (pc == getOtherMapCategory()) {
				continue;
			}
			addIf(tm, pc, matcher);
			for (PoiFilter pt : pc.getPoiFilters()) {
				addIf(tm, pt, matcher);
			}
			for (PoiType pt : pc.getPoiTypes()) {
				if (pt.isReference()) {
					continue;
				}
				addIf(tm, pt, matcher);
			}
		}

		return tm;
	}

	private void addIf(List<AbstractPoiType> tm, AbstractPoiType pc, StringMatcher matcher) {
		if (matcher.matches(pc.getTranslation()) || matcher.matches(pc.getKeyName().replace('_', ' '))) {
			tm.add(pc);
		}
		List<PoiType> additionals = pc.getPoiAdditionals();
		if (additionals != null) {
			for (PoiType a : additionals) {
				addIf(tm, a, matcher);
			}
		}
	}


	public Map<String, PoiType> getAllTranslatedNames(PoiCategory pc, boolean onlyTranslation) {
		Map<String, PoiType> translation = new TreeMap<String, PoiType>();
		for (PoiType pt : pc.getPoiTypes()) {
			translation.put(pt.getTranslation(), pt);

			if (!onlyTranslation) {
//				translation.put(pt.getKeyName(), pt);
				translation.put(Algorithms.capitalizeFirstLetterAndLowercase(pt.getKeyName().replace('_', ' ')), pt);
			}
		}
		return translation;
	}

	public PoiCategory getPoiCategoryByName(String name) {
		return getPoiCategoryByName(name, false);
	}

	public PoiCategory getPoiCategoryByName(String name, boolean create) {
		if (name.equals("leisure") && !create) {
			name = "entertainment";
		}
		if (name.equals("historic") && !create) {
			name = "tourism";
		}
		for (PoiCategory p : categories) {
			if (p.getKeyName().equalsIgnoreCase(name)) {
				return p;
			}
		}
		if (create) {
			PoiCategory lastCategory = new PoiCategory(this, name, categories.size());
			if (!lastCategory.getKeyName().equals(OTHER_MAP_CATEGORY)) {
				lastCategory.setTopVisible(true);
			}
			addCategory(lastCategory);
			return lastCategory;
		}
		return otherCategory;
	}

	private void addCategory(PoiCategory category) {
		List<PoiCategory> categories = new ArrayList<>(this.categories);
		categories.add(category);
		this.categories = categories;
	}
	
	public List<PoiCategory> getCategories() {
		return categories;
	}

	public PoiTranslator getPoiTranslator() {
		return poiTranslator;
	}

	public void setPoiTranslator(PoiTranslator poiTranslator) {
		this.poiTranslator = poiTranslator;
		List<PoiCategory> categories = new ArrayList<>(this.categories);
		sortList(categories);
		this.categories = categories;
	}

	public void init() {
		init(null);
	}

	public void init(String resourceName) {
		if (resourceName != null) {
			this.resourceName = resourceName;
		}
		try {
			InputStream is;
			if (this.resourceName == null) {
				is = MapPoiTypes.class.getResourceAsStream("poi_types.xml"); //$NON-NLS-1$
			} else {
				is = new FileInputStream(this.resourceName);
			}
			initFromInputStream(is);

		} catch (IOException e) {
			log.error("Unexpected error", e); //$NON-NLS-1$
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (RuntimeException e) {
			log.error("Unexpected error", e); //$NON-NLS-1$
			e.printStackTrace();
			throw e;
		}
	}

	public void initFromInputStream(InputStream is) {
		long time = System.currentTimeMillis();
		List<PoiType> referenceTypes = new ArrayList<>();
		final Map<String, PoiType> allTypes = new LinkedHashMap<>();
		final Map<String, List<PoiType>> categoryPoiAdditionalMap = new LinkedHashMap<>();
		final Map<AbstractPoiType, Set<String>> abstractTypeAdditionalCategories = new LinkedHashMap<>();
		final Map<String, PoiType> poiTypesByTag = new LinkedHashMap<>();
		final Map<String, String> deprecatedTags = new LinkedHashMap<>();
		final Map<String, String> poiAdditionalCategoryIconNames = new LinkedHashMap<>();
		final List<PoiType> textPoiAdditionals = new ArrayList<>();

		List<PoiCategory> categoriesList = new ArrayList<>();
		try {
			XmlPullParser parser = PlatformUtil.newXMLPullParser();
			int tok;
			parser.setInput(is, "UTF-8");
			PoiCategory lastCategory = null;
			Set<String> lastCategoryPoiAdditionalsCategories = new TreeSet<>();
			PoiFilter lastFilter = null;
			Set<String> lastFilterPoiAdditionalsCategories = new TreeSet<>();
			PoiType lastType = null;
			Set<String> lastTypePoiAdditionalsCategories = new TreeSet<>();
			String lastPoiAdditionalCategory = null;
			PoiCategory localOtherMapCategory = new PoiCategory(this, OTHER_MAP_CATEGORY, categoriesList.size());
			categoriesList.add(localOtherMapCategory);
			while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if (tok == XmlPullParser.START_TAG) {
					String name = parser.getName();
                    switch (name) {
                        case "poi_category" -> {
                            lastCategory = new PoiCategory(this, parser.getAttributeValue("", "name"), categoriesList.size());
                            lastCategory.setTopVisible(Boolean.parseBoolean(parser.getAttributeValue("", "top")));
                            lastCategory.setNotEditableOsm("true".equals(parser.getAttributeValue("", "no_edit")));
                            lastCategory.setDefaultTag(parser.getAttributeValue("", "default_tag"));
                            if (!Algorithms.isEmpty(parser.getAttributeValue("", "poi_additional_category"))) {
                                Collections.addAll(lastCategoryPoiAdditionalsCategories, parser.getAttributeValue("", "poi_additional_category").split(","));
                            }
                            if (!Algorithms.isEmpty(parser.getAttributeValue("", "excluded_poi_additional_category"))) {
                                lastCategory.addExcludedPoiAdditionalCategories(parser.getAttributeValue("", "excluded_poi_additional_category").split(","));
                                lastCategory.getExcludedPoiAdditionalCategories().forEach(lastCategoryPoiAdditionalsCategories::remove);
                            }
                            categoriesList.add(lastCategory);
                        }
                        case "poi_filter" -> {
                            String keyName = parser.getAttributeValue("", "name");
                            String iconName = parser.getAttributeValue("", "icon");
                            PoiFilter tp = new PoiFilter(this, lastCategory, keyName, iconName);
                            tp.setTopVisible(Boolean.parseBoolean(parser.getAttributeValue("", "top")));
                            lastFilter = tp;
                            lastFilterPoiAdditionalsCategories.addAll(lastCategoryPoiAdditionalsCategories);
                            if (!Algorithms.isEmpty(parser.getAttributeValue("", "poi_additional_category"))) {
                                Collections.addAll(lastFilterPoiAdditionalsCategories, parser.getAttributeValue("", "poi_additional_category").split(","));
                            }
                            if (!Algorithms.isEmpty(parser.getAttributeValue("", "excluded_poi_additional_category"))) {
                                lastFilter.addExcludedPoiAdditionalCategories(parser.getAttributeValue("", "excluded_poi_additional_category").split(","));
                                lastFilter.getExcludedPoiAdditionalCategories().forEach(lastFilterPoiAdditionalsCategories::remove);
                            }
                            if (lastCategory != null) {
                                lastCategory.addPoiType(tp);
                            }
                        }
                        case "poi_reference" -> {
                            PoiType tp = new PoiType(this, lastCategory, lastFilter, parser.getAttributeValue("", "name"));
                            referenceTypes.add(tp);
                            tp.setReferenceType(tp);
                            if (lastFilter != null) {
                                lastFilter.addPoiType(tp);
                            }
                            if (lastCategory != null) {
                                lastCategory.addPoiType(tp);
                            }
                        }
                        case "poi_additional" -> {
                            if (lastCategory == null) {
                                lastCategory = localOtherMapCategory;
                            }
                            PoiType baseType = parsePoiAdditional(parser, lastCategory, lastFilter, lastType, null, null,
                                    lastPoiAdditionalCategory, textPoiAdditionals);
                            if ("true".equals(parser.getAttributeValue("", "lang"))) {
                                for (String lng : MapRenderingTypes.langs) {
                                    parsePoiAdditional(parser, lastCategory, lastFilter, lastType, lng, baseType,
                                            lastPoiAdditionalCategory, textPoiAdditionals);
                                    if (baseType.isTopIndex()) {
                                        topIndexPoiAdditional.put(TOP_INDEX_ADDITIONAL_PREFIX + baseType.getKeyName() + ":" + lng, baseType);
                                    }
                                }
                                parsePoiAdditional(parser, lastCategory, lastFilter, lastType, "en", baseType,
                                        lastPoiAdditionalCategory, textPoiAdditionals);
                                if (baseType.isTopIndex()) {
                                    topIndexPoiAdditional.put(TOP_INDEX_ADDITIONAL_PREFIX + baseType.getKeyName() + ":en", baseType);
                                }
                            }
                            if (lastPoiAdditionalCategory != null) {
                                List<PoiType> categoryAdditionals = categoryPoiAdditionalMap.computeIfAbsent(lastPoiAdditionalCategory, k -> new ArrayList<>());
                                categoryAdditionals.add(baseType);
                            }
                            if (baseType.isTopIndex()) {
                                topIndexPoiAdditional.put(TOP_INDEX_ADDITIONAL_PREFIX + baseType.getKeyName(), baseType);
                            }
                        }
                        case "poi_additional_category" -> {
                            if (lastPoiAdditionalCategory == null) {
                                lastPoiAdditionalCategory = parser.getAttributeValue("", "name");
                                String icon = parser.getAttributeValue("", "icon");
                                if (!Algorithms.isEmpty(icon)) {
                                    poiAdditionalCategoryIconNames.put(lastPoiAdditionalCategory, icon);
                                }
                            }
                        }
                        case "poi_type" -> {
                            if (lastCategory == null) {
                                lastCategory = localOtherMapCategory;
                            }
                            if (!Algorithms.isEmpty(parser.getAttributeValue("", "deprecated_of"))) {
                                String vl = parser.getAttributeValue("", "name");
                                String target = parser.getAttributeValue("", "deprecated_of");
                                deprecatedTags.put(vl, target);
                            } else {
                                lastType = parsePoiType(allTypes, parser, lastCategory, lastFilter, null, null);
                                if ("true".equals(parser.getAttributeValue("", "lang"))) {
                                    for (String lng : MapRenderingTypes.langs) {
                                        parsePoiType(allTypes, parser, lastCategory, lastFilter, lng, lastType);
                                    }
                                }
                                lastTypePoiAdditionalsCategories.addAll(lastCategoryPoiAdditionalsCategories);
                                lastTypePoiAdditionalsCategories.addAll(lastFilterPoiAdditionalsCategories);
                                if (!Algorithms.isEmpty(parser.getAttributeValue("", "poi_additional_category"))) {
                                    Collections.addAll(lastTypePoiAdditionalsCategories, parser.getAttributeValue("", "poi_additional_category").split(","));
                                }
                                if (!Algorithms.isEmpty(parser.getAttributeValue("", "excluded_poi_additional_category"))) {
                                    lastType.addExcludedPoiAdditionalCategories(parser.getAttributeValue("", "excluded_poi_additional_category").split(","));
                                    lastType.getExcludedPoiAdditionalCategories().forEach(lastTypePoiAdditionalsCategories::remove);
                                }
                            }
                        }
	                    default -> log.warn("Unknown start tag encountered: " + name);
                    }
				} else if (tok == XmlPullParser.END_TAG) {
					String name = parser.getName();
                    switch (name) {
                        case "poi_filter" -> {
                            if (!lastFilterPoiAdditionalsCategories.isEmpty()) {
                                abstractTypeAdditionalCategories.put(lastFilter, lastFilterPoiAdditionalsCategories);
                                lastFilterPoiAdditionalsCategories = new TreeSet<>();
                            }
                            lastFilter = null;
                        }
                        case "poi_type" -> {
                            if (!lastTypePoiAdditionalsCategories.isEmpty()) {
                                abstractTypeAdditionalCategories.put(lastType, lastTypePoiAdditionalsCategories);
                                lastTypePoiAdditionalsCategories = new TreeSet<>();
                            }
                            lastType = null;
                        }
                        case "poi_category" -> {
                            if (!lastCategoryPoiAdditionalsCategories.isEmpty()) {
                                abstractTypeAdditionalCategories.put(lastCategory, lastCategoryPoiAdditionalsCategories);
                                lastCategoryPoiAdditionalsCategories = new TreeSet<>();
                            }
                            lastCategory = null;
                        }
                        case "poi_additional_category" -> lastPoiAdditionalCategory = null;
	                    default -> {
		                    if (!name.equals("poi_additional") && !name.equals("poi_reference") && !name.equals("poi_types")) {
			                    log.warn("Unknown end tag encountered: " + name);
		                    }
	                    }
                    }
				}
			}

			is.close();
		} catch (IOException | XmlPullParserException e) {
			log.error("Unexpected error", e); //$NON-NLS-1$
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (RuntimeException e) {
			log.error("Unexpected error", e); //$NON-NLS-1$
			e.printStackTrace();
			throw e;
		}
		for (PoiType gt : referenceTypes) {
			PoiType pt = allTypes.get(gt.getKeyName());
			if (pt == null || pt.getOsmTag() == null) {
				throw new IllegalStateException("Can't find poi type for poi reference '" + gt.keyName + "'");
			} else {
				gt.setReferenceType(pt);
			}
		}
		for (Entry<AbstractPoiType, Set<String>> entry : abstractTypeAdditionalCategories.entrySet()) {
			for (String category : entry.getValue()) {
				List<PoiType> poiAdditionals = categoryPoiAdditionalMap.get(category);
				if (poiAdditionals != null) {
					for (PoiType poiType : poiAdditionals) {
						buildPoiAdditionalReference(poiType, entry.getKey(), textPoiAdditionals);
					}
				}
			}
		}
		this.categories = categoriesList;
		this.poiTypesByTag = poiTypesByTag;
		this.deprecatedTags = deprecatedTags;
		this.poiAdditionalCategoryIconNames = poiAdditionalCategoryIconNames;
		this.textPoiAdditionals = textPoiAdditionals;
		otherCategory = getPoiCategoryByName("user_defined_other");
		if (otherCategory == null) {
			throw new IllegalArgumentException("No poi category other");
		}
		init = true;
		log.info("Time to init poi types " + (System.currentTimeMillis() - time)); //$NON-NLS-1$
	}

	private PoiType buildPoiAdditionalReference(PoiType poiAdditional, AbstractPoiType parent, List<PoiType> textPoiAdditionals) {
		PoiCategory lastCategory = null;
		PoiFilter lastFilter = null;
		PoiType lastType = null;
		PoiType ref = null;
		if (parent instanceof PoiCategory) {
			lastCategory = (PoiCategory) parent;
			ref = new PoiType(this, lastCategory, null, poiAdditional.getKeyName());
		} else if (parent instanceof PoiFilter) {
			lastFilter = (PoiFilter) parent;
			ref = new PoiType(this, lastFilter.getPoiCategory(), lastFilter, poiAdditional.getKeyName());
		} else if (parent instanceof PoiType) {
			lastType = (PoiType) parent;
			ref = new PoiType(this, lastType.getCategory(), lastType.getFilter(), poiAdditional.getKeyName());
		}
		if (ref == null) {
			return null;
		}
		if (poiAdditional.isReference()) {
			ref.setReferenceType(poiAdditional.getReferenceType());
		} else {
			ref.setReferenceType(poiAdditional);
		}
		ref.setBaseLangType(poiAdditional.getBaseLangType());
		ref.setLang(poiAdditional.getLang());
		ref.setAdditional(lastType != null ? lastType :
				(lastFilter != null ? lastFilter : lastCategory));
		ref.setTopVisible(poiAdditional.isTopVisible());
		ref.setText(poiAdditional.isText());
		ref.setOrder(poiAdditional.getOrder());
		ref.setOsmTag(poiAdditional.getOsmTag());
		ref.setNotEditableOsm(poiAdditional.isNotEditableOsm());
		ref.setOsmValue(poiAdditional.getOsmValue());
		ref.setOsmTag2(poiAdditional.getOsmTag2());
		ref.setOsmValue2(poiAdditional.getOsmValue2());
		ref.setPoiAdditionalCategory(poiAdditional.getPoiAdditionalCategory());
		ref.setFilterOnly(poiAdditional.isFilterOnly());
		if (lastType != null) {
			lastType.addPoiAdditional(ref);
		} else if (lastFilter != null) {
			lastFilter.addPoiAdditional(ref);
		} else if (lastCategory != null) {
			lastCategory.addPoiAdditional(ref);
		}
		if (ref.isText()) {
			textPoiAdditionals.add(ref);
		}
		return ref;
	}

	private PoiType parsePoiAdditional(XmlPullParser parser, PoiCategory lastCategory, PoiFilter lastFilter,
											  PoiType lastType, String lang, PoiType langBaseType,
											  String poiAdditionalCategory, List<PoiType> textPoiAdditionals) {
		String oname = parser.getAttributeValue("", "name");
		if (lang != null) {
			oname += ":" + lang;
		}
		String otag = parser.getAttributeValue("", "tag");
		if (lang != null) {
			otag += ":" + lang;
		}
		PoiType tp = new PoiType(this, lastCategory, lastFilter, oname);
		tp.setBaseLangType(langBaseType);
		tp.setLang(lang);
		tp.setAdditional(lastType != null ? lastType :
			 (lastFilter != null ? lastFilter : lastCategory));
		tp.setTopVisible(Boolean.parseBoolean(parser.getAttributeValue("", "top")));
		tp.setText("text".equals(parser.getAttributeValue("", "type")));
		String orderStr = parser.getAttributeValue("", "order");
		if (!Algorithms.isEmpty(orderStr)) {
			tp.setOrder(Integer.parseInt(orderStr));
		}
		tp.setOsmTag(otag);
		tp.setNotEditableOsm("true".equals(parser.getAttributeValue("", "no_edit")));
		tp.setOsmValue(parser.getAttributeValue("", "value"));
		tp.setOsmTag2(parser.getAttributeValue("", "tag2"));
		tp.setOsmValue2(parser.getAttributeValue("", "value2"));
		tp.setPoiAdditionalCategory(poiAdditionalCategory);
		tp.setFilterOnly(Boolean.parseBoolean(parser.getAttributeValue("", "filter_only")));
		tp.setTopIndex(Boolean.parseBoolean(parser.getAttributeValue("", "top_index")));
		String maxPerMap = parser.getAttributeValue("", "max_per_map");
		if (!Algorithms.isEmpty(maxPerMap)) {
			tp.setMaxPerMap(Integer.parseInt(maxPerMap));
		}
		String minCount = parser.getAttributeValue("", "min_count");
		if (!Algorithms.isEmpty(minCount)) {
			tp.setMinCount(Integer.parseInt(minCount));
		}
		if (lastType != null) {
			lastType.addPoiAdditional(tp);
		} else if (lastFilter != null) {
			lastFilter.addPoiAdditional(tp);
		} else if (lastCategory != null) {
			lastCategory.addPoiAdditional(tp);
		}
		if (tp.isText()) {
			textPoiAdditionals.add(tp);
		}
		return tp;
	}


	private PoiType parsePoiType(final Map<String, PoiType> allTypes, XmlPullParser parser, PoiCategory lastCategory,
			PoiFilter lastFilter, String lang, PoiType langBaseType) {
		String oname = parser.getAttributeValue("", "name");
		if (lang != null) {
			oname += ":" + lang;
		}
		PoiType tp = new PoiType(this, lastCategory, lastFilter, oname);
		String otag = parser.getAttributeValue("", "tag");
		if (lang != null) {
			otag += ":" + lang;
		}
		tp.setBaseLangType(langBaseType);
		tp.setLang(lang);
		tp.setOsmTag(otag);
		tp.setOsmValue(parser.getAttributeValue("", "value"));
		tp.setOsmEditTagValue(parser.getAttributeValue("", "edit_tag"),
				parser.getAttributeValue("", "edit_value"));
		tp.setOsmEditTagValue2(parser.getAttributeValue("", "edit_tag2"),
				parser.getAttributeValue("", "edit_value2"));

		tp.setOsmTag2(parser.getAttributeValue("", "tag2"));
		tp.setOsmValue2(parser.getAttributeValue("", "value2"));
		tp.setText("text".equals(parser.getAttributeValue("", "type")));
		String orderStr = parser.getAttributeValue("", "order");
		if (!Algorithms.isEmpty(orderStr)) {
			tp.setOrder(Integer.parseInt(orderStr));
		}
		tp.setNameOnly("true".equals(parser.getAttributeValue("", "name_only")));
		tp.setNameTag(parser.getAttributeValue("", "name_tag"));
		tp.setRelation("true".equals(parser.getAttributeValue("", "relation")));
		tp.setNotEditableOsm("true".equals(parser.getAttributeValue("", "no_edit")));
		tp.setTopVisible(Boolean.parseBoolean(parser.getAttributeValue("", "top")));
		if (lastFilter != null) {
			lastFilter.addPoiType(tp);
		}
		allTypes.put(tp.getKeyName(), tp);
		lastCategory.addPoiType(tp);
		if ("true".equals(parser.getAttributeValue("", "basemap"))) {
			lastCategory.addBasemapPoi(tp);
		}
		return tp;
	}


	public List<PoiCategory> getCategories(boolean includeMapCategory) {
		ArrayList<PoiCategory> lst = new ArrayList<PoiCategory>(categories);
		if (!includeMapCategory) {
			lst.remove(getOtherMapCategory());
		}
		return lst;
	}


	private static void print(MapPoiTypes df) {
		List<PoiCategory> pc = df.getCategories(true);
		for (PoiCategory p : pc) {
			System.out.println("Category " + p.getKeyName());
			for (PoiFilter f : p.getPoiFilters()) {
				System.out.println(" Filter " + f.getKeyName());
				print("  ", f);
			}
			print(" ", p);
		}
	}

	private PoiType getPoiAdditionalByKey(AbstractPoiType p, String name) {
		List<PoiType> pp = p.getPoiAdditionals();
		if (pp != null) {
			for (PoiType pt : pp) {
				if (pt.getKeyName().equals(name)) {
					return pt;
				}
			}
		}
		return null;
	}

	public PoiType getTextPoiAdditionalByKey(String name) {
		for (PoiType pt : textPoiAdditionals) {
			if (pt.getKeyName().equals(name)) {
				return pt;
			}
		}
		return null;
	}

	public AbstractPoiType getAnyPoiAdditionalTypeByKey(String name) {
		PoiType add = null;
		for (int i = 0; i < categories.size(); i++) {
			PoiCategory pc = categories.get(i);
			add = getPoiAdditionalByKey(pc, name);
			if (add != null) {
				return add;
			}
			for (PoiFilter pf : pc.getPoiFilters()) {
				add = getPoiAdditionalByKey(pf, name);
				if (add != null) {
					return add;
				}
			}
			for (PoiType p : pc.getPoiTypes()) {
				add = getPoiAdditionalByKey(p, name);
				if (add != null) {
					return add;
				}
			}
		}
		return null;
	}

	private static void print(String indent, PoiFilter f) {
		for (PoiType pt : f.getPoiTypes()) {
			System.out.println(indent + " Type " + pt.getKeyName() +
					(pt.isReference() ? (" -> " + pt.getReferenceType().getCategory().getKeyName()) : ""));
		}
	}

	public static void main(String[] args) {
		DEFAULT_INSTANCE = new MapPoiTypes("/Users/victorshcherb/osmand/repos/resources/poi/poi_types.xml");
		DEFAULT_INSTANCE.init();
//		print(DEFAULT_INSTANCE)	;
//		System.out.println("-----------------");
		List<AbstractPoiType> lf = DEFAULT_INSTANCE.getTopVisibleFilters();
		for (AbstractPoiType l : lf) {
			System.out.println("----------------- " + l.getKeyName());
//			print("", l);
			Map<PoiCategory, LinkedHashSet<String>> m =
					l.putTypes(new LinkedHashMap<PoiCategory, LinkedHashSet<String>>());
//			System.out.println(m);
		}

	}

	public String getSynonyms(AbstractPoiType abstractPoiType) {
		if (poiTranslator != null) {
			String translation = poiTranslator.getSynonyms(abstractPoiType);
			if (!Algorithms.isEmpty(translation)) {
				return translation;
			}
		}
		return "";
	}

	public String getEnTranslation(AbstractPoiType abstractPoiType) {
		if (poiTranslator != null) {
			String translation = poiTranslator.getEnTranslation(abstractPoiType);
			if (!Algorithms.isEmpty(translation)) {
				return translation;
			}
		}
		return getBasePoiName(abstractPoiType);
	}

	public String getTranslation(AbstractPoiType abstractPoiType) {
		if (poiTranslator != null) {
			String translation = poiTranslator.getTranslation(abstractPoiType);
			if (!Algorithms.isEmpty(translation)) {
				return translation;
			}
		}
		return getBasePoiName(abstractPoiType);
	}

	public String getAllLanguagesTranslationSuffix() {
		if (poiTranslator != null) {
			return poiTranslator.getAllLanguagesTranslationSuffix();
		}
		return "all languages";
	}

	public String getBasePoiName(AbstractPoiType abstractPoiType) {
		String name = abstractPoiType.getKeyName();
		if(name.startsWith("osmand_")) {
			name = name.substring("osmand_".length());
		}
		if(name.startsWith("amenity_")) {
			name = name.substring("amenity_".length());
		}
		name = name.replace('_', ' ');
		return Algorithms.capitalizeFirstLetterAndLowercase(name);
	}

	public String getPoiTranslation(String keyName) {
		if (poiTranslator != null) {
			String translation = poiTranslator.getTranslation(keyName);
			if (!Algorithms.isEmpty(translation)) {
				return translation;
			}
		}
		String name = keyName;
		name = name.replace('_', ' ');
		return Algorithms.capitalizeFirstLetter(name);
	}

	public boolean isRegisteredType(PoiCategory t) {
		return getPoiCategoryByName(t.getKeyName()) != otherCategory;
	}

	public void initPoiTypesByTag() {
		if (!poiTypesByTag.isEmpty()) {
			return;
		}
		for (int i = 0; i < categories.size(); i++) {
			PoiCategory poic = categories.get(i);
			for (PoiType p : poic.getPoiTypes()) {
				initPoiType(p);
				for (PoiType pts : p.getPoiAdditionals()) {
					initPoiType(pts);
				}
			}
			for (PoiType p : poic.getPoiAdditionals()) {
				initPoiType(p);
			}
		}
	}


	private void initPoiType(PoiType p) {
		if (!p.isReference()) {
			String key = null;
			if (p.isAdditional()) {
				key = p.isText() ? p.getRawOsmTag() :
						(p.getRawOsmTag() + "/" + p.getOsmValue());
			} else {
				key = p.getRawOsmTag() + "/" + p.getOsmValue();
			}
			if (poiTypesByTag.containsKey(key)) {
				throw new UnsupportedOperationException("!! Duplicate poi type " + key);
			}
			poiTypesByTag.put(key, p);
		}
	}

	public String replaceDeprecatedSubtype(PoiCategory type, String subtype) {
		if(deprecatedTags.containsKey(subtype)) {
			return deprecatedTags.get(subtype);
		}
		return subtype;
	}

	public Amenity parseAmenity(String tag, String val, boolean relation, Map<String, String> otherTags) {
		initPoiTypesByTag();
		PoiType pt = poiTypesByTag.get(tag + "/" + val);
		if (pt == null) {
			pt = poiTypesByTag.get(tag);
		}
		if (pt == null || pt.isAdditional()) {
			return null;
		}
		if (!Algorithms.isEmpty(pt.getOsmTag2())) {
			if (!Algorithms.objectEquals(otherTags.get(pt.getOsmTag2()), pt.getOsmValue2())) {
				return null;
			}
		}
		if (pt.getCategory() == getOtherMapCategory()) {
			return null;
		}
		String nameValue = otherTags.get("name");
		if (pt.getNameTag() != null) {
			nameValue = otherTags.get(pt.getNameTag());
		}
		boolean hasName = !Algorithms.isEmpty(nameValue);
		if (!hasName && pt.isNameOnly()) {
			return null;
		}
		boolean multy = "multipolygon".equals(otherTags.get("type")) || "site".equals(otherTags.get("type"));
		// example of site is scottish parliament POI
		if (!multy && relation && !pt.isRelation()) {
			return null;
		}

		Amenity a = new Amenity();
		a.setType(pt.getCategory());
		a.setSubType(pt.getKeyName());
		if (pt.getNameTag() != null) {
			a.setName(nameValue);
		}
		a.setOrder(pt.getOrder());
		// additional info
		Iterator<Entry<String, String>> it = otherTags.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, String> e = it.next();
			String otag = e.getKey();
			if (!otag.equals(tag) && !otag.equals("name")) {
				PoiType pat = poiTypesByTag.get(otag + "/" + e.getValue());
				if (pat == null) {
					for(String splValue : e.getValue().split(";")) {
						PoiType ps = poiTypesByTag.get(otag + "/" + splValue.trim());
						if(ps != null) {
							a.setAdditionalInfo(ps.getKeyName(), splValue.trim());
						}
					}
					pat = poiTypesByTag.get(otag);
				}
				if (pat != null) {
					a.setAdditionalInfo(pat.getKeyName(), e.getValue());
				}
			}
		}

		return a;
	}

	public boolean isTextAdditionalInfo(String key, String value) {
		if (key.startsWith("name:") || key.equals("name")) {
			return true;
		}
		PoiType pat = (PoiType) getAnyPoiAdditionalTypeByKey(key);
//		initPoiTypesByTag();
//		PoiType pat = poiTypesByTag.get(key + "/" + value);
//		if (pat == null) {
//			pat = poiTypesByTag.get(key);
//		}
		if (pat == null) {
			return true;
		} else {
			return pat.isText();
		}
	}

	public void setForbiddenTypes(Set<String> forbiddenTypes) {
		this.forbiddenTypes = forbiddenTypes;
	}

	public boolean isTypeForbidden(String typeName) {
		return forbiddenTypes.contains(typeName);
	}
}
