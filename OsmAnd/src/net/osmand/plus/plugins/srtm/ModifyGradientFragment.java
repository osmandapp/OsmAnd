package net.osmand.plus.plugins.srtm;

import static net.osmand.plus.dashboard.DashboardType.TERRAIN;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.card.color.ColoringStyle;
import net.osmand.plus.card.color.ColoringStyleCardController.IColorCardControllerListener;
import net.osmand.plus.card.color.palette.gradient.GradientColorsCollection;
import net.osmand.plus.card.color.palette.gradient.GradientColorsPaletteCard;
import net.osmand.plus.card.color.palette.gradient.GradientColorsPaletteController;
import net.osmand.plus.card.color.palette.gradient.PaletteGradientColor;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.configmap.ConfigureMapOptionFragment;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.srtm.TerrainMode.TerrainType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.util.Algorithms;

public class ModifyGradientFragment extends ConfigureMapOptionFragment implements IColorCardControllerListener {

	private static final String TYPE = "type";
	private static final String ORIGINAL_MODE = "original_mode";
	private static final String SELECTED_MODE = "selected_mode";

	private final SRTMPlugin plugin = PluginsHelper.getPlugin(SRTMPlugin.class);

	private TerrainType type;
	private TerrainMode selectedMode;
	private TerrainMode originalMode;

	private GradientColorsPaletteController controller;

	private TextView titleView;

	@Nullable
	@Override
	protected String getToolbarTitle() {
		return getString(R.string.srtm_color_scheme);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(TYPE)) {
				type = TerrainType.values()[savedInstanceState.getInt(TYPE)];
			}
			if (savedInstanceState.containsKey(ORIGINAL_MODE)) {
				originalMode = TerrainMode.getByKey(savedInstanceState.getString(ORIGINAL_MODE));
			}
			if (savedInstanceState.containsKey(SELECTED_MODE)) {
				selectedMode = TerrainMode.getByKey(savedInstanceState.getString(SELECTED_MODE));
			}
		} else if (plugin != null) {
			originalMode = plugin.getTerrainMode();
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
	protected void setupMainContent(@NonNull ViewGroup container) {
		container.addView(getHeaderView(container));
		addChart(container);
		addDivider(container);
		container.setBackgroundColor(ColorUtilities.getActivityBgColor(app, nightMode));
	}

	@Override
	protected void setupBottomContainer(@NonNull View bottomContainer) {
		bottomContainer.setPadding(0, 0, 0, bottomContainer.getPaddingBottom());
	}

	private void addChart(@NonNull ViewGroup container) {
		LinearLayout linearLayout = new LinearLayout(app);
		linearLayout.setOrientation(LinearLayout.VERTICAL);
		linearLayout.setBackgroundColor(ColorUtilities.getListBgColor(app, nightMode));
		View chartView = new GradientColorsPaletteCard(requireActivity(), getController()).build();
		linearLayout.addView(chartView);
		container.addView(linearLayout);
	}

	@NonNull
	public GradientColorsPaletteController getController() {
		if (controller == null) {
			controller = new GradientColorsPaletteController(app, null);
		}
		GradientColorsCollection collection = new GradientColorsCollection(app, type);
		controller.updateContent(collection, plugin.getTerrainMode().getKeyName());
		controller.setPaletteListener(this);

		return controller;
	}

	private void addDivider(@NonNull ViewGroup container) {
		container.addView(inflate(R.layout.card_bottom_divider));
		container.addView(inflate(R.layout.card_top_divider));
	}

	@NonNull
	private View getHeaderView(@NonNull ViewGroup container) {
		View view = inflate(R.layout.list_item_text_header, container, false);
		titleView = view.findViewById(R.id.title);
		updateTitle();
		return view;
	}

	private void updateTitle(){
		TerrainMode defaultMode = TerrainMode.getDefaultMode(type);
		if (defaultMode != null) {
			String titleString = Algorithms.capitalizeFirstLetter(selectedMode.getKeyName()).replace("_", " ");
			titleView.setText(titleString);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(TYPE, type.ordinal());
		outState.putString(ORIGINAL_MODE, originalMode.getKeyName());
		outState.putString(SELECTED_MODE, selectedMode.getKeyName());
	}

	@Override
	protected void resetToDefault() {
		plugin.setTerrainMode(originalMode);
		selectedMode = originalMode;
		plugin.updateLayers(requireMapActivity(), requireMapActivity());
		controller.updateContent(selectedMode.getKeyName());
		updateApplyButton(isChangesMade());
	}

	@Override
	protected void applyChanges() {
		originalMode = selectedMode;
		controller.refreshLastUsedTime();
	}

	private boolean isChangesMade() {
		return originalMode != selectedMode;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		plugin.setTerrainMode(originalMode);
		plugin.updateLayers(requireMapActivity(), requireMapActivity());
	}

	@Override
	public void onColorSelectedFromPalette(@NonNull PaletteColor paletteColor) {
		if (paletteColor instanceof PaletteGradientColor paletteGradientColor) {
			TerrainType terrainType = TerrainType.valueOf(paletteGradientColor.getTypeName());
			String key = paletteGradientColor.getPaletteName();
			TerrainMode mode = TerrainMode.getMode(terrainType, key);
			if (mode != null) {
				selectedMode = mode;
				plugin.setTerrainMode(mode);
				plugin.updateLayers(requireMapActivity(), requireMapActivity());
				updateApplyButton(isChangesMade());
				updateTitle();
			}
		}
	}

	@Override
	public void onColoringStyleSelected(@Nullable ColoringStyle coloringStyle) {
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull TerrainType type) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			ModifyGradientFragment fragment = new ModifyGradientFragment();
			fragment.type = type;
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss();
		}
	}
}