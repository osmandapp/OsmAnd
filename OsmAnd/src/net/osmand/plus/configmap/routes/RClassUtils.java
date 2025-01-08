package net.osmand.plus.configmap.routes;

import static net.osmand.render.RenderingRuleStorageProperties.ATTR_COLOR_VALUE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.configmap.RouteLegendCard;
import net.osmand.render.RenderingClass;
import net.osmand.render.RenderingRule;
import net.osmand.render.RenderingRulesStorage;

import java.util.ArrayList;
import java.util.List;

public class RClassUtils {
    public enum RClassType {
        HIKING_OSMC_NODES(".route.hiking.osmc_nodes");

        private final String path;

        RClassType(@NonNull String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }

    public enum RClassColors {
        IWN(".iwn", "iwnColor"),
        NWN(".nwn", "nwnColor"),
        RWN(".rwn", "rwnColor"),
        LWN(".lwn", "lwnColor");

        private final String className;
        private final String colorName;

        RClassColors(String className, String colorName) {
            this.className = className;
            this.colorName = colorName;
        }

        @Nullable
        public static String getColorName(String className) {
            for (RClassColors RClassColors : values()) {
                if (RClassColors.className.equals(className)) {
                    return RClassColors.colorName;
                }
            }
            return null;
        }
    }

    public static List<RouteLegendCard.DataClass> getDataClasses(@NonNull OsmandApplication app,
                                                                 @NonNull RClassType rClassType){
        List<RouteLegendCard.DataClass> dataClasses = new ArrayList<>();

        RenderingRulesStorage routeRender = app.getRendererRegistry().getCurrentSelectedRenderer();
        if (routeRender != null) {
            RenderingClass rClass = routeRender.getRenderingClass(rClassType.getPath());
            if (rClass != null) {
                for (RenderingClass children : rClass.getChildren()) {
                    String colorName = RClassColors.getColorName(children.getName());
                    Integer color = parseColor(routeRender, colorName);
                    if (color != null) {
                        dataClasses.add(new RouteLegendCard.DataClass(children.getTitle(), color));
                    }
                }
            }
        }
        return dataClasses;
    }

    public static Integer parseColor(@NonNull RenderingRulesStorage routeRender, @Nullable String colorName) {
        if (colorName == null) {
            return null;
        }
        Integer color = null;

        RenderingRule colorRule = routeRender.getRenderingAttributeRule(colorName);
        List<RenderingRule> rules = colorRule.getIfElseChildren();
        for (RenderingRule rule : rules) {
            int colorValue = rule.getIntPropertyValue(ATTR_COLOR_VALUE);
            if (colorValue != -1) {
                color = colorValue;
            }
        }
        return color;
    }
}
