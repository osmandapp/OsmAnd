package net.osmand.access.tasker;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.jcip.annotations.ThreadSafe;

@ThreadSafe
public final class PluginBundleValues {

	@NonNull
	public static final String BUNDLE_EXTRA_BOOLEAN_TEST
			= "net.osmand.access.tasker.extra.BUNDLE_EXTRA_BOOLEAN_TEST"; //$NON-NLS-1$

	public static boolean isBundleValid(@Nullable final Bundle bundle) {
		if (null == bundle) {
			return false;
		}

		// Probaby need stronger constraint
		return bundle.containsKey(BUNDLE_EXTRA_BOOLEAN_TEST);
	}

	@NonNull
	public static Bundle generateBundle(final boolean testValue) {
		final Bundle result = new Bundle();
		result.putBoolean(BUNDLE_EXTRA_BOOLEAN_TEST, testValue);
		return result;
	}

	private PluginBundleValues() {
		throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
	}
}