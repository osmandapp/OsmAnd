package net.osmand.plus.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import net.osmand.PlatformUtil;
import net.osmand.access.AccessibleToast;
import net.osmand.core.android.MapRendererContext;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.ContextMenuAdapter.OnRowItemClick;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.PluginActivity;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.activities.TransportRouteHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.views.GPXLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.RouteLayer;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRuleStorageProperties;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import gnu.trove.list.array.TIntArrayList;

public class ConfigureMapMenu {
	private static final Log LOG = PlatformUtil.getLog(ConfigureMapMenu.class);

	public interface OnClickListener {
		public void onClick(boolean result);
	}

	;

	private boolean allModes = false;

	public ContextMenuAdapter createListAdapter(final MapActivity ma) {
		ContextMenuAdapter adapter = new ContextMenuAdapter(ma, allModes);
		adapter.setDefaultLayoutId(R.layout.drawer_list_item);
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(R.string.app_modes_choose, ma)
				.setLayout(R.layout.mode_toggles).createItem());
		adapter.setChangeAppModeListener(new OnClickListener() {
			@Override
			public void onClick(boolean result) {
				allModes = true;
				ma.getDashboard().updateListAdapter(createListAdapter(ma));
			}
		});
		createLayersItems(adapter, ma);
		createRenderingAttributeItems(adapter, ma);

		return adapter;
	}

	private final class LayerMenuListener extends OnRowItemClick {
		private MapActivity ma;
		private ContextMenuAdapter cm;

		private LayerMenuListener(MapActivity ma, ContextMenuAdapter cm) {
			this.ma = ma;
			this.cm = cm;
		}

		private List<String> getAlreadySelectedGpx() {
			GpxSelectionHelper selectedGpxHelper = ma.getMyApplication().getSelectedGpxHelper();
			List<GpxSelectionHelper.SelectedGpxFile> selectedGpxFiles = selectedGpxHelper.getSelectedGPXFiles();
			List<String> files = new ArrayList<String>();
			for (GpxSelectionHelper.SelectedGpxFile file : selectedGpxFiles) {
				files.add(file.getGpxFile().path);
			}
			return files;
		}

		@Override
		public boolean onRowItemClick(ArrayAdapter<?> adapter, View view, int itemId, int pos) {
			if (itemId == R.string.layer_poi) {
				selectPOILayer(ma.getMyApplication().getSettings());
				return false;
			} else if (itemId == R.string.layer_gpx_layer && cm.getSelection(pos)) {
				ma.getMapLayers().showGPXFileLayer(getAlreadySelectedGpx(), ma.getMapView());
				return false;
			} else {
				return super.onRowItemClick(adapter, view, itemId, pos);
			}
		}

		@Override
		public boolean onContextMenuClick(final ArrayAdapter<?> adapter, int itemId, final int pos, boolean isChecked) {
			final OsmandSettings settings = ma.getMyApplication().getSettings();
			if (itemId == R.string.layer_poi) {
				settings.SELECTED_POI_FILTER_FOR_MAP.set(null);
				if (isChecked) {
					selectPOILayer(settings);
				}
			} else if (itemId == R.string.layer_amenity_label) {
				settings.SHOW_POI_LABEL.set(isChecked);
			} else if (itemId == R.string.shared_string_favorites) {
				settings.SHOW_FAVORITES.set(isChecked);
			} else if (itemId == R.string.layer_gpx_layer) {
				if (ma.getMyApplication().getSelectedGpxHelper().isShowingAnyGpxFiles()) {
					ma.getMyApplication().getSelectedGpxHelper().clearAllGpxFileToShow();
				} else {
					AlertDialog dialog = ma.getMapLayers().showGPXFileLayer(getAlreadySelectedGpx(), ma.getMapView());
					dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
						@Override
						public void onDismiss(DialogInterface dialog) {
							boolean areAnyGpxTracksVisible =
									ma.getMyApplication().getSelectedGpxHelper().isShowingAnyGpxFiles();
							cm.setSelection(pos, areAnyGpxTracksVisible);
							adapter.notifyDataSetChanged();
						}
					});
				}
			} else if (itemId == R.string.layer_map) {
				if (OsmandPlugin.getEnabledPlugin(OsmandRasterMapsPlugin.class) == null) {
					Intent intent = new Intent(ma, PluginActivity.class);
					intent.putExtra(PluginActivity.EXTRA_PLUGIN_ID, OsmandRasterMapsPlugin.ID);
					ma.startActivity(intent);
				} else {
					ma.getMapLayers().selectMapLayer(ma.getMapView());
				}
			} else if (itemId == R.string.layer_transport_route) {
				ma.getMapLayers().getTransportInfoLayer().setVisible(isChecked);
			}
			ma.getMapLayers().updateLayers(ma.getMapView());
			ma.getMapView().refreshMap();
			return false;
		}

		protected void selectPOILayer(final OsmandSettings settings) {
			final PoiUIFilter[] selected = new PoiUIFilter[1];
			AlertDialog dlg = ma.getMapLayers().selectPOIFilterLayer(ma.getMapView(), selected);
			dlg.setOnDismissListener(new DialogInterface.OnDismissListener() {

				@Override
				public void onDismiss(DialogInterface dialog) {
					ma.getDashboard().refreshContent(true);
				}
			});
		}
	}

	private void createLayersItems(ContextMenuAdapter adapter, MapActivity activity) {
		OsmandApplication app = activity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		LayerMenuListener l = new LayerMenuListener(activity, adapter);
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(R.string.shared_string_show, activity)
				.setCategory(true).setLayout(R.layout.drawer_list_sub_header).createItem());
		// String appMode = " [" + settings.getApplicationMode().toHumanString(view.getApplication()) +"] ";
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(R.string.layer_poi, activity)
				.setSelected(settings.SELECTED_POI_FILTER_FOR_MAP.get() != null)
				.setColorIcon(R.drawable.ic_action_info_dark)
				.setListener(l).createItem());
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(R.string.layer_amenity_label, activity)
				.setSelected(settings.SHOW_POI_LABEL.get())
				.setColorIcon(R.drawable.ic_action_text_dark)
				.setListener(l).createItem());
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(R.string.shared_string_favorites, activity)
				.setSelected(settings.SHOW_FAVORITES.get())
				.setColorIcon(R.drawable.ic_action_fav_dark)
				.setListener(l).createItem());
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(R.string.layer_gpx_layer, activity)
				.setSelected(app.getSelectedGpxHelper().isShowingAnyGpxFiles())
				.setColorIcon(R.drawable.ic_action_polygom_dark)
				.setListener(l).createItem());
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(R.string.layer_map, activity)
				.setColorIcon(R.drawable.ic_world_globe_dark)
				.setListener(l).createItem());
		if (TransportRouteHelper.getInstance().routeIsCalculated()) {
			adapter.addItem(new ContextMenuItem.ItemBuilder()
					.setTitleId(R.string.layer_transport_route, activity)
					.setSelected(true)
					.setColorIcon(R.drawable.ic_action_bus_dark)
					.setListener(l).createItem());
		}

		OsmandPlugin.registerLayerContextMenu(activity.getMapView(), adapter, activity);
		app.getAppCustomization().prepareLayerContextMenu(activity, adapter);
	}

	protected void refreshMapComplete(final MapActivity activity) {
		activity.getMyApplication().getResourceManager().getRenderer().clearCache();
		activity.updateMapSettings();
		GPXLayer gpx = activity.getMapView().getLayerByClass(GPXLayer.class);
		if (gpx != null) {
			gpx.updateLayerStyle();
		}
		RouteLayer rte = activity.getMapView().getLayerByClass(RouteLayer.class);
		if (rte != null) {
			rte.updateLayerStyle();
		}
		activity.getMapView().refreshMap(true);
	}

	private void createRenderingAttributeItems(final ContextMenuAdapter adapter, final MapActivity activity) {
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(R.string.map_widget_map_rendering, activity)
				.setCategory(true)
				.setLayout(R.layout.drawer_list_sub_header).createItem());
		String descr = getRenderDescr(activity);
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(R.string.map_widget_renderer, activity)
				.setListener(new OnContextMenuClick() {
					@Override
					public boolean onContextMenuClick(final ArrayAdapter<?> ad, int itemId, final int pos, boolean isChecked) {
						AlertDialog.Builder bld = new AlertDialog.Builder(activity);
						bld.setTitle(R.string.renderers);
						final OsmandApplication app = activity.getMyApplication();
						Collection<String> rendererNames = app.getRendererRegistry().getRendererNames();
						final String[] items = rendererNames.toArray(new String[rendererNames.size()]);
						final String[] visibleNames = new String[items.length];
						int selected = -1;
						final String selectedName = app.getRendererRegistry().getCurrentSelectedRenderer().getName();
						for (int j = 0; j < items.length; j++) {
							if (items[j].equals(selectedName)) {
								selected = j;
							}
							visibleNames[j] = items[j].replace('_', ' ').replace('-', ' ');
						}
						bld.setSingleChoiceItems(visibleNames, selected, new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								String renderer = items[which];
								RenderingRulesStorage loaded = app.getRendererRegistry().getRenderer(renderer);
								if (loaded != null) {
									OsmandMapTileView view = activity.getMapView();
									view.getSettings().RENDERER.set(renderer);
									app.getRendererRegistry().setCurrentSelectedRender(loaded);
									refreshMapComplete(activity);
								} else {
									AccessibleToast.makeText(app, R.string.renderer_load_exception, Toast.LENGTH_SHORT).show();
								}
								adapter.setItemDescription(pos, getRenderDescr(activity));
								activity.getDashboard().refreshContent(true);
								dialog.dismiss();
							}

						});
						bld.show();
						return false;
					}
				}).setDescription(descr).setLayout(R.layout.drawer_list_doubleitem).createItem());

		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(R.string.map_widget_day_night, activity)
				.setDescription(getDayNightDescr(activity))
				.setListener(new OnContextMenuClick() {
					@Override
					public boolean onContextMenuClick(final ArrayAdapter<?> ad, int itemId, final int pos, boolean isChecked) {
						final OsmandMapTileView view = activity.getMapView();
						AlertDialog.Builder bld = new AlertDialog.Builder(view.getContext());
						bld.setTitle(R.string.daynight);
						final String[] items = new String[OsmandSettings.DayNightMode.values().length];
						for (int i = 0; i < items.length; i++) {
							items[i] = OsmandSettings.DayNightMode.values()[i].toHumanString(activity.getMyApplication());
						}
						int i = view.getSettings().DAYNIGHT_MODE.get().ordinal();
						bld.setSingleChoiceItems(items, i, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								view.getSettings().DAYNIGHT_MODE.set(OsmandSettings.DayNightMode.values()[which]);
								refreshMapComplete(activity);
								dialog.dismiss();
								activity.getDashboard().refreshContent(true);
								//adapter.setItemDescription(pos, getDayNightDescr(activity));
								//ad.notifyDataSetInvalidated();
							}
						});
						bld.show();
						return false;
					}
				}).setLayout(R.layout.drawer_list_doubleitem).createItem());

		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(R.string.map_magnifier, activity).setListener(new OnContextMenuClick() {
					@Override
					public boolean onContextMenuClick(final ArrayAdapter<?> ad, int itemId, final int pos, boolean isChecked) {
						final OsmandMapTileView view = activity.getMapView();
						final OsmandSettings.OsmandPreference<Float> mapDensity = view.getSettings().MAP_DENSITY;
						final AlertDialog.Builder bld = new AlertDialog.Builder(view.getContext());
						int p = (int) (mapDensity.get() * 100);
						final TIntArrayList tlist = new TIntArrayList(new int[]{33, 50, 75, 100, 150, 200, 300, 400});
						final List<String> values = new ArrayList<String>();
						int i = -1;
						for (int k = 0; k <= tlist.size(); k++) {
							final boolean end = k == tlist.size();
							if (i == -1) {
								if ((end || p < tlist.get(k))) {
									values.add(p + " %");
									i = k;
								} else if (p == tlist.get(k)) {
									i = k;
								}
							}
							if (k < tlist.size()) {
								values.add(tlist.get(k) + " %");
							}
						}
						if (values.size() != tlist.size()) {
							tlist.insert(i, p);
						}

						bld.setTitle(R.string.map_magnifier);
						bld.setSingleChoiceItems(values.toArray(new String[values.size()]), i,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										int p = tlist.get(which);
										mapDensity.set(p / 100.0f);
										view.setComplexZoom(view.getZoom(), view.getSettingsMapDensity());
										MapRendererContext mapContext = NativeCoreContext.getMapRendererContext();
										if (mapContext != null) {
											mapContext.updateMapSettings();
										}
										adapter.setItemDescription(pos, String.format("%.0f", 100f * activity.getMyApplication().getSettings().MAP_DENSITY.get()) + " %");
										ad.notifyDataSetInvalidated();
										dialog.dismiss();
									}
								});
						bld.show();
						return false;
					}
				}).setDescription(String.format("%.0f", 100f * activity.getMyApplication().getSettings().MAP_DENSITY.get()) + " %")
				.setLayout(R.layout.drawer_list_doubleitem)
				.createItem());

		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(R.string.text_size, activity).setListener(new OnContextMenuClick() {
					@Override
					public boolean onContextMenuClick(final ArrayAdapter<?> ad, int itemId, final int pos, boolean isChecked) {
						final OsmandMapTileView view = activity.getMapView();
						AlertDialog.Builder b = new AlertDialog.Builder(view.getContext());
						// test old descr as title
						b.setTitle(R.string.text_size);
						final Float[] txtValues = new Float[]{0.75f, 1f, 1.25f, 1.5f, 2f, 3f};
						int selected = -1;
						final String[] txtNames = new String[txtValues.length];
						for (int i = 0; i < txtNames.length; i++) {
							txtNames[i] = (int) (txtValues[i] * 100) + " %";
							if (Math.abs(view.getSettings().TEXT_SCALE.get() - txtValues[i]) < 0.1f) {
								selected = i;
							}
						}
						b.setSingleChoiceItems(txtNames, selected, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								view.getSettings().TEXT_SCALE.set(txtValues[which]);
								refreshMapComplete(activity);
								adapter.setItemDescription(pos, getScale(activity));
								ad.notifyDataSetInvalidated();
								dialog.dismiss();
							}
						});
						b.show();
						return false;
					}
				}).setDescription(getScale(activity)).setLayout(R.layout.drawer_list_doubleitem).createItem());

		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(R.string.map_locale, activity).setListener(new OnContextMenuClick() {
					@Override
					public boolean onContextMenuClick(final ArrayAdapter<?> ad, int itemId, final int pos, boolean isChecked) {
						final OsmandMapTileView view = activity.getMapView();
						AlertDialog.Builder b = new AlertDialog.Builder(view.getContext());
						// test old descr as title
						b.setTitle(R.string.map_preferred_locale);
						final String[] txtIds = getSortedMapNamesIds(activity);
						final String[] txtValues = getMapNamesValues(activity, txtIds);
						int selected = -1;
						for (int i = 0; i < txtIds.length; i++) {
							if (view.getSettings().MAP_PREFERRED_LOCALE.get().equals(txtIds[i])) {
								selected = i;
								break;
							}
						}
						b.setSingleChoiceItems(txtValues, selected, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								view.getSettings().MAP_PREFERRED_LOCALE.set(txtIds[which]);
								refreshMapComplete(activity);
								adapter.setItemDescription(pos, txtIds[which]);
								ad.notifyDataSetInvalidated();
								dialog.dismiss();
							}
						});
						b.show();
						return false;
					}
				})
				.setDescription(activity.getMyApplication().getSettings().MAP_PREFERRED_LOCALE.get())
				.setLayout(R.layout.drawer_list_doubleitem).createItem());

		RenderingRulesStorage renderer = activity.getMyApplication().getRendererRegistry().getCurrentSelectedRenderer();
		if (renderer != null) {
			List<RenderingRuleProperty> customRules = new ArrayList<RenderingRuleProperty>();
			for (RenderingRuleProperty p : renderer.PROPS.getCustomRules()) {
				if (!RenderingRuleStorageProperties.UI_CATEGORY_HIDDEN.equals(p.getCategory())) {
					customRules.add(p);
				}
			}

			createProperties(customRules, R.string.rendering_category_transport, "transport",
					adapter, activity);
			createProperties(customRules, R.string.rendering_category_details, "details",
					adapter, activity);
			createProperties(customRules, R.string.rendering_category_hide, "hide",
					adapter, activity);
			createProperties(customRules, R.string.rendering_category_routes, "routes",
					adapter, activity);

			if (customRules.size() > 0) {
				adapter.addItem(new ContextMenuItem.ItemBuilder()
						.setTitleId(R.string.rendering_category_others, activity).setCategory(true)
						.setLayout(R.layout.drawer_list_sub_header).createItem());
				createCustomRenderingProperties(adapter, activity, customRules);
			}
		}
	}

	public static String[] mapNamesIds = new String[]{"", "en", "als", "af", "ar", "az", "be", "bg", "bn", "bpy", "br", "bs", "ca", "ceb", "cs", "cy", "da", "de", "el", "et", "es", "eu", "fa", "fi", "fr", "fy", "ga", "gl", "he", "hi", "hr", "ht", "hu", "hy", "id", "is", "it", "ja", "ka", "ko", "ku", "la", "lb", "lt", "lv", "mk", "ml", "mr", "ms", "nds", "new", "nl", "nn", "no", "nv", "os", "pl", "pms", "pt", "ro", "ru", "sh", "sc", "sk", "sl", "sq", "sr", "sv", "sw", "ta", "te", "th", "tl", "tr", "uk", "vi", "vo", "zh"};


	public static String[] getSortedMapNamesIds(Context ctx) {
		String[] vls = getMapNamesValues(ctx, mapNamesIds);
		final Map<String, String> mp = new HashMap<String, String>();
		for (int i = 0; i < mapNamesIds.length; i++) {
			mp.put(mapNamesIds[i], vls[i]);
		}
		ArrayList<String> lst = new ArrayList<String>(mp.keySet());
		Collections.sort(lst, new Comparator<String>() {
			@Override
			public int compare(String lhs, String rhs) {
				int i1 = Algorithms.isEmpty(lhs) ? 0 : (lhs.equals("en") ? 1 : 2);
				int i2 = Algorithms.isEmpty(rhs) ? 0 : (rhs.equals("en") ? 1 : 2);
				if (i1 != i2) {
					return i1 < i2 ? -1 : 1;
				}
				return mp.get(lhs).compareTo(mp.get(rhs));
			}
		});
		return lst.toArray(new String[lst.size()]);
	}

	public static String[] getMapNamesValues(Context ctx, String[] ids) {
		String[] translates = new String[ids.length];
		for (int i = 0; i < translates.length; i++) {
			if (Algorithms.isEmpty(ids[i])) {
				translates[i] = ctx.getString(R.string.local_map_names);
			} else {
				translates[i] = ((OsmandApplication) ctx.getApplicationContext()).getLangTranslation(ids[i]);
			}
		}

		return translates;
	}

	private void createProperties(List<RenderingRuleProperty> customRules, final int strId, String cat,
								  final ContextMenuAdapter adapter, final MapActivity activity) {
		final List<RenderingRuleProperty> ps = new ArrayList<RenderingRuleProperty>();
		final List<OsmandSettings.CommonPreference<Boolean>> prefs = new ArrayList<OsmandSettings.CommonPreference<Boolean>>();
		Iterator<RenderingRuleProperty> it = customRules.iterator();

		while (it.hasNext()) {
			RenderingRuleProperty p = it.next();
			if (cat.equals(p.getCategory()) && p.isBoolean()) {
				ps.add(p);
				final OsmandSettings.CommonPreference<Boolean> pref = activity.getMyApplication().getSettings()
						.getCustomRenderBooleanProperty(p.getAttrName());
				prefs.add(pref);
				it.remove();
			}
		}
		if (prefs.size() > 0) {
			final String descr = getDescription(prefs);
			adapter.addItem(new ContextMenuItem.ItemBuilder()
					.setTitleId(strId, activity)
					.setDescription(descr)
					.setLayout(R.layout.drawer_list_doubleitem)
					.setListener(new OnContextMenuClick() {

						@Override
						public boolean onContextMenuClick(ArrayAdapter<?> a, int itemId, int pos, boolean isChecked) {
							showPreferencesDialog(adapter, a, pos, activity, activity.getString(strId), ps, prefs);
							return false;
						}
					}).createItem());
//			createCustomRenderingProperties(adapter, activity, ps);
		}
	}

	protected String getDescription(final List<OsmandSettings.CommonPreference<Boolean>> prefs) {
		int count = 0;
		int enabled = 0;
		for (OsmandSettings.CommonPreference<Boolean> p : prefs) {
			count++;
			if (p.get()) {
				enabled++;
			}
		}
		final String descr = enabled + "/" + count;
		return descr;
	}

	protected void showPreferencesDialog(final ContextMenuAdapter adapter, final ArrayAdapter<?> a, final int pos, final MapActivity activity,
										 String category, List<RenderingRuleProperty> ps, final List<CommonPreference<Boolean>> prefs) {
		AlertDialog.Builder bld = new AlertDialog.Builder(activity);
		boolean[] checkedItems = new boolean[prefs.size()];
		for (int i = 0; i < prefs.size(); i++) {
			checkedItems[i] = prefs.get(i).get();
		}

		final boolean[] tempPrefs = new boolean[prefs.size()];
		for (int i = 0; i < prefs.size(); i++) {
			tempPrefs[i] = prefs.get(i).get();
		}
		final String[] vals = new String[ps.size()];
		for (int i = 0; i < ps.size(); i++) {
			RenderingRuleProperty p = ps.get(i);
			String propertyName = SettingsActivity.getStringPropertyName(activity, p.getAttrName(),
					p.getName());
			vals[i] = propertyName;
		}

		bld.setMultiChoiceItems(vals, checkedItems, new OnMultiChoiceClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				tempPrefs[which] = isChecked;
			}
		});

		bld.setTitle(category);

		bld.setNegativeButton(R.string.shared_string_cancel, null);

		bld.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				for (int i = 0; i < prefs.size(); i++) {
					prefs.get(i).set(tempPrefs[i]);
				}
				adapter.setItemDescription(pos, getDescription(prefs));
				a.notifyDataSetInvalidated();
				refreshMapComplete(activity);
				activity.getMapLayers().updateLayers(activity.getMapView());
			}
		});

		bld.show();
	}

	protected String getRenderDescr(final MapActivity activity) {
		RendererRegistry rr = activity.getMyApplication().getRendererRegistry();
		RenderingRulesStorage storage = rr.getCurrentSelectedRenderer();
		if (storage == null) {
			return "";
		}
		return storage.getName();
	}

	protected String getDayNightDescr(final MapActivity activity) {
		return activity.getMyApplication().getSettings().DAYNIGHT_MODE.get().toHumanString(activity);
	}

	protected String getScale(final MapActivity activity) {
		int scale = (int) (activity.getMyApplication().getSettings().TEXT_SCALE.get() * 100);
		return scale + " %";
	}


	private void createCustomRenderingProperties(final ContextMenuAdapter adapter, final MapActivity activity,
												 List<RenderingRuleProperty> customRules) {
		final OsmandMapTileView view = activity.getMapView();
		for (final RenderingRuleProperty p : customRules) {
			if (p.getAttrName().equals(RenderingRuleStorageProperties.A_APP_MODE) ||
					p.getAttrName().equals(RenderingRuleStorageProperties.A_ENGINE_V1)) {
				continue;
			}
			String propertyName = SettingsActivity.getStringPropertyName(view.getContext(), p.getAttrName(),
					p.getName());
			// test old descr as title
			final String propertyDescr = SettingsActivity.getStringPropertyDescription(view.getContext(),
					p.getAttrName(), p.getName());
			if (p.isBoolean()) {
				final OsmandSettings.CommonPreference<Boolean> pref = view.getApplication().getSettings()
						.getCustomRenderBooleanProperty(p.getAttrName());
				adapter.addItem(ContextMenuItem.createBuilder(propertyName)
						.setListener(new OnContextMenuClick() {

							@Override
							public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
								pref.set(!pref.get());
								refreshMapComplete(activity);
								return false;
							}
						})
						.setSelected(pref.get())
						.createItem());
			} else {
				final OsmandSettings.CommonPreference<String> pref = view.getApplication().getSettings()
						.getCustomRenderProperty(p.getAttrName());
				String descr;
				if (!Algorithms.isEmpty(pref.get())) {
					descr = SettingsActivity.getStringPropertyValue(activity, pref.get());
				} else {
					descr = SettingsActivity.getStringPropertyValue(view.getContext(),
							p.getDefaultValueDescription());
				}
				adapter.addItem(ContextMenuItem.createBuilder(propertyName).setListener(new OnContextMenuClick() {

					@Override
					public boolean onContextMenuClick(final ArrayAdapter<?> ad, int itemId, final int pos, boolean isChecked) {
						AlertDialog.Builder b = new AlertDialog.Builder(view.getContext());
						// test old descr as title
						b.setTitle(propertyDescr);

						int i = Arrays.asList(p.getPossibleValues()).indexOf(pref.get());
						if (i >= 0) {
							i++;
						} else if (Algorithms.isEmpty(pref.get())) {
							i = 0;
						}

						String[] possibleValuesString = new String[p.getPossibleValues().length + 1];
						possibleValuesString[0] = SettingsActivity.getStringPropertyValue(view.getContext(),
								p.getDefaultValueDescription());

						for (int j = 0; j < p.getPossibleValues().length; j++) {
							possibleValuesString[j + 1] = SettingsActivity.getStringPropertyValue(view.getContext(),
									p.getPossibleValues()[j]);
						}

						b.setSingleChoiceItems(possibleValuesString, i, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								if (which == 0) {
									pref.set("");
								} else {
									pref.set(p.getPossibleValues()[which - 1]);
								}
								refreshMapComplete(activity);
								adapter.setItemDescription(pos, SettingsActivity.getStringPropertyValue(activity, pref.get()));
								dialog.dismiss();
								ad.notifyDataSetInvalidated();
							}
						});
						b.show();
						return false;
					}
				}).setDescription(descr).setLayout(R.layout.drawer_list_doubleitem).createItem());
			}
		}
	}
}