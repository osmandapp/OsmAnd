package net.osmand.plus.settings.backend.backup;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.CustomOsmandPlugin;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PluginSettingsItem extends SettingsItem {

	private CustomOsmandPlugin plugin;
	private List<SettingsItem> pluginDependentItems;

	PluginSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app, json);
	}

	@Override
	protected void init() {
		super.init();
		pluginDependentItems = new ArrayList<>();
	}

	@NonNull
	@Override
	public SettingsItemType getType() {
		return SettingsItemType.PLUGIN;
	}

	@NonNull
	@Override
	public String getName() {
		return plugin.getId();
	}

	@NonNull
	@Override
	public String getPublicName(@NonNull Context ctx) {
		return plugin.getName();
	}

	@NonNull
	@Override
	public String getDefaultFileName() {
		return getName();
	}

	public CustomOsmandPlugin getPlugin() {
		return plugin;
	}

	public List<SettingsItem> getPluginDependentItems() {
		return pluginDependentItems;
	}

	@Override
	public boolean exists() {
		return OsmandPlugin.getPlugin(getPluginId()) != null;
	}

	@Override
	public void apply() {
		if (shouldReplace || !exists()) {
			for (SettingsItem item : pluginDependentItems) {
				if (item instanceof FileSettingsItem) {
					FileSettingsItem fileItem = (FileSettingsItem) item;
					if (fileItem.getSubtype() == FileSettingsItem.FileSubtype.RENDERING_STYLE) {
						plugin.addRenderer(fileItem.getName());
					} else if (fileItem.getSubtype() == FileSettingsItem.FileSubtype.ROUTING_CONFIG) {
						plugin.addRouter(fileItem.getName());
					} else if (fileItem.getSubtype() == FileSettingsItem.FileSubtype.OTHER) {
						plugin.setResourceDirName(item.getFileName());
					}
				} else if (item instanceof SuggestedDownloadsItem) {
					plugin.updateSuggestedDownloads(((SuggestedDownloadsItem) item).getItems());
				} else if (item instanceof DownloadsItem) {
					plugin.updateDownloadItems(((DownloadsItem) item).getItems());
				}
			}
			OsmandPlugin.addCustomPlugin(app, plugin);
		}
	}

	@Override
	void readFromJson(@NonNull JSONObject json) throws JSONException {
		super.readFromJson(json);
		plugin = new CustomOsmandPlugin(app, json);
	}

	@Override
	void writeToJson(@NonNull JSONObject json) throws JSONException {
		super.writeToJson(json);
		json.put("version", plugin.getVersion());
		plugin.writeAdditionalDataToJson(json);
	}

	@Nullable
	@Override
	SettingsItemReader<? extends SettingsItem> getReader() {
		return null;
	}

	@Nullable
	@Override
	SettingsItemWriter<? extends SettingsItem> getWriter() {
		return null;
	}
}
