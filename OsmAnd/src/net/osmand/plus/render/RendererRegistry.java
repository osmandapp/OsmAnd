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

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.osmand.LogUtil;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.render.RenderingRulesStorage.RenderingRulesStorageResolver;

import org.apache.commons.logging.Log;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public class RendererRegistry {

	private final static Log log = LogUtil.getLog(RendererRegistry.class);
	
	public final static String DEFAULT_RENDER = "default";  //$NON-NLS-1$
	
	private RenderingRulesStorage defaultRender = null;
	private RenderingRulesStorage currentSelectedRender = null;
	
	private Map<String, File> externalRenderers = new LinkedHashMap<String, File>();
	private Map<String, String> internalRenderers = new LinkedHashMap<String, String>();
	
	private Map<String, RenderingRulesStorage> renderers = new LinkedHashMap<String, RenderingRulesStorage>();
	
	public RendererRegistry(){
		internalRenderers.put(DEFAULT_RENDER, "default.render.xml");
		internalRenderers.put("road-atlas-style", "road-atlas-style.render.xml");
		internalRenderers.put("high-contrast-roads", "high-contrast-roads.render.xml");
		internalRenderers.put("winter+ski", "winter+ski.render.xml");
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
		try {
			return loadRenderer(name, new LinkedHashMap<String, RenderingRulesStorage>(), new LinkedHashMap<String, String>());
		} catch (IOException e) {
			log.error("Error loading renderer", e); //$NON-NLS-1$
		} catch (SAXException e) {
			log.error("Error loading renderer", e); //$NON-NLS-1$
		}
		return null;
	}

	private boolean hasRender(String name) {
		return externalRenderers.containsKey(name) || internalRenderers.containsKey(name);
	}
	
	private RenderingRulesStorage loadRenderer(String name, final Map<String, RenderingRulesStorage> loadedRenderers, 
			final Map<String, String> renderingConstants) throws IOException, SAXException {
		InputStream is = getInputStream(name);
		try {
			final SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
			saxParser.parse(is, new DefaultHandler() {
				@Override
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
					String tagName = saxParser.isNamespaceAware() ? localName : qName;
					if ("renderingConstant".equals(tagName)) { //$NON-NLS-1$
						if (!renderingConstants.containsKey(attributes.getValue("name"))) {
							renderingConstants.put(attributes.getValue("name"), attributes.getValue("value"));
						}
					}
				}
			});
		} catch (ParserConfigurationException e1) {
			throw new IllegalStateException(e1);
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
				public RenderingRulesStorage resolve(String name, RenderingRulesStorageResolver ref) throws SAXException {
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
		renderers.put(name, main);
		return main;
	}

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
