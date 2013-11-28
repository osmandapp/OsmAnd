package net.osmand.plus.views.mapwidgets;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Toast;
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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class AppearanceWidgetsFactory {

    public static AppearanceWidgetsFactory INSTANCE = new AppearanceWidgetsFactory();
    private String ADDITIONAL_VECTOR_RENDERING_CATEGORY;
    public static boolean EXTRA_SETTINGS = true;


    public void registerAppearanceWidgets(final MapActivity map, final MapInfoLayer mapInfoLayer,
                                          final MapWidgetRegistry mapInfoControls) {
        final OsmandMapTileView view = map.getMapView();


        final MapWidgetRegistry.MapWidgetRegInfo displayViewDirections = mapInfoControls.registerAppearanceWidget(R.drawable.widget_viewing_direction, R.string.map_widget_view_direction,
                "viewDirection", view.getSettings().SHOW_VIEW_ANGLE);
        displayViewDirections.setStateChangeListener(new Runnable() {
            @Override
            public void run() {
                view.getSettings().SHOW_VIEW_ANGLE.set(!view.getSettings().SHOW_VIEW_ANGLE.get());
                map.getMapViewTrackingUtilities().updateSettings();
            }
        });

        if (EXTRA_SETTINGS) {
            // previous extra settings

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

//            final OsmandSettings.OsmandPreference<Integer> posPref = view.getSettings().POSITION_ON_MAP;
//            final MapWidgetRegistry.MapWidgetRegInfo posMap = mapInfoControls.registerAppearanceWidget(R.drawable.widget_position_marker, R.string.position_on_map,
//                    "position_on_map", textSizePref);
//            posMap.setStateChangeListener(new Runnable() {
//                @Override
//                public void run() {
//                    String[]  entries = new String[] {map.getString(R.string.position_on_map_center), map.getString(R.string.position_on_map_bottom) };
//                    final Integer[] vals = new Integer[] { OsmandSettings.CENTER_CONSTANT, OsmandSettings.BOTTOM_CONSTANT };
//                    AlertDialog.Builder b = new AlertDialog.Builder(view.getContext());
//                    int i = Arrays.binarySearch(vals, posPref.get());
//                    b.setSingleChoiceItems(entries, i, new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            posPref.set(vals[which]);
//                            map.updateApplicationModeSettings();
//                            view.refreshMap(true);
//                            dialog.dismiss();
//                        }
//                    });
//                    b.show();
//                }
//            });

        }

        final MapWidgetRegistry.MapWidgetRegInfo vectorRenderer = mapInfoControls.registerAppearanceWidget(R.drawable.widget_rendering_style, R.string.map_widget_renderer,
                "renderer", view.getSettings().RENDERER, map.getString(R.string.map_widget_map_rendering));
        final OsmandApplication app = view.getApplication();
        vectorRenderer.setStateChangeListener(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder bld = new AlertDialog.Builder(view.getContext());
                bld.setTitle(R.string.renderers);
                Collection<String> rendererNames = app.getRendererRegistry().getRendererNames();
                final String[] items = rendererNames.toArray(new String[rendererNames.size()]);
                int i = -1;
                for(int j = 0; j< items.length; j++) {
                    if(items[j].equals(app.getRendererRegistry().getCurrentSelectedRenderer().getName())) {
                        i = j;
                        break;
                    }
                }
                bld.setSingleChoiceItems(items, i, new DialogInterface.OnClickListener() {

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

        final MapWidgetRegistry.MapWidgetRegInfo dayNight = mapInfoControls.registerAppearanceWidget(R.drawable.widget_day_night_mode, R.string.map_widget_day_night,
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

            /*final OsmandSettings.OsmandPreference<Float> textSizePref = view.getSettings().MAP_TEXT_SIZE;
            final MapWidgetRegistry.MapWidgetRegInfo textSize = mapInfoControls.registerAppearanceWidget(R.drawable.widget_text_size, R.string.map_text_size,
                    "text_size", textSizePref, map.getString(R.string.map_widget_map_rendering));
            textSize.setStateChangeListener(new Runnable() {
                @Override
                public void run() {
                    final Float[] floatValues = new Float[] {0.6f, 0.8f, 1.0f, 1.2f, 1.5f, 1.75f, 2f};
                    String[] entries = new String[floatValues.length];
                    for (int i = 0; i < floatValues.length; i++) {
                        entries[i] = (int) (floatValues[i] * 100) +" %";
                    }
                    AlertDialog.Builder b = new AlertDialog.Builder(view.getContext());
                    b.setTitle(R.string.map_text_size);
                    int i = Arrays.binarySearch(floatValues, textSizePref.get());
                    b.setSingleChoiceItems(entries, i, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            textSizePref.set(floatValues[which]);
                            app.getResourceManager().getRenderer().clearCache();
                            view.refreshMap(true);
                            dialog.dismiss();
                        }
                    });
                    b.show();
                }
            });*/

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
                }
                MapWidgetRegistry.MapWidgetRegInfo w = mapInfoControls.registerAppearanceWidget(icon, propertyName, "rend_"+p.getAttrName(), pref, categoryName);
                w.setStateChangeListener(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder b = new AlertDialog.Builder(view.getContext());
                        //test old descr as title
                        b.setTitle(propertyDescr);
                        int i = Arrays.asList(p.getPossibleValues()).indexOf(pref.get());
                        b.setSingleChoiceItems(p.getPossibleValues(), i, new DialogInterface.OnClickListener() {
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
