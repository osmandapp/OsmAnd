package net.osmand.plus.plugins.srtm;

import static net.osmand.plus.dashboard.DashboardType.BUILDINGS_3D;

import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.card.color.palette.main.ColorsPaletteCard;
import net.osmand.plus.card.color.palette.main.ColorsPaletteController;
import net.osmand.plus.configmap.ConfigureMapOptionFragment;
import net.osmand.plus.configmap.tracks.appearance.data.AppearanceData;
import net.osmand.plus.configmap.tracks.appearance.subcontrollers.ColorCardController;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.editors.controller.EditorColorController;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;
import net.osmand.shared.gpx.GpxParameter;

import java.util.List;
import java.util.Locale;

public class Buildings3DColorFragment extends ConfigureMapOptionFragment implements BaseCard.CardListener {
	public static final String DAY_COLOR = "day_color";
	public static final String NIGHT_COLOR = "night_color";
	public static final String COLOR_TYPE = "color_type";
	public static final String DAY_NIGHT_SELECTOR = "day_night_selector";

	private SRTMPlugin srtmPlugin;
	private Buildings3DColorType colorType = Buildings3DColorType.MAP_STYLE;
	private TextView colorTypeTv;
	private View customColorViewContainer;
	private View mapStyleColorViewContainer;
	private TextToggleButton dayNightToggleButton;

	private ColorCardController colorCardController;
	private boolean isDayModeColorSelection = true;
	private int dayColor;
	private int nightColor;
	private ColorsPaletteCard colorsPaletteCard;


	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		srtmPlugin = PluginsHelper.getPlugin(SRTMPlugin.class);
		if (srtmPlugin != null) {
			if (savedInstanceState != null) {
				dayColor = savedInstanceState.getInt(DAY_COLOR);
				nightColor = savedInstanceState.getInt(NIGHT_COLOR);
				colorType = Buildings3DColorType.Companion.getById(savedInstanceState.getInt(COLOR_TYPE));
				isDayModeColorSelection = savedInstanceState.getBoolean(DAY_NIGHT_SELECTOR);
			} else {
				dayColor = srtmPlugin.BUILDINGS_3D_CUSTOM_DAY_COLOR.get();
				nightColor = srtmPlugin.BUILDINGS_3D_CUSTOM_NIGHT_COLOR.get();
				colorType = Buildings3DColorType.Companion.getById(srtmPlugin.BUILDINGS_3D_COLOR_STYLE.get());
				isDayModeColorSelection = true;
			}
		}
		MapActivity activity = requireMapActivity();
		activity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				activity.getSupportFragmentManager().popBackStack();
				activity.getDashboard().setDashboardVisibility(true, BUILDINGS_3D, false);
			}
		});
	}

	@Override
	public void onDestroy() {
		srtmPlugin.apply3DBuildingsColorStyle(Buildings3DColorType.Companion.getById(srtmPlugin.BUILDINGS_3D_COLOR_STYLE.get()));
		srtmPlugin.apply3DBuildingsColor(nightMode ? srtmPlugin.BUILDINGS_3D_CUSTOM_NIGHT_COLOR.get() : srtmPlugin.BUILDINGS_3D_CUSTOM_DAY_COLOR.get());
		super.onDestroy();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(DAY_COLOR, dayColor);
		outState.putInt(NIGHT_COLOR, nightColor);
	}

	@Nullable
	@Override
	protected String getToolbarTitle() {
		return getString(R.string.enable_3d_objects);
	}

	@Override
	protected void resetToDefault() {
		dayColor = SRTMPlugin.BUILDINGS_3D_DEFAULT_COLOR;
		nightColor = SRTMPlugin.BUILDINGS_3D_DEFAULT_COLOR;
		colorType = Buildings3DColorType.MAP_STYLE;
		srtmPlugin.apply3DBuildingsColorStyle(colorType);
		updateColorType();
		updateDayNightSelection();
		updateApplyButton(isChangesMade());
		refreshMap();
	}

	@Override
	protected void setupMainContent(@NonNull ViewGroup container) {
		View view = inflate(R.layout.buildings_3d_color_fragment, container, false);
		createColorSelector(view);
		container.addView(view);
	}

	@Override
	protected void applyChanges() {
		srtmPlugin.BUILDINGS_3D_CUSTOM_DAY_COLOR.set(dayColor);
		srtmPlugin.BUILDINGS_3D_CUSTOM_NIGHT_COLOR.set(nightColor);
		srtmPlugin.BUILDINGS_3D_COLOR_STYLE.set(colorType.getId());
		srtmPlugin.apply3DBuildingsColorStyle(colorType);
	}

	private boolean isChangesMade() {

		return srtmPlugin.BUILDINGS_3D_CUSTOM_DAY_COLOR.get() != dayColor ||
				srtmPlugin.BUILDINGS_3D_CUSTOM_NIGHT_COLOR.get() != nightColor ||
				srtmPlugin.BUILDINGS_3D_COLOR_STYLE.get() != colorType.getId();
	}


	private ColorCardController getColorCardController() {
		if (colorCardController == null) {
			AppearanceData data = new AppearanceData();
			data.setParameter(GpxParameter.COLOR, null);
			data.setParameter(GpxParameter.COLORING_TYPE, null);
			colorCardController = new ColorCardController(app, data, true);
		}
		return colorCardController;
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, new Buildings3DColorFragment(), TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss();
		}
	}

	@Override
	public void onCardLayoutNeeded(@NonNull BaseCard card) {
		BaseCard.CardListener.super.onCardLayoutNeeded(card);
	}

	@Override
	public void onCardPressed(@NonNull BaseCard card) {
		BaseCard.CardListener.super.onCardPressed(card);
	}

	@Override
	public void onCardButtonPressed(@NonNull BaseCard card, int buttonIndex) {
		BaseCard.CardListener.super.onCardButtonPressed(card, buttonIndex);
	}

	private void createColorSelector(@NonNull View view) {
		ViewGroup cardsContainer = view.findViewById(R.id.color_card_container);
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			colorsPaletteCard = new ColorsPaletteCard(mapActivity, getColorController());
			cardsContainer.addView(colorsPaletteCard.build(cardsContainer.getContext()));
			getColorController().selectColor(isDayModeColorSelection ? dayColor : nightColor);
			colorsPaletteCard.updatePaletteColors(getColorController().getSelectedColor());
		}
		getColorController().setPaletteListener(paletteColor -> {
			if (isDayModeColorSelection) {
				dayColor = paletteColor.getColor();
			} else {
				nightColor = paletteColor.getColor();
			}
			if(isDayModeColorSelection && !nightMode || !isDayModeColorSelection && nightMode) {
				srtmPlugin.apply3DBuildingsColor(paletteColor.getColor());
				MapActivity activity = requireMapActivity();
				activity.refreshMapComplete();
				activity.updateLayers();
			}
			updateApplyButton(isChangesMade());
		});

		colorTypeTv = view.findViewById(R.id.coloring_type);

		int iconRes = R.drawable.ic_action_arrow_down;
		boolean nightMode = app.getDaynightHelper().isNightMode(settings.getApplicationMode(), ThemeUsageContext.APP);
		AppCompatImageView selectorIndicator = view.findViewById(R.id.select_coloring_indicator);
		selectorIndicator.setImageDrawable(app.getUIUtilities().getIcon(iconRes, nightMode));
		selectorIndicator.setOnClickListener(this::showColorTypeMenu);

		TextView customColorDescription = view.findViewById(R.id.custom_color_description);
		customColorDescription.setText(String.format(Locale.getDefault(), app.getString(R.string.buildings_3d_use_map_style_color_description), "OsmAnd"));

		customColorViewContainer = view.findViewById(R.id.custom_color_selection);
		mapStyleColorViewContainer = view.findViewById(R.id.map_style_color_selection);

		TextToggleButton.TextRadioItem day = new TextToggleButton.TextRadioItem(app.getString(R.string.daynight_mode_day));
		day.setOnClickListener((radioItem, v) -> {
			isDayModeColorSelection = true;
			getColorController().selectColor(dayColor);
			colorsPaletteCard.updatePaletteColors(getColorController().getSelectedColor());
			updateDayNightSelection();
			return true;
		});

		TextToggleButton.TextRadioItem night = new TextToggleButton.TextRadioItem(app.getString(R.string.daynight_mode_night));
		night.setOnClickListener((radioItem, v) -> {
			isDayModeColorSelection = false;
			getColorController().selectColor(nightColor);
			colorsPaletteCard.updatePaletteColors(getColorController().getSelectedColor());
			updateDayNightSelection();
			return true;
		});

		LinearLayout container = customColorViewContainer.findViewById(R.id.custom_radio_buttons);
		dayNightToggleButton = new TextToggleButton(app, container, nightMode);
		dayNightToggleButton.setItems(day, night);
		dayNightToggleButton.setSelectedItem(day);

		updateDayNightSelection();
		updateColorType();
	}

	private void updateDayNightSelection() {
		getColorController().selectColor(isDayModeColorSelection ? dayColor : nightColor);
		if (colorsPaletteCard != null) {
			colorsPaletteCard.updatePaletteColors(getColorController().getSelectedColor());
		}
		dayNightToggleButton.setSelectedItem(isDayModeColorSelection ? 0 : 1);
	}

	private void showColorTypeMenu(View v) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			PopupMenu popup = new PopupMenu(mapActivity, v);
			List<Buildings3DColorType> types = Buildings3DColorType.getEntries();

			for (int i = 0; i < types.size(); i++) {
				popup.getMenu().add(Menu.NONE, i, Menu.NONE, types.get(i).getLabelId());
			}

			popup.setOnMenuItemClickListener(item -> {
				colorType = types.get(item.getItemId());
				updateColorType();
				updateApplyButton(isChangesMade());
				return true;
			});

			popup.show();
		}
	}

	private void updateColorType() {
		colorTypeTv.setText(colorType.getLabelId());
		AndroidUiHelper.updateVisibility(customColorViewContainer, colorType == Buildings3DColorType.CUSTOM);
		AndroidUiHelper.updateVisibility(mapStyleColorViewContainer, colorType != Buildings3DColorType.CUSTOM);
	}

	@NonNull
	private ColorsPaletteController getColorController() {
		return EditorColorController.getInstance(app, this, getColor());
	}

	@ColorInt
	public int getColor() {
		return isDayModeColorSelection ? dayColor : nightColor;
	}
}