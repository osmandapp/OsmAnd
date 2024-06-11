package net.osmand.plus.helpers;

import android.os.AsyncTask;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.ColorPalette;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.srtm.CollectColorPalletTask;
import net.osmand.plus.plugins.srtm.CollectColorPalletTask.CollectColorPalletListener;
import net.osmand.plus.plugins.srtm.TerrainMode;
import net.osmand.router.RouteColorize;
import net.osmand.router.RouteColorize.ColorizationType;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ColorPaletteHelper {

    public static final String EXT = ".txt";
    public static final String ROUTE_PREFIX = "route_";
    public static final String GRADIENT_ID_SPLITTER = "_";

    private final OsmandApplication app;
    private final ConcurrentHashMap<String, ColorPalette> cachedTerrainModeColorPalette = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ColorPalette> cachedRouteColorPalette = new ConcurrentHashMap<>();


    public ColorPaletteHelper(@NonNull OsmandApplication app) {
        this.app = app;
    }

    public Map<String, Pair<ColorPalette, Long>> getPalletsForType(@NonNull ColorizationType colorizationType) {
        Map<String, Pair<ColorPalette, Long>> colorPalettes = new HashMap<>();
        File colorPalletsDir = getColorPaletteDir();
        File[] colorFiles = colorPalletsDir.listFiles();
        if (colorFiles != null) {
            String colorTypePrefix = ROUTE_PREFIX + colorizationType.name().toLowerCase() + GRADIENT_ID_SPLITTER;
            for (File file : colorFiles) {
                String fileName = file.getName();
                if (fileName.startsWith(colorTypePrefix) && fileName.endsWith(EXT)) {
                    String colorPalletName = fileName.replace(colorTypePrefix, "").replace(EXT, "");
                    ColorPalette colorPalette = getGradientColorPalette(file);
                    colorPalettes.put(colorPalletName, new Pair<>(colorPalette, file.lastModified()));
                }
            }
        }
        return colorPalettes;
    }

    private boolean isValidPalette(ColorPalette palette) {
        return palette != null && palette.getColors().size() >= 2;
    }

    private File getColorPaletteDir(){
        return app.getAppPath(IndexConstants.CLR_PALETTE_DIR);
    }

    @NonNull
    public ColorPalette requireRouteGradientPaletteSync(@NonNull ColorizationType colorizationType, @NonNull String gradientPaletteName) {
        ColorPalette colorPalette = getRouteColorPaletteSync(colorizationType, gradientPaletteName);
        return isValidPalette(colorPalette) ? colorPalette : RouteColorize.getDefaultPalette(colorizationType);
    }

    @Nullable
    public ColorPalette getRouteColorPaletteSync(@NonNull ColorizationType colorizationType, @NonNull String gradientPaletteName) {
        String colorPaletteFileName = ROUTE_PREFIX + colorizationType.name().toLowerCase() + GRADIENT_ID_SPLITTER + gradientPaletteName + EXT;

        ColorPalette colorPalette = cachedRouteColorPalette.get(colorPaletteFileName);
        if (colorPalette != null) {
            return colorPalette;
        }

        File mainColorFile = new File(getColorPaletteDir(), colorPaletteFileName);
        colorPalette = getGradientColorPalette(mainColorFile);

        if (colorPalette != null) {
            cachedRouteColorPalette.put(colorPaletteFileName, colorPalette);
        }
        return colorPalette;
    }

    @Nullable
    public ColorPalette getGradientColorPalette(@NonNull File colorPaletteFile) {
        ColorPalette colorPalette = null;
        try {
            if (colorPaletteFile.exists()) {
                colorPalette = ColorPalette.parseColorPalette(new FileReader(colorPaletteFile));
            }
        } catch (IOException e) {
            PlatformUtil.getLog(ColorPaletteHelper.class).error("Error reading color file ",
                    e);
        }

        return colorPalette;
    }

    public void getTerrainModeColorPaletteAsync(@NonNull String modeKey, @NonNull CollectColorPalletListener listener) {
        TerrainMode mode = TerrainMode.getByKey(modeKey);
        File mainColorFile = new File(getColorPaletteDir(), mode.getMainFile());

        ColorPalette colorPalette = cachedTerrainModeColorPalette.get(modeKey);
        if (colorPalette != null) {
            listener.collectingPalletFinished(colorPalette);
        } else {
            getGradientColorPaletteAsync(mainColorFile, new CollectColorPalletListener() {

                @Override
                public void collectingPalletStarted() {
                    listener.collectingPalletStarted();
                }

                @Override
                public void collectingPalletFinished(@Nullable ColorPalette colorPalette) {
                    if (colorPalette != null) {
                        cachedTerrainModeColorPalette.put(modeKey, colorPalette);
                    }
                    listener.collectingPalletFinished(colorPalette);
                }
            });
        }
    }

    private void getGradientColorPaletteAsync(@NonNull File colorPaletteFile, @NonNull CollectColorPalletListener listener){
        CollectColorPalletTask collectColorPalletTask = new CollectColorPalletTask(app, colorPaletteFile, listener);
        collectColorPalletTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
