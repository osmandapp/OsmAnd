package net.osmand.plus.plugins.srtm;

import static net.osmand.plus.plugins.srtm.CollectColorPalletTask.CollectColorPalletListener;
import static net.osmand.plus.quickaction.QuickActionIds.TERRAIN_COLOR_SCHEME_ACTION;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.card.color.palette.gradient.GradientUiHelper;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.quickaction.SwitchableAction;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.shared.ColorPalette;
import net.osmand.shared.ColorPalette.ColorValue;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TerrainColorSchemeAction extends SwitchableAction<String> {
	public static final QuickActionType TYPE = new QuickActionType(TERRAIN_COLOR_SCHEME_ACTION,
			"terrain.colorscheme.change", TerrainColorSchemeAction.class).
			nameRes(R.string.quick_action_terrain_color_scheme).iconRes(R.drawable.ic_action_appearance).
			category(QuickActionType.CONFIGURE_MAP).nameActionRes(R.string.shared_string_change);

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
		SRTMPlugin srtmPlugin = getSrtmPlugin();
		if (srtmPlugin != null) {
			return srtmPlugin.getTerrainMode().getKeyName();
		}
		return "";
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

	@Nullable
	private SRTMPlugin getSrtmPlugin() {
		return PluginsHelper.getPlugin(SRTMPlugin.class);
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
			SRTMPlugin srtmPlugin = getSrtmPlugin();
			if (srtmPlugin != null) {
				srtmPlugin.setTerrainMode(newMode);
				srtmPlugin.updateLayers(mapActivity, mapActivity);
			}
		}
	}

	@Override
	public String getTranslatedItemName(Context context, String item) {
		return TerrainMode.getByKey(item).getDescription();
	}

	@NonNull
	public List<String> getFilteredStyles() {
		List<String> loadedListFromParams = loadListFromParams();
		List<String> filteredList = new ArrayList<>();
		for (String mode : loadedListFromParams) {
			if (TerrainMode.isModeExist(mode)) {
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
				visibleNamesList.add(mode.getDescription());
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
		List<String> loadedModes = new ArrayList<>();
		String filtersId = getParams().get(getListKey());
		if (filtersId != null && !filtersId.trim().isEmpty()) {
			Collections.addAll(loadedModes, filtersId.split(","));
		}

		List<String> existingModes = new ArrayList<>();
		for (String mode : loadedModes) {
			if (TerrainMode.isModeExist(mode)) {
				existingModes.add(mode);
			}
		}
		return existingModes;
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
	protected void setIcon(@NonNull OsmandApplication app, String item, @NonNull ImageView imageView, @NonNull ProgressBar iconProgressBar) {
		SRTMPlugin srtmPlugin = getSrtmPlugin();
		if (srtmPlugin != null) {
			srtmPlugin.getTerrainModeIcon(item, new CollectColorPalletListener() {

				@Override
				public void collectingPalletStarted() {
					changeProgressBarVisibility(true);
				}

				@Override
				public void collectingPalletFinished(@Nullable ColorPalette colorPalette) {
					if (colorPalette != null) {
						List<ColorValue> colorsList = colorPalette.getColors();
						imageView.setImageDrawable(GradientUiHelper.getGradientDrawable(app, colorsList, GradientDrawable.OVAL));
					} else {
						TerrainColorSchemeAction.super.setIcon(app, item, imageView, iconProgressBar);
					}
					changeProgressBarVisibility(false);
				}

				private void changeProgressBarVisibility(boolean showProgressBar){
					AndroidUiHelper.updateVisibility(imageView, !showProgressBar);
					AndroidUiHelper.updateVisibility(iconProgressBar, showProgressBar);
				}
			});
		} else {
			super.setIcon(app, item, imageView, iconProgressBar);
		}
	}

	@Override
	protected String getTitle(List<String> filters) {
		if (filters.isEmpty()) return "";

		String translatedFirstItem = TerrainMode.getByKey(filters.get(0)).getDescription();
		return filters.size() > 1
				? translatedFirstItem + " +" + (filters.size() - 1)
				: translatedFirstItem;
	}
}