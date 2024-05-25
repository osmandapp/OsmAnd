package net.osmand.plus.plugins.srtm;

import static net.osmand.plus.quickaction.QuickActionIds.TERRAIN_COLOR_SCHEME_ACTION;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;

import net.osmand.ColorPalette;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.quickaction.SwitchableAction;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TerrainColorSchemeAction extends SwitchableAction<String> {
	public static final QuickActionType TYPE = new QuickActionType(TERRAIN_COLOR_SCHEME_ACTION,
			"terrain.colorscheme.change", TerrainColorSchemeAction.class).
			nameRes(R.string.change_terrain_color_scheme).iconRes(R.drawable.ic_action_appearance).
			category(QuickActionType.TOPOGRAPHY);

	private static final String KEY_TERRAIN_MODES = "terrain_modes";

	public TerrainColorSchemeAction() {
		super(TYPE);
	}

	public TerrainColorSchemeAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public String getDisabledItem(OsmandApplication app) {
		return null;
	}

	@Override
	public String getSelectedItem(OsmandApplication app) {
		return PluginsHelper.getPlugin(SRTMPlugin.class).getTerrainMode().getKeyName();
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
	public void execute(@NonNull MapActivity mapActivity) {
		List<String> mapStyles = getFilteredStyles();
		if (!Algorithms.isEmpty(mapStyles)) {
			boolean showBottomSheetStyles = Boolean.parseBoolean(getParams().get(KEY_DIALOG));
			if (showBottomSheetStyles) {
				showChooseDialog(mapActivity);
				return;
			}
			String nextStyle = getNextSelectedItem(mapActivity.getMyApplication());
			executeWithParams(mapActivity, nextStyle);
		} else {
			Toast.makeText(mapActivity, R.string.quick_action_need_to_add_item_to_list,
					Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void executeWithParams(@NonNull MapActivity mapActivity, String params) {
		TerrainMode newMode = TerrainMode.getByKey(params);
		if (newMode != null) {
			SRTMPlugin srtmPlugin = PluginsHelper.getPlugin(SRTMPlugin.class);
			if (srtmPlugin != null) {
				srtmPlugin.setTerrainMode(newMode);
				srtmPlugin.updateLayers(mapActivity, mapActivity);
			}
		}
	}

	@Override
	public String getTranslatedItemName(Context context, String item) {
		return TerrainMode.getByKey(item).translateName;
	}

	public List<String> getFilteredStyles() {
		List<String> loadedListFromParams = loadListFromParams();
		List<String> filteredList = new ArrayList<>();
		for (String mode : loadedListFromParams) {
			if (TerrainMode.getByKey(mode) != null) {
				filteredList.add(mode);
			}
		}

		return filteredList;
	}

	@Override
	protected int getAddBtnText() {
		return R.string.shared_string_add;
	}

	@Override
	protected int getDiscrHint() {
		return R.string.quick_action_page_list_descr;
	}

	@Override
	protected int getDiscrTitle() {
		return R.string.srtm_color_scheme;
	}

	@Override
	protected String getListKey() {
		return KEY_TERRAIN_MODES;
	}

	@Override
	protected View.OnClickListener getOnAddBtnClickListener(MapActivity activity, Adapter adapter) {
		return view -> {
			OsmandApplication app = activity.getMyApplication();
			boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
			Context themedContext = UiUtilities.getThemedContext(activity, nightMode);

			AlertDialog.Builder bld = new AlertDialog.Builder(themedContext);
			bld.setTitle(R.string.srtm_color_scheme);

			List<String> visibleNamesList = new ArrayList<>();
			List<String> items = new ArrayList<>();
			for (TerrainMode mode : TerrainMode.values(app)) {
				items.add(mode.getKeyName());
				visibleNamesList.add(mode.translateName);
			}

			ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(themedContext, R.layout.dialog_text_item);

			arrayAdapter.addAll(visibleNamesList);
			bld.setAdapter(arrayAdapter, (dialogInterface, i) -> {
				String renderer = items.get(i);
				adapter.addItem(renderer, activity);
				dialogInterface.dismiss();
			});

			bld.setNegativeButton(R.string.shared_string_dismiss, null);
			bld.show();
		};
	}

	@Override
	protected void saveListToParams(List<String> terrainModes) {
		getParams().put(getListKey(), TextUtils.join(",", terrainModes));
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
		List<String> modes = new ArrayList<>();

		String filtersId = getParams().get(getListKey());

		if (filtersId != null && !filtersId.trim().isEmpty()) {
			Collections.addAll(modes, filtersId.split(","));
		}

		return modes;
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
	protected Drawable getIcon(@NonNull OsmandApplication app, String item) {
		TerrainMode mode = TerrainMode.getByKey(item);
		if (mode == null) {
			return null;
		}

		File heightmapDir = app.getAppPath(IndexConstants.CLR_PALETTE_DIR);
		File mainColorFile = new File(heightmapDir, mode.getMainFile());

		ColorPalette colorPalette = null;
		try {
			if (mainColorFile.exists()) {
				colorPalette = ColorPalette.parseColorPalette(new FileReader(mainColorFile));
			}
		} catch (IOException e) {
			PlatformUtil.getLog(TerrainColorSchemeAction.class).error("Error reading color file ", e);
		}

		if (colorPalette == null) {
			return null;
		}
		int[] colors = new int[colorPalette.getColors().size()];
		for (int i = 0; i < colorPalette.getColors().size(); i++) {
			ColorPalette.ColorValue value = colorPalette.getColors().get(i);
			colors[i] = Color.argb(value.a, value.r, value.g, value.b);
		}
		GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
		gradientDrawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
		gradientDrawable.setShape(GradientDrawable.OVAL);
		return gradientDrawable;
	}

	@Override
	protected String getTitle(List<String> filters) {
		if (filters.isEmpty()) return "";

		String translatedFirstItem = TerrainMode.getByKey(filters.get(0)).translateName;
		return filters.size() > 1
				? translatedFirstItem + " +" + (filters.size() - 1)
				: translatedFirstItem;
	}
}