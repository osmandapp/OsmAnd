package net.osmand.plus.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import net.osmand.access.AccessibleToast;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.ContextMenuAdapter.OnRowItemClick;
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
import net.osmand.plus.dashboard.DashboardOnMap.DashboardType;
import net.osmand.plus.poi.PoiLegacyFilter;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.corenative.NativeCoreContext;
import gnu.trove.list.array.TIntArrayList;
import net.osmand.core.android.MapRendererContext;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRuleStorageProperties;
import net.osmand.render.RenderingRulesStorage;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

public class ConfigureMapMenu {

	public interface OnClickListener{
		public void onClick(boolean result);
	};

	private boolean allModes = false;

	public ContextMenuAdapter createListAdapter(final MapActivity ma) {
		ContextMenuAdapter adapter = new ContextMenuAdapter(ma, allModes);
		adapter.setDefaultLayoutId(R.layout.drawer_list_item);
		adapter.item(R.string.app_modes_choose).layout(R.layout.mode_toggles).reg();
		adapter.setChangeAppModeListener(new OnClickListener() {
			@Override
			public void onClick(boolean result) {
				allModes = true;
				ma.getDashboard().updateListAdapter(createListAdapter(ma));
			}
		});
		createLayersItems(adapter, ma);
		createRenderingAttributeItems(adapter, ma);
		adapter.item(R.string.layer_map_appearance).
			iconColor(R.drawable.ic_configure_screen_dark).listen(new OnContextMenuClick() {
				@Override
				public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
					ma.getDashboard().setDashboardVisibility(true, DashboardType.CONFIGURE_SCREEN);
					return false;
				}
			}).reg();
		
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
			if(itemId == R.string.layer_poi) {
				selectPOILayer(ma.getMyApplication().getSettings());
				return false;
			} else if(itemId == R.string.layer_gpx_layer && cm.getSelection(pos) == 1) {
				ma.getMapLayers().showGPXFileLayer(getAlreadySelectedGpx(), ma.getMapView());
				return false;
			} else  {
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
					dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialogInterface) {
							cm.setSelection(pos, 0);
							adapter.notifyDataSetChanged();
						}
					});
				}
			} else if (itemId == R.string.layer_map) {
				if(OsmandPlugin.getEnabledPlugin(OsmandRasterMapsPlugin.class) == null) {
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
			final PoiLegacyFilter[] selected = new PoiLegacyFilter[1];
			AlertDialog dlg = ma.getMapLayers().selectPOIFilterLayer(ma.getMapView(), selected);
			dlg.setOnDismissListener(new DialogInterface.OnDismissListener() {

				@Override
				public void onDismiss(DialogInterface dialog) {
					ma.getDashboard().refreshContent(true);
				}
			});
		}
	}

	private void createLayersItems(ContextMenuAdapter adapter , MapActivity activity) {
		OsmandApplication app = activity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		LayerMenuListener l = new LayerMenuListener(activity, adapter);
		adapter.item(R.string.shared_string_show).setCategory(true).layout(R.layout.drawer_list_sub_header).reg();
		// String appMode = " [" + settings.getApplicationMode().toHumanString(view.getApplication()) +"] ";
		adapter.item(R.string.layer_poi).selected(settings.SELECTED_POI_FILTER_FOR_MAP.get() != null ? 1 : 0)
				.iconColor(R.drawable.ic_action_info_dark).listen(l).reg();
		adapter.item(R.string.layer_amenity_label).selected(settings.SHOW_POI_LABEL.get() ? 1 : 0)
				.iconColor(R.drawable.ic_action_text_dark).listen(l).reg();
		adapter.item(R.string.shared_string_favorites).selected(settings.SHOW_FAVORITES.get() ? 1 : 0)
				.iconColor(R.drawable.ic_action_fav_dark).listen(l).reg();
		adapter.item(R.string.layer_gpx_layer).selected(
				app.getSelectedGpxHelper().isShowingAnyGpxFiles() ? 1 : 0)
				.iconColor(R.drawable.ic_action_polygom_dark)
				.listen(l).reg();
		adapter.item(R.string.layer_map).iconColor(R.drawable.ic_world_globe_dark)
				.listen(l).reg();
		if(TransportRouteHelper.getInstance().routeIsCalculated()){
			adapter.item(R.string.layer_transport_route).selected(1)
				.iconColor(R.drawable.ic_action_bus_dark).listen(l).reg();
		}
		
		OsmandPlugin.registerLayerContextMenu(activity.getMapView(), adapter, activity);
		app.getAppCustomization().prepareLayerContextMenu(activity, adapter);
	}

	protected void refreshMapComplete(final MapActivity activity) {
		activity.getMyApplication().getResourceManager().getRenderer().clearCache();
		activity.updateMapSettings();
		activity.getMapView().refreshMap(true);
	}
	
	private void createRenderingAttributeItems(final ContextMenuAdapter adapter, final MapActivity activity) {
		adapter.item(R.string.map_widget_map_rendering).setCategory(true).layout(R.layout.drawer_list_sub_header).reg();
		String descr = getRenderDescr(activity);
		adapter.item(R.string.map_widget_renderer).listen(new OnContextMenuClick() {
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
		}).description(descr).layout(R.layout.drawer_list_doubleitem).reg();

		adapter.item(R.string.map_widget_day_night).description(getDayNightDescr(activity)).listen(new OnContextMenuClick() {
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
						adapter.setItemDescription(pos, getDayNightDescr(activity));
						ad.notifyDataSetInvalidated();
					}
				});
				bld.show();
				return false;
			}
		}).layout(R.layout.drawer_list_doubleitem).reg();

		adapter.item(R.string.map_magnifier).listen(new OnContextMenuClick() {
			@Override
			public boolean onContextMenuClick(final ArrayAdapter<?> ad, int itemId, final int pos, boolean isChecked) {
				final OsmandMapTileView view = activity.getMapView();
				final OsmandSettings.OsmandPreference<Float> mapDensity = view.getSettings().MAP_DENSITY;
				final AlertDialog.Builder bld = new AlertDialog.Builder(view.getContext());
				int p = (int) (mapDensity.get() * 100);
				final TIntArrayList tlist = new TIntArrayList(new int[] { 33, 50, 75, 100, 150, 200, 300, 400 });
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
		}).description(String.format("%.0f", 100f * activity.getMyApplication().getSettings().MAP_DENSITY.get()) + " %").layout(R.layout.drawer_list_doubleitem).reg();

		adapter.item(R.string.text_size).listen(new OnContextMenuClick() {
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
		}).description(getScale(activity)).layout(R.layout.drawer_list_doubleitem).reg();
		
		adapter.item(R.string.map_locale).listen(new OnContextMenuClick() {
			@Override
			public boolean onContextMenuClick(final ArrayAdapter<?> ad, int itemId, final int pos, boolean isChecked) {
				final OsmandMapTileView view = activity.getMapView();
				AlertDialog.Builder b = new AlertDialog.Builder(view.getContext());
				// test old descr as title
				b.setTitle(R.string.map_preferred_locale);
				final String[] txtValues = mapNamesIds;
				final String[] txtNames = getMapNamesValues(activity);
				int selected = -1;
				for (int i = 0; i < txtValues.length; i++) {
					if(view.getSettings().MAP_PREFERRED_LOCALE.get().equals(txtValues[i])) {
						selected = i;
						break;
					}
				}
				b.setSingleChoiceItems(txtNames, selected, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						view.getSettings().MAP_PREFERRED_LOCALE.set(txtValues[which]);
						refreshMapComplete(activity);
						adapter.setItemDescription(pos, txtValues[which]);
						ad.notifyDataSetInvalidated();
						dialog.dismiss();
					}
				});
				b.show();
				return false;
			}
		}).description(activity.getMyApplication().getSettings().MAP_PREFERRED_LOCALE.get()).layout(R.layout.drawer_list_doubleitem).reg();

		RenderingRulesStorage renderer = activity.getMyApplication().getRendererRegistry().getCurrentSelectedRenderer();
		if (renderer != null) {
			List<RenderingRuleProperty> customRules = new ArrayList<RenderingRuleProperty>(renderer.PROPS.getCustomRules());
			
			createProperties(customRules, R.string.rendering_category_transport, "transport",
					adapter, activity);
			createProperties(customRules, R.string.rendering_category_details, "details",
					adapter, activity);
			createProperties(customRules, R.string.rendering_category_hide, "hide",
					adapter, activity);
			createProperties(customRules, R.string.rendering_category_routes, "routes",
					adapter, activity);
			
			if(customRules.size() > 0) {
				adapter.item(R.string.rendering_category_others).setCategory(true).layout(R.layout.drawer_list_sub_header).reg();
				createCustomRenderingProperties(adapter, activity, customRules);
			}
		}
	}
	
	static String[] mapNamesIds = new String[] { "", "en", "ar", "be", "ca", "cs", "da", "de", "el", "es", "fi", "fr", "he", "hi",
			"hr", "hu", "it", "ja", "ko", "lt", "lv", "nl", "pl", "ro", "ru", "sk", "sl", "sv", "sw", "zh" };

	private String[] getMapNamesValues(Context ctx) {
		return new String[] { ctx.getString(R.string.local_map_names), ctx.getString(R.string.lang_en),
				ctx.getString(R.string.lang_ar),
				ctx.getString(R.string.lang_be), ctx.getString(R.string.lang_ca), ctx.getString(R.string.lang_cs),
				ctx.getString(R.string.lang_da), ctx.getString(R.string.lang_de), ctx.getString(R.string.lang_el),
				ctx.getString(R.string.lang_es), ctx.getString(R.string.lang_fi), ctx.getString(R.string.lang_fr),
				ctx.getString(R.string.lang_he), ctx.getString(R.string.lang_hi), ctx.getString(R.string.lang_hr),
				ctx.getString(R.string.lang_hu), ctx.getString(R.string.lang_it), ctx.getString(R.string.lang_ja),
				ctx.getString(R.string.lang_ko), ctx.getString(R.string.lang_lt),
				ctx.getString(R.string.lang_lv), ctx.getString(R.string.lang_nl),
				ctx.getString(R.string.lang_pl), ctx.getString(R.string.lang_ro), ctx.getString(R.string.lang_ru),
				ctx.getString(R.string.lang_sk), ctx.getString(R.string.lang_sl), ctx.getString(R.string.lang_sv),
				ctx.getString(R.string.lang_sw), ctx.getString(R.string.lang_zh) };
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
		if(prefs.size() > 0) {
			final String descr = getDescription(prefs);
			adapter.item(strId).description(descr).
					layout(R.layout.drawer_list_doubleitem).listen(new OnContextMenuClick() {
						
						@Override
						public boolean onContextMenuClick(ArrayAdapter<?> a, int itemId, int pos, boolean isChecked) {
							showPreferencesDialog(adapter, a, pos, activity, activity.getString(strId), ps, prefs);
							return false;
						}
					}).reg();
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
		Builder bld = new AlertDialog.Builder(activity);
		boolean[] checkedItems = new boolean[prefs.size()];
		for (int i = 0; i < prefs.size(); i++) {
			checkedItems[i] = prefs.get(i).get();
		}
		
		final boolean[] tempPrefs = new boolean[prefs.size()];
		for (int i = 0; i < prefs.size(); i++) {
			tempPrefs[i] = prefs.get(i).get();
		}
		final String[] vals = new String[ps.size()];
		for(int i = 0; i < ps.size(); i++) {
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
		int scale = (int)(activity.getMyApplication().getSettings().TEXT_SCALE.get() * 100);
		return scale + " %";
	}
	
	
	private void createCustomRenderingProperties(final ContextMenuAdapter adapter , final MapActivity activity,
			List<RenderingRuleProperty> customRules ){
		final OsmandMapTileView view = activity.getMapView();
		for (final RenderingRuleProperty p : customRules) {
			if (p.getAttrName().equals(RenderingRuleStorageProperties.A_APP_MODE) ||
					p.getAttrName().equals(RenderingRuleStorageProperties.A_ENGINE_V1)){
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
				adapter.item(propertyName).listen(new OnContextMenuClick() {

					@Override
					public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
						pref.set(!pref.get());
						refreshMapComplete(activity);
						return false;
					}
				}).selected(pref.get() ? 1 : 0).reg();
			} else {
				final OsmandSettings.CommonPreference<String> pref = view.getApplication().getSettings()
						.getCustomRenderProperty(p.getAttrName());
				String descr = SettingsActivity.getStringPropertyValue(activity, pref.get());
				adapter.item(propertyName).listen(new OnContextMenuClick() {

					@Override
					public boolean onContextMenuClick(final ArrayAdapter<?> ad, int itemId, final int pos, boolean isChecked) {
						AlertDialog.Builder b = new AlertDialog.Builder(view.getContext());
						// test old descr as title
						b.setTitle(propertyDescr);

						int i = Arrays.asList(p.getPossibleValues()).indexOf(pref.get());

						String[] possibleValuesString = new String[p.getPossibleValues().length];

						for (int j = 0; j < p.getPossibleValues().length; j++) {
							possibleValuesString[j] = SettingsActivity.getStringPropertyValue(view.getContext(),
									p.getPossibleValues()[j]);
						}

						b.setSingleChoiceItems(possibleValuesString, i, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								pref.set(p.getPossibleValues()[which]);
								refreshMapComplete(activity);
								adapter.setItemDescription(pos, SettingsActivity.getStringPropertyValue(activity, pref.get()));
								dialog.dismiss();
								ad.notifyDataSetInvalidated();
							}
						});
						b.show();
						return false;
					}
				}).description(descr).layout(R.layout.drawer_list_doubleitem).reg();
			}
		}
	}

}
