package net.osmand.plus.views.mapwidgets;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import net.osmand.access.AccessibleToast;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Toast;

public class AppearanceWidgetsFactory {

    public static AppearanceWidgetsFactory INSTANCE = new AppearanceWidgetsFactory();
    private String ADDITIONAL_VECTOR_RENDERING_CATEGORY;
    public static boolean EXTRA_SETTINGS = true;
    public static boolean POSITION_ON_THE_MAP = false;


    public void registerAppearanceWidgets(final MapActivity map, final MapInfoLayer mapInfoLayer,
                                          final MapWidgetRegistry mapInfoControls) {
        final OsmandMapTileView view = map.getMapView();


//        final MapWidgetRegistry.MapWidgetRegInfo displayViewDirections = mapInfoControls.registerAppearanceWidget(R.drawable.widget_viewing_direction, R.string.map_widget_view_direction,
//                "viewDirection", view.getSettings().SHOW_VIEW_ANGLE);
//        displayViewDirections.setStateChangeListener(new Runnable() {
//            @Override
//            public void run() {
//                view.getSettings().SHOW_VIEW_ANGLE.set(!view.getSettings().SHOW_VIEW_ANGLE.get());
//                map.getMapViewTrackingUtilities().updateSettings();
//            }
//        });

        if (EXTRA_SETTINGS) {
            final MapWidgetRegistry.MapWidgetRegInfo showRuler = mapInfoControls.registerAppearanceWidget(R.drawable.widget_ruler, R.string.map_widget_show_ruler,
                    "showRuler", view.getSettings().SHOW_RULER);
            showRuler.setStateChangeListener(new Runnable() {
                @Override
                public void run() {
                    view.getSettings().SHOW_RULER.set(!view.getSettings().SHOW_RULER.get());
                    view.refreshMap();
                }
            });

            final MapWidgetRegistry.MapWidgetRegInfo showDestinationArrow = mapInfoControls.registerAppearanceWidget(R.drawable.widget_show_destination_arrow, R.string.map_widget_show_destination_arrow,
                    "show_destination_arrow", view.getSettings().SHOW_DESTINATION_ARROW);
            showDestinationArrow.setStateChangeListener(new Runnable() {
                @Override
                public void run() {
                    view.getSettings().SHOW_DESTINATION_ARROW.set(!view.getSettings().SHOW_DESTINATION_ARROW.get());
                    mapInfoLayer.recreateControls();
                }
            });

            final MapWidgetRegistry.MapWidgetRegInfo transparent = mapInfoControls.registerAppearanceWidget(R.drawable.widget_transparent_skin, R.string.map_widget_transparent,
                    "transparent", view.getSettings().TRANSPARENT_MAP_THEME);
            transparent.setStateChangeListener(new Runnable() {
                @Override
                public void run() {
                    view.getSettings().TRANSPARENT_MAP_THEME.set(!view.getSettings().TRANSPARENT_MAP_THEME.get());
                    mapInfoLayer.recreateControls();
                }
            });
            final MapWidgetRegistry.MapWidgetRegInfo centerPosition = mapInfoControls.registerAppearanceWidget(R.drawable.widget_position_marker,
            		R.string.always_center_position_on_map,
                    "centerPosition", view.getSettings().CENTER_POSITION_ON_MAP);
            centerPosition.setStateChangeListener(new Runnable() {
                @Override
                public void run() {
                    view.getSettings().CENTER_POSITION_ON_MAP.set(!view.getSettings().CENTER_POSITION_ON_MAP.get());
                    map.updateApplicationModeSettings();
					view.refreshMap(true);
                    mapInfoLayer.recreateControls();
                }
            });
        }

        final MapWidgetRegistry.MapWidgetRegInfo vectorRenderer = mapInfoControls.registerAppearanceWidget(R.drawable.widget_rendering_style, map.getString(R.string.map_widget_renderer),
                "renderer", view.getSettings().RENDERER, map.getString(R.string.map_widget_map_rendering));
        final OsmandApplication app = view.getApplication();
        vectorRenderer.setStateChangeListener(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder bld = new AlertDialog.Builder(view.getContext());
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
                            view.getSettings().RENDERER.set(renderer);
                            app.getRendererRegistry().setCurrentSelectedRender(loaded);
                            app.getResourceManager().getRenderer().clearCache();
                            view.refreshMap(true);
                        } else {
                            AccessibleToast.makeText(app, R.string.renderer_load_exception, Toast.LENGTH_SHORT).show();
                        }
                        if(EXTRA_SETTINGS) {
                            createCustomRenderingProperties(loaded, map, mapInfoLayer, mapInfoControls);
                        }
                        dialog.dismiss();
                    }
                });
                bld.show();
            }
        });

        final MapWidgetRegistry.MapWidgetRegInfo dayNight = mapInfoControls.registerAppearanceWidget(R.drawable.widget_day_night_mode, map.getString(R.string.map_widget_day_night),
                "dayNight", view.getSettings().DAYNIGHT_MODE, map.getString(R.string.map_widget_map_rendering));
        dayNight.setStateChangeListener(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder bld = new AlertDialog.Builder(view.getContext());
                bld.setTitle(R.string.daynight);
                final String[] items = new String[OsmandSettings.DayNightMode.values().length];
                for (int i = 0; i < items.length; i++) {
                    items[i] = OsmandSettings.DayNightMode.values()[i].toHumanString(map.getMyApplication());
                }
                int i = view.getSettings().DAYNIGHT_MODE.get().ordinal();
                bld.setSingleChoiceItems(items,  i, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        view.getSettings().DAYNIGHT_MODE.set(OsmandSettings.DayNightMode.values()[which]);
                        app.getResourceManager().getRenderer().clearCache();
                        view.refreshMap(true);
                        dialog.dismiss();
                    }
                });
                bld.show();
            }
        });
        RenderingRulesStorage renderer = app.getRendererRegistry().getCurrentSelectedRenderer();
        if(renderer != null && EXTRA_SETTINGS) {
            createCustomRenderingProperties(renderer, map, mapInfoLayer, mapInfoControls);
        }
    }

    private void createCustomRenderingProperties(RenderingRulesStorage renderer, final MapActivity map,
                                                 final MapInfoLayer mapInfoLayer, final MapWidgetRegistry mapInfoControls) {
        final OsmandMapTileView view = map.getMapView();
        if(ADDITIONAL_VECTOR_RENDERING_CATEGORY == null) {
            ADDITIONAL_VECTOR_RENDERING_CATEGORY = map.getString(R.string.map_widget_vector_attributes);
        }
        String categoryName = ADDITIONAL_VECTOR_RENDERING_CATEGORY;
        mapInfoControls.removeApperanceWidgets(categoryName);
        final OsmandApplication app = view.getApplication();
        List<RenderingRuleProperty> customRules = renderer.PROPS.getCustomRules();
        for (final RenderingRuleProperty p : customRules) {
            String propertyName = SettingsActivity.getStringPropertyName(view.getContext(), p.getAttrName(), p.getName());
            //test old descr as title
            final String propertyDescr = SettingsActivity.getStringPropertyDescription(view.getContext(), p.getAttrName(), p.getName());
            if(p.isBoolean()) {
                final OsmandSettings.CommonPreference<Boolean> pref = view.getApplication().getSettings().getCustomRenderBooleanProperty(p.getAttrName());
                int icon = 0;
                try {
                    Field f = R.drawable.class.getField("widget_" + p.getAttrName().toLowerCase());
                    icon = f.getInt(null);
                } catch(Exception e){
                    try {
                        Field f = R.drawable.class.getField("widget_no_icon");
                        icon = f.getInt(null);
                    } catch(Exception e0){
                    }
                }
                MapWidgetRegistry.MapWidgetRegInfo w = mapInfoControls.registerAppearanceWidget(icon, propertyName, "rend_"+p.getAttrName(), pref, categoryName);
                w.setStateChangeListener(new Runnable() {
                    @Override
                    public void run() {
                        pref.set(!pref.get());
                        app.getResourceManager().getRenderer().clearCache();
                        view.refreshMap(true);
                    }
                });

            } else {
                final OsmandSettings.CommonPreference<String> pref = view.getApplication().getSettings().getCustomRenderProperty(p.getAttrName());
                int icon = 0;
                try {
                    Field f = R.drawable.class.getField("widget_" + p.getAttrName().toLowerCase());
                    icon = f.getInt(null);
                } catch(Exception e){
                    try {
                        Field f = R.drawable.class.getField("widget_no_icon");
                        icon = f.getInt(null);
                    } catch(Exception e0){
                    }
                }
                MapWidgetRegistry.MapWidgetRegInfo w = mapInfoControls.registerAppearanceWidget(icon, propertyName, "rend_"+p.getAttrName(), pref, categoryName);
                w.setStateChangeListener(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder b = new AlertDialog.Builder(view.getContext());
                        //test old descr as title
                        b.setTitle(propertyDescr);

                        int i = Arrays.asList(p.getPossibleValues()).indexOf(pref.get());

                        String[] possibleValuesString = new String[p.getPossibleValues().length];
                        
                        for (int j = 0; j < p.getPossibleValues().length; j++) {
                            possibleValuesString[j] = SettingsActivity.getStringPropertyValue(view.getContext(), p.getPossibleValues()[j]);
                        }
                        
                        b.setSingleChoiceItems(possibleValuesString, i, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                pref.set(p.getPossibleValues()[which]);
                                app.getResourceManager().getRenderer().clearCache();
                                view.refreshMap(true);
                                dialog.dismiss();
                            }
                        });
                        b.show();
                    }
                });
            }
        }
    }
}
