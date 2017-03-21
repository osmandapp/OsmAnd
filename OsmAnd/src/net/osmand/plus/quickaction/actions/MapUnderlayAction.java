package net.osmand.plus.quickaction.actions;

import android.content.DialogInterface;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.SwitchableAction;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapUnderlayAction extends SwitchableAction<Pair<String, String>> {

	public static final int TYPE = 16;

	private final static String KEY_UNDERLAYS = "underlays";
	private final static String KEY_NO_UNDERLAY = "no_underlay";

	public MapUnderlayAction() {
		super(TYPE);
	}

	public MapUnderlayAction(QuickAction quickAction) {
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
	protected List<Pair<String, String>> loadListFromParams() {

		String json = getParams().get(getListKey());

		if (json == null || json.isEmpty()) return new ArrayList<>();

		Type listType = new TypeToken<ArrayList<Pair<String, String>>>() {
		}.getType();

		return new Gson().fromJson(json, listType);
	}

	@Override
	protected String getItemName(Pair<String, String> item) {
		return item.second;
	}

	@Override
	public void execute(MapActivity activity) {
		OsmandRasterMapsPlugin plugin = OsmandPlugin.getEnabledPlugin(OsmandRasterMapsPlugin.class);

		if (plugin != null) {

			OsmandSettings settings = activity.getMyApplication().getSettings();
			List<Pair<String, String>> sources = loadListFromParams();

			Pair<String, String> currentSource = new Pair<>(
					settings.MAP_UNDERLAY.get(),
					settings.MAP_UNDERLAY.get());

			Pair<String, String> nextSource = sources.get(0);
			int index = sources.indexOf(currentSource);

			if (index >= 0 && index + 1 < sources.size()) {
				nextSource = sources.get(index + 1);
			}

			boolean hasUnderlay = !nextSource.first.equals(KEY_NO_UNDERLAY);
			if (hasUnderlay) {
				settings.MAP_UNDERLAY.set(nextSource.first);
				settings.MAP_UNDERLAY_PREVIOUS.set(nextSource.first);
			} else {
				settings.MAP_UNDERLAY.set(null);
				settings.MAP_UNDERLAY_PREVIOUS.set(null);
			}

			final OsmandSettings.CommonPreference<Boolean> hidePolygonsPref =
					activity.getMyApplication().getSettings().getCustomRenderBooleanProperty("noPolygons");
			hidePolygonsPref.set(hasUnderlay);

			plugin.updateMapLayers(activity.getMapView(), settings.MAP_UNDERLAY, activity.getMapLayers());
			Toast.makeText(activity, activity.getString(R.string.quick_action_map_underlay_switch, nextSource.second), Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	protected int getAddBtnText() {
		return R.string.quick_action_map_underlay_action;
	}

	@Override
	protected int getDiscrHint() {
		return R.string.quick_action_page_list_descr;
	}

	@Override
	protected int getDiscrTitle() {
		return R.string.quick_action_map_underlay_title;
	}

	@Override
	protected String getListKey() {
		return KEY_UNDERLAYS;
	}

	@Override
	protected View.OnClickListener getOnAddBtnClickListener(final MapActivity activity, final Adapter adapter) {
		return new View.OnClickListener() {
			@Override
			public void onClick(View view) {

				final OsmandSettings settings = activity.getMyApplication().getSettings();
				Map<String, String> entriesMap = settings.getTileSourceEntries();
				entriesMap.put(KEY_NO_UNDERLAY, activity.getString(R.string.no_underlay));
				AlertDialog.Builder builder = new AlertDialog.Builder(activity);
				final ArrayList<String> keys = new ArrayList<>(entriesMap.keySet());
				final String[] items = new String[entriesMap.size()];
				int i = 0;

				for (String it : entriesMap.values()) {
					items[i++] = it;
				}

				final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(activity, R.layout.dialog_text_item);
				arrayAdapter.addAll(items);
				builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int i) {

						Pair<String, String> layer = new Pair<>(
								keys.get(i), items[i]);

						adapter.addItem(layer, activity);

						dialog.dismiss();

					}
				}).setNegativeButton(R.string.shared_string_cancel, null);

				builder.show();
			}
		};
	}
}
