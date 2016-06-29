package net.osmand.core.samples.android.sample1;

import net.osmand.CollatorStringMatcher;
import net.osmand.StringMatcher;
import net.osmand.core.samples.android.sample1.search.objects.PoiTypeSearchObject;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiFilter;
import net.osmand.osm.PoiType;
import net.osmand.util.Algorithms;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PoiTypesHelper {

	private SampleApplication app;

	public PoiTypesHelper(SampleApplication app) {
		this.app = app;
	}

	public List<AbstractPoiType> findPoiTypes(String s) {
		List<AbstractPoiType> poiTypes = new ArrayList<>();
		if (Algorithms.isEmpty(s)) {
			poiTypes.addAll(app.getPoiTypes().getTopVisibleFilters());
		} else {
			List<AbstractPoiType> res = app.getPoiTypes().getAllTypesTranslatedNames(
					new CollatorStringMatcher(s, CollatorStringMatcher.StringMatcherMode.CHECK_STARTS_FROM_SPACE));
			final Collator inst = Collator.getInstance();
			Collections.sort(res, new Comparator<AbstractPoiType>() {
				@Override
				public int compare(AbstractPoiType lhs, AbstractPoiType rhs) {
					return inst.compare(lhs.getTranslation(), rhs.getTranslation());
				}

			});
			for (AbstractPoiType p : res) {
				poiTypes.add(p);
			}
		}
		return poiTypes;
	}

	public AbstractPoiType fetchPoiType(PoiTypeSearchObject poiTypeObject) {
		switch (poiTypeObject.getObjectType()) {
			case CATEGORY:
				return app.getPoiTypes().getPoiCategoryByName(poiTypeObject.getKeyName());
			case FILTER:
				PoiCategory category = app.getPoiTypes().getPoiCategoryByName(poiTypeObject.getCategoryKeyName());
				for (PoiFilter filter : category.getPoiFilters()) {
					if (filter.getKeyName().equalsIgnoreCase(poiTypeObject.getKeyName())) {
						return filter;
					}
				}
				break;
			case TYPE:
				AbstractPoiType p = app.getPoiTypes().getPoiTypeByKey(poiTypeObject.getKeyName());
				if(p == null) {
					return app.getPoiTypes().getAnyPoiAdditionalTypeByKey(poiTypeObject.getKeyName());
				}
				return p;
		}
		return null;
	}

	public List<AbstractPoiType> getPoiCategoryTypesTranslatedNames(PoiCategory pc, StringMatcher matcher) {
		List<AbstractPoiType> tm = new ArrayList<AbstractPoiType>();
		for (PoiFilter pt : pc.getPoiFilters()) {
			addIf(tm, pt, matcher);
		}
		for (PoiType pt : pc.getPoiTypes()) {
			if (pt.isReference()) {
				continue;
			}
			addIf(tm, pt, matcher);
		}

		return tm;
	}

	public List<AbstractPoiType> getPoiFilterTypesTranslatedNames(PoiFilter pf, StringMatcher matcher) {
		List<AbstractPoiType> tm = new ArrayList<AbstractPoiType>();
		for (PoiType pt : pf.getPoiTypes()) {
			if (pt.isReference()) {
				continue;
			}
			addIf(tm, pt, matcher);
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
}
