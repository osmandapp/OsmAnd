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
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Model3dHelper {

	private final OsmandApplication app;
	private final OsmandSettings settings;

	@Nullable
	private Model3D locationModel;
	@Nullable
	private Model3D navigationModel;

	@Nullable
	private String locationModelName;
	@Nullable
	private String navigationModelName;

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
	public Model3D getLocationModel() {
		return locationModel;
	}

	@Nullable
	public Model3D getNavigationModel() {
		return navigationModel;
	}

	@Nullable
	public String getLocationModelName() {
		return locationModelName;
	}

	@Nullable
	public String getNavigationModelName() {
		return navigationModelName;
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
		String locationModelName = appMode.getLocationIcon();
		String navigationModelName = appMode.getNavigationIcon();
		boolean sameIcon = locationModelName.equals(navigationModelName);

		String locationModelFileName = locationModelName.replace(IndexConstants.MODEL_NAME_PREFIX, "");
		String navigationModelFileName = navigationModelName.replace(IndexConstants.MODEL_NAME_PREFIX, "");

		boolean parsingLocationIcon = false;
		if (!locationModelName.equals(this.locationModelName) && isModelExist(app, locationModelFileName)) {
			parsingLocationIcon = true;
			parseModelInBackground(locationModelFileName, model -> {
				this.locationModel = model;
				this.locationModelName = locationModelName;
				if (sameIcon) {
					navigationModel = model;
					this.navigationModelName = locationModelName;
				}
				return true;
			});
		}

		if ((!sameIcon || parsingLocationIcon) && !navigationModelName.equals(this.navigationModelName) && isModelExist(app, navigationModelFileName)) {
			parseModelInBackground(navigationModelFileName, model -> {
				this.navigationModel = model;
				this.navigationModelName = navigationModelName;
				return true;
			});
		}
	}

	private void parseModelInBackground(@NonNull String modelName, @NonNull CallbackWithObject<Model3D> callback) {
		String modelDir = new File(app.getAppPath(IndexConstants.MODEL_3D_DIR), modelName).getAbsolutePath();
		new Parse3dModelTask(modelDir, callback).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
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

	public static boolean isModelExist(@NonNull OsmandApplication app, @NonNull String dirName) {
		return isModelExist(new File(app.getAppPath(IndexConstants.MODEL_3D_DIR), dirName));
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

	private static class Parse3dModelTask extends AsyncTask<Void, Void, Model3D> {

		private final String modelDirPath;
		private final CallbackWithObject<Model3D> callback;

		public Parse3dModelTask(@NonNull String modelDirPath, @NonNull CallbackWithObject<Model3D> callback) {
			this.modelDirPath = modelDirPath;
			this.callback = callback;
		}

		@Override
		protected Model3D doInBackground(Void... voids) {
			ObjParser parser = new ObjParser(modelDirPath + "/model.obj", modelDirPath + "/" + "mtl");
			return parser.parse();
		}

		@Override
		protected void onPostExecute(Model3D model3D) {
			callback.processResult(model3D);
		}
	}
}
