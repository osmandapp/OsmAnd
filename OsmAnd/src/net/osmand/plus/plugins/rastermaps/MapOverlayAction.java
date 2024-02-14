package net.osmand.plus.plugins.rastermaps;

import static net.osmand.plus.quickaction.QuickActionIds.MAP_OVERLAY_ACTION_ID;

import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.util.Pair;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.quickaction.SwitchableAction;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.UiUtilities;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapOverlayAction extends SwitchableAction<Pair<String, String>> {

	private static final String KEY_OVERLAYS = "overlays";
	private static final String KEY_NO_OVERLAY = "no_overlay";

	public static final QuickActionType TYPE = new QuickActionType(MAP_OVERLAY_ACTION_ID,
			"mapoverlay.change", MapOverlayAction.class).
			nameRes(R.string.quick_action_map_overlay).iconRes(R.drawable.ic_layer_top).
			category(QuickActionType.CONFIGURE_MAP);


	public MapOverlayAction() {
		super(TYPE);
	}

	public MapOverlayAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	protected String getTitle(List<Pair<String, String>> filters) {

		if (filters.isEmpty()) return "";

		return filters.size() > 1
				? filters.get(0).second + " +" + (filters.size() - 1)
				: filters.get(0).second;
	}

	@Override
	public String getDisabledItem(OsmandApplication app) {
		return KEY_NO_OVERLAY;
	}

	@Override
	public String getSelectedItem(OsmandApplication app) {
		String mapOverlay = app.getSettings().MAP_OVERLAY.get();
		return mapOverlay != null ? mapOverlay : KEY_NO_OVERLAY;
	}

	@Override
	public String getNextSelectedItem(OsmandApplication app) {
		List<Pair<String, String>> sources = loadListFromParams();
		return getNextItemFromSources(app, sources, KEY_NO_OVERLAY);
	}

	@Override
	protected void saveListToParams(List<Pair<String, String>> list) {
		getParams().put(getListKey(), new Gson().toJson(list));
	}

	@Override
	public List<Pair<String, String>> loadListFromParams() {
		String json = getParams().get(getListKey());
		if (json == null || json.isEmpty()) return new ArrayList<>();

		Type listType = new TypeToken<ArrayList<Pair<String, String>>>() {
		}.getType();

		return new Gson().fromJson(json, listType);
	}

	@Override
	protected String getItemName(Context context, Pair<String, String> item) {
		return item.second;
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		OsmandRasterMapsPlugin plugin = PluginsHelper.getActivePlugin(OsmandRasterMapsPlugin.class);
		if (plugin != null) {
			List<Pair<String, String>> sources = loadListFromParams();
			if (sources.size() > 0) {
				boolean showBottomSheetStyles = Boolean.parseBoolean(getParams().get(KEY_DIALOG));
				if (showBottomSheetStyles) {
					showChooseDialog(mapActivity);
					return;
				}
				String nextItem = getNextSelectedItem(mapActivity.getMyApplication());
				executeWithParams(mapActivity, nextItem);
			}
		}
	}

	@Override
	public void executeWithParams(@NonNull MapActivity mapActivity, String params) {
		OsmandRasterMapsPlugin plugin = PluginsHelper.getActivePlugin(OsmandRasterMapsPlugin.class);
		if (plugin != null) {
			OsmandSettings settings = mapActivity.getMyApplication().getSettings();
			boolean hasOverlay = !params.equals(KEY_NO_OVERLAY);
			if (hasOverlay) {
				settings.MAP_OVERLAY.set(params);
				settings.MAP_OVERLAY_PREVIOUS.set(params);
				if (settings.LAYER_TRANSPARENCY_SEEKBAR_MODE.get() == LayerTransparencySeekbarMode.UNDEFINED) {
					settings.LAYER_TRANSPARENCY_SEEKBAR_MODE.set(LayerTransparencySeekbarMode.OVERLAY);
				}
				if (settings.LAYER_TRANSPARENCY_SEEKBAR_MODE.get() == LayerTransparencySeekbarMode.OVERLAY) {
					mapActivity.getMapLayers().getMapControlsLayer().getMapTransparencyHelper().showTransparencyBar(settings.MAP_OVERLAY_TRANSPARENCY);
				}
			} else {
				settings.MAP_OVERLAY.set(null);
				mapActivity.getMapLayers().getMapControlsLayer().getMapTransparencyHelper().hideTransparencyBar();
				settings.MAP_OVERLAY_PREVIOUS.set(null);
			}
			plugin.updateMapLayers(mapActivity, mapActivity, settings.MAP_OVERLAY);
			Toast.makeText(mapActivity, mapActivity.getString(R.string.quick_action_map_overlay_switch,
					getTranslatedItemName(mapActivity, params)), Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public String getTranslatedItemName(Context context, String item) {
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		OsmandSettings settings = app.getSettings();
		if (item.equals(KEY_NO_OVERLAY)) {
			return context.getString(R.string.no_overlay);
		} else {
			return item.endsWith(IndexConstants.SQLITE_EXT)
					? settings.getTileSourceTitle(item)
					: item;
		}
	}

	@Override
	protected int getAddBtnText() {
		return R.string.quick_action_map_overlay_action;
	}

	@Override
	protected int getDiscrHint() {
		return R.string.quick_action_page_list_descr;
	}

	@Override
	protected int getDiscrTitle() {
		return R.string.quick_action_map_overlay_title;
	}

	@Override
	protected String getListKey() {
		return KEY_OVERLAYS;
	}

	@Override
	protected View.OnClickListener getOnAddBtnClickListener(MapActivity activity, Adapter adapter) {
		return new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				OsmandApplication app = activity.getMyApplication();
				Map<String, String> entriesMap = app.getSettings().getTileSourceEntries();
				entriesMap.put(KEY_NO_OVERLAY, activity.getString(R.string.no_overlay));
				boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
				Context themedContext = UiUtilities.getThemedContext(activity, nightMode);
				AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);
				ArrayList<String> keys = new ArrayList<>(entriesMap.keySet());
				String[] items = new String[entriesMap.size()];
				int i = 0;

				for (String it : entriesMap.values()) {
					items[i++] = it;
				}

				ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(themedContext, R.layout.dialog_text_item);
				arrayAdapter.addAll(items);
				builder.setAdapter(arrayAdapter, (dialog, index) -> {
					Pair<String, String> layer = new Pair<>(keys.get(index), items[index]);
					adapter.addItem(layer, activity);
					dialog.dismiss();
				}).setNegativeButton(R.string.shared_string_cancel, null);

				builder.show();
			}
		};
	}

	@Override
	public boolean fillParams(@NonNull View root, @NonNull MapActivity mapActivity) {
		getParams().put(KEY_DIALOG, Boolean.toString(((SwitchCompat) root.findViewById(R.id.saveButton)).isChecked()));
		return super.fillParams(root, mapActivity);
	}

	@Override
	public String getItemIdFromObject(Pair<String, String> object) {
		return object.first;
	}
}
