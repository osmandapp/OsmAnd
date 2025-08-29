package net.osmand.plus.base;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.base.dialog.IOsmAndFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.InsetsUtils;
import net.osmand.plus.utils.InsetsUtils.InsetSide;
import net.osmand.plus.utils.UiUtilities;

import java.util.EnumSet;

/**
 * Base fragment class for all UI components in OsmAnd that directly extend Android's Fragment.
 *
 * This class provides centralized access to core components of the application,
 * such as the current Application Mode, UI theming (night mode, theme context),
 * and other shared utilities.
 *
 * All fragments in OsmAnd that would otherwise extend the standard Android Fragment
 * must instead extend this class (or one of its subclasses such as {@link BaseFullScreenFragment}
 * or {@link BaseNestedFragment}). This ensures proper theming behavior, access to application
 * services, and consistent lifecycle handling.
 *
 * Note: Fragments based on DialogFragment or BottomSheetFragment should NOT inherit from this class.
 */
public class BaseOsmAndFragment extends Fragment implements IOsmAndFragment {

	protected OsmandApplication app;
	protected ApplicationMode appMode;
	protected OsmandSettings settings;
	protected UiUtilities uiUtilities;
	protected boolean nightMode;

	private LayoutInflater themedInflater;
	protected WindowInsetsCompat lastRootInsets = null;
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (OsmandApplication) requireActivity().getApplication();
		settings = app.getSettings();
		uiUtilities = app.getUIUtilities();
		appMode = restoreAppMode(app, appMode, savedInstanceState, getArguments());
		updateNightMode();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		saveAppModeToBundle(appMode, outState);
	}


	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		Activity activity = requireActivity();
		if (activity instanceof MapActivity) {
			InsetsUtils.setWindowInsetsListener(view, (view1, insets) -> {
				InsetsUtils.applyPadding(view1, insets, getSideInsets());
				lastRootInsets = insets;
				onApplyInsets(insets);
			}, true);
		}
	}

	@Nullable
	protected EnumSet<InsetSide> getSideInsets(){
		return EnumSet.of(InsetSide.TOP, InsetSide.BOTTOM);
	}

	protected void onApplyInsets(@NonNull WindowInsetsCompat insets){

	}

	protected void updateNightMode() {
		nightMode = resolveNightMode();
		themedInflater = UiUtilities.getInflater(requireContext(), nightMode);
	}

	@NonNull
	@Override
	public LayoutInflater getThemedInflater() {
		return themedInflater;
	}

	@NonNull
	public ApplicationMode getAppMode() {
		return appMode;
	}

	public void setAppMode(@NonNull ApplicationMode appMode) {
		this.appMode = appMode;
	}

	public boolean isNightMode() {
		return nightMode;
	}

	protected boolean isUsedOnMap() {
		return false;
	}

	@Nullable
	protected OsmandActionBarActivity getMyActivity() {
		return (OsmandActionBarActivity) getActivity();
	}

	@NonNull
	protected OsmandActionBarActivity requireMyActivity() {
		return (OsmandActionBarActivity) requireActivity();
	}

	@NonNull
	@Override
	public ThemeUsageContext getThemeUsageContext() {
		return ThemeUsageContext.valueOf(isUsedOnMap());
	}

	@NonNull
	@Override
	public UiUtilities getIconsCache() {
		return uiUtilities;
	}

	@NonNull
	@Override
	public OsmandApplication getApp() {
		return app;
	}
}
