package net.osmand.plus.plugins.rastermaps;

import static net.osmand.plus.quickaction.QuickActionIds.MAP_SOURCE_ACTION_ID;

import android.content.Context;
import android.content.DialogInterface;
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
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.quickaction.SwitchableAction;
import net.osmand.plus.settings.backend.OsmandSettings;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MapSourceAction extends SwitchableAction<Pair<String, String>> {

	public static final String LAYER_OSM_VECTOR = "LAYER_OSM_VECTOR";
	public static final QuickActionType TYPE = new QuickActionType(MAP_SOURCE_ACTION_ID,
			"mapsource.change", MapSourceAction.class).
			nameRes(R.string.map_source).iconRes(R.drawable.ic_world_globe_dark).
			category(QuickActionType.CONFIGURE_MAP).nameActionRes(R.string.shared_string_change);

	private static final String KEY_SOURCE = "source";

	public MapSourceAction() {
		super(TYPE);
	}

	public MapSourceAction(QuickAction quickAction) {
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
		return LAYER_OSM_VECTOR;
	}

	@Override
	public String getSelectedItem(OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		return settings.MAP_ONLINE_DATA.get() ? settings.MAP_TILE_SOURCES.get() : LAYER_OSM_VECTOR;
	}

	@Override
	public String getNextSelectedItem(OsmandApplication app) {
		List<Pair<String, String>> sources = loadListFromParams();
		return getNextItemFromSources(app, sources, LAYER_OSM_VECTOR);
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
		OsmandSettings settings = mapActivity.getMyApplication().getSettings();
		if (params.equals(LAYER_OSM_VECTOR)) {
			settings.MAP_ONLINE_DATA.set(false);
			mapActivity.getMapLayers().updateMapSource(mapActivity.getMapView(), null);
		} else {
			settings.MAP_TILE_SOURCES.set(params);
			settings.MAP_ONLINE_DATA.set(true);
			mapActivity.getMapLayers().updateMapSource(mapActivity.getMapView(), settings.MAP_TILE_SOURCES);
		}
		Toast.makeText(mapActivity, mapActivity.getString(R.string.quick_action_map_source_switch,
				getTranslatedItemName(mapActivity, params)), Toast.LENGTH_SHORT).show();
	}

	@Override
	public String getTranslatedItemName(Context context, String item) {
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		OsmandSettings settings = app.getSettings();
		if (item.equals(LAYER_OSM_VECTOR)) {
			return context.getString(R.string.vector_data);
		} else {
			return item.endsWith(IndexConstants.SQLITE_EXT)
					? settings.getTileSourceTitle(item)
					: item;
		}
	}

	@Override
	protected int getAddBtnText() {
		return R.string.quick_action_map_source_action;
	}

	@Override
	protected int getDiscrHint() {
		return R.string.quick_action_page_list_descr;
	}

	@Override
	protected int getDiscrTitle() {
		return R.string.quick_action_map_source_title;
	}

	@Override
	protected String getListKey() {
		return KEY_SOURCE;
	}

	@Override
	protected View.OnClickListener getOnAddBtnClickListener(MapActivity activity, Adapter adapter) {
		return new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				OsmandApplication app = activity.getMyApplication();

				LinkedHashMap<String, String> entriesMap = new LinkedHashMap<>();

				entriesMap.put(LAYER_OSM_VECTOR, activity.getString(R.string.vector_data));
				entriesMap.putAll(app.getSettings().getTileSourceEntries());

				List<Map.Entry<String, String>> entriesMapList = new ArrayList<>(entriesMap.entrySet());

				boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
				Context themedContext = UiUtilities.getThemedContext(activity, nightMode);
				AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);

				String[] items = new String[entriesMapList.size()];
				int i = 0;

				for (Map.Entry<String, String> entry : entriesMapList) {
					items[i++] = entry.getValue();
				}

				ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(themedContext, R.layout.dialog_text_item);

				arrayAdapter.addAll(items);
				builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int i) {

						Pair<String, String> layer = new Pair<>(
								entriesMapList.get(i).getKey(),
								entriesMapList.get(i).getValue());

						adapter.addItem(layer, activity);

						dialog.dismiss();
					}
				});

				builder.setNegativeButton(R.string.shared_string_dismiss, null);
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
