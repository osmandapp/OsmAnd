package net.osmand.plus.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.ListFragment;

import net.osmand.plus.base.dialog.IOsmAndFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.UiUtilities;

/**
 * Base fragment class for list-based screens in the OsmAnd application
 * that directly inherit from Android's {@link androidx.fragment.app.ListFragment}.
 * <p>
 * This class ensures consistent access to essential application-level components such as
 * {@link OsmandApplication}, {@link OsmandSettings}, and {@link UiUtilities}, and also
 * handles theming (day/night mode) based on the current {@link ApplicationMode}.
 * <p>
 * Every list fragment in the app that would otherwise directly extend {@link ListFragment}
 * must instead extend this class (or one of its descendants like {@link BaseNestedListFragment})
 * to ensure proper UI behavior and access to shared resources.
 */
public abstract class BaseOsmAndListFragment extends ListFragment implements IOsmAndFragment {

	protected OsmandApplication app;
	protected ApplicationMode appMode;
	protected OsmandSettings settings;
	protected UiUtilities iconsCache;
	protected boolean nightMode;

	private LayoutInflater themedInflater;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (OsmandApplication) requireActivity().getApplication();
		settings = app.getSettings();
		iconsCache = app.getUIUtilities();
		appMode = restoreAppMode(app, appMode, savedInstanceState, getArguments());
		updateNightMode();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getListView().setBackgroundColor(getBackgroundColor());
	}

	protected void updateNightMode() {
		nightMode = resolveNightMode();
		themedInflater = UiUtilities.getInflater(requireContext(), nightMode);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		saveAppModeToBundle(appMode, outState);
	}

	@NonNull
	@Override
	public OsmandApplication getApp() {
		return app;
	}

	@NonNull
	@Override
	public LayoutInflater getThemedInflater() {
		return themedInflater;
	}

	protected boolean isUsedOnMap() {
		return false;
	}

	@NonNull
	@Override
	public ThemeUsageContext getThemeUsageContext() {
		return ThemeUsageContext.valueOf(isUsedOnMap());
	}

	public void setAppMode(@NonNull ApplicationMode appMode) {
		this.appMode = appMode;
	}

	@NonNull
	public ApplicationMode getAppMode() {
		return appMode;
	}

	@NonNull
	@Override
	public UiUtilities getIconsCache() {
		return iconsCache;
	}

	public boolean isNightMode() {
		return nightMode;
	}
	
	public abstract ArrayAdapter<?> getAdapter();

	@ColorInt
	protected int getBackgroundColor() {
		return ColorUtilities.getListBgColor(app, nightMode);
	}
	
}
