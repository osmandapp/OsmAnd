package net.osmand.osm;

import net.osmand.CollatorStringMatcher;
import net.osmand.PlatformUtil;
import net.osmand.StringMatcher;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;


public class MapPoiTypes {
	private static MapPoiTypes DEFAULT_INSTANCE = null;
	private static final Log log = PlatformUtil.getLog(MapRenderingTypes.class);
	private String resourceName;
	private List<PoiCategory> categories = new ArrayList<PoiCategory>();
	private PoiCategory otherCategory;
	private PoiCategory otherMapCategory;
	
	static final String OSM_WIKI_CATEGORY = "osmwiki";
	private PoiTranslator poiTranslator = null;
	private boolean init;
	
	public MapPoiTypes(String fileName){
		this.resourceName = fileName;
	}
	
	public interface PoiTranslator {
		
		public String getTranslation(AbstractPoiType type);
		
	}
	
	public static MapPoiTypes getDefaultNoInit() {
		if(DEFAULT_INSTANCE == null){
			DEFAULT_INSTANCE = new MapPoiTypes(null);
		}
		return DEFAULT_INSTANCE;
	}
	

	public static void setDefault(MapPoiTypes types) {
		DEFAULT_INSTANCE = types;
		DEFAULT_INSTANCE.init();
	}
	
	public static MapPoiTypes getDefault() {
		if(DEFAULT_INSTANCE == null){
			DEFAULT_INSTANCE = new MapPoiTypes(null);
			DEFAULT_INSTANCE.init();
		}
		return DEFAULT_INSTANCE;
	}
	
	public boolean isInit() {
		return init;
	}
	
	public PoiCategory getOtherPoiCategory() {
		return otherCategory;
	}
	
	public PoiCategory getOtherMapCategory() {
		if(otherMapCategory == null) {
			otherMapCategory = getPoiCategoryByName("Other", true);
		}
		return otherMapCategory;
	}
	
	public List<PoiFilter> getTopVisibleFilters() {
		List<PoiFilter> lf = new ArrayList<PoiFilter>();
		for(PoiCategory pc : categories) {
			if(pc.isTopVisible()) {
				lf.add(pc);
			}
			for(PoiFilter p : pc.getPoiFilters()) {
				if(p.isTopVisible()) {
					lf.add(p);
				}
			}
		}
		sortList(lf);
		return lf;
	}


	private void sortList(List<? extends PoiFilter> lf) {
		final Collator instance = Collator.getInstance();
		Collections.sort(lf, new Comparator<PoiFilter>() {
			@Override
			public int compare(PoiFilter object1, PoiFilter object2) {
				return instance.compare(object1.getTranslation(), object2.getTranslation());
			}
		});
	}
	
	public PoiCategory getUserDefinedCategory() {
		return otherCategory;
	}
	
	public PoiType getPoiTypeByKey(String name) {
		for(PoiCategory pc : categories) {
			PoiType pt = pc.getPoiTypeByKeyName(name);
			if(pt != null && !pt.isReference()) {
				return pt;
			}
		}
		return null;
	}
	
	public AbstractPoiType getAnyPoiTypeByKey(String name) {
		for(PoiCategory pc : categories) {
			if(pc.getKeyName().equals(name)) {
				return pc;
			}
			for(PoiFilter pf : pc.getPoiFilters()) {
				if(pf.getKeyName().equals(name)) {
					return pf;
				}	
			}
			PoiType pt = pc.getPoiTypeByKeyName(name);
			if(pt != null && !pt.isReference()) {
				return pt;
			}
		}
		return null;
	}
	
	public Map<String, PoiType> getAllTranslatedNames(boolean skipNonEditable) {
		Map<String, PoiType> translation = new HashMap<String, PoiType>();
		for(PoiCategory pc : categories) {
			if(skipNonEditable && pc.isNotEditableOsm()) {
				continue;
			}
			for(PoiType pt :  pc.getPoiTypes()) {
				if(pt.isReference() ) {
					continue;
				}
				if(pt.getBaseLangType() != null) {
					continue;
				}
				if(skipNonEditable && pt.isNotEditableOsm()) {
					continue;
				}
				translation.put(pt.getKeyName().replace('_', ' ').toLowerCase(), pt);
				translation.put(pt.getTranslation().toLowerCase(), pt);
			}
		}
		return translation;
	}
	
	public List<AbstractPoiType> getAllTypesTranslatedNames(StringMatcher matcher) {
		List<AbstractPoiType> tm = new ArrayList<AbstractPoiType>(); 
		for (PoiCategory pc : categories) {
			if(pc == otherMapCategory) {
				continue;
			}
			addIf(tm, pc, matcher);
			for (PoiFilter pt : pc.getPoiFilters()) {
				addIf(tm, pt, matcher);
			}
			for (PoiType pt : pc.getPoiTypes()) {
				if (pt.isReference()){
					continue;
				}
				addIf(tm, pt, matcher);
			}
		}
		
		return tm;
	}
	
	private void addIf(List<AbstractPoiType> tm, AbstractPoiType pc, StringMatcher matcher) {
		if(matcher.matches(pc.getTranslation()) || matcher.matches(pc.getKeyName().replace('_', ' '))) {
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
		if(name.equals("leisure") && !create) {
			name = "entertainment";
		}
		if(name.equals("historic") && !create) {
			name = "tourism";
		}
		for(PoiCategory p : categories ) {
			if(p.getKeyName().equalsIgnoreCase(name)) {
				return p;
			}
		}
		if(create) {
			PoiCategory lastCategory = new PoiCategory(this, name, categories.size());
			categories.add(lastCategory);
			return lastCategory;
		}
		return otherCategory;
	}
	
	public PoiTranslator getPoiTranslator() {
		return poiTranslator;
	}
	
	public void setPoiTranslator(PoiTranslator poiTranslator) {
		this.poiTranslator = poiTranslator;
		sortList(categories);
		
	}
	public void init() {
		init(null);
	}

	public void init(String resourceName) {
		InputStream is;
		long time = System.currentTimeMillis();
		List<PoiType> referenceTypes = new ArrayList<PoiType>();
		final Map<String, PoiType> allTypes = new LinkedHashMap<String, PoiType>();
		if(resourceName != null) {
			this.resourceName = resourceName;
		}
		try {
			if (this.resourceName == null) {
				is = MapPoiTypes.class.getResourceAsStream("poi_types.xml"); //$NON-NLS-1$
			} else {
				is = new FileInputStream(this.resourceName);
			}
			time = System.currentTimeMillis();
			XmlPullParser parser = PlatformUtil.newXMLPullParser();
			int tok;
			parser.setInput(is, "UTF-8");
			PoiCategory lastCategory = null;
			PoiFilter lastFilter = null;
			PoiType lastType = null;
			while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if (tok == XmlPullParser.START_TAG) {
					String name = parser.getName();
					if (name.equals("poi_category")) {
						lastCategory = new PoiCategory(this, parser.getAttributeValue("", "name"), categories.size());
						lastCategory.setTopVisible(Boolean.parseBoolean(parser.getAttributeValue("", "top")));
						lastCategory.setNotEditableOsm("true".equals(parser.getAttributeValue("", "no_edit")));
						lastCategory.setDefaultTag(parser.getAttributeValue("", "default_tag"));
						categories.add(lastCategory);
					} else if (name.equals("poi_filter")) {
						PoiFilter tp = new PoiFilter(this, lastCategory, parser.getAttributeValue("", "name"));
						tp.setTopVisible(Boolean.parseBoolean(parser.getAttributeValue("", "top")));
						lastFilter = tp;
						lastCategory.addPoiType(tp);
					} else if (name.equals("poi_reference")) {
						PoiType tp = new PoiType(this, lastCategory, parser.getAttributeValue("", "name"));
						referenceTypes.add(tp);
						tp.setReferenceType(tp);
						if (lastFilter != null) {
							lastFilter.addPoiType(tp);
						}
						lastCategory.addPoiType(tp);
					} else if (name.equals("poi_additional")) {
						if(lastCategory == null) {
							lastCategory = getOtherMapCategory();
						}
						PoiType baseType = parsePoiAdditional(parser, lastCategory, lastFilter, lastType, null, null);
						if("true".equals(parser.getAttributeValue("", "lang"))) {
							for(String lng : MapRenderingTypes.langs) {
								parsePoiAdditional(parser, lastCategory, lastFilter, lastType, lng, baseType);
							}
							parsePoiAdditional(parser, lastCategory, lastFilter, lastType, "en", baseType);
						}
						
					} else if (name.equals("poi_type")) {
						if(lastCategory == null) {
							lastCategory = getOtherMapCategory();
						}
						lastType = parsePoiType(allTypes, parser, lastCategory, lastFilter, null, null);
						if("true".equals(parser.getAttributeValue("", "lang"))) {
							for(String lng : MapRenderingTypes.langs) {
								parsePoiType(allTypes, parser, lastCategory, lastFilter, lng, lastType);
							}
						}
						
					}
				} else if (tok == XmlPullParser.END_TAG) {
					String name = parser.getName();
					if (name.equals("poi_filter")) {
						lastFilter = null;
					} else if (name.equals("poi_type")) {
						lastType = null;
					} else if (name.equals("poi_category")) {
						lastCategory = null;
					}
				}
			}
			is.close();
		} catch (IOException e) {
			log.error("Unexpected error", e); //$NON-NLS-1$
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (RuntimeException e) {
			log.error("Unexpected error", e); //$NON-NLS-1$
			e.printStackTrace();
			throw e;
		} catch (XmlPullParserException e) {
			log.error("Unexpected error", e); //$NON-NLS-1$
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		for (PoiType gt : referenceTypes) {
			PoiType pt = allTypes.get(gt.getKeyName());
			if (pt == null || pt.getOsmTag() == null) {
				throw new IllegalStateException("Can't find poi type for poi reference '" + gt.keyName + "'");
			} else {
				gt.setReferenceType(pt);
			}
		}
		findDefaultOtherCategory();
		init = true;
		log.info("Time to init poi types " + (System.currentTimeMillis() - time)); //$NON-NLS-1$
	}


	private PoiType parsePoiAdditional(XmlPullParser parser, PoiCategory lastCategory, PoiFilter lastFilter,
			PoiType lastType, String lang, PoiType langBaseType) {
		String oname = parser.getAttributeValue("", "name");
		if(lang != null) {
			oname += ":" + lang;
		}
		String otag = parser.getAttributeValue("", "tag");
		if(lang != null) {
			otag += ":" + lang;
		}
		PoiType tp = new PoiType(this, lastCategory, oname);
		tp.setBaseLangType(langBaseType);
		tp.setLang(lang);
		tp.setAdditional(lastType != null ? lastType : 
			 (lastFilter != null ? lastFilter : lastCategory));
		tp.setTopVisible(Boolean.parseBoolean(parser.getAttributeValue("", "top")));
		tp.setText("text".equals(parser.getAttributeValue("", "type")));
		tp.setOsmTag(otag);
		tp.setNotEditableOsm("true".equals(parser.getAttributeValue("", "no_edit")));
		tp.setOsmValue(parser.getAttributeValue("", "value"));
		tp.setOsmTag2(parser.getAttributeValue("", "tag2"));
		tp.setOsmValue2(parser.getAttributeValue("", "value2"));
		if (lastType != null) {
			lastType.addPoiAdditional(tp);
		} else if (lastFilter != null) {
			lastFilter.addPoiAdditional(tp);
		} else if (lastCategory != null) {
			lastCategory.addPoiAdditional(tp);
		}
		return tp;
	}


	private PoiType parsePoiType(final Map<String, PoiType> allTypes, XmlPullParser parser, PoiCategory lastCategory,
			PoiFilter lastFilter, String lang, PoiType langBaseType) {
		String oname = parser.getAttributeValue("", "name");
		if(lang != null) {
			oname += ":" + lang;
		}
		PoiType tp = new PoiType(this, lastCategory, oname);
		String otag = parser.getAttributeValue("", "tag");
		if(lang != null) {
			otag += ":" + lang;
		}
		tp.setBaseLangType(langBaseType);
		tp.setLang(lang);
		tp.setOsmTag(otag);
		tp.setOsmValue(parser.getAttributeValue("", "value"));
		tp.setOsmTag2(parser.getAttributeValue("", "tag2"));
		tp.setOsmValue2(parser.getAttributeValue("", "value2"));
		tp.setText("text".equals(parser.getAttributeValue("", "type")));
		tp.setNameOnly("true".equals(parser.getAttributeValue("", "name_only")));
		tp.setNameTag(parser.getAttributeValue("", "name_tag"));
		tp.setRelation("true".equals(parser.getAttributeValue("", "relation")));
		tp.setNotEditableOsm("true".equals(parser.getAttributeValue("", "no_edit")));
		if (lastFilter != null) {
			lastFilter.addPoiType(tp);
		}
		allTypes.put(tp.getKeyName(), tp);
		lastCategory.addPoiType(tp);
		if("true".equals(parser.getAttributeValue("", "basemap"))) {
			lastCategory.addBasemapPoi(tp);
		}
		return tp;
	}
	
	private void findDefaultOtherCategory() {
		PoiCategory pc = getPoiCategoryByName("user_defined_other");
		if(pc == null) {
			throw new IllegalArgumentException("No poi category other");
		}
		otherCategory = pc;
	}

	public List<PoiCategory> getCategories(boolean includeMapCategory) {
		ArrayList<PoiCategory> lst = new ArrayList<PoiCategory>(categories);
		if(!includeMapCategory) {
			lst.remove(getOtherMapCategory());
		}
		return lst;
	}
	
	
	private static void print(MapPoiTypes df) {
		List<PoiCategory> pc = df.getCategories(true);
		for(PoiCategory p : pc) {
			System.out.println("Category " + p.getKeyName());
			for(PoiFilter f : p.getPoiFilters()) {
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

	public AbstractPoiType getAnyPoiAdditionalTypeByKey(String name) {
		PoiType add = null;
		for (PoiCategory pc : categories) {
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
		for(PoiType pt : f.getPoiTypes()) {
			System.out.println(indent + " Type " + pt.getKeyName() + 
					(pt.isReference() ? (" -> " + pt.getReferenceType().getCategory().getKeyName()  ): ""));
		}
	}

	public static void main(String[] args) {
		DEFAULT_INSTANCE = new MapPoiTypes("/Users/victorshcherb/osmand/repos/resources/poi/poi_types.xml");
		DEFAULT_INSTANCE.init();
//		print(DEFAULT_INSTANCE)	;
//		System.out.println("-----------------");
		List<PoiFilter> lf = DEFAULT_INSTANCE.getTopVisibleFilters();
		for(PoiFilter l : lf) {
			System.out.println("----------------- " + l.getKeyName());
//			print("", l);
			Map<PoiCategory, LinkedHashSet<String>> m = 
					l.putTypes(new LinkedHashMap<PoiCategory, LinkedHashSet<String>>());
//			System.out.println(m);
		}
		
	}
	
	public String getTranslation(AbstractPoiType abstractPoiType) {
		if(poiTranslator != null) {
			String translation = poiTranslator.getTranslation(abstractPoiType);
			if(!Algorithms.isEmpty(translation)) {
				return translation;
			}
		}
		return Algorithms.capitalizeFirstLetterAndLowercase(abstractPoiType.getKeyName().replace('_', ' '));
	}


	public boolean isRegisteredType(PoiCategory t) {
		return getPoiCategoryByName(t.getKeyName()) != otherCategory;
	}

	
	Map<String, PoiType> poiTypesByTag = new LinkedHashMap<String, PoiType>();
	
	public void initPoiTypesByTag() {
		if(!poiTypesByTag.isEmpty()) {
			return;
		}
		for(PoiCategory poic : categories) {
			for(PoiType p : poic.getPoiTypes()) {
				initPoiType(p);
				for(PoiType pts : p.getPoiAdditionals()) {
					initPoiType(pts);
				}
			}
			for(PoiType p : poic.getPoiAdditionals()) {
				initPoiType(p);
			}
		}
	}


	private void initPoiType(PoiType p) {
		if(!p.isReference()) {
			String key = null;
			if(p.isAdditional()) {
				key = p.isText() ? p.getOsmTag() :
						(p.getOsmTag() + "/" + p.getOsmValue());
			} else {
				key = p.getOsmTag() + "/" + p.getOsmValue();
			}
			if(poiTypesByTag.containsKey(key)) {
				throw new UnsupportedOperationException("!! Duplicate poi type " + key);
			}
			poiTypesByTag.put(key, p);
		}
	}
	
	
	public Amenity parseAmenity(String tag, String val, boolean relation, Map<String, String> otherTags) {
		initPoiTypesByTag();
		PoiType pt = poiTypesByTag.get(tag+"/"+val);
		if(pt == null) {
			 pt = poiTypesByTag.get(tag);
		}
		if(pt == null || pt.isAdditional()) {
			return null;
		}
		if(!Algorithms.isEmpty(pt.getOsmTag2())) {
			if(!Algorithms.objectEquals(otherTags.get(pt.getOsmTag2()), pt.getOsmValue2())) {
				return null;
			}
		}
		if(pt.getCategory() == getOtherMapCategory()) {
			return null;
		}
		String nameValue = otherTags.get("name");
		if(pt.getNameTag() != null) {
			 nameValue = otherTags.get(pt.getNameTag());
		}
		boolean hasName = !Algorithms.isEmpty(nameValue);
		if(!hasName && pt.isNameOnly()) {
			return null;
		}
		if(relation && !pt.isRelation()) {
			return null;
		}
		
		Amenity a = new Amenity();
		a.setType(pt.getCategory());
		a.setSubType(pt.getKeyName());
		if(pt.getNameTag() != null) {
			a.setName(nameValue);
		}
		// additional info
		Iterator<Entry<String, String>> it = otherTags.entrySet().iterator();
		while(it.hasNext()) {
			Entry<String, String> e = it.next();
			String otag = e.getKey();
			if(!otag.equals(tag) && !otag.equals("name")) {
				PoiType pat = poiTypesByTag.get(otag+"/"+e.getValue());
				if(pat == null) {
					 pat = poiTypesByTag.get(otag);
				}
				if(pat != null && pat.isAdditional()) {
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

}
