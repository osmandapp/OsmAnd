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
import net.osmand.plus.OsmandSettings.LayerTransparencySeekbarMode;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.SwitchableAction;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapOverlayAction extends SwitchableAction<Pair<String, String>> {
	public static final int TYPE = 15;

	private final static String KEY_OVERLAYS = "overlays";
	private final static String KEY_NO_OVERLAY = "no_overlay";

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
	public void execute(MapActivity activity) {

		OsmandRasterMapsPlugin plugin = OsmandPlugin.getEnabledPlugin(OsmandRasterMapsPlugin.class);

		if (plugin != null) {

			OsmandSettings settings = activity.getMyApplication().getSettings();
			List<Pair<String, String>> sources = loadListFromParams();

			if (sources.size() > 0) {
				boolean showBottomSheetStyles = Boolean.valueOf(getParams().get(KEY_DIALOG));
				if (showBottomSheetStyles) {
					showChooseDialog(activity.getSupportFragmentManager());
					return;
				}

				int index = -1;
				final String currentSource = settings.MAP_OVERLAY.get() == null ? KEY_NO_OVERLAY
					: settings.MAP_OVERLAY.get();

				for (int idx = 0; idx < sources.size(); idx++) {
					if (sources.get(idx).first.equals(currentSource)) {
						index = idx;
						break;
					}
				}

				Pair<String, String> nextSource = sources.get(0);

				if (index >= 0 && index + 1 < sources.size()) {
					nextSource = sources.get(index + 1);
				}
				executeWithParams(activity, nextSource.first);
			}
		}
	}

	@Override
	public void executeWithParams(MapActivity activity, String params) {
		OsmandRasterMapsPlugin plugin = OsmandPlugin.getEnabledPlugin(OsmandRasterMapsPlugin.class);
		if (plugin != null) {
			OsmandSettings settings = activity.getMyApplication().getSettings();
			boolean hasOverlay = !params.equals(KEY_NO_OVERLAY);
			if (hasOverlay) {
				settings.MAP_OVERLAY.set(params);
				settings.MAP_OVERLAY_PREVIOUS.set(params);
				if (settings.LAYER_TRANSPARENCY_SEEKBAR_MODE.get() == LayerTransparencySeekbarMode.UNDEFINED) {
					settings.LAYER_TRANSPARENCY_SEEKBAR_MODE.set(LayerTransparencySeekbarMode.OVERLAY);
				}
				if (settings.LAYER_TRANSPARENCY_SEEKBAR_MODE.get() == OsmandSettings.LayerTransparencySeekbarMode.OVERLAY) {
					activity.getMapLayers().getMapControlsLayer().showTransparencyBar(settings.MAP_OVERLAY_TRANSPARENCY, true);
				}
			} else {
				settings.MAP_OVERLAY.set(null);
				activity.getMapLayers().getMapControlsLayer().hideTransparencyBar();
				settings.MAP_OVERLAY_PREVIOUS.set(null);
			}
			plugin.updateMapLayers(activity.getMapView(), settings.MAP_OVERLAY, activity.getMapLayers());
			Toast.makeText(activity, activity.getString(R.string.quick_action_map_overlay_switch,
					getTranslatedItemName(activity, params)), Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public String getTranslatedItemName(Context context, String item) {
		if (item.equals(KEY_NO_OVERLAY)) {
			return context.getString(R.string.no_overlay);
		} else {
			return item;
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
	protected View.OnClickListener getOnAddBtnClickListener(final MapActivity activity, final Adapter adapter) {
		return new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				OsmandApplication app = activity.getMyApplication();
				Map<String, String> entriesMap = app.getSettings().getTileSourceEntries();
				entriesMap.put(KEY_NO_OVERLAY, activity.getString(R.string.no_overlay));
				boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
				Context themedContext = UiUtilities.getThemedContext(activity, nightMode);
				AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);
				final ArrayList<String> keys = new ArrayList<>(entriesMap.keySet());
				final String[] items = new String[entriesMap.size()];
				int i = 0;

				for (String it : entriesMap.values()) {
					items[i++] = it;
				}

				final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(themedContext, R.layout.dialog_text_item);
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

	@Override
	public boolean fillParams(View root, MapActivity activity) {
		getParams().put(KEY_DIALOG, Boolean.toString(((SwitchCompat) root.findViewById(R.id.saveButton)).isChecked()));
		return super.fillParams(root, activity);
	}
}
