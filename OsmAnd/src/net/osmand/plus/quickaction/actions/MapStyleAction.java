package net.osmand.plus.quickaction.actions;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dialogs.ConfigureMapMenu;
import net.osmand.plus.openseamapsplugin.NauticalMapsPlugin;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.SwitchableAction;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class MapStyleAction extends SwitchableAction<String> {

	public static final int TYPE = 14;

	private final static String KEY_STYLES = "styles";

	public MapStyleAction() {
		super(TYPE);
	}

	public MapStyleAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(MapActivity activity) {
		List<String> mapStyles = getFilteredStyles();
		if (!Algorithms.isEmpty(mapStyles)) {
			boolean showBottomSheetStyles = Boolean.valueOf(getParams().get(KEY_DIALOG));
			if (showBottomSheetStyles) {
				showChooseDialog(activity.getSupportFragmentManager());
				return;
			}
			String curStyle = activity.getMyApplication().getSettings().RENDERER.get();
			int index = mapStyles.indexOf(curStyle);
			String nextStyle = mapStyles.get(0);

			if (index >= 0 && index + 1 < mapStyles.size()) {
				nextStyle = mapStyles.get(index + 1);
			}
			executeWithParams(activity, nextStyle);
		} else {
			Toast.makeText(activity, R.string.quick_action_need_to_add_item_to_list,
				Toast.LENGTH_LONG).show();
		}

	}

	@Override
	public void executeWithParams(MapActivity activity, String params) {
		OsmandApplication app = activity.getMyApplication();
		RenderingRulesStorage loaded = app.getRendererRegistry().getRenderer(params);
		if (loaded != null) {
			OsmandMapTileView view = activity.getMapView();
			view.getSettings().RENDERER.set(params);

			app.getRendererRegistry().setCurrentSelectedRender(loaded);
			ConfigureMapMenu.refreshMapComplete(activity);

			Toast.makeText(activity, activity.getString(R.string.quick_action_map_style_switch,
					getTranslatedItemName(activity, params)), Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(activity, R.string.renderer_load_exception, Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public String getTranslatedItemName(Context context, String item) {
		String translation = RendererRegistry.getTranslatedRendererName(context, item);
		return translation != null ? translation
				: item.replace('_', ' ').replace('-', ' ');
	}

	public List<String> getFilteredStyles() {

		List<String> filtered = new ArrayList<>();
		boolean enabled = OsmandPlugin.getEnabledPlugin(NauticalMapsPlugin.class) != null;

		if (enabled) return loadListFromParams();
		else {

			for (String style : loadListFromParams()) {

				if (!style.equals(RendererRegistry.NAUTICAL_RENDER)) {
					filtered.add(style);
				}
			}
		}

		return filtered;
	}

	@Override
	protected int getAddBtnText() {
		return R.string.quick_action_map_style_action;
	}

	@Override
	protected int getDiscrHint() {
		return R.string.quick_action_page_list_descr;
	}

	@Override
	protected int getDiscrTitle() {
		return R.string.quick_action_map_styles;
	}

	@Override
	protected String getListKey() {
		return KEY_STYLES;
	}

	@Override
	protected View.OnClickListener getOnAddBtnClickListener(final MapActivity activity, final Adapter adapter) {
		return new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final OsmandApplication app = activity.getMyApplication();
				boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
				Context themedContext = UiUtilities.getThemedContext(activity, nightMode);

				AlertDialog.Builder bld = new AlertDialog.Builder(themedContext);
				bld.setTitle(R.string.renderers);

				final List<String> visibleNamesList = new ArrayList<>();
				final ArrayList<String> items = new ArrayList<>(app.getRendererRegistry().getRendererNames());
				final boolean nauticalPluginDisabled = OsmandPlugin.getEnabledPlugin(NauticalMapsPlugin.class) == null;

				Iterator<String> iterator = items.iterator();
				while (iterator.hasNext()) {
					String item = iterator.next();
					if (nauticalPluginDisabled && item.equals(RendererRegistry.NAUTICAL_RENDER)) {
						iterator.remove();
					} else {
						String translation = RendererRegistry.getTranslatedRendererName(activity, item);
						visibleNamesList.add(translation != null ? translation
								: item.replace('_', ' ').replace('-', ' '));
					}
				}

				final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(themedContext, R.layout.dialog_text_item);

				arrayAdapter.addAll(visibleNamesList);
				bld.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {

						String renderer = items.get(i);
						RenderingRulesStorage loaded = app.getRendererRegistry().getRenderer(renderer);

						if (loaded != null) {

							adapter.addItem(renderer, activity);
						}

						dialogInterface.dismiss();
					}
				});

				bld.setNegativeButton(R.string.shared_string_dismiss, null);
				bld.show();
			}
		};
	}

	@Override
	protected void saveListToParams(List<String> styles) {
		getParams().put(getListKey(), TextUtils.join(",", styles));
	}

	@Override
	public boolean fillParams(View root, MapActivity activity) {
		getParams().put(KEY_DIALOG, Boolean.toString(((SwitchCompat) root.findViewById(R.id.saveButton)).isChecked()));
		return super.fillParams(root, activity);
	}

	@Override
	public List<String> loadListFromParams() {

		List<String> styles = new ArrayList<>();

		String filtersId = getParams().get(getListKey());

		if (filtersId != null && !filtersId.trim().isEmpty()) {
			Collections.addAll(styles, filtersId.split(","));
		}

		return styles;
	}

	@Override
	protected String getItemName(Context context, String item) {
		if (context != null) {
			return getTranslatedItemName(context, item);
		} else {
			return item;
		}
	}

	@Override
	protected String getTitle(List<String> filters) {

		if (filters.isEmpty()) return "";

		return filters.size() > 1
				? filters.get(0) + " +" + (filters.size() - 1)
				: filters.get(0);
	}
}