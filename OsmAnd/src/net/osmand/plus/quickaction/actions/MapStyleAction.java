package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.MAP_STYLE_ACTION_ID;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.openseamaps.NauticalMapsPlugin;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.quickaction.SwitchableAction;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MapStyleAction extends SwitchableAction<String> {


	private static final String KEY_STYLES = "styles";
	public static final QuickActionType TYPE = new QuickActionType(MAP_STYLE_ACTION_ID,
			"mapstyle.change", MapStyleAction.class).
			nameRes(R.string.quick_action_map_style).iconRes(R.drawable.ic_map).
			category(QuickActionType.CONFIGURE_MAP).nameActionRes(R.string.shared_string_change);


	public MapStyleAction() {
		super(TYPE);
	}

	public MapStyleAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public String getDisabledItem(OsmandApplication app) {
		return null;
	}

	@Override
	public String getSelectedItem(OsmandApplication app) {
		return app.getSettings().RENDERER.get();
	}

	@Override
	public String getNextSelectedItem(OsmandApplication app) {
		List<String> mapStyles = getFilteredStyles();
		if (!Algorithms.isEmpty(mapStyles)) {
			String curStyle = getSelectedItem(app);
			int index = mapStyles.indexOf(curStyle);
			String nextStyle = mapStyles.get(0);

			if (index >= 0 && index + 1 < mapStyles.size()) {
				nextStyle = mapStyles.get(index + 1);
			}
			return nextStyle;
		}
		return null;
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity, @Nullable Bundle params) {
		List<String> mapStyles = getFilteredStyles();
		if (!Algorithms.isEmpty(mapStyles)) {
			boolean showBottomSheetStyles = Boolean.parseBoolean(getParams().get(KEY_DIALOG));
			if (showBottomSheetStyles) {
				showChooseDialog(mapActivity);
				return;
			}
			String nextStyle = getNextSelectedItem(mapActivity.getApp());
			executeWithParams(mapActivity, nextStyle);
		} else {
			AndroidUtils.getApp(mapActivity).showToastMessage(R.string.quick_action_need_to_add_item_to_list);
		}
	}

	@Override
	public void executeWithParams(@NonNull MapActivity mapActivity, String params) {
		OsmandApplication app = mapActivity.getApp();
		RenderingRulesStorage loaded = app.getRendererRegistry().getRenderer(params);
		if (loaded != null) {
			OsmandMapTileView view = mapActivity.getMapView();
			view.getSettings().RENDERER.set(params);

			app.getRendererRegistry().setCurrentSelectedRender(loaded);
			mapActivity.refreshMapComplete();

			app.showShortToastMessage(R.string.quick_action_map_style_switch,
					getTranslatedItemName(mapActivity, params));
		} else {
			app.showShortToastMessage(R.string.renderer_load_exception);
		}
	}

	@Override
	public String getTranslatedItemName(Context context, String item) {
		return RendererRegistry.getRendererName(context, item);
	}

	public List<String> getFilteredStyles() {

		List<String> filtered = new ArrayList<>();
		boolean enabled = PluginsHelper.isActive(NauticalMapsPlugin.class);

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
	protected View.OnClickListener getOnAddBtnClickListener(MapActivity activity, Adapter adapter) {
		return view -> {
			OsmandApplication app = activity.getApp();
			boolean nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.OVER_MAP);
			Context themedContext = UiUtilities.getThemedContext(activity, nightMode);

			AlertDialog.Builder bld = new AlertDialog.Builder(themedContext);
			bld.setTitle(R.string.renderers);

			Map<String, String> renderers = app.getRendererRegistry().getRenderers(false);
			List<String> disabledRendererNames = PluginsHelper.getDisabledRendererNames();

			if (!Algorithms.isEmpty(disabledRendererNames)) {
				Iterator<Map.Entry<String, String>> iterator = renderers.entrySet().iterator();
				while (iterator.hasNext()) {
					String rendererVal = iterator.next().getValue();
					String rendererFileName = Algorithms.getFileWithoutDirs(rendererVal);
					if (disabledRendererNames.contains(rendererFileName)) {
						iterator.remove();
					}
				}
			}

			List<String> visibleNamesList = new ArrayList<>();
			List<String> items = new ArrayList<>(renderers.keySet());
			for (String item : items) {
				String name = RendererRegistry.getRendererName(activity, item);
				visibleNamesList.add(name);
			}

			ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(themedContext, R.layout.dialog_text_item);

			arrayAdapter.addAll(visibleNamesList);
			bld.setAdapter(arrayAdapter, (dialogInterface, i) -> {

				String renderer = items.get(i);
				RenderingRulesStorage loaded = app.getRendererRegistry().getRenderer(renderer);

				if (loaded != null) {

					adapter.addItem(renderer, activity);
				}

				dialogInterface.dismiss();
			});

			bld.setNegativeButton(R.string.shared_string_dismiss, null);
			bld.show();
		};
	}

	@Override
	protected void saveListToParams(List<String> styles) {
		getParams().put(getListKey(), TextUtils.join(",", styles));
	}

	@Override
	public boolean fillParams(@NonNull View root, @NonNull MapActivity mapActivity) {
		getParams().put(KEY_DIALOG, Boolean.toString(((SwitchCompat) root.findViewById(R.id.saveButton)).isChecked()));
		return super.fillParams(root, mapActivity);
	}

	@Override
	public String getItemIdFromObject(String object) {
		return object;
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
	protected String getTitle(List<String> filters, @NonNull Context ctx) {

		if (filters.isEmpty()) return "";

		return filters.size() > 1
				? filters.get(0) + " +" + (filters.size() - 1)
				: filters.get(0);
	}

}