package net.osmand.plus.helpers;

import android.os.AsyncTask;

import net.osmand.CallbackWithObject;
import net.osmand.IndexConstants;
import net.osmand.core.jni.Model3D;
import net.osmand.core.jni.ObjParser;
import net.osmand.plus.AppInitEvents;
import net.osmand.plus.AppInitializeListener;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Model3dHelper {

	private final OsmandApplication app;
	private final OsmandSettings settings;

	private final Map<String, Model3D> modelsCache = new HashMap<>();
	private final Set<String> modelsInProgress = new HashSet<>();
	private final Set<String> failedModels = new HashSet<>();

	public Model3dHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
	}

	@Nullable
	public Model3D getModel(@NonNull String modelName, @Nullable CallbackWithObject<Model3D> callbackOnLoad) {
		if (!modelName.startsWith(IndexConstants.MODEL_NAME_PREFIX)) {
			if (callbackOnLoad != null) {
				callbackOnLoad.processResult(null);
			}
			return null;
		}

		String pureModelName = modelName.replace(IndexConstants.MODEL_NAME_PREFIX, "");
		Model3D model3D = modelsCache.get(pureModelName);
		if (model3D == null) {
			loadModel(pureModelName, callbackOnLoad);
		}

		return model3D;
	}

	private void loadModel(@NonNull String modelName, @Nullable CallbackWithObject<Model3D> callback) {
		if (app.isApplicationInitializing() && !NativeCoreContext.isInit()) {
			app.getAppInitializer().addListener(new AppInitializeListener() {
				@Override
				public void onProgress(@NonNull AppInitializer init, @NonNull AppInitEvents event) {
					if (event == AppInitEvents.NATIVE_OPEN_GL_INITIALIZED) {
						loadModelImpl(modelName, callback);
						init.removeListener(this);
					}
				}
			});
		} else {
			loadModelImpl(modelName, callback);
		}
	}

	private void loadModelImpl(@NonNull String modelName, @Nullable CallbackWithObject<Model3D> callback) {
		if (!app.useOpenGlRenderer()) {
			return;
		}

		if (modelsCache.containsKey(modelName)
				|| modelsInProgress.contains(modelName)
				|| failedModels.contains(modelName)) {
			return;
		}

		File dir = new File(app.getAppPath(IndexConstants.MODEL_3D_DIR), modelName);
		if (!isModelExist(dir)) {
			return;
		}

		modelsInProgress.add(modelName);
		new Load3dModelTask(dir.getAbsolutePath(), model -> {
			if (model == null) {
				failedModels.add(modelName);
			} else {
				modelsCache.put(modelName, model);
			}
			modelsInProgress.remove(modelName);
			if (callback != null) {
				callback.processResult(model);
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

	private static class Load3dModelTask extends AsyncTask<Void, Void, Model3D> {

		private final String modelDirPath;
		private final CallbackWithObject<Model3D> callback;

		public Load3dModelTask(@NonNull String modelDirPath, @NonNull CallbackWithObject<Model3D> callback) {
			this.modelDirPath = modelDirPath;
			this.callback = callback;
		}

		@Override
		protected Model3D doInBackground(Void... voids) {
			ObjParser parser = new ObjParser(modelDirPath + "/model.obj", modelDirPath + "/mtl");
			return parser.parse();
		}

		@Override
		protected void onPostExecute(Model3D result) {
			callback.processResult(result);
		}
	}
}
