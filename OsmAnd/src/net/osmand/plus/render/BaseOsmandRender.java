package net.osmand.plus.render;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.LogUtil;
import net.osmand.plus.render.OsmandRenderer.RenderingContext;
import net.osmand.plus.render.OsmandRenderer.RenderingPaintProperties;
import net.osmand.render.OsmandRenderingRulesParser;
import net.osmand.render.OsmandRenderingRulesParser.EffectAttributes;
import net.osmand.render.OsmandRenderingRulesParser.FilterState;
import net.osmand.render.OsmandRenderingRulesParser.RenderingRuleVisitor;

import org.apache.commons.logging.Log;
import org.xml.sax.SAXException;

import android.graphics.Color;
import android.graphics.Paint.Cap;


public class BaseOsmandRender implements RenderingRuleVisitor {
	
	public String name = "default"; //$NON-NLS-1$
	public List<String> depends = new ArrayList<String>();
	public List<BaseOsmandRender> dependRenderers = new ArrayList<BaseOsmandRender>();
	private static final Log log = LogUtil.getLog(BaseOsmandRender.class);

	@SuppressWarnings("unchecked")
	private Map<String, Map<String, List<FilterState>>>[] rules = new LinkedHashMap[6]; 
	
	
	private int defaultColor;
	private int defaultNightColor;
	
	
	public void init(InputStream is) throws IOException, SAXException{
		long time = System.currentTimeMillis();
		OsmandRenderingRulesParser parser = new OsmandRenderingRulesParser();
		parser.parseRenderingRules(is, this);
		log.info("Init render " + name + " for " + (System.currentTimeMillis() - time) + " ms");   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
	}
	
	public BaseOsmandRender() {
	}

	@Override
	public void rendering(String name, String depends, int defaultColor, int defaultNightColor) {
		this.name = name;
		this.defaultColor = defaultColor;
		this.defaultNightColor = defaultNightColor;
		if(depends != null && depends.length() > 0){
			for(String s : depends.split(",")) { //$NON-NLS-1$
				if(s.trim().length() > 0){
					this.depends.add(s.trim());
				}
			}
		}
	}
	
	public int getDefaultColor(boolean nightMode) {
		int r = nightMode ? defaultNightColor : defaultColor ;
		if (r == 0) {
			for (BaseOsmandRender d : dependRenderers) {
				r = d.getDefaultColor(nightMode);
				if (r != 0) {
					break;
				}
			}
		}
		return r;
	}

	@Override
	public void visitRule(int state, FilterState filter) {
		boolean accept = filter.minzoom != -1 || state == OsmandRenderingRulesParser.ORDER_STATE;
		if(state == OsmandRenderingRulesParser.POINT_STATE){
			accept &= RenderingIcons.getIcons().containsKey(filter.icon);
		}
		if(state == OsmandRenderingRulesParser.ORDER_STATE){
			accept &= filter.order != 0 && filter.orderType != 0;
		}
		if (accept) {
			if (rules[state] == null) {
				rules[state] = new LinkedHashMap<String, Map<String, List<FilterState>>>();
			}
			if (rules[state].get(filter.tag) == null) {
				rules[state].put(filter.tag, new LinkedHashMap<String, List<FilterState>>());
			}
			if (rules[state].get(filter.tag).get(filter.val) == null) {
				rules[state].get(filter.tag).put(filter.val, new ArrayList<FilterState>(3));
			}
			rules[state].get(filter.tag).get(filter.val).add(filter);
		}
	}
	
	public Integer getPointIcon(String tag, String val, int zoom, boolean nightMode) {
		Integer i = getPointIconImpl(tag, val, zoom, nightMode);
		if (i == null) {
			i = getPointIconImpl(tag, null, zoom, nightMode);
		}
		if (i == null) {
			for (BaseOsmandRender d : dependRenderers) {
				i = d.getPointIcon(tag, val, zoom, nightMode);
				if (i != null) {
					break;
				}
			}
		}
		return i;
	}
	
	// type 
	public float getObjectOrder(String tag, String val, int type, int layer) {
		if(type == 0){
			// replace multipolygon with polygon 
			type = 3;
		}
		float f = getObjectOrderImpl(tag, val, type, layer);
		if (f == 0) {
			f = getObjectOrderImpl(tag, null, type, layer);
		}
		if (f == 0) {
			f = getObjectOrderImpl("", null, type, layer); //$NON-NLS-1$
		}
		if(f == 0){
			for(BaseOsmandRender d : dependRenderers){
				f = d.getObjectOrder(tag, val, type, layer);
				if (f != 0) {
					break;
				}
			}
		}
		
		if (f == 0) {
			if (type == 0 || type == 3) {
				return 1f;
			} else if (type == 1) {
				return 128f;
			} else {
				return 35f;
			}
		}
		return f;
	}
	
	public boolean renderPolyline(String tag, String val, int zoom, RenderingContext rc, OsmandRenderer o, int layer, boolean nightMode){
		boolean r = renderPolylineImpl(tag, val, zoom, rc, o, layer, nightMode);
		if(!r){
			r = renderPolylineImpl(tag, null, zoom, rc, o, layer, nightMode);
		}
		if(!r){
			for(BaseOsmandRender d : dependRenderers){
				r = d.renderPolyline(tag, val, zoom, rc, o, layer, nightMode);
				if (r) {
					break;
				}
			}
		}
		return r;
	}
	public boolean renderPolygon(String tag, String val, int zoom, RenderingContext rc, OsmandRenderer o, boolean nightMode){
		boolean r = renderPolygonImpl(tag,val, zoom, rc, o, nightMode);
		if(!r){
			r = renderPolygonImpl(tag, null, zoom, rc, o, nightMode);
		}
		if(!r){
			for(BaseOsmandRender d : dependRenderers){
				r = d.renderPolygon(tag, val, zoom, rc, o, nightMode);
				if (r) {
					break;
				}
			}
		}
		return r;
	}

	public String renderObjectText(String name, String tag, String val, RenderingContext rc, boolean ref, boolean nightMode) {
		if(name == null || name.length() == 0){
			return null;
		}
		String ret = renderObjectTextImpl(name, tag, val, rc, ref, nightMode);
		if(ret == null){
			ret = renderObjectTextImpl(name, tag, null, rc, ref, nightMode);
		}
		if(ret == null){
			for(BaseOsmandRender d : dependRenderers){
				ret = d.renderObjectText(name, tag, val, rc, ref, nightMode);
				if (ret != null) {
					break;
				}
			}
		}
		
		return ret;
	}
	
	public boolean isObjectVisible(String tag, String val, int zoom, int type, boolean nightMode) {
		if (type == 0) {
			// replace multipolygon with polygon
			type = 3;
		}
		if (isObjectVisibleImpl(tag, val, zoom, type, nightMode)) {
			return true;
		}
		if (isObjectVisibleImpl(tag, null, zoom, type, nightMode)) {
			return true;
		}
		for (BaseOsmandRender d : dependRenderers) {
			if (d.isObjectVisible(tag, val, zoom, type, nightMode)) {
				return true;
			}
		}
		return false;
	}


	private boolean isObjectVisibleImpl(String tag, String val, int zoom, int type, boolean nightMode) {
		FilterState fs = findBestFilterState(tag, val, zoom, nightMode, 0, null, 0, rules[type]);
		return fs != null;
	}

	private float getObjectOrderImpl(String tag, String val, int type, int layer) {
		if (rules[OsmandRenderingRulesParser.ORDER_STATE] != null) {
			Map<String, List<FilterState>> map = rules[OsmandRenderingRulesParser.ORDER_STATE].get(tag);
			if (map != null) {
				List<FilterState> list = map.get(val);
				if (list != null) {
					int sz = list.size();
					for (int i = 0; i < sz; i++) {
						FilterState f = list.get(i);
						if (f.orderType == type && f.layer == layer) {
							return f.order;
						}
					}
				}
			}
		}
		return 0;
	}
	

	private Integer getPointIconImpl(String tag, String val, int zoom, boolean nightMode) {
		FilterState fs = findBestFilterState(tag, val, zoom, nightMode, 0, null, 0, rules[OsmandRenderingRulesParser.POINT_STATE]);
		if (fs != null) {
			Integer i = RenderingIcons.getIcons().get(fs.icon);
			return i == null ? 0 : i;
		}
		return null;
	}
	
	private FilterState findBestFilterState(String tag, String val, int zoom, boolean nightMode, int layer, Boolean ref,
			int nameLength, Map<String, Map<String, List<FilterState>>> mapTag) {
		if (mapTag != null) {
			Map<String, List<FilterState>> map = mapTag.get(tag);
			if (map != null) {
				List<FilterState> list = map.get(val);
				if (list != null) {
					FilterState bestResult = null;
					boolean prevDayNightMatches = false;
					boolean prevLayerMatches = false;
					int sz = list.size();
					for (int i = 0; i < sz; i++) {
						FilterState f = list.get(i);
						if (f.minzoom <= zoom && (zoom <= f.maxzoom || f.maxzoom == -1)) {
							if(ref != null && !checkRefTextRule(f, ref.booleanValue())){
								continue;
							}
							if(f.textLength != nameLength){
								continue;
							}
							boolean dayNightMatches = (f.nightMode != null && f.nightMode.booleanValue() == nightMode) || 
													(!nightMode && f.nightMode == null);
							boolean layerMatches = f.layer == layer;
							boolean defLayerMatches = f.layer == 0;
							if (dayNightMatches || !prevDayNightMatches){
								if(dayNightMatches && !prevDayNightMatches){
									if(layerMatches || defLayerMatches){
										prevDayNightMatches = true;
										prevLayerMatches = false;
									}
								}
								if(layerMatches){
									prevLayerMatches = true;
									bestResult = f;
								} else if(defLayerMatches && !prevLayerMatches){
									bestResult = f;
								}
							}
						}
					}
					return bestResult;
				}
			}
		}
		return null;
	}

	private boolean renderPolylineImpl(String tag, String val, int zoom, RenderingContext rc, OsmandRenderer o, int layer, boolean nightMode) {
		FilterState found = findBestFilterState(tag, val, zoom, nightMode, layer, null, 0, rules[OsmandRenderingRulesParser.LINE_STATE]);
		if (found != null) {
			// to not make transparent
			rc.main.color = Color.BLACK;
			if (found.shader != null) {
				Integer i = RenderingIcons.getIcons().get(found.shader);
				if (i != null) {
					rc.main.shader = o.getShader(i);
				}
			}
			rc.main.fillArea = false;
			applyEffectAttributes(found.main, rc.main, o);
			if (found.effectAttributes.size() > 0) {
				applyEffectAttributes(found.effectAttributes.get(0), rc.second, o);
				if (found.effectAttributes.size() > 1) {
					applyEffectAttributes(found.effectAttributes.get(1), rc.third, o);
				}
			}
			return true;
		}
		return false;
	}
	
	

	private boolean renderPolygonImpl(String tag, String val, int zoom, RenderingContext rc, OsmandRenderer o, boolean nightMode) {
		FilterState f = findBestFilterState(tag, val, zoom, nightMode, 0, null, 0, rules[OsmandRenderingRulesParser.POLYGON_STATE]);
		if (f != null) {
			if (f.shader != null) {
				Integer i = RenderingIcons.getIcons().get(f.shader);
				if (i != null) {
					// to not make transparent
					rc.main.color = Color.BLACK;
					rc.main.shader = o.getShader(i);
				}
			}
			rc.main.fillArea = true;
			applyEffectAttributes(f.main, rc.main, o);
			if (f.effectAttributes.size() > 0) {
				applyEffectAttributes(f.effectAttributes.get(0), rc.second, o);
				if (f.effectAttributes.size() > 1) {
					applyEffectAttributes(f.effectAttributes.get(1), rc.third, o);
				}
			}
			return true;
		}
		return false;
	}
	
	private void applyEffectAttributes(EffectAttributes ef, RenderingPaintProperties props, OsmandRenderer o){
		if(ef.cap != null){
			props.cap = Cap.valueOf(ef.cap.toUpperCase());
		}
		if(ef.color != 0){
			// do not set transparent color
			props.color = ef.color;
		}
		if(ef.pathEffect != null){
			props.pathEffect = o.getDashEffect(ef.pathEffect); 
		}
		if(ef.strokeWidth > 0){
			props.strokeWidth = ef.strokeWidth;
		}
		if(ef.shadowColor != 0 && ef.shadowRadius > 0){
			props.shadowColor = ef.shadowColor;
			props.shadowRadius = (int) ef.shadowRadius;
		}
	}
	
	
	private boolean checkRefTextRule(FilterState f, boolean ref){
		if(ref){
			return f.text != null && f.text.ref != null;
		} else {
			return f.text == null || f.text.ref == null || "true".equals(f.text.ref); //$NON-NLS-1$
		}
	}

	private String renderObjectTextImpl(String name, String tag, String val, RenderingContext rc, boolean ref, boolean nightMode) {
		FilterState fs = findBestFilterState(tag, val, rc.zoom, nightMode, 0, ref, name.length(), rules[OsmandRenderingRulesParser.TEXT_STATE]);
		if(fs == null){
			fs = findBestFilterState(tag, val, rc.zoom, nightMode, 0, ref, 0, rules[OsmandRenderingRulesParser.TEXT_STATE]);
		}
		if(fs != null){
			fillTextProperties(fs, rc);
			return name;
		} 
		return null;
	}

	private void fillTextProperties(FilterState f, RenderingContext rc) {
		rc.textSize = f.text.textSize;
		rc.textColor = f.text.textColor == 0 ? Color.BLACK : f.text.textColor;
		rc.textOrder = f.text.textOrder;
		rc.textMinDistance = f.text.textMinDistance;
		rc.showTextOnPath = f.text.textOnPath;
		Integer i = RenderingIcons.getIcons().get(f.text.textShield);
		rc.textShield = i== null ? 0 : i.intValue();
		rc.textWrapWidth = f.text.textWrapWidth;
		rc.textHaloRadius = f.text.textHaloRadius;
		rc.textBold = f.text.textBold;
		rc.textDy = f.text.textDy;
	}
	
	public List<String> getDepends() {
		return depends;
	}
	
	public void setDependRenderers(List<BaseOsmandRender> dependRenderers) {
		this.dependRenderers = dependRenderers;
	}
}
