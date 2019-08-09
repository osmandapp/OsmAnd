package net.osmand.plus.settings;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.SettingsBaseActivity;
import net.osmand.plus.activities.SettingsNavigationActivity;
import net.osmand.router.GeneralRouter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.osmand.plus.activities.SettingsNavigationActivity.getRouter;
import static net.osmand.plus.activities.SettingsNavigationActivity.setupSpeedSlider;

public class VehicleParametersFragment extends BaseSettingsFragment {

	public static final String TAG = "VehicleParametersFragment";

	@Override
	protected int getPreferenceResId() {
		return R.xml.vehicle_parameters;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);

		return view;
	}

	@Override
	protected int getToolbarResId() {
		return R.layout.profile_preference_toolbar;
	}

	protected String getToolbarTitle() {
		return getString(R.string.vehicle_parameters);
	}

	@Override
	protected void createUI() {
		PreferenceScreen screen = getPreferenceScreen();

		GeneralRouter router = getRouter(getMyApplication().getRoutingConfig(), getSelectedMode());
		if (router != null) {
			Map<String, GeneralRouter.RoutingParameter> parameters = router.getParameters();
			List<GeneralRouter.RoutingParameter> others = new ArrayList<GeneralRouter.RoutingParameter>();

			for (Map.Entry<String, GeneralRouter.RoutingParameter> e : parameters.entrySet()) {
				String param = e.getKey();
				GeneralRouter.RoutingParameter routingParameter = e.getValue();
				if (param.equals(GeneralRouter.VEHICLE_HEIGHT) || param.equals(GeneralRouter.VEHICLE_WEIGHT)) {
					others.add(routingParameter);
				}
			}

			for (GeneralRouter.RoutingParameter p : others) {
				Preference basePref;
				if (p.getType() == GeneralRouter.RoutingParameterType.BOOLEAN) {
					basePref = createSwitchPreference(settings.getCustomRoutingBooleanProperty(p.getId(), p.getDefaultBoolean()));
				} else {
					Object[] vls = p.getPossibleValues();
					String[] svlss = new String[vls.length];
					int i = 0;
					for (Object o : vls) {
						svlss[i++] = o.toString();
					}
					basePref = createListPreference(settings.getCustomRoutingProperty(p.getId(),
							p.getType() == GeneralRouter.RoutingParameterType.NUMERIC ? "0.0" : "-"),
							p.getPossibleValueDescriptions(), svlss,
							SettingsBaseActivity.getRoutingStringPropertyName(getActivity(), p.getId(), p.getName()),
							SettingsBaseActivity.getRoutingStringPropertyDescription(getActivity(), p.getId(), p.getDescription()));

					((ListPreference) basePref).setEntries(p.getPossibleValueDescriptions());
					((ListPreference) basePref).setEntryValues(svlss);
				}
				basePref.setTitle(SettingsBaseActivity.getRoutingStringPropertyName(getActivity(), p.getId(), p.getName()));
				basePref.setSummary(SettingsBaseActivity.getRoutingStringPropertyDescription(getActivity(), p.getId(), p.getDescription()));
				screen.addPreference(basePref);
			}
			GeneralRouter.GeneralRouterProfile routerProfile = router.getProfile();
			Preference defaultSpeed = findAndRegisterPreference("default_speed");
			if (routerProfile != GeneralRouter.GeneralRouterProfile.PUBLIC_TRANSPORT) {
				defaultSpeed.setOnPreferenceClickListener(this);
			} else {
				screen.removePreference(defaultSpeed);
			}
		}
	}

	private void showSeekbarSettingsDialog() {
		final ApplicationMode mode = settings.getApplicationMode();
		GeneralRouter router = getRouter(getMyApplication().getRoutingConfig(), mode);
		OsmandSettings.SpeedConstants units = settings.SPEED_SYSTEM.get();
		String speedUnits = units.toShortString(getActivity());
		final float[] ratio = new float[1];
		switch (units) {
			case MILES_PER_HOUR:
				ratio[0] = 3600 / OsmAndFormatter.METERS_IN_ONE_MILE;
				break;
			case KILOMETERS_PER_HOUR:
				ratio[0] = 3600 / OsmAndFormatter.METERS_IN_KILOMETER;
				break;
			case MINUTES_PER_KILOMETER:
				ratio[0] = 3600 / OsmAndFormatter.METERS_IN_KILOMETER;
				speedUnits = getString(R.string.km_h);
				break;
			case NAUTICALMILES_PER_HOUR:
				ratio[0] = 3600 / OsmAndFormatter.METERS_IN_ONE_NAUTICALMILE;
				break;
		}

		float settingsMinSpeed = settings.MIN_SPEED.get();
		float settingsMaxSpeed = settings.MAX_SPEED.get();

		final int[] defaultValue = {Math.round(mode.getDefaultSpeed() * ratio[0])};
		final int[] minValue = {Math.round((settingsMinSpeed > 0 ? settingsMinSpeed : router.getMinSpeed()) * ratio[0])};
		final int[] maxValue = {Math.round((settingsMaxSpeed > 0 ? settingsMaxSpeed : router.getMaxSpeed()) * ratio[0])};
		final int min = Math.round(router.getMinSpeed() * ratio[0] / 2f);
		final int max = Math.round(router.getMaxSpeed() * ratio[0] * 1.5f);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		boolean lightMode = getMyApplication().getSettings().isLightContent();
		int themeRes = lightMode ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		View seekbarView = LayoutInflater.from(new ContextThemeWrapper(getActivity(), themeRes))
				.inflate(R.layout.default_speed_dialog, null, false);
		builder.setView(seekbarView);
		builder.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mode.setDefaultSpeed(getMyApplication(), defaultValue[0] / ratio[0]);
				settings.MIN_SPEED.set(minValue[0] / ratio[0]);
				settings.MAX_SPEED.set(maxValue[0] / ratio[0]);
			}
		});
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.setNeutralButton("Revert", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mode.resetDefaultSpeed(getMyApplication());
				settings.MIN_SPEED.set(0f);
				settings.MAX_SPEED.set(0f);
			}
		});

		setupSpeedSlider(SettingsNavigationActivity.SpeedSliderType.MIN_SPEED, speedUnits, minValue, defaultValue, maxValue, min, max, seekbarView);
		setupSpeedSlider(SettingsNavigationActivity.SpeedSliderType.DEFAULT_SPEED, speedUnits, minValue, defaultValue, maxValue, min, max, seekbarView);
		setupSpeedSlider(SettingsNavigationActivity.SpeedSliderType.MAX_SPEED, speedUnits, minValue, defaultValue, maxValue, min, max, seekbarView);

		builder.show();
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference.getKey().equals("default_speed")) {
			showSeekbarSettingsDialog();
			return true;
		}
		return false;
	}

	public static boolean showInstance(FragmentManager fragmentManager) {
		try {
			VehicleParametersFragment settingsNavigationFragment = new VehicleParametersFragment();
			fragmentManager.beginTransaction()
					.add(R.id.fragmentContainer, settingsNavigationFragment, VehicleParametersFragment.TAG)
					.addToBackStack(VehicleParametersFragment.TAG)
					.commitAllowingStateLoss();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}