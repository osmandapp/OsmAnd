package net.osmand.plus.dialogs;

import static net.osmand.plus.settings.fragments.BaseSettingsFragment.buildArguments;
import static net.osmand.plus.transport.TransportLinesMenu.RENDERING_CATEGORY_TRANSPORT;
import static net.osmand.render.RenderingRuleStorageProperties.UI_CATEGORY_DETAILS;
import static net.osmand.render.RenderingRuleStorageProperties.UI_CATEGORY_HIDDEN;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.google.common.collect.ImmutableList;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemTwoChoicesButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.configmap.ConfigureMapUtils;
import net.osmand.plus.configmap.ViewOfSettingHighlighter;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.bottomsheets.BasePreferenceBottomSheet;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.render.RenderingRuleProperty;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import de.KnollFrank.lib.settingssearch.client.searchDatabaseConfig.InitializePreferenceFragmentWithFragmentBeforeOnCreate;
import de.KnollFrank.lib.settingssearch.common.IndexSearchResultConverter;
import de.KnollFrank.lib.settingssearch.results.Setting;
import de.KnollFrank.lib.settingssearch.results.SettingHighlighter;
import de.KnollFrank.lib.settingssearch.results.SettingHighlighterProvider;

// FK-FIXME: search for "nautical depth", select it and press the close button, then the Details menu item should display 1/12 but it erroneously displays 0/12
public class DetailsBottomSheet extends BasePreferenceBottomSheet implements SettingHighlighterProvider {

	public static final String TAG = DetailsBottomSheet.class.getName();
	public static final String STREET_LIGHTING = "streetLighting";
	public static final String STREET_LIGHTING_NIGHT = "streetLightingNight";
	public static final String MORE_DETAILED = "moreDetailed";
	public static final String SHOW_SURFACE_GRADE = "showSurfaceGrade";
	public static final String COLORED_BUILDINGS = "coloredBuildings";

	private OsmandApplication app;
	private List<RenderingRuleProperty> properties;
	private List<RenderingRuleProperty> propertiesRepresentingMenuItems = List.of();
	private List<CommonPreference<Boolean>> preferences;
	private OnDataChangeUiAdapter uiAdapter;
	private ContextMenuItem item;
	private int padding;
	private int paddingSmall;
	private int paddingHalf;

	public static DetailsBottomSheet createInstance(final List<RenderingRuleProperty> properties,
													final List<CommonPreference<Boolean>> preferences,
													final OnDataChangeUiAdapter uiAdapter,
													final ContextMenuItem item) {
		final DetailsBottomSheet detailsBottomSheet = new DetailsBottomSheet();
		detailsBottomSheet.setProperties(properties);
		detailsBottomSheet.setPreferences(preferences);
		detailsBottomSheet.setUiAdapter(uiAdapter);
		detailsBottomSheet.setMenuItem(item);
		return detailsBottomSheet;
	}

	public void show(final FragmentManager fragmentManager) {
		showIfStateNotSaved(fragmentManager, this::show);
	}

	public void showNow(final FragmentManager fragmentManager) {
		showIfStateNotSaved(fragmentManager, this::showNow);
	}

	private void showIfStateNotSaved(final FragmentManager fragmentManager,
									 final BiConsumer<FragmentManager, String> show) {
		if (!fragmentManager.isStateSaved()) {
			show.accept(fragmentManager, TAG);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		padding = (int) getResources().getDimension(R.dimen.content_padding);
		paddingSmall = (int) getResources().getDimension(R.dimen.content_padding_small);
		paddingHalf = (int) getResources().getDimension(R.dimen.content_padding_half);
		app = requiredMyApplication();
		if (properties == null || preferences == null) {
			properties = new ArrayList<>();
			preferences = new ArrayList<>();
			List<RenderingRuleProperty> customRules = ConfigureMapUtils.getCustomRules(app,
					UI_CATEGORY_HIDDEN, RENDERING_CATEGORY_TRANSPORT);
			for (RenderingRuleProperty pr : customRules) {
				if (UI_CATEGORY_DETAILS.equals(pr.getCategory()) && pr.isBoolean()) {
					properties.add(pr);
					CommonPreference<Boolean> pref = app.getSettings()
							.getCustomRenderBooleanProperty(pr.getAttrName());
					preferences.add(pref);
				}
			}
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		int selectedProfileColor = app.getSettings().APPLICATION_MODE.get().getProfileColor(nightMode);
		float spacing = getResources().getDimension(R.dimen.line_spacing_extra_description);
		LinearLayout linearLayout = new LinearLayout(requireActivity());
		linearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		linearLayout.setOrientation(LinearLayout.VERTICAL);

		TextView title = new TextView(requireActivity());
		title.setPadding(padding, paddingHalf, padding, 0);
		title.setTypeface(FontCache.getMediumFont());
		title.setText(R.string.rendering_category_details);
		title.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.default_list_text_size));
		title.setTextColor(ColorUtilities.getPrimaryTextColor(requireActivity(), nightMode));

		TextView description = new TextView(requireActivity());
		description.setLineSpacing(spacing, 1.0f);
		description.setPadding(padding, 0, padding, paddingSmall);
		description.setText(R.string.details_dialog_decr);
		description.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.default_desc_text_size));
		description.setTextColor(ColorUtilities.getSecondaryTextColor(requireActivity(), nightMode));
		linearLayout.addView(title);
		linearLayout.addView(description);
		items.add(new BaseBottomSheetItem.Builder().setCustomView(linearLayout).create());
		if (preferences != null && properties != null) {
			final ImmutableList.Builder<RenderingRuleProperty> propertiesRepresentingMenuItemsBuilder = ImmutableList.builder();
			RenderingRuleProperty streetLightNightProp = getStreetLightNightProp();
			for (int i = 0; i < properties.size(); i++) {
				RenderingRuleProperty property = properties.get(i);
				CommonPreference<Boolean> pref = preferences.get(i);
				String propertyName = getRenderingStringPropertyName(property, requireActivity());
				if (STREET_LIGHTING.equals(property.getAttrName()) && streetLightNightProp != null) {
					CommonPreference<Boolean> streetLightsNightPref = preferences.get(properties.indexOf(streetLightNightProp));
					BottomSheetItemTwoChoicesButton[] item = new BottomSheetItemTwoChoicesButton[1];
					item[0] = (BottomSheetItemTwoChoicesButton) new BottomSheetItemTwoChoicesButton.Builder()
							.setLeftBtnSelected(!streetLightsNightPref.get())
							.setLeftBtnTitleRes(R.string.shared_string_always)
							.setRightBtnTitleRes(R.string.shared_string_night_map)
							.setOnBottomBtnClickListener(onLeftClick -> streetLightsNightPref.set(!onLeftClick))
							.setCompoundButtonColor(selectedProfileColor)
							.setChecked(pref.get())
							.setTitle(propertyName)
							.setIconHidden(true)
							.setLayoutId(R.layout.bottom_sheet_item_two_choices)
							.setOnClickListener(view -> {
								boolean checked = !pref.get();
								pref.set(checked);
								streetLightsNightPref.set(false);
								item[0].setChecked(checked);
								item[0].setIsLeftBtnSelected(true);
								setupHeightAndBackground(getView());
							})
							.setTag(property.getAttrName())
							.create();
					items.add(item[0]);
					propertiesRepresentingMenuItemsBuilder.add(property);
				} else if (!STREET_LIGHTING_NIGHT.equals(property.getAttrName())) {
					BottomSheetItemWithCompoundButton[] item = new BottomSheetItemWithCompoundButton[1];
					item[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
							.setCompoundButtonColor(selectedProfileColor)
							.setChecked(pref.get())
							.setTitle(propertyName)
							.setIconHidden(true)
							.setLayoutId(R.layout.bottom_sheet_item_with_switch)
							.setOnClickListener(view -> {
								boolean checked = !pref.get();
								pref.set(checked);
								item[0].setChecked(checked);
							})
							.setTag(property.getAttrName())
							.create();
					items.add(item[0]);
					propertiesRepresentingMenuItemsBuilder.add(property);
				}
				String attrName = property.getAttrName();
				if (MORE_DETAILED.equals(attrName) || SHOW_SURFACE_GRADE.equals(attrName)
						|| COLORED_BUILDINGS.equals(attrName) || STREET_LIGHTING.equals(attrName)) {
					DividerItem divider = new DividerItem(requireActivity());
					divider.setMargins(padding, 0, 0, 0);
					items.add(divider);
				}
			}
			propertiesRepresentingMenuItems = propertiesRepresentingMenuItemsBuilder.build();
		}
	}

	private static String getRenderingStringPropertyName(final RenderingRuleProperty property, final Context context) {
		return AndroidUtils.getRenderingStringPropertyName(context, property.getAttrName(), property.getName());
	}

	@Nullable
	private RenderingRuleProperty getStreetLightNightProp() {
		if (properties != null) {
			for (RenderingRuleProperty property : properties) {
				if (STREET_LIGHTING_NIGHT.equals(property.getAttrName())) {
					return property;
				}
			}
		}
		return null;
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		boolean checked = false;
		int selected = 0;
		for (int i = 0; i < preferences.size(); i++) {
			boolean active = preferences.get(i).get();
			checked |= active;
			if (active) {
				selected++;
			}
		}
		if (item != null) {
			item.setSelected(checked);
			item.setColor(app, checked ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
			item.setDescription(getString(
					R.string.ltr_or_rtl_combine_via_slash,
					String.valueOf(selected),
					String.valueOf(preferences.size())));
		}
		if (uiAdapter != null) {
			uiAdapter.onDataSetInvalidated();
		}
		Activity activity = getActivity();
		if (activity instanceof final MapActivity mapActivity) {
			mapActivity.refreshMapComplete();
			mapActivity.getMapLayers().updateLayers(mapActivity);
		}
		super.onDismiss(dialog);
	}

	public void setProperties(List<RenderingRuleProperty> properties) {
		this.properties = properties;
	}

	public void setPreferences(List<CommonPreference<Boolean>> preferences) {
		this.preferences = preferences;
	}

	public void setUiAdapter(OnDataChangeUiAdapter uiAdapter) {
		this.uiAdapter = uiAdapter;
	}

	public void setMenuItem(ContextMenuItem item) {
		this.item = item;
	}

	@Override
	public SettingHighlighter getSettingHighlighter() {
		return new ViewOfSettingHighlighter(
				this::getView,
				Duration.ofSeconds(1));
	}

	private View getView(final Setting setting) {
		return itemsContainer.getChildAt(getPositionOfSetting(setting).orElseThrow());
	}

	private OptionalInt getPositionOfSetting(final Setting setting) {
		return Optional
				.<View>ofNullable(itemsContainer.findViewWithTag(setting.getKey()))
				.map(view -> IndexSearchResultConverter.minusOneToEmpty(itemsContainer.indexOfChild(view)))
				.orElse(OptionalInt.empty());
	}

	public static class DetailsBottomSheetProxy extends PreferenceFragmentCompat implements InitializePreferenceFragmentWithFragmentBeforeOnCreate<DetailsBottomSheet> {

		private List<RenderingRuleProperty> properties;

		@Override
		public void initializePreferenceFragmentWithFragmentBeforeOnCreate(final DetailsBottomSheet detailsBottomSheet) {
			properties = detailsBottomSheet.propertiesRepresentingMenuItems;
			setArguments(buildArguments(detailsBottomSheet.app.getSettings().APPLICATION_MODE.get()));
		}

		@Override
		public void onCreatePreferences(@Nullable final Bundle savedInstanceState, @Nullable final String rootKey) {
			final Context context = getPreferenceManager().getContext();
			final PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);
			screen.setTitle("screen title");
			screen.setSummary("screen summary");
			DetailsBottomSheetProxy
					.asPreferences(properties, context)
					.forEach(screen::addPreference);
			setPreferenceScreen(screen);
		}

		private static List<Preference> asPreferences(final List<RenderingRuleProperty> properties, final Context context) {
			return properties
					.stream()
					.map(property -> asPreference(property, context))
					.collect(Collectors.toList());
		}

		private static Preference asPreference(final RenderingRuleProperty property, final Context context) {
			final Preference preference = new Preference(context);
			preference.setKey(property.getAttrName());
			preference.setTitle(getRenderingStringPropertyName(property, context));
			return preference;
		}
	}
}
