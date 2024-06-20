package net.osmand.plus.plugins.srtm;

import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.TERRAIN;

import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.ColorPalette;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.card.color.ColoringStyle;
import net.osmand.plus.card.color.ColoringStyleCardController.IColorCardControllerListener;
import net.osmand.plus.card.color.palette.gradient.GradientCollection;
import net.osmand.plus.card.color.palette.gradient.GradientColorsPaletteCard;
import net.osmand.plus.card.color.palette.gradient.GradientColorsPaletteController;
import net.osmand.plus.card.color.palette.gradient.PaletteGradientColor;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.configmap.ConfigureMapOptionFragment;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.srtm.TerrainMode.TerrainType;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

import java.util.Map;

public class ModifyGradientFragment extends ConfigureMapOptionFragment implements IColorCardControllerListener {
	public static final String TAG = ModifyGradientFragment.class.getSimpleName();

	public static final String TYPE = "type";
	public static final String ORIGINAL_MODE = "original_mode";
	public static final String SELECTED_MODE = "selected_mode";

	private TerrainType type;
	private TerrainMode selectedMode;
	private TerrainMode originalMode;

	private GradientColorsPaletteController gradientPaletteController;
	private SRTMPlugin srtmPlugin;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		srtmPlugin = PluginsHelper.getPlugin(SRTMPlugin.class);
		if (savedInstanceState != null && savedInstanceState.containsKey(TYPE)) {
			if (savedInstanceState.containsKey(TYPE)) {
				type = TerrainType.values()[savedInstanceState.getInt(TYPE)];
			}
			if (savedInstanceState.containsKey(TYPE)) {
				originalMode = TerrainMode.getByKey(savedInstanceState.getString(ORIGINAL_MODE));
			}
			if (savedInstanceState.containsKey(TYPE)) {
				selectedMode = TerrainMode.getByKey(savedInstanceState.getString(SELECTED_MODE));
			}
		} else if (srtmPlugin != null) {
			originalMode = srtmPlugin.getTerrainMode();
			selectedMode = originalMode;
		}
		MapActivity activity = requireMapActivity();
		activity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				activity.getSupportFragmentManager().popBackStack();
				activity.getDashboard().setDashboardVisibility(true, TERRAIN, false);
			}
		});
	}

	@Override
	public void onDestroy() {
		srtmPlugin.setTerrainMode(originalMode);
		srtmPlugin.updateLayers(requireMapActivity(), requireMapActivity());
		super.onDestroy();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(TYPE, type.ordinal());
		outState.putString(ORIGINAL_MODE, originalMode.getKeyName());
		outState.putString(SELECTED_MODE, selectedMode.getKeyName());
	}

	public void setType(TerrainType type) {
		this.type = type;
	}

	@Nullable
	@Override
	protected String getToolbarTitle() {
		return getString(R.string.srtm_color_scheme);
	}

	@Override
	protected void resetToDefault() {
		srtmPlugin.setTerrainMode(originalMode);
		selectedMode = originalMode;
		srtmPlugin.updateLayers(requireMapActivity(), requireMapActivity());
		gradientPaletteController.updateContent(selectedMode.getKeyName());
		updateApplyButton(isChangesMade());
	}

	@Override
	protected void setupMainContent(@NonNull ViewGroup container) {
		container.addView(getHeaderView(container));
		addChart(container);
		addDivider(container);
		container.setBackgroundColor(ColorUtilities.getActivityBgColor(app, nightMode));
	}

	private void addChart(@NonNull ViewGroup container) {
		LinearLayout linearLayout = new LinearLayout(app);
		linearLayout.setOrientation(LinearLayout.VERTICAL);
		linearLayout.setBackgroundColor(ColorUtilities.getListBgColor(app, nightMode));
		View chartView = new GradientColorsPaletteCard(requireActivity(), getGradientPaletteController()).build();
		linearLayout.addView(chartView);
		container.addView(linearLayout);
	}

	private void addDivider(@NonNull ViewGroup container) {
		View bottomDivider = themedInflater.inflate(R.layout.card_bottom_divider, container, false);
		container.addView(bottomDivider);
		View topDivider = themedInflater.inflate(R.layout.card_top_divider, container, false);
		container.addView(topDivider);
	}

	private View getHeaderView(@NonNull ViewGroup container) {
		View view = themedInflater.inflate(R.layout.list_item_text_header, container, false);
		TextView title = view.findViewById(R.id.title);
		LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) title.getLayoutParams();
		params.topMargin = 0;
		params. bottomMargin = 0;
		title.setLayoutParams(params);
		TerrainMode defaultMode = TerrainMode.getDefaultMode(type);
		if (defaultMode != null) {
			title.setText(defaultMode.translateName);
		}
		return view;
	}

	@NonNull
	public GradientColorsPaletteController getGradientPaletteController() {
		OsmandSettings settings = app.getSettings();

		Map<String, Pair<ColorPalette, Long>> colorPaletteMap = app.getColorPaletteHelper().getPalletsForType(type);
		GradientCollection gradientCollection = new GradientCollection(colorPaletteMap, settings.GRADIENT_PALETTES, type);

		if (gradientPaletteController == null) {
			gradientPaletteController = new GradientColorsPaletteController(app, null);
		}
		gradientPaletteController.updateContent(gradientCollection, srtmPlugin.getTerrainMode().getKeyName());
		gradientPaletteController.setPaletteListener(this);
		return gradientPaletteController;
	}

	@Override
	protected void applyChanges() {
		originalMode = selectedMode;
		gradientPaletteController.refreshLastUsedTime();
	}

	private boolean isChangesMade() {
		return originalMode != selectedMode;
	}

	public static void showInstance(@NonNull FragmentManager manager, TerrainType type) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			ModifyGradientFragment fragment = new ModifyGradientFragment();
			fragment.setType(type);
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss();
		}
	}

	@Override
	public void onColoringStyleSelected(@Nullable ColoringStyle coloringStyle) {

	}

	@Override
	public void onColorSelectedFromPalette(@NonNull PaletteColor paletteColor) {
		if (paletteColor instanceof PaletteGradientColor) {
			PaletteGradientColor paletteGradientColor = (PaletteGradientColor) paletteColor;
			TerrainType terrainType = TerrainType.valueOf(paletteGradientColor.getTypeName());
			String key = paletteGradientColor.getPaletteName();
			TerrainMode mode = TerrainMode.getMode(terrainType, key);
			if (mode != null) {
				selectedMode = mode;
				srtmPlugin.setTerrainMode(mode);
				srtmPlugin.updateLayers(requireMapActivity(), requireMapActivity());
				updateApplyButton(isChangesMade());
			}
		}
	}

	@Override
	public void onColorAddedToPalette(@Nullable PaletteColor oldColor, @NonNull PaletteColor newColor) {
		IColorCardControllerListener.super.onColorAddedToPalette(oldColor, newColor);
	}
}