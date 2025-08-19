package net.osmand.plus.base;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.DialogFragment;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.dialog.IOsmAndFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

public class BaseOsmAndDialogFragment extends DialogFragment implements IOsmAndFragment {

	protected OsmandApplication app;
	protected OsmandSettings settings;
	protected ApplicationMode appMode;
	protected UiUtilities iconsCache;
	protected boolean nightMode;

	private LayoutInflater themedInflater;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (OsmandApplication) requireActivity().getApplication();
		settings = app.getSettings();
		iconsCache = app.getUIUtilities();
		appMode = restoreAppMode(app, appMode, savedInstanceState, getArguments());
		updateNightMode();
	}

	@SuppressLint("UseGetLayoutInflater")
	protected void updateNightMode() {
		nightMode = resolveNightMode();
		Context themedCtx = new ContextThemeWrapper(requireActivity(), getDialogThemeId());
		themedInflater = LayoutInflater.from(themedCtx);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		AndroidUtils.addStatusBarPadding21v(null, view);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		saveAppModeToBundle(appMode, outState);
	}

	@StyleRes
	protected int getDialogThemeId() {
		return nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
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

	@NonNull
	@Override
	public ThemeUsageContext getThemeUsageContext() {
		return ThemeUsageContext.valueOf(isUsedOnMap());
	}

	protected boolean isUsedOnMap() {
		return false;
	}

	public final void setAppMode(@Nullable ApplicationMode appMode) {
		this.appMode = appMode;
	}

	@NonNull
	public final ApplicationMode getAppMode() {
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
}
