package net.osmand.plus.render;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.osmand.Algoritms;
import net.osmand.LogUtil;
import net.osmand.render.RenderingRulesStorage;

import org.apache.commons.logging.Log;
import org.xml.sax.SAXException;


public class RendererRegistry {

	private final static Log log = LogUtil.getLog(RendererRegistry.class);
	
	public final static String DEFAULT_RENDER = "default";  //$NON-NLS-1$
	
	private RenderingRulesStorage defaultRender = null;
	private RenderingRulesStorage currentSelectedRender = null;
	
	private Map<String, File> externalRenderers = new LinkedHashMap<String, File>();
	private Map<String, String> internalRenderers = new LinkedHashMap<String, String>();
	
	private Map<String, RenderingRulesStorage> renderers = new LinkedHashMap<String, RenderingRulesStorage>();
	
	public RendererRegistry(){
		internalRenderers.put(DEFAULT_RENDER, "new_default.render.xml");
	}
	
	public RenderingRulesStorage defaultRender() {
		if(defaultRender == null){
			defaultRender = getRenderer(DEFAULT_RENDER);
		}
		return defaultRender;
	}

	public RenderingRulesStorage getRenderer(String name){
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
	
	private RenderingRulesStorage getRenderer(String name, Set<String> loadedRenderers) {
		try {
			return loadRenderer(name);
		} catch (IOException e) {
			log.error("Error loading renderer", e); //$NON-NLS-1$
		} catch (SAXException e) {
			log.error("Error loading renderer", e); //$NON-NLS-1$
		}
		return null;
	}
	
	public RenderingRulesStorage loadRenderer(String name) throws IOException, SAXException {
		return loadRenderer(name, new LinkedHashSet<String>());
	}
	
	private RenderingRulesStorage loadRenderer(String name, Set<String> loadedRenderers) throws IOException, SAXException {
		InputStream is = null;
		if(externalRenderers.containsKey(name)){
			is = new FileInputStream(externalRenderers.get(name));
		} else if(internalRenderers.containsKey(name)){
			is = RenderingRulesStorage.class.getResourceAsStream(internalRenderers.get(name));
		} else {
			throw new IllegalArgumentException("Not found " + name); //$NON-NLS-1$
		}
		RenderingRulesStorage b = new RenderingRulesStorage();
		b.parseRulesFromXmlInputStream(is);
		loadedRenderers.add(name);
		if(!Algoritms.isEmpty(b.getDepends())){
			if (loadedRenderers.contains(b.getDepends())) {
				log.warn("Circular dependencies found " + name); //$NON-NLS-1$
			} else {
				RenderingRulesStorage dep = getRenderer(b.getDepends(), loadedRenderers);
				if (dep == null) {
					log.warn("Dependent renderer not found : "  + name); //$NON-NLS-1$
				}
				// TODO set dependent renders
			}
		}
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

	public RenderingRulesStorage getCurrentSelectedRenderer() {
		if(currentSelectedRender == null){
			return defaultRender();
		}
		return currentSelectedRender;
	}
	
	public void setCurrentSelectedRender(RenderingRulesStorage currentSelectedRender) {
		this.currentSelectedRender = currentSelectedRender;
	}

	
}
