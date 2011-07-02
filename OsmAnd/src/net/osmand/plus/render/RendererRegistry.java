package net.osmand.plus.render;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.osmand.LogUtil;
import net.osmand.render.OsmandRenderingRulesParser;

import org.apache.commons.logging.Log;
import org.xml.sax.SAXException;


public class RendererRegistry {

	private final static Log log = LogUtil.getLog(RendererRegistry.class);
	
	public final static String DEFAULT_RENDER = "default";  //$NON-NLS-1$
	public final static String CAR_RENDER = "car";  //$NON-NLS-1$
	public final static String BICYCLE_RENDER = "bicycle";  //$NON-NLS-1$
	public final static String PEDESTRIAN_RENDER = "pedestrian";  //$NON-NLS-1$
	//public final static String NIGHT_SUFFIX = "-night"; //$NON-NLS-1$
//	public final static String DEFAULT_NIGHT_RENDER = DEFAULT_RENDER + NIGHT_SUFFIX; 
//	public final static String CAR_NIGHT_RENDER = CAR_RENDER + NIGHT_SUFFIX; 

	
	
	public RendererRegistry(){
		internalRenderers.put(DEFAULT_RENDER, "default.render.xml"); //$NON-NLS-1$
//		internalRenderers.put(DEFAULT_NIGHT_RENDER, "default-night.render.xml"); //$NON-NLS-1$
		internalRenderers.put(CAR_RENDER, "car.render.xml"); //$NON-NLS-1$
//		internalRenderers.put(CAR_NIGHT_RENDER, "car-night.render.xml"); //$NON-NLS-1$
		internalRenderers.put(BICYCLE_RENDER, "bicycle.render.xml"); //$NON-NLS-1$
		internalRenderers.put("hm", "hm.render.xml"); //$NON-NLS-1$
//		internalRenderers.put("hm-night", "hm-night.render.xml"); //$NON-NLS-1$
	}
	
	private BaseOsmandRender defaultRender = null;
	private BaseOsmandRender currentSelectedRender = null;
	
	private Map<String, File> externalRenderers = new LinkedHashMap<String, File>();
	private Map<String, String> internalRenderers = new LinkedHashMap<String, String>();
	
	private Map<String, BaseOsmandRender> renderers = new LinkedHashMap<String, BaseOsmandRender>(); 
	
	public BaseOsmandRender defaultRender() {
		if(defaultRender == null){
			defaultRender = getRenderer(DEFAULT_RENDER);
			if (defaultRender == null) {
				try {
					defaultRender = new BaseOsmandRender();
					defaultRender.init(OsmandRenderingRulesParser.class.getResourceAsStream("default.render.xml")); //$NON-NLS-1$
				} catch (IOException e) {
					log.error("Exception initialize renderer", e); //$NON-NLS-1$
				} catch (SAXException e) {
					log.error("Exception initialize renderer", e); //$NON-NLS-1$
				}
			}
		}
		return defaultRender;
	}
	
	public BaseOsmandRender carRender() {
		BaseOsmandRender renderer = getRenderer(CAR_RENDER);
		if(renderer == null){
			return defaultRender();
		}
		return renderer;
	}
	
	public BaseOsmandRender bicycleRender() {
		BaseOsmandRender renderer = getRenderer(BICYCLE_RENDER);
		if(renderer == null){
			return defaultRender();
		}
		return renderer;
	}
	
	public BaseOsmandRender pedestrianRender() {
		BaseOsmandRender renderer = getRenderer(PEDESTRIAN_RENDER);
		if(renderer == null){
			return defaultRender();
		}
		return renderer;
	}

	public BaseOsmandRender getRenderer(String name){
		if(renderers.containsKey(name)){
			return renderers.get(name);
		}
		if(!hasRender(name)){
			return null;
		}
		return getRenderer(name, new LinkedHashSet<String>());
	}

	private boolean hasRender(String name) {
		return externalRenderers.containsKey(name) || internalRenderers.containsKey(name);
	}
	
	private BaseOsmandRender getRenderer(String name, Set<String> loadedRenderers) {
		try {
			return loadRenderer(name);
		} catch (IOException e) {
			log.error("Error loading renderer", e); //$NON-NLS-1$
		} catch (SAXException e) {
			log.error("Error loading renderer", e); //$NON-NLS-1$
		}
		return null;
	}
	
	public BaseOsmandRender loadRenderer(String name) throws IOException, SAXException {
		return loadRenderer(name, new LinkedHashSet<String>());
	}
	
	private BaseOsmandRender loadRenderer(String name, Set<String> loadedRenderers) throws IOException, SAXException {
		InputStream is = null;
		if(externalRenderers.containsKey(name)){
			is = new FileInputStream(externalRenderers.get(name));
		} else if(internalRenderers.containsKey(name)){
			is = OsmandRenderingRulesParser.class.getResourceAsStream(internalRenderers.get(name));
		} else {
			throw new IllegalArgumentException("Not found " + name); //$NON-NLS-1$
		}
		BaseOsmandRender b = new BaseOsmandRender();
		b.init(is);
		loadedRenderers.add(name);
		List<BaseOsmandRender> dependencies = new ArrayList<BaseOsmandRender>();
		for (String s : b.getDepends()) {
			if (loadedRenderers.contains(s)) {
				log.warn("Circular dependencies found " + name); //$NON-NLS-1$
			} else {
				BaseOsmandRender dep = getRenderer(s, loadedRenderers);
				if (dep == null) {
					log.warn("Dependent renderer not found : "  + name); //$NON-NLS-1$
				} else{
					dependencies.add(dep);
				}
			}
		}
		b.setDependRenderers(dependencies);
		renderers.put(name, b);
		return b;
	}
	
	
	public void setExternalRenderers(Map<String, File> externalRenderers) {
		this.externalRenderers = externalRenderers;
	}
	
	public Collection<String> getRendererNames(){
		LinkedHashSet<String> names = new LinkedHashSet<String>();
		names.add(DEFAULT_RENDER);
		names.addAll(internalRenderers.keySet());
		names.addAll(externalRenderers.keySet());
		return names;
	}

	public BaseOsmandRender getCurrentSelectedRenderer() {
		if(currentSelectedRender == null){
			return defaultRender();
		}
		return currentSelectedRender;
	}
	
	public void setCurrentSelectedRender(BaseOsmandRender currentSelectedRender) {
		this.currentSelectedRender = currentSelectedRender;
	}

	
}
