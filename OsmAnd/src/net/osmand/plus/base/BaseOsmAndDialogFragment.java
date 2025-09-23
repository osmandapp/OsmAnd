package net.osmand.plus.base;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.dialog.IOsmAndFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.InsetsUtils;
import net.osmand.plus.utils.InsetsUtils.InsetSide;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class BaseOsmAndDialogFragment extends DialogFragment implements IOsmAndFragment, ISupportInsets {

	protected OsmandApplication app;
	protected OsmandSettings settings;
	protected ApplicationMode appMode;
	protected UiUtilities iconsCache;
	protected boolean nightMode;

	private LayoutInflater themedInflater;

	private WindowInsetsCompat lastRootInsets = null;

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
	public void onStart() {
		super.onStart();

		Dialog dialog = getDialog();
		if (dialog != null && dialog.getWindow() != null && Build.VERSION.SDK_INT > 29) {
			if (Build.VERSION.SDK_INT >= 36) {
				//WindowCompat.enableEdgeToEdge(window);
			} else {
				WindowCompat.setDecorFitsSystemWindows(dialog.getWindow(), false);
			}
		}
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		Dialog dialog = getDialog();
		if (dialog != null && dialog.getWindow() != null && Build.VERSION.SDK_INT > 29) {
			InsetsUtils.processInsets(this, dialog.getWindow().getDecorView(), view);
		} else {
			InsetsUtils.processInsets(this, view, null);
		}
	}

	@Nullable
	@Override
	public Set<InsetSide> getRootInsetSides() {
		return EnumSet.of(InsetSide.TOP);
	}

	@Nullable
	@Override
	public List<Integer> getScrollableViewIds() {
		List<Integer> ids = new ArrayList<>();
		ids.add(android.R.id.list);
		ids.add(R.id.scroll_view);
		return ids;
	}

	@Nullable
	@Override
	public List<Integer> getFabIds() {
		return null;
	}

	public void onApplyInsets(@NonNull WindowInsetsCompat insets) {

	}

	@Nullable
	@Override
	public WindowInsetsCompat getLastRootInsets() {
		return lastRootInsets;
	}

	@Override
	public void setLastRootInsets(@NonNull WindowInsetsCompat rootInsets) {
		lastRootInsets = rootInsets;
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
