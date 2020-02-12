package net.osmand.plus.quickaction.actions;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.SwitchableAction;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MapSourceAction extends SwitchableAction<Pair<String, String>> {

	public static final int TYPE = 17;
	public static final String LAYER_OSM_VECTOR = "LAYER_OSM_VECTOR";

	private final static String KEY_SOURCE = "source";

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
	public void execute(MapActivity activity) {

		if (OsmandPlugin.getEnabledPlugin(OsmandRasterMapsPlugin.class) != null) {

			OsmandSettings settings = activity.getMyApplication().getSettings();
			List<Pair<String, String>> sources = loadListFromParams();
			if (sources.size() > 0) {
				boolean showBottomSheetStyles = Boolean.valueOf(getParams().get(KEY_DIALOG));
				if (showBottomSheetStyles) {
					showChooseDialog(activity.getSupportFragmentManager());
					return;
				}

				Pair<String, String> currentSource = settings.MAP_ONLINE_DATA.get()
					? new Pair<>(settings.MAP_TILE_SOURCES.get(), settings.MAP_TILE_SOURCES.get())
					: new Pair<>(LAYER_OSM_VECTOR, activity.getString(R.string.vector_data));

				Pair<String, String> nextSource = sources.get(0);
				int index = sources.indexOf(currentSource);

				if (index >= 0 && index + 1 < sources.size()) {
					nextSource = sources.get(index + 1);
				}
				executeWithParams(activity, nextSource.first);
			}
		}
	}

	@Override
	public void executeWithParams(MapActivity activity, String params) {
		OsmandSettings settings = activity.getMyApplication().getSettings();
		if (params.equals(LAYER_OSM_VECTOR)) {
			settings.MAP_ONLINE_DATA.set(false);
			activity.getMapLayers().updateMapSource(activity.getMapView(), null);
		} else {
			settings.MAP_TILE_SOURCES.set(params);
			settings.MAP_ONLINE_DATA.set(true);
			activity.getMapLayers().updateMapSource(activity.getMapView(), settings.MAP_TILE_SOURCES);
		}
		Toast.makeText(activity, activity.getString(R.string.quick_action_map_source_switch,
				getTranslatedItemName(activity, params)), Toast.LENGTH_SHORT).show();
	}

	@Override
	public String getTranslatedItemName(Context context, String item) {
		if (item.equals(LAYER_OSM_VECTOR)) {
			return context.getString(R.string.vector_data);
		} else {
			return item;
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
	protected View.OnClickListener getOnAddBtnClickListener(final MapActivity activity, final Adapter adapter) {
		return new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				OsmandApplication app = activity.getMyApplication();

				final LinkedHashMap<String, String> entriesMap = new LinkedHashMap<>();

				entriesMap.put(LAYER_OSM_VECTOR, activity.getString(R.string.vector_data));
				entriesMap.putAll(app.getSettings().getTileSourceEntries());

				final List<Map.Entry<String, String>> entriesMapList = new ArrayList<>(entriesMap.entrySet());

				boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
				Context themedContext = UiUtilities.getThemedContext(activity, nightMode);
				AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);

				final String[] items = new String[entriesMapList.size()];
				int i = 0;

				for (Map.Entry<String, String> entry : entriesMapList) {
					items[i++] = entry.getValue();
				}

				final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(themedContext, R.layout.dialog_text_item);

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
	protected String getItemName(Context context, Pair<String, String> item) {
		return item.second;
	}

	@Override
	public boolean fillParams(View root, MapActivity activity) {
		getParams().put(KEY_DIALOG, Boolean.toString(((SwitchCompat) root.findViewById(R.id.saveButton)).isChecked()));
		return super.fillParams(root, activity);
	}
}
