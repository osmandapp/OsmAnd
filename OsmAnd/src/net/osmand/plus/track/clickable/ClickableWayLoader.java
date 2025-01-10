package net.osmand.plus.track.clickable;

import static net.osmand.data.MapObject.AMENITY_ID_RIGHT_SHIFT;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.binary.ObfConstants;
import net.osmand.core.jni.ObfMapObject;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer;

import java.util.Map;
import java.util.Set;

// TODO cache <id, GpxFile>
// TODO activity type (by tags)
// TODO GpxFile naming (tag-defined + name)
// TODO fetch elevation from routing-section
// TODO calc distance stats, elevation stats, etc (automatically if data exists)
// THINK auto-reverse Way (assume downhill OR detect start by minDist to currentLocation)

public class ClickableWayLoader {
    public static final Set<String> clickableTags = Set.of("piste:type", "mtb:scale", "dirtbike:scale");
    public static final Map<String, String> forbiddenTags = Map.of("area", "yes", "access", "no");

    private final ClickableWayActivator activator;

    public ClickableWayLoader(@NonNull OsmandApplication app, @NonNull OsmandMapTileView view) {
        this.activator = new ClickableWayActivator(app, view);
    }

    @NonNull
    public ContextMenuLayer.IContextMenuProvider getContextMenuProvider() {
        return activator;
    }

    public boolean isClickableWayTags(@NonNull Map<String, String> tags) {
        for (Map.Entry<String, String> forbidden : forbiddenTags.entrySet()) {
            if (forbidden.getValue().equals(tags.get(forbidden.getKey()))) {
                return false;
            }
        }
        for (String key : tags.keySet()) {
            if (clickableTags.contains(key)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public ClickableWay searchClickableWay(long osmId, @Nullable String name, @NonNull Map<String, String> tags,
                                           @NonNull LatLon searchLatLon) {
        return new ClickableWay(osmId, name, tags, searchLatLon); // TODO test
    }
}
