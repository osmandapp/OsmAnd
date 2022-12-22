package net.osmand.render;

import net.osmand.util.Algorithms;
import org.xmlpull.v1.XmlPullParserException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class RendererRulesStorageAmenityFetcher {

    public static Map<String, String> main(String[] args) throws XmlPullParserException, IOException {
        String styleName = args.length > 0 ? args[0] : "default";
        String path = getStylePath(styleName);
        RenderingRulesStorage storage = RenderingRulesStorage.getTestStorageForStyle(path);

        Map<String, String> iconStyle = null;
        if(args.length > 5) {
            iconStyle = getAmenityIconStyle(args[1], args[2], args[3],args[4], args[5].equals("true"), storage);
        } else {
            // for manual testing case
            // iconStyle = getAmenityIconStyle("amenity", "fuel", null, null, false, storage);
        }
        return iconStyle;
    }

    private static String getStylePath(String styleName) {
        //String stylesDirPath = "../resources/rendering_styles/";
        String stylesDirPath = "/Users/nnngrach/Documents/Projects/Coding/OsmAnd/resources/rendering_styles/";

        String[] allStylesNames = new String[] {"default", "desert", "LightRS", "mapnik", "nautical",
                "offroad", "osm-carto", "regions", "skimap", "snowmobile", "standalone-template",
                "Topo-map-assimilation", "topo", "Touring-view_(more-contrast-and-details)", "UniRS",
                "weather.addon"};
        if (Algorithms.isEmpty(styleName) || !Arrays.asList(allStylesNames).contains(styleName)) {
            styleName = "default";
        }
        return stylesDirPath + styleName + ".render.xml";
    }

    public static Map<String, String> getAmenityIconStyle(String tag, String value, String tag2, String value2,
                                                          boolean nightMode, RenderingRulesStorage storage)  throws XmlPullParserException, IOException {

        RenderingRuleSearchRequest searchRequest = new RenderingRuleSearchRequest(storage);

        searchRequest.setStringFilter(storage.PROPS.R_TAG, tag);
        searchRequest.setStringFilter(storage.PROPS.R_VALUE, value);

        if (!Algorithms.isEmpty(tag2) && !Algorithms.isEmpty(value2)) {
            searchRequest.setStringFilter(storage.PROPS.R_ADDITIONAL, tag2 + "=" + value2);
        }

        searchRequest.setBooleanFilter(storage.PROPS.R_NIGHT_MODE, nightMode);
        searchRequest.setIntFilter(storage.PROPS.R_MINZOOM, 19);
        searchRequest.setIntFilter(storage.PROPS.R_MAXZOOM, 19);
        for (RenderingRuleProperty customProp : storage.PROPS.getCustomRules()) {
            if (customProp.isBoolean()) {
                searchRequest.setBooleanFilter(customProp, false);
            } else {
                searchRequest.setStringFilter(customProp, "");
            }
        }

        searchRequest.search(RenderingRulesStorage.POINT_RULES);

        Map<String, String> result = new HashMap<String, String>();
        result.put("iconName", searchRequest.getStringPropertyValue(searchRequest.ALL.R_ICON));
        result.put("shieldName", searchRequest.getStringPropertyValue(searchRequest.ALL.R_SHIELD));
        result.put("iconSize", String.valueOf(searchRequest.getFloatPropertyValue(searchRequest.ALL.R_ICON_VISIBLE_SIZE, -1)));
        //System.out.println("RendererRulesStorageAmenityFetcher result: " + result);
        return result;
    }

}
