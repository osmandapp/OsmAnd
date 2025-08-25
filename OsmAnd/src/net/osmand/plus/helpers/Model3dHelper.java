package net.osmand.plus.helpers;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.IndexConstants;
import net.osmand.core.jni.Model3D;
import net.osmand.core.jni.ObjParser;
import net.osmand.plus.AppInitEvents;
import net.osmand.plus.AppInitializeListener;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.profiles.LocationIcon;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Model3dHelper {

	private final OsmandApplication app;

	private final Map<String, Model3D> modelsCache = new HashMap<>();
	private final Set<String> modelsInProgress = new HashSet<>();
	private final Set<String> failedModels = new HashSet<>();
	private final Map<String, List<CallbackWithObject<Model3D>>> pendingCallbacks = new HashMap<>();

	public Model3dHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	@Nullable
	public Model3D getModel(@NonNull String modelName, @Nullable CallbackWithObject<Model3D> callback) {
		String pureModelName = modelName.replace(IndexConstants.MODEL_NAME_PREFIX, "");
		if (!modelName.startsWith(IndexConstants.MODEL_NAME_PREFIX)) {
			processCallback(pureModelName, null, callback);
			return null;
		}
		Model3D model3D = modelsCache.get(pureModelName);
		if (model3D == null) {
			loadModel(pureModelName, callback);
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

	private void processCallback(@NonNull String modelName, @Nullable Model3D model, @Nullable CallbackWithObject<Model3D> callback) {
		if (callback != null) {
			List<CallbackWithObject<Model3D>> callbacks = pendingCallbacks.remove(modelName);
			if (callbacks != null) {
				callbacks.remove(callback);
				for (CallbackWithObject<Model3D> pendingCallback : callbacks) {
					pendingCallback.processResult(model);
				}
			}
			callback.processResult(model);
		}
	}

	private void loadModelImpl(@NonNull String modelName, @Nullable CallbackWithObject<Model3D> callback) {
		if (!app.useOpenGlRenderer()) {
			processCallback(modelName, null, callback);
			return;
		}

		if (modelsCache.containsKey(modelName)) {
			processCallback(modelName, modelsCache.get(modelName), callback);
			return;
		}
		if (failedModels.contains(modelName)) {
			processCallback(modelName, null, callback);
			return;
		}
		if (modelsInProgress.contains(modelName)) {
			if (callback != null) {
				pendingCallbacks.computeIfAbsent(modelName, list -> new ArrayList<>()).add(callback);
			}
			return;
		}

		File dir = new File(app.getAppPath(IndexConstants.MODEL_3D_DIR), modelName);
		if (!isModelExist(dir)) {
			processCallback(modelName, null, callback);
			return;
		}

		modelsInProgress.add(modelName);
		OsmAndTaskManager.executeTask(new Load3dModelTask(dir, model -> {
			if (model == null) {
				failedModels.add(modelName);
			} else {
				modelsCache.put(modelName, model);
			}
			modelsInProgress.remove(modelName);
			processCallback(modelName, model, callback);
			return true;
		}));
	}

	@NonNull
	public static List<String> listModels(@NonNull OsmandApplication app) {
		List<String> modelsDirNames = new ArrayList<>();
		File modelsDir = app.getAppPath(IndexConstants.MODEL_3D_DIR);
		File[] modelsDirs = modelsDir.listFiles();
		if (!Algorithms.isEmpty(modelsDirs)) {
			for (File model : modelsDirs) {
				if (isModelExist(model)) {
					String modelKey = IndexConstants.MODEL_NAME_PREFIX + model.getName();
					if (!LocationIcon.isDefaultModel(modelKey)) {
						modelsDirNames.add(IndexConstants.MODEL_NAME_PREFIX + model.getName());
					}
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

		private final File modelDirPath;
		private final CallbackWithObject<Model3D> callback;

		public Load3dModelTask(@NonNull File modelDirPath, @NonNull CallbackWithObject<Model3D> callback) {
			this.modelDirPath = modelDirPath;
			this.callback = callback;
		}

		@Override
		protected @Nullable Model3D doInBackground(Void... voids) {
			ObjParser parser = new ObjParser(modelDirPath.getAbsolutePath() + "/"
					+ modelDirPath.getName() + ".obj", modelDirPath.getAbsolutePath());
			return parser.parse();
		}

		@Override
		protected void onPostExecute(@Nullable Model3D result) {
			callback.processResult(result);
		}
	}
}
