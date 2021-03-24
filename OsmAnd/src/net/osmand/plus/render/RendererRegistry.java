package net.osmand.plus.render;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.render.RenderingRulesStorage.RenderingRulesStorageResolver;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;


public class RendererRegistry {

	private final static Log log = PlatformUtil.getLog(RendererRegistry.class);
	
	public final static String DEFAULT_RENDER = "OsmAnd";  //$NON-NLS-1$
	public final static String DEFAULT_RENDER_FILE_PATH = "default.render.xml";
	public final static String TOURING_VIEW = "Touring view (contrast and details)";  //$NON-NLS-1$
	public final static String WINTER_SKI_RENDER = "Winter and ski";  //$NON-NLS-1$
	public final static String NAUTICAL_RENDER = "Nautical";  //$NON-NLS-1$
	public final static String TOPO_RENDER = "Topo";  //$NON-NLS-1$
	public final static String MAPNIK_RENDER = "Mapnik";  //$NON-NLS-1$
	public final static String OFFROAD_RENDER = "Offroad";  //$NON-NLS-1$
	public final static String LIGHTRS_RENDER = "LightRS";  //$NON-NLS-1$
	public final static String UNIRS_RENDER = "UniRS";  //$NON-NLS-1$
	public final static String DESERT_RENDER = "Desert";  //$NON-NLS-1$
	public final static String SNOWMOBILE_RENDER = "Snowmobile";  //$NON-NLS-1$

	private RenderingRulesStorage defaultRender = null;
	private RenderingRulesStorage currentSelectedRender = null;

	private Map<String, File> externalRenderers = new LinkedHashMap<String, File>();
	private Map<String, String> internalRenderers = new LinkedHashMap<String, String>();
	
	private Map<String, RenderingRulesStorage> renderers = new LinkedHashMap<String, RenderingRulesStorage>();

    public interface IRendererLoadedEventListener {
        void onRendererLoaded(String name, RenderingRulesStorage rules, InputStream source);
    }

    private IRendererLoadedEventListener rendererLoadedEventListener;

	private OsmandApplication app;
	
	public RendererRegistry(OsmandApplication app){
		this.app = app;
		internalRenderers.put(DEFAULT_RENDER, DEFAULT_RENDER_FILE_PATH);
		internalRenderers.put(TOURING_VIEW, "Touring-view_(more-contrast-and-details)" +".render.xml");
		internalRenderers.put(TOPO_RENDER, "topo" + ".render.xml");
		internalRenderers.put(MAPNIK_RENDER, "mapnik" + ".render.xml");
		internalRenderers.put(LIGHTRS_RENDER, "LightRS" + ".render.xml");
		internalRenderers.put(UNIRS_RENDER, "UniRS" + ".render.xml");
		internalRenderers.put(NAUTICAL_RENDER, "nautical" + ".render.xml");
		internalRenderers.put(WINTER_SKI_RENDER, "skimap" + ".render.xml");
		internalRenderers.put(OFFROAD_RENDER, "offroad" + ".render.xml");
		internalRenderers.put(DESERT_RENDER, "desert" + ".render.xml");
		internalRenderers.put(SNOWMOBILE_RENDER, "snowmobile" + ".render.xml");
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
		return externalRenderers.containsKey(name) || getInternalRender(name) != null;
	}
	
	private String getInternalRender(String name) {
		// check by key and by value
		Iterator<Entry<String, String>> mapIt = internalRenderers.entrySet().iterator();
		while(mapIt.hasNext()) {
			Entry<String, String> e = mapIt.next();
			if(e.getKey().equalsIgnoreCase(name)) {
				return e.getValue();
			}
			String simpleFileName = e.getValue().substring(0, e.getValue().indexOf('.'));
			if(simpleFileName.equalsIgnoreCase(name)) {
				return e.getValue();
			}
		}
		return null;
	}
	
//	private static boolean USE_PRECOMPILED_STYLE = false;
	private RenderingRulesStorage loadRenderer(String name, final Map<String, RenderingRulesStorage> loadedRenderers, 
			final Map<String, String> renderingConstants) throws IOException,  XmlPullParserException {
//		if ((name.equals(DEFAULT_RENDER) || name.equalsIgnoreCase("default")) && USE_PRECOMPILED_STYLE) {
//			RenderingRulesStorage rrs = new RenderingRulesStorage("", null);
//			new DefaultRenderingRulesStorage().createStyle(rrs);
//			log.info("INIT rendering from class");
//			return rrs;
//		}
		InputStream is = getInputStream(name);
		if(is == null) {
			return null;
		}
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

        if (rendererLoadedEventListener != null)
            rendererLoadedEventListener.onRendererLoaded(name, main, getInputStream(name));

		return main;
	}

	public InputStream getInputStream(String name) throws FileNotFoundException {
		InputStream is;
		if("default".equalsIgnoreCase(name)) {
			name = DEFAULT_RENDER;
		} 
		if(externalRenderers.containsKey(name)){
			is = new FileInputStream(externalRenderers.get(name));
		} else {
			if (getInternalRender(name) == null) {
				log.error("Rendering style not found: " + name);
				name = DEFAULT_RENDER;
			}
			File fl = getFileForInternalStyle(name);
			if (fl.exists()) {
				is = new FileInputStream(fl);
			} else {
				copyFileForInternalStyle(name);
				is = RenderingRulesStorage.class.getResourceAsStream(getInternalRender(name));
			}
		}
		return is;
	}

	public void copyFileForInternalStyle(String name) {
		try {
			FileOutputStream fout = new FileOutputStream(getFileForInternalStyle(name));
			Algorithms.streamCopy(RenderingRulesStorage.class.getResourceAsStream(getInternalRender(name)),
					fout);
			fout.close();
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}
	
	public Map<String, String> getInternalRenderers() {
		return internalRenderers;
	}

	public File getFileForInternalStyle(String name) {
		String file = getInternalRender(name);
		if(file == null) {
			return new File(app.getAppPath(IndexConstants.RENDERERS_DIR), "default.render.xml");
		}
		File fl = new File(app.getAppPath(IndexConstants.RENDERERS_DIR), file);
		return fl;
	}
	
	public void initRenderers(IProgress progress) {
		updateExternalRenderers();
		String r = app.getSettings().RENDERER.get();
		if(r != null){
			RenderingRulesStorage obj = getRenderer(r);
			if(obj != null){
				setCurrentSelectedRender(obj);
			}
		}
	}

	public void updateExternalRenderers() {
		File file = app.getAppPath(IndexConstants.RENDERERS_DIR);
		file.mkdirs();
		Map<String, File> externalRenderers = new LinkedHashMap<String, File>();
		if (file.exists() && file.canRead()) {
			File[] lf = file.listFiles();
			if (lf != null) {
				for (File f : lf) {
					if (f != null && f.getName().endsWith(IndexConstants.RENDERER_INDEX_EXT)) {
						if (!internalRenderers.containsValue(f.getName())) {
							String name = formatRendererFileName(f.getName());
							externalRenderers.put(name, f);
						}
					}
				}
			}
		}
		this.externalRenderers = externalRenderers;
	}

	public static String formatRendererFileName(String fileName) {
		String name = fileName.substring(0, fileName.length() - IndexConstants.RENDERER_INDEX_EXT.length());
		name = name.replace('_', ' ').replace('-', ' ');
		return Algorithms.capitalizeFirstLetter(name);
	}

	@NonNull
	public Map<String, String> getRenderers() {
		Map<String, String> renderers = new LinkedHashMap<String, String>();
		renderers.put(DEFAULT_RENDER, DEFAULT_RENDER_FILE_PATH);
		renderers.putAll(internalRenderers);

		for (Map.Entry<String, File> entry : externalRenderers.entrySet()) {
			renderers.put(entry.getKey(), entry.getValue().getName());
		}
		return renderers;
	}

	public String getSelectedRendererName() {
		RenderingRulesStorage storage = getCurrentSelectedRenderer();
		if (storage == null) {
			return "";
		}
		return RendererRegistry.getRendererName(app, storage.getName());
	}

	public static String getRendererName(@NonNull Context ctx, @NonNull String name) {
		String translation = getTranslatedRendererName(ctx, name);
		return translation != null ? translation :
				name.replace('_', ' ').replace('-', ' ');
	}

	@Nullable
	public static String getTranslatedRendererName(@NonNull Context ctx, @NonNull String key) {
		switch (key) {
			case TOURING_VIEW:
				return ctx.getString(R.string.touring_view_renderer);
			case WINTER_SKI_RENDER:
				return ctx.getString(R.string.winter_and_ski_renderer);
			case NAUTICAL_RENDER:
				return ctx.getString(R.string.nautical_renderer);
		}
		return null;
	}

	@NonNull
	public static String getRendererDescription(@NonNull Context ctx, @NonNull String key) {
		switch (key) {
			case DEFAULT_RENDER:
				return ctx.getString(R.string.default_render_descr);
			case TOURING_VIEW:
				return ctx.getString(R.string.touring_view_render_descr);
			case MAPNIK_RENDER:
				return ctx.getString(R.string.mapnik_render_descr);
			case TOPO_RENDER:
				return ctx.getString(R.string.topo_render_descr);
			case LIGHTRS_RENDER:
				return ctx.getString(R.string.light_rs_render_descr);
			case UNIRS_RENDER:
				return ctx.getString(R.string.unirs_render_descr);
			case WINTER_SKI_RENDER:
				return ctx.getString(R.string.ski_map_render_descr);
			case NAUTICAL_RENDER:
				return ctx.getString(R.string.nautical_render_descr);
			case OFFROAD_RENDER:
				return ctx.getString(R.string.off_road_render_descr);
			case DESERT_RENDER:
				return ctx.getString(R.string.desert_render_descr);
			case SNOWMOBILE_RENDER:
			return ctx.getString(R.string.snowmobile_render_descr);
		}
		return ""; 
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

    public void setRendererLoadedEventListener(IRendererLoadedEventListener listener) {
        rendererLoadedEventListener = listener;
    }

    public IRendererLoadedEventListener getRendererLoadedEventListener() {
        return rendererLoadedEventListener;
    }

	public RenderingRuleProperty getCustomRenderingRuleProperty(String attrName) {
		RenderingRulesStorage renderer = getCurrentSelectedRenderer();
		if (renderer != null) {
			for (RenderingRuleProperty p : renderer.PROPS.getCustomRules()) {
				if (p.getAttrName().equals(attrName)) {
					return p;
				}
			}
		}
		return null;
	}

	public Map<String, File> getExternalRenderers() {
		return externalRenderers;
	}
}
