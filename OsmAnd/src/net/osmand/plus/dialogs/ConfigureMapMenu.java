package net.osmand.plus.dialogs;

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.osmand.PlatformUtil;
import net.osmand.core.android.MapRendererContext;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.ItemClickListener;
import net.osmand.plus.ContextMenuAdapter.OnRowItemClick;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.OsmandSettings.ListStringPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityLayers;
import net.osmand.plus.activities.PluginActivity;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.poi.PoiFiltersHelper;
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

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Toast;

public class ConfigureMapMenu {
	private static final Log LOG = PlatformUtil.getLog(ConfigureMapMenu.class);

	public interface OnClickListener {
		void onClick();
	}

	public ContextMenuAdapter createListAdapter(final MapActivity ma) {
		ContextMenuAdapter adapter = new ContextMenuAdapter();
		adapter.setDefaultLayoutId(R.layout.list_item_icon_and_menu);
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(R.string.app_modes_choose, ma)
				.setLayout(R.layout.mode_toggles).createItem());
		adapter.setChangeAppModeListener(new OnClickListener() {
			@Override
			public void onClick() {
				ma.getDashboard().updateListAdapter(createListAdapter(ma));
			}
		});
		RenderingRulesStorage renderer = ma.getMyApplication().getRendererRegistry().getCurrentSelectedRenderer();
		List<RenderingRuleProperty> customRules = new ArrayList<>();
		if (renderer != null) {
			for (RenderingRuleProperty p : renderer.PROPS.getCustomRules()) {
				if (!RenderingRuleStorageProperties.UI_CATEGORY_HIDDEN.equals(p.getCategory())) {
					customRules.add(p);
				}
			}
		}
		createLayersItems(customRules, adapter, ma);
		createRenderingAttributeItems(customRules, adapter, ma);

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
			List<String> files = new ArrayList<>();
			for (GpxSelectionHelper.SelectedGpxFile file : selectedGpxFiles) {
				files.add(file.getGpxFile().path);
			}
			return files;
		}

		@Override
		public boolean onRowItemClick(ArrayAdapter<ContextMenuItem> adapter, View view, int itemId, int pos) {
			if (itemId == R.string.layer_poi) {
				showPoiFilterDialog(adapter, adapter.getItem(pos));
				return false;
			} else if (itemId == R.string.layer_gpx_layer && cm.getItem(pos).getSelected()) {
				showGpxSelectionDialog(adapter, adapter.getItem(pos));
				return false;
			} else {
				CompoundButton btn = (CompoundButton) view.findViewById(R.id.toggle_item);
				if (btn != null && btn.getVisibility() == View.VISIBLE) {
					btn.setChecked(!btn.isChecked());
					cm.getItem(pos).setColorRes(btn.isChecked() ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
					adapter.notifyDataSetChanged();
					return false;
				} else {
					return onContextMenuClick(adapter, itemId, pos, false);
				}
			}
		}

		@Override
		public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> adapter, int itemId, final int pos, boolean isChecked) {
			final OsmandSettings settings = ma.getMyApplication().getSettings();
			final PoiFiltersHelper poiFiltersHelper = ma.getMyApplication().getPoiFilters();
			final ContextMenuItem item = cm.getItem(pos);
			if (item.getSelected() != null) {
				item.setColorRes(isChecked ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
			}
			if (itemId == R.string.layer_poi) {
				poiFiltersHelper.clearSelectedPoiFilters();
				if (isChecked) {
					showPoiFilterDialog(adapter, adapter.getItem(pos));
				} else {
					adapter.getItem(pos).setDescription(poiFiltersHelper.getSelectedPoiFiltersName());
				}
			} else if (itemId == R.string.layer_amenity_label) {
				settings.SHOW_POI_LABEL.set(isChecked);
			} else if (itemId == R.string.shared_string_favorites) {
				settings.SHOW_FAVORITES.set(isChecked);
			} else if (itemId == R.string.layer_gpx_layer) {
				final GpxSelectionHelper selectedGpxHelper = ma.getMyApplication().getSelectedGpxHelper();
				if (selectedGpxHelper.isShowingAnyGpxFiles()) {
					selectedGpxHelper.clearAllGpxFileToShow();
					adapter.getItem(pos).setDescription(selectedGpxHelper.getGpxDescription());
				} else {
					showGpxSelectionDialog(adapter, adapter.getItem(pos));
				}
			} else if (itemId == R.string.layer_map) {
				if (OsmandPlugin.getEnabledPlugin(OsmandRasterMapsPlugin.class) == null) {
					Intent intent = new Intent(ma, PluginActivity.class);
					intent.putExtra(PluginActivity.EXTRA_PLUGIN_ID, OsmandRasterMapsPlugin.ID);
					ma.startActivity(intent);
				} else {
					ContextMenuItem it = adapter.getItem(pos);
					ma.getMapLayers().selectMapLayer(ma.getMapView(), it, adapter);
				}
			}
			adapter.notifyDataSetChanged();
			ma.getMapLayers().updateLayers(ma.getMapView());
			ma.getMapView().refreshMap();
			return false;
		}

		private void showGpxSelectionDialog(final ArrayAdapter<ContextMenuItem> adapter,
											final ContextMenuItem item) {
			AlertDialog dialog = ma.getMapLayers().showGPXFileLayer(getAlreadySelectedGpx(),
					ma.getMapView());
			dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					OsmandApplication app = ma.getMyApplication();
					boolean selected = app.getSelectedGpxHelper().isShowingAnyGpxFiles();
					item.setSelected(app.getSelectedGpxHelper().isShowingAnyGpxFiles());
					item.setDescription(app.getSelectedGpxHelper().getGpxDescription());
					item.setColorRes(selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
					adapter.notifyDataSetChanged();
				}
			});
		}

		protected void showPoiFilterDialog(final ArrayAdapter<ContextMenuItem> adapter,
										   final ContextMenuItem item) {
			final PoiFiltersHelper poiFiltersHelper = ma.getMyApplication().getPoiFilters();
			MapActivityLayers.DismissListener dismissListener =
					new MapActivityLayers.DismissListener() {
				@Override
				public void dismiss() {
					PoiFiltersHelper pf = ma.getMyApplication().getPoiFilters();
					boolean selected = pf.isShowingAnyPoi();
					item.setSelected(selected);
					item.setDescription(pf.getSelectedPoiFiltersName());
					item.setColorRes(selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
					adapter.notifyDataSetChanged();
				}
			};
			if (poiFiltersHelper.getSelectedPoiFilters().size() > 1) {
				ma.getMapLayers().showMultichoicePoiFilterDialog(ma.getMapView(),
						dismissListener);
			} else {
				ma.getMapLayers().showSingleChoicePoiFilterDialog(ma.getMapView(),
						dismissListener);
			}
		}
	}

	private void createLayersItems(List<RenderingRuleProperty> customRules, ContextMenuAdapter adapter, MapActivity activity) {
		OsmandApplication app = activity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		LayerMenuListener l = new LayerMenuListener(activity, adapter);
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(R.string.shared_string_show, activity)
				.setCategory(true).setLayout(R.layout.list_group_title_with_switch).createItem());
		// String appMode = " [" + settings.getApplicationMode().toHumanString(view.getApplication()) +"] ";
		boolean selected = settings.SHOW_FAVORITES.get();
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(R.string.shared_string_favorites, activity)
				.setSelected(settings.SHOW_FAVORITES.get())
				.setColor(selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setIcon(R.drawable.ic_action_fav_dark)
				.setListener(l).createItem());
		selected = app.getPoiFilters().isShowingAnyPoi();
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(R.string.layer_poi, activity)
				.setSelected(selected)
				.setDescription(app.getPoiFilters().getSelectedPoiFiltersName())
				.setColor(selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setIcon(R.drawable.ic_action_info_dark)
				.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setListener(l).createItem());
		ContextMenuItem item = createProperties(customRules, R.string.rendering_category_transport, R.drawable.ic_action_bus_dark,
				"transport", settings.TRANSPORT_DEFAULT_SETTINGS, adapter, activity, false);
		if(item != null) {
			adapter.addItem(item);	
		}
		selected = settings.SHOW_POI_LABEL.get();
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(R.string.layer_amenity_label, activity)
				.setSelected(settings.SHOW_POI_LABEL.get())
				.setColor(selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setIcon(R.drawable.ic_action_text_dark)
				.setListener(l).createItem());
		selected = app.getSelectedGpxHelper().isShowingAnyGpxFiles();
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(R.string.layer_gpx_layer, activity)
				.setSelected(app.getSelectedGpxHelper().isShowingAnyGpxFiles())
				.setDescription(app.getSelectedGpxHelper().getGpxDescription())
				.setColor(selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setIcon(R.drawable.ic_action_polygom_dark)
				.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setListener(l).createItem());
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(R.string.layer_map, activity)
				.setIcon(R.drawable.ic_world_globe_dark)
				.setDescription(settings.MAP_ONLINE_DATA.get() ? settings.MAP_TILE_SOURCES.get() : null)
				.setListener(l).createItem());

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

	private void createRenderingAttributeItems(List<RenderingRuleProperty> customRules,
			final ContextMenuAdapter adapter, final MapActivity activity) {
		adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.map_widget_map_rendering, activity)
				.setCategory(true).setLayout(R.layout.list_group_title_with_switch).createItem());
		adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.map_widget_renderer, activity)
				.setDescription(getRenderDescr(activity)).setLayout(R.layout.list_item_single_line_descrition_narrow)
				.setIcon(R.drawable.ic_map).setListener(new ContextMenuAdapter.ItemClickListener() {
					@Override
					public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> ad, int itemId,
							final int pos, boolean isChecked) {
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
									Toast.makeText(app, R.string.renderer_load_exception, Toast.LENGTH_SHORT).show();
								}
								adapter.getItem(pos).setDescription(getRenderDescr(activity));
								activity.getDashboard().refreshContent(true);
								dialog.dismiss();
							}

						});
						bld.show();
						return false;
					}
				}).createItem());

		adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.map_mode, activity)
				.setDescription(getDayNightDescr(activity)).setLayout(R.layout.list_item_single_line_descrition_narrow)
				.setIcon(getDayNightIcon(activity)).setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> ad, int itemId,
							final int pos, boolean isChecked) {
						final OsmandMapTileView view = activity.getMapView();
						AlertDialog.Builder bld = new AlertDialog.Builder(view.getContext());
						bld.setTitle(R.string.daynight);
						final String[] items = new String[OsmandSettings.DayNightMode.values().length];
						for (int i = 0; i < items.length; i++) {
							items[i] = OsmandSettings.DayNightMode.values()[i].toHumanString(activity
									.getMyApplication());
						}
						int i = view.getSettings().DAYNIGHT_MODE.get().ordinal();
						bld.setSingleChoiceItems(items, i, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								view.getSettings().DAYNIGHT_MODE.set(OsmandSettings.DayNightMode.values()[which]);
								refreshMapComplete(activity);
								dialog.dismiss();
								activity.getDashboard().refreshContent(true);
								// adapter.getItem(pos).setDescription(s, getDayNightDescr(activity));
								// ad.notifyDataSetInvalidated();
							}
						});
						bld.show();
						return false;
					}
				}).createItem());

		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(R.string.map_magnifier, activity)
				.setDescription(
						String.format(Locale.UK, "%.0f",
								100f * activity.getMyApplication().getSettings().MAP_DENSITY.get())
								+ " %").setLayout(R.layout.list_item_single_line_descrition_narrow)
				.setIcon(R.drawable.ic_action_map_magnifier).setListener(new ContextMenuAdapter.ItemClickListener() {
					@Override
					public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> ad, int itemId,
							final int pos, boolean isChecked) {
						final OsmandMapTileView view = activity.getMapView();
						final OsmandSettings.OsmandPreference<Float> mapDensity = view.getSettings().MAP_DENSITY;
						final AlertDialog.Builder bld = new AlertDialog.Builder(view.getContext());
						int p = (int) (mapDensity.get() * 100);
						final TIntArrayList tlist = new TIntArrayList(new int[] { 33, 50, 75, 100, 150, 200, 300, 400 });
						final List<String> values = new ArrayList<>();
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
										adapter.getItem(pos).setDescription(
												String.format(Locale.UK, "%.0f", 100f * activity.getMyApplication()
														.getSettings().MAP_DENSITY.get())
														+ " %");
										ad.notifyDataSetInvalidated();
										dialog.dismiss();
									}
								});
						bld.show();
						return false;
					}
				}).createItem());

		adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.text_size, activity)
				.setDescription(getScale(activity)).setLayout(R.layout.list_item_single_line_descrition_narrow)
				.setIcon(R.drawable.ic_action_map_text_size).setListener(new ContextMenuAdapter.ItemClickListener() {
					@Override
					public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> ad, int itemId,
							final int pos, boolean isChecked) {
						final OsmandMapTileView view = activity.getMapView();
						AlertDialog.Builder b = new AlertDialog.Builder(view.getContext());
						// test old descr as title
						b.setTitle(R.string.text_size);
						final Float[] txtValues = new Float[] { 0.75f, 1f, 1.25f, 1.5f, 2f, 3f };
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
								adapter.getItem(pos).setDescription(getScale(activity));
								ad.notifyDataSetInvalidated();
								dialog.dismiss();
							}
						});
						b.show();
						return false;
					}
				}).createItem());

		String localeDescr = activity.getMyApplication().getSettings().MAP_PREFERRED_LOCALE.get();
		localeDescr = localeDescr == null || localeDescr.equals("") ? activity.getString(R.string.local_map_names)
				: localeDescr;
		adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.map_locale, activity)
				.setDescription(localeDescr).setLayout(R.layout.list_item_single_line_descrition_narrow)
				.setIcon(R.drawable.ic_action_map_language).setListener(new ContextMenuAdapter.ItemClickListener() {
					@Override
					public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> ad, int itemId,
							final int pos, boolean isChecked) {
						final OsmandMapTileView view = activity.getMapView();
						AlertDialog.Builder b = new AlertDialog.Builder(view.getContext());
						// test old descr as title
						b.setTitle(R.string.map_preferred_locale);
						final String[] txtIds = getSortedMapNamesIds(activity, mapNamesIds,
								getMapNamesValues(activity, mapNamesIds));
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
								String localeDescr = txtIds[which];
								localeDescr = localeDescr == null || localeDescr.equals("") ? activity
										.getString(R.string.local_map_names) : localeDescr;
								adapter.getItem(pos).setDescription(localeDescr);
								ad.notifyDataSetInvalidated();
								dialog.dismiss();
							}
						});
						b.show();
						return false;
					}
				}).createItem());

		ContextMenuItem props;
		props = createProperties(customRules, R.string.rendering_category_transport, R.drawable.ic_action_bus_dark,
				"transport", null, adapter, activity, true);
		if(props != null) {
			adapter.addItem(props);
		}
		props = createProperties(customRules, R.string.rendering_category_details, R.drawable.ic_action_layers_dark,
				"details", null, adapter, activity, true);
		if(props != null) {
			adapter.addItem(props);
		}
		props = createProperties(customRules, R.string.rendering_category_hide, R.drawable.ic_action_hide, 
				"hide", null, adapter, activity, true);
		if(props != null) {
			adapter.addItem(props);
		}
		props = createProperties(customRules, R.string.rendering_category_routes, R.drawable.ic_action_map_routes,
				"routes", null, adapter, activity, true);
		if(props != null) {
			adapter.addItem(props);
		}

		if (customRules.size() > 0) {
			adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.rendering_category_others, activity)
					.setCategory(true).setLayout(R.layout.list_group_title_with_switch).createItem());
			createCustomRenderingProperties(adapter, activity, customRules);
		}
	}

	public static String[] mapNamesIds = new String[]{"", "en", "als", "af", "ar", "az", "be", "bg", "bn", "bpy", "br", "bs", "ca", "ceb", "cs", "cy", "da", "de", "el", "eo", "et", "es", "eu", "fa", "fi", "fr", "fy", "ga", "gl", "he", "hi", "hsb", "hr", "ht", "hu", "hy", "id", "is", "it", "ja", "ka", "ko", "ku", "la", "lb", "lt", "lv", "mk", "ml", "mr", "ms", "nds", "new", "nl", "nn", "no", "nv", "os", "pl", "pms", "pt", "ro", "ru", "sh", "sc", "sk", "sl", "sq", "sr", "sv", "sw", "ta", "te", "th", "tl", "tr", "uk", "vi", "vo", "zh"};

	public static String[] getSortedMapNamesIds(Context ctx, String[] ids, String[] values) {
		final Map<String, String> mp = new HashMap<>();
		for (int i = 0; i < ids.length; i++) {
			mp.put(ids[i], values[i]);
		}
		ArrayList<String> lst = new ArrayList<>(mp.keySet());
		final String systemLocale = ctx.getString(R.string.system_locale) + " (" + ctx.getString(R.string.system_locale_no_translate) + ")";
		final String englishLocale = ctx.getString(R.string.lang_en);
		Collections.sort(lst, new Comparator<String>() {
			@Override
			public int compare(String lhs, String rhs) {
				int i1 = Algorithms.isEmpty(lhs) ? 0 : (lhs.equals("en") ? 1 : 2);
				int i2 = Algorithms.isEmpty(rhs) ? 0 : (rhs.equals("en") ? 1 : 2);
				if (i1 != i2) {
					return i1 < i2 ? -1 : 1;
				}
				i1 = systemLocale.equals(lhs) ? 0 : (englishLocale.equals(lhs) ? 1 : 2);
				i2 = systemLocale.equals(rhs) ? 0 : (englishLocale.equals(rhs) ? 1 : 2);
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

	private ContextMenuItem createProperties(List<RenderingRuleProperty> customRules,
								  @StringRes final int strId,
								  @DrawableRes final int icon,
								  String category,  final ListStringPreference defaultSettings,
								  final ContextMenuAdapter adapter, final MapActivity activity, final boolean useDescription) {
		final List<RenderingRuleProperty> ps = new ArrayList<>();
		final List<OsmandSettings.CommonPreference<Boolean>> prefs = new ArrayList<>();
		Iterator<RenderingRuleProperty> it = customRules.iterator();

		while (it.hasNext()) {
			RenderingRuleProperty p = it.next();
			if (category.equals(p.getCategory()) && p.isBoolean()) {
				ps.add(p);
				final OsmandSettings.CommonPreference<Boolean> pref = activity.getMyApplication().getSettings()
						.getCustomRenderBooleanProperty(p.getAttrName());
				prefs.add(pref);
				it.remove();
			}
		}
		if (prefs.size() > 0) {
			final ItemClickListener clickListener = new ContextMenuAdapter.ItemClickListener() {
				@Override
				public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> a, int itemId, int pos,
						boolean isChecked) {
					if (!isChecked && !useDescription) {
						if (defaultSettings != null) {
							defaultSettings.set("");
							for (int i = 0; i < prefs.size(); i++) {
								if (prefs.get(i).get()) {
									defaultSettings.addValue(prefs.get(i).getId());
								}
							}
						}
						for (int i = 0; i < prefs.size(); i++) {
							prefs.get(i).set(false);
						}
						adapter.getItem(pos).setColorRes(ContextMenuItem.INVALID_ID);
						a.notifyDataSetInvalidated();
						refreshMapComplete(activity);
						activity.getMapLayers().updateLayers(activity.getMapView());
					} else {
						showPreferencesDialog(adapter, a, pos, activity, activity.getString(strId), ps, prefs,
								useDescription, defaultSettings, true);
					}
					return false;
				}

			};
			ContextMenuItem.ItemBuilder builder = new ContextMenuItem.ItemBuilder().setTitleId(strId, activity)
					.setIcon(icon).setListener(clickListener);
			boolean selected = false;
			for(OsmandSettings.CommonPreference<Boolean> p : prefs) {
				if(p.get()) {
					selected = true;
					break;
				}
			}
			builder.setColor(selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
			if (useDescription) {
				final String descr = getDescription(prefs);
				builder.setDescription(descr);
				builder.setLayout(R.layout.list_item_single_line_descrition_narrow);
			} else {
				builder.setListener(new OnRowItemClick() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> a, int itemId, int pos, boolean isChecked) {
						return clickListener.onContextMenuClick(a, itemId, pos, isChecked);
					}
					
					@Override
					public boolean onRowItemClick(ArrayAdapter<ContextMenuItem> a, View view, int itemId,
							int pos) {
						showPreferencesDialog(adapter, a, pos, activity, activity.getString(strId), ps, prefs,
								useDescription, defaultSettings, false);
						return false;
					}
				});
				builder.setSecondaryIcon(R.drawable.ic_action_additional_option);
				builder.setSelected(selected);
			}
			return builder.createItem();
//			createCustomRenderingProperties(adapter, activity, ps);
		}
		return null;
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
		return enabled + "/" + count;
	}

	protected void showPreferencesDialog(final ContextMenuAdapter adapter, final ArrayAdapter<?> a, final int pos, final MapActivity activity,
										 String category, List<RenderingRuleProperty> ps, final List<CommonPreference<Boolean>> prefs, 
										 final boolean useDescription, ListStringPreference defaultSettings, boolean useDefault) {
		AlertDialog.Builder bld = new AlertDialog.Builder(activity);
		boolean[] checkedItems = new boolean[prefs.size()];
		final boolean[] tempPrefs = new boolean[prefs.size()];
		for (int i = 0; i < prefs.size(); i++) {
			tempPrefs[i] = checkedItems[i] = defaultSettings != null && useDefault ? 
					defaultSettings.containsValue(prefs.get(i).getId()) : prefs.get(i).get();
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

		bld.setNegativeButton(R.string.shared_string_cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				boolean selected = false;
				for (int i = 0; i < prefs.size(); i++) {
					selected |= prefs.get(i).get();
				}
				adapter.getItem(pos).setSelected(selected);
				adapter.getItem(pos).setColorRes(selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
				a.notifyDataSetInvalidated();
			}
		});

		bld.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				boolean selected = false;
				for (int i = 0; i < prefs.size(); i++) {
					prefs.get(i).set(tempPrefs[i]);
					selected |= tempPrefs[i];
				}
				if(adapter != null) {
					if(useDescription) {
						adapter.getItem(pos).setDescription(getDescription(prefs));
					} else{
						adapter.getItem(pos).setSelected(selected);
					}
					adapter.getItem(pos).setColorRes(selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
				}
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

	@DrawableRes
	protected int getDayNightIcon(final MapActivity activity) {
		return activity.getMyApplication().getSettings().DAYNIGHT_MODE.get().getIconRes();
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
						.setListener(new ContextMenuAdapter.ItemClickListener() {

							@Override
							public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked) {
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
				adapter.addItem(ContextMenuItem.createBuilder(propertyName).setListener(new ContextMenuAdapter.ItemClickListener() {

					@Override
					public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> ad,
													  int itemId, final int pos, boolean isChecked) {
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
								adapter.getItem(pos).setDescription(SettingsActivity.getStringPropertyValue(activity, pref.get()));
								dialog.dismiss();
								ad.notifyDataSetInvalidated();
							}
						});
						b.show();
						return false;
					}
				}).setDescription(descr).setLayout(R.layout.list_item_single_line_descrition_narrow).createItem());
			}
		}
	}
}