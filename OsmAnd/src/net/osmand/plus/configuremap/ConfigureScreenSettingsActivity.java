package net.osmand.plus.configuremap;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.*;
import android.view.KeyEvent;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.Toast;
import net.osmand.access.AccessibleToast;
import net.osmand.data.AmenityType;
import net.osmand.plus.*;
import net.osmand.plus.activities.*;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.AppearanceWidgetsFactory;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRulesStorage;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created by Denis on 14.10.2014.
 */
public class ConfigureScreenSettingsActivity extends SettingsBaseActivity {

	private String ADDITIONAL_VECTOR_RENDERING_CATEGORY;
	private ApplicationMode previousMode;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		for (ApplicationMode a : ApplicationMode.values(settings)) {
			modes.add(a);
		}
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		List<String> s = new ArrayList<String>();
		for (ApplicationMode a : modes) {
			s.add(a.toHumanString(getMyApplication()));
		}

		ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(getSupportActionBar().getThemedContext(),
				R.layout.sherlock_spinner_item, s);
		spinnerAdapter.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
		getSupportActionBar().setListNavigationCallbacks(spinnerAdapter, new com.actionbarsherlock.app.ActionBar.OnNavigationListener() {

			@Override
			public boolean onNavigationItemSelected(int itemPosition, long itemId) {
				settings.APPLICATION_MODE.set(modes.get(itemPosition));
				createAllCategories();
				return true;
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		previousMode = settings.getApplicationMode();
		boolean found = setSelectedAppMode(previousMode);
		if (!found) {
			getSupportActionBar().setSelectedNavigationItem(0);
		}
		createAllCategories();
	}


	@Override
	protected void onPause() {
		super.onPause();
		settings.APPLICATION_MODE.set(previousMode);
	}

	private void createAllCategories() {
		getPreferenceScreen().removeAll();
		createLayersPreferences();
		createRenderingAttributePreferences();

	}

	private void createRenderingAttributePreferences() {
		PreferenceCategory renderingCategory = new PreferenceCategory(this);
		renderingCategory.setTitle(R.string.map_widget_map_rendering);
		PreferenceScreen grp = getPreferenceScreen();
		grp.addPreference(renderingCategory);
		Preference p = new Preference(this);
		p.setTitle(R.string.map_widget_renderer);
		p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				AlertDialog.Builder bld = new AlertDialog.Builder(getActivity());
				bld.setTitle(R.string.renderers);
				Collection<String> rendererNames = getMyApplication().getRendererRegistry().getRendererNames();
				final String[] items = rendererNames.toArray(new String[rendererNames.size()]);
				final String[] visibleNames = new String[items.length];
				int selected = -1;
				final String selectedName = getMyApplication().getRendererRegistry().getCurrentSelectedRenderer().getName();
				for (int j = 0; j < items.length; j++) {
					if (items[j].equals(selectedName)) {
						selected = j;
					}
					visibleNames[j] = items[j].replace('_', ' ').replace(
							'-', ' ');
				}
				bld.setSingleChoiceItems(visibleNames, selected, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						String renderer = items[which];
						RenderingRulesStorage loaded = getMyApplication().getRendererRegistry().getRenderer(renderer);
						if (loaded != null) {
							getMyApplication().getSettings().RENDERER.set(renderer);
							getMyApplication().getRendererRegistry().setCurrentSelectedRender(loaded);
							getMyApplication().getResourceManager().getRenderer().clearCache();
						} else {
							AccessibleToast.makeText(getMyApplication(), R.string.renderer_load_exception, Toast.LENGTH_SHORT).show();
						}
						dialog.dismiss();
						createAllCategories();
					}
				});
				bld.show();
				return true;
			}
		});
		grp.addPreference(p);

		p = new Preference(this);
		p.setTitle(R.string.map_widget_day_night);
		p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				AlertDialog.Builder bld = new AlertDialog.Builder(getActivity());
				bld.setTitle(R.string.daynight);
				final String[] items = new String[OsmandSettings.DayNightMode.values().length];
				for (int i = 0; i < items.length; i++) {
					items[i] = OsmandSettings.DayNightMode.values()[i].toHumanString(getMyApplication());
				}
				int i = getMyApplication().getSettings().DAYNIGHT_MODE.get().ordinal();
				bld.setSingleChoiceItems(items,  i, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						getMyApplication().getSettings().DAYNIGHT_MODE.set(OsmandSettings.DayNightMode.values()[which]);
						getMyApplication().getResourceManager().getRenderer().clearCache();
						dialog.dismiss();
					}
				});
				bld.show();
				return true;
			}
		});
		grp.addPreference(p);

		RenderingRulesStorage renderer = getMyApplication().getRendererRegistry().getCurrentSelectedRenderer();
		if(renderer != null && AppearanceWidgetsFactory.EXTRA_SETTINGS) {
			createMapRenderingPreferences();
		}
	}


	private void createMapRenderingPreferences() {
		PreferenceCategory renderingCategory = new PreferenceCategory(this);
		renderingCategory.setTitle(R.string.map_widget_vector_attributes);
		PreferenceScreen grp = getPreferenceScreen();
		grp.addPreference(renderingCategory);

		RenderingRulesStorage renderer = getMyApplication().getRendererRegistry().getCurrentSelectedRenderer();
		final OsmandApplication app = getMyApplication();
		List<RenderingRuleProperty> customRules = renderer.PROPS.getCustomRules();
		for (final RenderingRuleProperty p : customRules) {
			String propertyName = SettingsActivity.getStringPropertyName(getActivity(), p.getAttrName(), p.getName());
			//test old descr as title
			final String propertyDescr = SettingsActivity.getStringPropertyDescription(getActivity(), p.getAttrName(), p.getName());
			if(p.isBoolean()) {
				final OsmandSettings.CommonPreference<Boolean> pref = getMyApplication().getSettings().getCustomRenderBooleanProperty(p.getAttrName());
				CheckBoxPreference preference = createCheckBoxPreference(pref);
				preference.setTitle(propertyName);
				renderingCategory.addPreference(preference);
				preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						pref.set(!pref.get());
						app.getResourceManager().getRenderer().clearCache();
						return true;
					}
				});

			} else {
				final OsmandSettings.CommonPreference<String> pref = getMyApplication().getSettings().getCustomRenderProperty(p.getAttrName());
				Preference preference = new Preference(getActivity());
				preference.setTitle(propertyName);
				renderingCategory.addPreference(preference);
				preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
						//test old descr as title
						b.setTitle(propertyDescr);

						int i = Arrays.asList(p.getPossibleValues()).indexOf(pref.get());

						String[] possibleValuesString = new String[p.getPossibleValues().length];

						for (int j = 0; j < p.getPossibleValues().length; j++) {
							possibleValuesString[j] = SettingsActivity.getStringPropertyValue(getApplication(), p.getPossibleValues()[j]);
						}

						b.setSingleChoiceItems(possibleValuesString, i, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								pref.set(p.getPossibleValues()[which]);
								app.getResourceManager().getRenderer().clearCache();
								dialog.dismiss();
							}
						});
						b.show();
						return true;
					}
				});

			}
		}
	}


	private void createLayersPreferences() {
		PreferenceCategory mapLayersCategory = new PreferenceCategory(this);
		mapLayersCategory.setTitle(R.string.menu_layers);
		PreferenceScreen grp = getPreferenceScreen();
		grp.addPreference(mapLayersCategory);
		final CheckBoxPreference poi = new CheckBoxPreference(this);
		poi.setTitle(R.string.layer_poi);
		poi.setChecked(settings.SHOW_POI_OVER_MAP.get());
		addIcon(poi, R.drawable.ic_action_info_light, R.drawable.ic_action_info_dark);
		poi.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				settings.SHOW_POI_OVER_MAP.set(!settings.SHOW_POI_OVER_MAP.get());
				if (settings.SHOW_POI_OVER_MAP.get()) {
					selectPOIFilterLayer(null);
				} else {
					poi.setSummary("");
				}
				return true;
			}
		});
		if (settings.SHOW_POI_OVER_MAP.get()) {
			poi.setSummary(settings.getPoiFilterForMap());
		}
		mapLayersCategory.addPreference(poi);
		CheckBoxPreference p = createCheckBoxPreference(settings.SHOW_POI_LABEL, R.string.layer_amenity_label,
				R.drawable.ic_action_text_dark, R.drawable.ic_action_text_light);
		mapLayersCategory.addPreference(p);

		p = createCheckBoxPreference(settings.SHOW_FAVORITES, R.string.layer_favorites,
				R.drawable.ic_action_fav_dark, R.drawable.ic_action_fav_light);
		mapLayersCategory.addPreference(p);
		p = createCheckBoxPreference(settings.SHOW_POI_LABEL, R.string.layer_gpx_layer,
				R.drawable.ic_action_polygom_dark, R.drawable.ic_action_polygom_light);
		mapLayersCategory.addPreference(p);
		p = createCheckBoxPreference(settings.SHOW_TRANSPORT_OVER_MAP, R.string.layer_transport,
				R.drawable.ic_action_bus_dark, R.drawable.ic_action_bus_light);
		mapLayersCategory.addPreference(p);
		if (TransportRouteHelper.getInstance().routeIsCalculated()) {
			p = new CheckBoxPreference(this);
			p.setChecked(true);
			addIcon(p, R.drawable.ic_action_bus_light, R.drawable.ic_action_bus_dark);
			mapLayersCategory.addPreference(p);
		}
	}

	public void addIcon(CheckBoxPreference p, int lightIcon, int darkIcon) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			if (getMyApplication().getSettings().isLightContent()) {
				p.setIcon(lightIcon);
			} else {
				p.setIcon(darkIcon);
			}
		}
	}

	public AlertDialog selectPOIFilterLayer(final PoiFilter[] selected) {
		final List<PoiFilter> userDefined = new ArrayList<PoiFilter>();
		OsmandApplication app = (OsmandApplication) getApplication();
		final PoiFiltersHelper poiFilters = app.getPoiFilters();
		final ContextMenuAdapter adapter = new ContextMenuAdapter(this);

		ContextMenuAdapter.Item is = adapter.item(getString(R.string.any_poi));
		if (RenderingIcons.containsBigIcon("null")) {
			is.icon(RenderingIcons.getBigIconResourceId("null"));
		}
		is.reg();
		// 2nd custom
		adapter.item(getString(R.string.poi_filter_custom_filter)).icon(RenderingIcons.getBigIconResourceId("user_defined")).reg();

		for (PoiFilter f : poiFilters.getUserDefinedPoiFilters()) {
			ContextMenuAdapter.Item it = adapter.item(f.getName());
			if (RenderingIcons.containsBigIcon(f.getSimplifiedId())) {
				it.icon(RenderingIcons.getBigIconResourceId(f.getSimplifiedId()));
			} else {
				it.icon(RenderingIcons.getBigIconResourceId("user_defined"));
			}
			it.reg();
			userDefined.add(f);
		}
		final AmenityType[] categories = AmenityType.getCategories();
		for (AmenityType t : categories) {
			ContextMenuAdapter.Item it = adapter.item(OsmAndFormatter.toPublicString(t, getMyApplication()));
			if (RenderingIcons.containsBigIcon(t.toString().toLowerCase())) {
				it.icon(RenderingIcons.getBigIconResourceId(t.toString().toLowerCase()));
			}
			it.reg();
		}
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		ListAdapter listAdapter;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			listAdapter =
					adapter.createListAdapter(this, R.layout.list_menu_item, app.getSettings().isLightContentMenu());
		} else {
			listAdapter =
					adapter.createListAdapter(this, R.layout.list_menu_item_native, app.getSettings().isLightContentMenu());
		}
		builder.setAdapter(listAdapter, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == 1) {
					String filterId = PoiFilter.CUSTOM_FILTER_ID;
					getMyApplication().getSettings().setPoiFilterForMap(filterId);
					Intent newIntent = new Intent(getApplication(), EditPOIFilterActivity.class);
					newIntent.putExtra(EditPOIFilterActivity.AMENITY_FILTER, filterId);
					//newIntent.putExtra(EditPOIFilterActivity.SEARCH_LAT, getMyApplication().getMapActivity().getMapView().getLatitude());
					//newIntent.putExtra(EditPOIFilterActivity.SEARCH_LON, getMyApplication().getMapActivity().getMapView().getLongitude());
					getActivity().startActivity(newIntent);
				} else {
					String filterId;
					if (which == 0) {
						filterId = PoiFiltersHelper.getOsmDefinedFilterId(null);
					} else if (which <= userDefined.size() + 1) {
						filterId = userDefined.get(which - 2).getFilterId();
					} else {
						filterId = PoiFiltersHelper.getOsmDefinedFilterId(categories[which - userDefined.size() - 2]);
					}
					getMyApplication().getSettings().setPoiFilterForMap(filterId);
					PoiFilter f = poiFilters.getFilterById(filterId);
					if (f != null) {
						f.clearNameFilter();
					}
					if (selected != null && selected.length > 0) {
						selected[0] = f;
					}
					createAllCategories();
				}
			}

		});
		builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
			@Override
			public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent event) {
				if (i == KeyEvent.KEYCODE_BACK &&
						event.getAction() == KeyEvent.ACTION_UP &&
						!event.isCanceled()) {
					dialogInterface.cancel();
					settings.SHOW_POI_OVER_MAP.set(false);
					createAllCategories();
					return true;
				}
				return false;
			}
		});
		return builder.show();
	}

	private Activity getActivity() {
		return this;
	}

}
