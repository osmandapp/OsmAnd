package net.osmand.plus.plugins.online;

import static net.osmand.plus.plugins.PluginsHelper.OSMAND_URL;

import android.app.ProgressDialog;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.dialogs.ProgressDialogFragment;
import net.osmand.plus.importfiles.ImportTaskListener;
import net.osmand.plus.plugins.custom.CustomOsmandPlugin;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class OnlineOsmandPlugin extends CustomOsmandPlugin implements ImportTaskListener {

	private static final Log LOG = PlatformUtil.getLog(OnlineOsmandPlugin.class);

	private final String osfUrl;
	private final String publishedDate;
	private PluginInstallListener installListener;

	public OnlineOsmandPlugin(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app, json);
		publishedDate = json.optString("publishedDate", "");
		osfUrl = json.optString("osfUrl", "");
		fetchData(json, false);
		loadResources();
	}

	@Override
	public boolean isOnline() {
		return true;
	}

	public String getOsfUrl() {
		return osfUrl;
	}

	public String getPublishedDate() {
		return publishedDate;
	}

	@Override
	public File getPluginDir() {
		return new File(app.getCacheDir(), IndexConstants.PLUGINS_DIR + pluginId);
	}

	@Override
	public void readAdditionalDataFromJson(JSONObject json) throws JSONException {
		String iconPath = json.optString("iconPath", "");
		String imagePath = json.optString("imagePath", "");
		if (!iconPath.isEmpty()) {
			iconNames.put("", iconPath);
		}
		if (!imagePath.isEmpty()) {
			imageNames.put("", imagePath);
		}
		String name = json.optString("name", "");
		String description = json.optString("description", "");
		if (!name.isEmpty()) {
			names.put("", name);
		}
		if (!description.isEmpty()) {
			descriptions.put("", description);
		}
	}

	@Override
	public void readDependentFilesFromJson(JSONObject json) {
	}

	public void fetchData(@NonNull JSONObject json, boolean force) {
		File pluginDir = getPluginDir();
		String iconUrl = json.optString("iconUrl", "");
		String iconPath = json.optString("iconPath", "");
		if (iconPath.startsWith("@")) {
			iconPath = iconPath.substring(1);
		}
		File iconFile = new File(pluginDir, iconPath);
		if ((force || !iconFile.exists()) && !Algorithms.isEmpty(iconUrl)) {
			AndroidNetworkUtils.downloadFile(OSMAND_URL + iconUrl, iconFile, false, null);
		}

		String imageUrl = json.optString("imageUrl", "");
		String imagePath = json.optString("imagePath", "");
		if (imagePath.startsWith("@")) {
			imagePath = imagePath.substring(1);
		}
		File imageFile = new File(pluginDir, imagePath);
		if ((force || !imageFile.exists()) && !Algorithms.isEmpty(imageUrl)) {
			AndroidNetworkUtils.downloadFile(OSMAND_URL + imageUrl, imageFile, false, null);
		}
	}

	@Override
	public void install(@Nullable FragmentActivity activity, @Nullable PluginInstallListener installListener) {
		if (Algorithms.isEmpty(osfUrl)) {
			LOG.error("Cannot install online plugin. OSF url is empty for " + pluginId);
			return;
		}

		ProgressDialogFragment dialog = ProgressDialogFragment.createInstance(R.string.shared_string_plugin,
				R.string.shared_string_downloading, ProgressDialog.STYLE_SPINNER);
		if (activity != null) {
			dialog.show(activity.getSupportFragmentManager(), ProgressDialogFragment.TAG);
		}
		File osfFile = new File(getPluginDir(), osfUrl);
		AndroidNetworkUtils.downloadFileAsync(OSMAND_URL + osfUrl, osfFile, result -> {
			dialog.dismiss();
			if (result == null) {
				this.installListener = installListener;
				app.getImportHelper().addImportTaskListener(this);
				app.getImportHelper().handleOsmAndSettingsImport(Uri.fromFile(osfFile), osfFile.getName(), null, false, false, null, -1);
			}
			return true;
		});
	}

	@Override
	public void onImportFinished() {
		app.getImportHelper().removeImportTaskListener(this);
		File osfFile = new File(getPluginDir(), osfUrl);
		osfFile.delete();
		if (installListener != null) {
			installListener.onPluginInstalled();
			installListener = null;
		}
	}
}
