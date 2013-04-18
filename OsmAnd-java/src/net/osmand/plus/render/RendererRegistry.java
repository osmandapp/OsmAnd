package net.osmand.plus.render;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import net.osmand.PlatformUtil;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.render.RenderingRulesStorage.RenderingRulesStorageResolver;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;


public class RendererRegistry {

	private final static Log log = PlatformUtil.getLog(RendererRegistry.class);
	
	public final static String DEFAULT_RENDER = "default";  //$NON-NLS-1$
	
	private RenderingRulesStorage defaultRender = null;
	private RenderingRulesStorage currentSelectedRender = null;
	
	private Map<String, File> externalRenderers = new LinkedHashMap<String, File>();
	private Map<String, String> internalRenderers = new LinkedHashMap<String, String>();
	
	private Map<String, RenderingRulesStorage> renderers = new LinkedHashMap<String, RenderingRulesStorage>();
	
	public RendererRegistry(){
		internalRenderers.put(DEFAULT_RENDER, "default.render.xml");
		internalRenderers.put("Touring-view_(more-contrast-and-details)", "Touring-view_(more-contrast-and-details)" +".render.xml");
		internalRenderers.put("High-contrast-roads", "High-contrast-roads" + ".render.xml");
		internalRenderers.put("Winter-and-ski", "Winter-and-ski" + ".render.xml");
	}
	
	public RenderingRulesStorage defaultRender() {
		if(defaultRender == null){
			defaultRender = getRenderer(DEFAULT_RENDER);
		}
		return defaultRender;
	}

	public RenderingRulesStorage getRenderer(String name) {
		if(renderers.containsKey(name)){
			return renderers.get(name);
		}
		if(!hasRender(name)){
			return null;
		}
		try {
			RenderingRulesStorage r = loadRenderer(name, new LinkedHashMap<String, RenderingRulesStorage>(), new LinkedHashMap<String, String>());
			renderers.put(name, r);
			return r;
		} catch (IOException e) {
			log.error("Error loading renderer", e); //$NON-NLS-1$
		} catch (XmlPullParserException e) {
			log.error("Error loading renderer", e); //$NON-NLS-1$
		}
		return null;
	}

	private boolean hasRender(String name) {
		return externalRenderers.containsKey(name) || internalRenderers.containsKey(name);
	}
	
	private RenderingRulesStorage loadRenderer(String name, final Map<String, RenderingRulesStorage> loadedRenderers, 
			final Map<String, String> renderingConstants) throws IOException,  XmlPullParserException {
		InputStream is = getInputStream(name);
		try {
			XmlPullParser parser = PlatformUtil.newXMLPullParser();
			parser.setInput(is, "UTF-8");
			int tok;
			while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if (tok == XmlPullParser.START_TAG) {
					String tagName = parser.getName();
					if (tagName.equals("renderingConstant")) {
						if (!renderingConstants.containsKey(parser.getAttributeValue("", "name"))) {
							renderingConstants.put(parser.getAttributeValue("", "name"), 
									parser.getAttributeValue("", "value"));
						}
					}
				}
			}
		} finally {
			is.close();
		}

		// parse content
		is = getInputStream(name);
		final RenderingRulesStorage main = new RenderingRulesStorage(name, renderingConstants);
		
		loadedRenderers.put(name, main);
		try {
			main.parseRulesFromXmlInputStream(is, new RenderingRulesStorageResolver() {

				@Override
				public RenderingRulesStorage resolve(String name, RenderingRulesStorageResolver ref) throws XmlPullParserException {
					// reload every time to propogate rendering constants
					if (loadedRenderers.containsKey(name)) {
						log.warn("Circular dependencies found " + name); //$NON-NLS-1$
					}
					RenderingRulesStorage dep = null;
					try {
						dep = loadRenderer(name, loadedRenderers, renderingConstants);
					} catch (IOException e) {
						log.warn("Dependent renderer not found : " + e.getMessage(), e); //$NON-NLS-1$
					}
					if (dep == null) {
						log.warn("Dependent renderer not found : " + name); //$NON-NLS-1$
					}
					return dep;
				}
			});
		} finally {
			is.close();
		}
		return main;
	}

	@SuppressWarnings("resource")
	private InputStream getInputStream(String name) throws FileNotFoundException {
		InputStream is = null;
		if(externalRenderers.containsKey(name)){
			is = new FileInputStream(externalRenderers.get(name));
		} else if(internalRenderers.containsKey(name)){
			is = RenderingRulesStorage.class.getResourceAsStream(internalRenderers.get(name));
		} else {
			throw new IllegalArgumentException("Not found " + name); //$NON-NLS-1$
		}
		return is;
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
