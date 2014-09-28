package net.osmand.core.android;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;

import net.osmand.core.jni.*;

// This class provides reverse mapping from 'embed-resources.list' to scheme used by current (legacy in future) OsmAnd for Android
public class CoreResourcesFromAndroid_LegacyOsmAndCompatible extends ICoreResourcesProvider {
    public CoreResourcesFromAndroid_LegacyOsmAndCompatible(final Context context) {
        _context = context;
    }

    private final Context _context;

    @Override
    public SWIGTYPE_p_QByteArray getResource(String name, float displayDensityFactor, SWIGTYPE_p_bool ok) {
        return SwigUtilities.emptyQByteArray();
    }

    @Override
    public SWIGTYPE_p_QByteArray getResource(String name, SWIGTYPE_p_bool ok) {
        return SwigUtilities.emptyQByteArray();
    }

    @Override
    public boolean containsResource(String name, float displayDensityFactor) {
        return false;
    }

    @Override
    public boolean containsResource(String name) {
        return false;
    }
}