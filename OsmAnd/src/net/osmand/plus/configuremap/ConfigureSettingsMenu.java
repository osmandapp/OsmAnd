package net.osmand.plus.configuremap;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.preference.*;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import net.osmand.CallbackWithObject;
import net.osmand.access.AccessibleToast;
import net.osmand.data.AmenityType;
import net.osmand.plus.*;
import net.osmand.plus.activities.*;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.render.RenderingIcons;
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
public class ConfigureSettingsMenu {

	public static final int BACK_HEADER = 0;
	public static final int HEADER = 1;
	public static final int LAYER = 2;
	public static final int MAP_REDNDER = 3;
	public static final int RENDERING_PROPERTY = 4;


	private ListView listView;
	private OsmandApplication app;
	List<ConfigureMapMenuItem> items = new ArrayList<ConfigureMapMenuItem>();

	public class ConfigureMapMenuItem {
		int nameId;
		int type;
		int darkIcon = -1;
		int lightIcon = -1;
		Object preference;


		ConfigureMapMenuItem(int type, int name, int darkIcon, int whiteIcon, Object preference) {
			this.type = type;
			this.nameId = name;
			this.darkIcon = darkIcon;
			this.lightIcon = whiteIcon;
			this.preference = preference;
		}
	}

	public ConfigureSettingsMenu(OsmandApplication app) {
		this.app = app;
	}

	public void setListView(ListView list) {
		this.listView = list;
		listView.setAdapter(createSettingsAdapter());
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
				onMenuItemClick(items.get(pos), (CheckBox) view.findViewById(R.id.check));

			}
		});
	}

	//checkBox should be set only if u have checkBox preference
	private void onMenuItemClick(ConfigureMapMenuItem item, CheckBox ch) {
		if (item.type == LAYER) {
			if (ch != null){
				ch.setChecked(!ch.isChecked());
			}
			if (item.nameId == R.string.layer_poi) {
				final OsmandSettings.OsmandPreference<Boolean> pref = (OsmandSettings.OsmandPreference<Boolean>) item.preference;
				boolean value = !pref.get();
				if (value) {
					selectPOIFilterLayer(null);
				}
				} else {
					showGPXFileDialog(getAlreadySelectedGpx());
				}
			} else {
				final OsmandSettings.OsmandPreference<Boolean> pref = (OsmandSettings.OsmandPreference<Boolean>) item.preference;
				pref.set(!pref.get());
		} else if (item.type == MAP_REDNDER) {
			if (item.nameId == R.string.map_widget_renderer) {
				AlertDialog.Builder bld = new AlertDialog.Builder(app.getMapActivity());
				bld.setTitle(R.string.renderers);
				Collection<String> rendererNames = app.getRendererRegistry().getRendererNames();
				final String[] items = rendererNames.toArray(new String[rendererNames.size()]);
				final String[] visibleNames = new String[items.length];
				int selected = -1;
				final String selectedName = app.getRendererRegistry().getCurrentSelectedRenderer().getName();
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
						RenderingRulesStorage loaded = app.getRendererRegistry().getRenderer(renderer);
						if (loaded != null) {
							app.getSettings().RENDERER.set(renderer);
							app.getRendererRegistry().setCurrentSelectedRender(loaded);
							app.getResourceManager().getRenderer().clearCache();
							listView.setAdapter(createSettingsAdapter());
						} else {
							AccessibleToast.makeText(app, R.string.renderer_load_exception, Toast.LENGTH_SHORT).show();
						}
						dialog.dismiss();
										createSettingsAdapter();
					}
				});
				bld.show();
			} else if (item.nameId == R.string.map_widget_day_night) {
				AlertDialog.Builder bld = new AlertDialog.Builder(app.getMapActivity());
				bld.setTitle(R.string.daynight);
				final String[] items = new String[OsmandSettings.DayNightMode.values().length];
				for (int i = 0; i < items.length; i++) {
					items[i] = OsmandSettings.DayNightMode.values()[i].toHumanString(app);
				}
				int i = app.getSettings().DAYNIGHT_MODE.get().ordinal();
				bld.setSingleChoiceItems(items, i, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						app.getSettings().DAYNIGHT_MODE.set(OsmandSettings.DayNightMode.values()[which]);
						app.getResourceManager().getRenderer().clearCache();
						dialog.dismiss();
					}
				});
				bld.show();
			}
		} else if (item.type == RENDERING_PROPERTY) {
			if (ch != null){
				ch.setChecked(!ch.isChecked());
			}
			final RenderingRuleProperty p = (RenderingRuleProperty) item.preference;
			final String propertyDescription = SettingsActivity.getStringPropertyDescription(app, p.getAttrName(), p.getName());
			if (p.isBoolean()) {
				final OsmandSettings.CommonPreference<Boolean> pref = app.getSettings().getCustomRenderBooleanProperty(p.getAttrName());
				pref.set(!pref.get());
				app.getResourceManager().getRenderer().clearCache();
			} else {
				final OsmandSettings.CommonPreference<String> pref = app.getSettings().getCustomRenderProperty(p.getAttrName());
				AlertDialog.Builder b = new AlertDialog.Builder(app.getMapActivity());
				//test old descr as title
				b.setTitle(propertyDescription);

				int i = Arrays.asList(p.getPossibleValues()).indexOf(pref.get());

				String[] possibleValuesString = new String[p.getPossibleValues().length];

				for (int j = 0; j < p.getPossibleValues().length; j++) {
					possibleValuesString[j] = SettingsActivity.getStringPropertyValue(app, p.getPossibleValues()[j]);
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

			}
		}
		app.getMapActivity().getMapLayers().updateLayers(app.getMapActivity().getMapView());
		app.getMapActivity().getMapView().refreshMap();
	}

	private ArrayAdapter<ConfigureMapMenuItem> createSettingsAdapter() {
		items.clear();
		items.add(new ConfigureMapMenuItem(BACK_HEADER, R.string.configure_map, R.drawable.ic_back_drawer_dark, R.drawable.ic_back_drawer_white, null));
		createLayersItems(items);
		createRenderingAttributeItems(items);
		return new ArrayAdapter<ConfigureMapMenuItem>(app, R.layout.map_settings_item, items) {
			@Override
			public View getView(int position,View convertView, ViewGroup parent) {
				if (convertView == null) {
					convertView = app.getMapActivity().getLayoutInflater().inflate(R.layout.map_settings_item, null);
				}
				final ConfigureMapMenuItem item = getItem(position);
				prepareView(convertView, item);
				if (item.type == BACK_HEADER) {
					((TextView) convertView.findViewById(R.id.name)).setText(item.nameId);
					ImageButton button = (ImageButton) convertView.findViewById(R.id.back);
					button.setImageResource(getIcon(item));
					button.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							app.getMapActivity().getMapActions().createOptionsMenuAsDrawer(false);
						}
					});
				} else if (item.type == HEADER) {
					((TextView) convertView.findViewById(R.id.name)).setText((String) item.preference);
				} else if (item.type == LAYER) {
					((TextView) convertView.findViewById(R.id.name)).setText(item.nameId);
					final CheckBox ch = (CheckBox) convertView.findViewById(R.id.check);
					ch.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							onMenuItemClick(item, null);
						}
					});
					if (item.nameId == R.string.layer_gpx_layer){
						ch.setChecked(app.getSelectedGpxHelper().isShowingAnyGpxFiles());
					} else {
						OsmandSettings.OsmandPreference<Boolean> pref = (OsmandSettings.OsmandPreference<Boolean>) item.preference;
						ch.setChecked(pref.get());
					}
				} else if (item.type == MAP_REDNDER) {
					((TextView) convertView.findViewById(R.id.name)).setText(item.nameId);
					if (item.nameId == R.string.map_widget_renderer) {
						((TextView) convertView.findViewById(R.id.descr)).setText(app.getSettings().RENDERER.get());
					} else if (item.nameId == R.string.map_widget_day_night) {
						((TextView) convertView.findViewById(R.id.descr)).setText(app.getSettings().DAYNIGHT_MODE.get().toHumanString(app));
					}
				} else if (item.type == RENDERING_PROPERTY) {
					convertView.findViewById(R.id.icon).setVisibility(View.GONE);
					final RenderingRuleProperty p = (RenderingRuleProperty) item.preference;
					String propertyName = SettingsActivity.getStringPropertyName(app, p.getAttrName(), p.getName());
					TextView header = (TextView) convertView.findViewById(R.id.name);
					header.setText(propertyName);
					header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
					convertView.findViewById(R.id.icon).setVisibility(View.GONE);
					final String propertyDescription = SettingsActivity.getStringPropertyDescription(app, p.getAttrName(), p.getName());
					if (p.isBoolean()) {
						OsmandSettings.CommonPreference<Boolean> pref = app.getSettings().getCustomRenderBooleanProperty(p.getAttrName());
						final CheckBox ch = (CheckBox) convertView.findViewById(R.id.check);
						ch.setChecked(pref.get());
						ch.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								onMenuItemClick(item, null);
							}
						});
					}
				}
				return convertView;
			}

			//Hiding and showing items based on current item
			//setting proper visual property
			private void prepareView(View convertView, ConfigureMapMenuItem item) {
				((TextView) convertView.findViewById(R.id.descr)).setTypeface(null, Typeface.ITALIC);

				int type = item.type;
				//setting name textview
				if (type == BACK_HEADER) {
					TextView header = (TextView) convertView.findViewById(R.id.name);
					header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
					header.setTypeface(Typeface.DEFAULT_BOLD);
				} else if (type == HEADER) {
					TextView header = (TextView) convertView.findViewById(R.id.name);
					header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
					header.setTypeface(Typeface.DEFAULT_BOLD);
				} else {
					TextView header = ((TextView) convertView.findViewById(R.id.name));
					header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
					header.setTypeface(Typeface.DEFAULT);

				}

				//setting backbutton
				if (type == BACK_HEADER) {
					convertView.findViewById(R.id.back).setVisibility(View.VISIBLE);
				} else {
					convertView.findViewById(R.id.back).setVisibility(View.GONE);
				}
				//other elements
				if (type == BACK_HEADER) {
					convertView.findViewById(R.id.check).setVisibility(View.GONE);
					convertView.findViewById(R.id.descr).setVisibility(View.GONE);
					convertView.findViewById(R.id.icon).setVisibility(View.GONE);
				} else if (type == HEADER) {
					convertView.findViewById(R.id.icon).setVisibility(View.GONE);
					convertView.findViewById(R.id.check).setVisibility(View.GONE);
					convertView.findViewById(R.id.descr).setVisibility(View.GONE);
				} else if (type == LAYER) {
					((ImageView) convertView.findViewById(R.id.icon)).setImageResource(getIcon(item));
					convertView.findViewById(R.id.icon).setVisibility(View.VISIBLE);
					convertView.findViewById(R.id.check).setVisibility(View.VISIBLE);
				} else if (type == MAP_REDNDER) {
					convertView.findViewById(R.id.icon).setVisibility(View.GONE);
					convertView.findViewById(R.id.check).setVisibility(View.GONE);
					convertView.findViewById(R.id.descr).setVisibility(View.VISIBLE);
				} else if (type == RENDERING_PROPERTY) {
					final RenderingRuleProperty p = (RenderingRuleProperty) item.preference;
					if (p.isBoolean()) {
						convertView.findViewById(R.id.check).setVisibility(View.VISIBLE);
					} else {
						convertView.findViewById(R.id.check).setVisibility(View.GONE);
					}

				}
			}

			private int getIcon(ConfigureMapMenuItem item) {
				if (app.getSettings().isLightContent()) {
					return item.lightIcon;
				} else {
					return item.darkIcon;
				}
			}

		};
	}

	private void createLayersItems(List<ConfigureMapMenuItem> items) {
		items.add(new ConfigureMapMenuItem(HEADER, -1, -1, -1, "Show:"));
		items.add(new ConfigureMapMenuItem(LAYER, R.string.layer_poi, R.drawable.ic_action_info_dark,
				R.drawable.ic_action_info_light, app.getSettings().SHOW_POI_OVER_MAP));
		items.add(new ConfigureMapMenuItem(LAYER, R.string.layer_amenity_label, R.drawable.ic_action_text_dark,
				R.drawable.ic_action_text_light, app.getSettings().SHOW_POI_LABEL));
		items.add(new ConfigureMapMenuItem(LAYER, R.string.layer_favorites, R.drawable.ic_action_fav_dark,
				R.drawable.ic_action_fav_light, app.getSettings().SHOW_FAVORITES));
		items.add(new ConfigureMapMenuItem(LAYER, R.string.layer_gpx_layer, R.drawable.ic_action_polygom_dark,
				R.drawable.ic_action_polygom_light, null));
		items.add(new ConfigureMapMenuItem(LAYER, R.string.layer_transport, R.drawable.ic_action_bus_dark,
				R.drawable.ic_action_bus_light, app.getSettings().SHOW_TRANSPORT_OVER_MAP));
		if (TransportRouteHelper.getInstance().routeIsCalculated()) {
			items.add(new ConfigureMapMenuItem(R.string.layer_transport, LAYER,
					R.drawable.ic_action_bus_dark, R.drawable.ic_action_bus_light, 1));
		}
	}

	private void createRenderingAttributeItems(List<ConfigureMapMenuItem> items) {
		items.add(new ConfigureMapMenuItem(HEADER, -1, -1, -1, app.getString(R.string.map_widget_map_rendering)));
		items.add(new ConfigureMapMenuItem(MAP_REDNDER, R.string.map_widget_renderer, -1, -1, null));
		items.add(new ConfigureMapMenuItem(MAP_REDNDER, R.string.map_widget_day_night, -1, -1, null));

		RenderingRulesStorage renderer = app.getRendererRegistry().getCurrentSelectedRenderer();
		if (renderer != null && AppearanceWidgetsFactory.EXTRA_SETTINGS) {
			createMapRenderingPreferences(items);
		}
	}

	private void createMapRenderingPreferences(List<ConfigureMapMenuItem> items) {
		items.add(new ConfigureMapMenuItem(HEADER, -1, -1, -1, app.getString(R.string.map_widget_vector_attributes)));
		RenderingRulesStorage renderer = app.getRendererRegistry().getCurrentSelectedRenderer();
		List<RenderingRuleProperty> customRules = renderer.PROPS.getCustomRules();
		for (RenderingRuleProperty p : customRules) {
			items.add(new ConfigureMapMenuItem(RENDERING_PROPERTY, -1, -1, -1, p));
		}
	}

	public AlertDialog selectPOIFilterLayer(final PoiFilter[] selected) {
		final List<PoiFilter> userDefined = new ArrayList<PoiFilter>();
		final PoiFiltersHelper poiFilters = app.getPoiFilters();
		final ContextMenuAdapter adapter = new ContextMenuAdapter(app.getMapActivity());

		ContextMenuAdapter.Item is = adapter.item(app.getString(R.string.any_poi));
		if (RenderingIcons.containsBigIcon("null")) {
			is.icon(RenderingIcons.getBigIconResourceId("null"));
		}
		is.reg();
		// 2nd custom
		adapter.item(app.getString(R.string.poi_filter_custom_filter)).icon(RenderingIcons.getBigIconResourceId("user_defined")).reg();

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
			ContextMenuAdapter.Item it = adapter.item(OsmAndFormatter.toPublicString(t, app));
			if (RenderingIcons.containsBigIcon(t.toString().toLowerCase())) {
				it.icon(RenderingIcons.getBigIconResourceId(t.toString().toLowerCase()));
			}
			it.reg();
		}
		final AlertDialog.Builder builder = new AlertDialog.Builder(app.getMapActivity());
		final ListAdapter listAdapter;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			listAdapter =
					adapter.createListAdapter(app.getMapActivity(), R.layout.list_menu_item, app.getSettings().isLightContentMenu());
		} else {
			listAdapter =
					adapter.createListAdapter(app.getMapActivity(), R.layout.list_menu_item_native, app.getSettings().isLightContentMenu());
		}
		builder.setAdapter(listAdapter, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == 1) {
					String filterId = PoiFilter.CUSTOM_FILTER_ID;
					app.getSettings().setPoiFilterForMap(filterId);
					Intent newIntent = new Intent(app, EditPOIFilterActivity.class);
					newIntent.putExtra(EditPOIFilterActivity.AMENITY_FILTER, filterId);
					newIntent.putExtra(EditPOIFilterActivity.SEARCH_LAT, app.getMapActivity().getMapView().getLatitude());
					newIntent.putExtra(EditPOIFilterActivity.SEARCH_LON, app.getMapActivity().getMapView().getLongitude());
					app.getMapActivity().startActivity(newIntent);
				} else {
					String filterId;
					if (which == 0) {
						filterId = PoiFiltersHelper.getOsmDefinedFilterId(null);
					} else if (which <= userDefined.size() + 1) {
						filterId = userDefined.get(which - 2).getFilterId();
					} else {
						filterId = PoiFiltersHelper.getOsmDefinedFilterId(categories[which - userDefined.size() - 2]);
					}
					app.getSettings().setPoiFilterForMap(filterId);
					PoiFilter f = poiFilters.getFilterById(filterId);
					if (f != null) {
						f.clearNameFilter();
					}
					app.getMapActivity().getMapLayers().setPoiFilter(f);
					app.getMapActivity().getMapView().refreshMap();
					if (selected != null && selected.length > 0) {
						selected[0] = f;
					}
					listView.setAdapter(createSettingsAdapter());
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
					app.getSettings().SHOW_POI_OVER_MAP.set(false);
					listView.setAdapter(createSettingsAdapter());
					return true;
				}
				return false;
			}
		});
		return builder.show();
	}

	public void showGPXFileDialog(List<String> files) {
		CallbackWithObject<GPXUtilities.GPXFile[]> callbackWithObject = new CallbackWithObject<GPXUtilities.GPXFile[]>() {
			@Override
			public boolean processResult(GPXUtilities.GPXFile[] result) {
				GPXUtilities.WptPt locToShow = null;
				for (GPXUtilities.GPXFile g : result) {
					if (g.showCurrentTrack) {
						if (!app.getSettings().SAVE_TRACK_TO_GPX.get() && !
								app.getSettings().SAVE_GLOBAL_TRACK_TO_GPX.get()) {
							AccessibleToast.makeText(app.getMapActivity(), R.string.gpx_monitoring_disabled_warn, Toast.LENGTH_SHORT).show();
						}
						break;
					}
					if (!g.showCurrentTrack || locToShow == null) {
						locToShow = g.findPointToShow();
					}
				}
				app.getSelectedGpxHelper().setGpxFileToDisplay(result);
				listView.setAdapter(createSettingsAdapter());
				return true;
			}
		};

		AlertDialog dialog;
		if (files == null) {
			dialog = GpxUiHelper.selectGPXFile(app.getMapActivity(), true, true, callbackWithObject);
		} else {
			dialog = GpxUiHelper.selectGPXFile(files, app.getMapActivity(), true, true, callbackWithObject);
		}
		if (dialog != null) {
			dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
				@Override
				public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent event) {
					if (i == KeyEvent.KEYCODE_BACK &&
							event.getAction() == KeyEvent.ACTION_UP &&
							!event.isCanceled()) {
						dialogInterface.cancel();
						listView.setAdapter(createSettingsAdapter());
						return true;
					}
					return false;
				}
			});
		}
	}

	private List<String> getAlreadySelectedGpx() {
		GpxSelectionHelper gpxSelectionHelper = app.getSelectedGpxHelper();
		if (gpxSelectionHelper == null) {
			return null;
		}
		List<GpxSelectionHelper.SelectedGpxFile> selectedGpxFiles = gpxSelectionHelper.getSelectedGPXFiles();
		List<String> files = new ArrayList<String>();
		for (GpxSelectionHelper.SelectedGpxFile file : selectedGpxFiles) {
			files.add(file.getGpxFile().path);
		}
		return files;
	}

}
