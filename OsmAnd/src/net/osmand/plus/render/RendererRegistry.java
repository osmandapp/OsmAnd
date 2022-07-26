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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static net.osmand.IndexConstants.RENDERER_INDEX_EXT;


public class RendererRegistry {

	private static final Log log = PlatformUtil.getLog(RendererRegistry.class);
	
	public static final String DEFAULT_RENDER = "OsmAnd";
	public static final String DEFAULT_RENDER_FILE_PATH = "default.render.xml";
	public static final String TOURING_VIEW = "Touring view (contrast and details)";
	public static final String WINTER_SKI_RENDER = "Winter and ski";
	public static final String NAUTICAL_RENDER = "Nautical";
	public static final String TOPO_RENDER = "Topo";
	public static final String OSM_CARTO_RENDER = "OSM-carto";
	public static final String OFFROAD_RENDER = "Offroad";
	public static final String LIGHTRS_RENDER = "LightRS";
	public static final String UNIRS_RENDER = "UniRS";
	public static final String DESERT_RENDER = "Desert";
	public static final String SNOWMOBILE_RENDER = "Snowmobile";

	private final OsmandApplication app;

	private RenderingRulesStorage defaultRender;
	private RenderingRulesStorage currentSelectedRender;

	private Map<String, File> externalRenderers = new LinkedHashMap<>();
	private final Map<String, String> internalRenderers = new LinkedHashMap<>();
	private final Map<String, RenderingRulesStorage> loadedRenderers = new LinkedHashMap<>();
	private final List<IRendererLoadedEventListener> rendererLoadedListeners = new ArrayList<>();

    public interface IRendererLoadedEventListener {
		void onRendererLoaded(String name, RenderingRulesStorage rules, InputStream source);
	}

	public RendererRegistry(@NonNull OsmandApplication app) {
		this.app = app;
		internalRenderers.put(DEFAULT_RENDER, DEFAULT_RENDER_FILE_PATH);
		internalRenderers.put(TOURING_VIEW, "Touring-view_(more-contrast-and-details)" + RENDERER_INDEX_EXT);
		internalRenderers.put(TOPO_RENDER, "topo" + RENDERER_INDEX_EXT);
		internalRenderers.put(OSM_CARTO_RENDER, "osm-carto" + RENDERER_INDEX_EXT);
		internalRenderers.put(LIGHTRS_RENDER, "LightRS" + RENDERER_INDEX_EXT);
		internalRenderers.put(UNIRS_RENDER, "UniRS" + RENDERER_INDEX_EXT);
		internalRenderers.put(NAUTICAL_RENDER, "nautical" + RENDERER_INDEX_EXT);
		internalRenderers.put(WINTER_SKI_RENDER, "skimap" + RENDERER_INDEX_EXT);
		internalRenderers.put(OFFROAD_RENDER, "offroad" + RENDERER_INDEX_EXT);
		internalRenderers.put(DESERT_RENDER, "desert" + RENDERER_INDEX_EXT);
		internalRenderers.put(SNOWMOBILE_RENDER, "snowmobile" + RENDERER_INDEX_EXT);
	}
	
	public RenderingRulesStorage defaultRender() {
		if (defaultRender == null) {
			defaultRender = getRenderer(DEFAULT_RENDER);
		}
		return defaultRender;
	}

	@Nullable
	public RenderingRulesStorage getRenderer(String name) {
		if (loadedRenderers.containsKey(name)) {
			return loadedRenderers.get(name);
		}

		if (!hasRender(name)) {
			return null;
		}

		try {
			RenderingRulesStorage renderer = loadRenderer(name, new LinkedHashMap<>(), new LinkedHashMap<>());
			if (renderer != null) {
				loadedRenderers.put(name, renderer);
			}
			return renderer;
		} catch (IOException | XmlPullParserException e) {
			log.error("Error loading renderer", e);
		}
		return null;
	}

	public void updateRenderer(@NonNull RenderingRulesStorage storage) {
		RenderingRulesStorage renderer = getRenderer(storage.getName());
		if (defaultRender == renderer) {
			defaultRender = storage;
		}
		if (currentSelectedRender == renderer) {
			currentSelectedRender = storage;
		}
		loadedRenderers.put(storage.getName(), storage);
	}

	private boolean hasRender(String name) {
		updateExternalRenderers();
		return externalRenderers.containsKey(name) || getInternalRender(name) != null;
	}
	
	private String getInternalRender(String name) {
		// check by key and by value
		for (Entry<String, String> e : internalRenderers.entrySet()) {
			if (e.getKey().equalsIgnoreCase(name)) {
				return e.getValue();
			}
			String simpleFileName = e.getValue().substring(0, e.getValue().indexOf('.'));
			if (simpleFileName.equalsIgnoreCase(name)) {
				return e.getValue();
			}
		}
		return null;
	}
	
	@Nullable
	private RenderingRulesStorage loadRenderer(String name, Map<String, RenderingRulesStorage> loadedRenderers,
			Map<String, String> renderingConstants) throws IOException,  XmlPullParserException {
		InputStream is = getInputStream(name);
		if (is == null) {
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
		RenderingRulesStorage main = null;
		is = getInputStream(name);
		if (is != null) {
			main = new RenderingRulesStorage(name, renderingConstants);
			loadedRenderers.put(name, main);
			try {
				main.parseRulesFromXmlInputStream(is, (nm, ref) -> {
					// reload every time to propogate rendering constants
					if (loadedRenderers.containsKey(nm)) {
						log.warn("Circular dependencies found " + nm);
					}
					RenderingRulesStorage dep = null;
					try {
						dep = loadRenderer(nm, loadedRenderers, renderingConstants);
					} catch (IOException e) {
						log.warn("Dependent renderer not found: " + e.getMessage(), e);
					}
					if (dep == null) {
						log.warn("Dependent renderer not found: " + nm);
					}
					return dep;
				});
			} finally {
				is.close();
			}
			for (IRendererLoadedEventListener listener : rendererLoadedListeners) {
				listener.onRendererLoaded(name, main, getInputStream(name));
			}
		}
		return main;
	}

	@Nullable
	public InputStream getInputStream(String name) throws FileNotFoundException {
		InputStream is = null;
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
				String internalRender = getInternalRender(name);
				if (!Algorithms.isEmpty(internalRender)) {
					is = RenderingRulesStorage.class.getResourceAsStream(internalRender);
				}
			}
		}
		return is;
	}

	public void copyFileForInternalStyle(String name) {
		try {
			FileOutputStream fout = new FileOutputStream(getFileForInternalStyle(name));
			String internalRender = getInternalRender(name);
			if (!Algorithms.isEmpty(internalRender)) {
				InputStream resourceAsStream = RenderingRulesStorage.class.getResourceAsStream(internalRender);
				if (resourceAsStream != null) {
					Algorithms.streamCopy(resourceAsStream, fout);
				}
			}
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
		return file == null
				? new File(app.getAppPath(IndexConstants.RENDERERS_DIR), "default.render.xml")
				: new File(app.getAppPath(IndexConstants.RENDERERS_DIR), file);
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
		Map<String, File> externalRenderers = new LinkedHashMap<>();
		if (file.exists() && file.canRead()) {
			File[] lf = file.listFiles();
			if (lf != null) {
				for (File f : lf) {
					if (f != null && f.getName().endsWith(RENDERER_INDEX_EXT)) {
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
		String name = fileName.substring(0, fileName.length() - RENDERER_INDEX_EXT.length());
		name = name.replace('_', ' ').replace('-', ' ');
		return Algorithms.capitalizeFirstLetter(name);
	}

	@NonNull
	public Map<String, String> getRenderers() {
		Map<String, String> renderers = new LinkedHashMap<>();
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
		return getRendererName(app, storage.getName());
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
			case OSM_CARTO_RENDER:
				return ctx.getString(R.string.osm_carto_render_descr);
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
		if (currentSelectedRender == null) {
			return defaultRender();
		}
		return currentSelectedRender;
	}
	
	public void setCurrentSelectedRender(RenderingRulesStorage currentSelectedRender) {
		this.currentSelectedRender = currentSelectedRender;
	}

	public void addRendererLoadedEventListener(IRendererLoadedEventListener listener) {
		rendererLoadedListeners.add(listener);
	}

	public void removeRendererLoadedEventListener(IRendererLoadedEventListener listener) {
		rendererLoadedListeners.remove(listener);
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
