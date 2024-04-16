package net.osmand.plus.helpers;

import android.os.AsyncTask;

import net.osmand.CallbackWithObject;
import net.osmand.IndexConstants;
import net.osmand.StateChangedListener;
import net.osmand.core.jni.Model3D;
import net.osmand.core.jni.ObjParser;
import net.osmand.plus.AppInitEvents;
import net.osmand.plus.AppInitializeListener;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Model3dHelper {

	private final OsmandApplication app;
	private final OsmandSettings settings;

	private final Map<String, Model3D> modelsCache = new HashMap<>();
	private final Set<String> modelsInProgress = new HashSet<>();
	private final Set<String> failedModels = new HashSet<>();

	private final StateChangedListener<ApplicationMode> appModeListener;
	private final StateChangedListener<String> locationIconListener;
	private final StateChangedListener<String> navigationIconListener;

	public Model3dHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		this.appModeListener = newAppMode -> parseModels();
		this.locationIconListener = newIcon -> parseModels();
		this.navigationIconListener = newIcon -> parseModels();

		settings.APPLICATION_MODE.addListener(appModeListener);
		settings.LOCATION_ICON.addListener(locationIconListener);
		settings.NAVIGATION_ICON.addListener(navigationIconListener);

		parseModels();
	}

	@Nullable
	public Model3D getModel(@NonNull String modelName) {
		return modelsCache.get(modelName.replace(IndexConstants.MODEL_NAME_PREFIX, ""));
	}

	public void parseModels() {
		if (app.isApplicationInitializing()) {
			app.getAppInitializer().addListener(new AppInitializeListener() {
				@Override
				public void onProgress(@NonNull AppInitializer init, @NonNull AppInitEvents event) {
					if (event == AppInitEvents.NATIVE_OPEN_GL_INITIALIZED) {
						parseModelsImpl();
						init.removeListener(this);
					}
				}
			});
		} else {
			parseModelsImpl();
		}
	}

	private void parseModelsImpl() {
		if (!app.useOpenGlRenderer()) {
			return;
		}

		ApplicationMode appMode = settings.getApplicationMode();
		Set<String> iconsNames = new HashSet<>();
		iconsNames.add(appMode.getLocationIcon());
		iconsNames.add(appMode.getNavigationIcon());

		Set<String> dirsPaths = new HashSet<>();
		for (String iconName : iconsNames) {
			if (!iconName.startsWith(IndexConstants.MODEL_NAME_PREFIX)) {
				continue;
			}

			String modelName = iconName.replace(IndexConstants.MODEL_NAME_PREFIX, "");

			if (modelsCache.containsKey(modelName)
					|| modelsInProgress.contains(modelName)
					|| failedModels.contains(modelName)) {
				continue;
			}

			File dir = new File(app.getAppPath(IndexConstants.MODEL_3D_DIR), modelName);
			if (isModelExist(dir)) {
				dirsPaths.add(dir.getAbsolutePath());
				modelsInProgress.add(modelName);
			}
		}

		new Parse3dModelTask(dirsPaths, result -> {
			for (Entry<String, Model3D> entry : result.entrySet()) {
				String modelName = entry.getKey();
				Model3D model = entry.getValue();

				if (model == null) {
					failedModels.add(modelName);
				} else {
					modelsCache.put(modelName, model);
				}
				modelsInProgress.remove(modelName);
			}

			return true;
		}).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@NonNull
	public static List<String> listModels(@NonNull OsmandApplication app) {
		List<String> modelsDirNames = new ArrayList<>();
		File modelsDir = app.getAppPath(IndexConstants.MODEL_3D_DIR);
		File[] modelsDirs = modelsDir.listFiles();
		if (!Algorithms.isEmpty(modelsDirs)) {
			for (File model : modelsDirs) {
				if (isModelExist(model)) {
					modelsDirNames.add(IndexConstants.MODEL_NAME_PREFIX + model.getName());
				}
			}
		}
		return modelsDirNames;
	}

	public static boolean isModelExist(@NonNull File dir) {
		if (!dir.exists() || !dir.isDirectory()) {
			return false;
		}

		File[] modelFiles = dir.listFiles();
		if (!Algorithms.isEmpty(modelFiles)) {
			for (File file : modelFiles) {
				if (file.getName().endsWith(IndexConstants.OBJ_FILE_EXT)) {
					return true;
				}
			}
		}

		return false;
	}

	private static class Parse3dModelTask extends AsyncTask<Void, Void, Map<String, Model3D>> {

		private final Set<String> modelDirPaths;
		private final CallbackWithObject< Map<String, Model3D> > callback;

		public Parse3dModelTask(@NonNull Set<String> modelDirPaths, @NonNull CallbackWithObject< Map<String, Model3D> > callback) {
			this.modelDirPaths = modelDirPaths;
			this.callback = callback;
		}

		@Override
		protected Map<String, Model3D> doInBackground(Void... voids) {
			Map<String, Model3D> result = new HashMap<>();
			for (String modelDirPath : modelDirPaths) {
				String modelName = new File(modelDirPath).getName();
				ObjParser parser = new ObjParser(modelDirPath + "/model.obj", modelDirPath + "/" + "mtl");
				Model3D model = parser.parse();
				result.put(modelName, model);
			}
			return result;
		}

		@Override
		protected void onPostExecute(Map<String, Model3D> result) {
			callback.processResult(result);
		}
	}
}
