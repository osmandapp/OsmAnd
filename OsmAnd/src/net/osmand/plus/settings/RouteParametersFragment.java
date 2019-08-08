package net.osmand.plus.settings;

import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v4.app.FragmentManager;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.R;
import net.osmand.plus.activities.SettingsBaseActivity;
import net.osmand.plus.routepreparationmenu.AvoidRoadsBottomSheetDialogFragment;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper;
import net.osmand.plus.routing.RouteProvider;
import net.osmand.router.GeneralRouter;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.osmand.plus.activities.SettingsNavigationActivity.getRouter;

public class RouteParametersFragment extends BaseProfileSettingsFragment {

	public static final String TAG = "RouteParametersFragment";
	private List<GeneralRouter.RoutingParameter> avoidParameters = new ArrayList<GeneralRouter.RoutingParameter>();
	private List<GeneralRouter.RoutingParameter> preferParameters = new ArrayList<GeneralRouter.RoutingParameter>();
	private List<GeneralRouter.RoutingParameter> reliefFactorParameters = new ArrayList<GeneralRouter.RoutingParameter>();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);

		return view;
	}

	@Override
	protected int getPreferenceResId() {
		return R.xml.route_parameters;
	}

	@Override
	protected int getToolbarResId() {
		return R.layout.profile_preference_toolbar;
	}

	protected String getToolbarTitle() {
		return getString(R.string.route_parameters);
	}

	public static boolean showInstance(FragmentManager fragmentManager) {
		try {
			RouteParametersFragment settingsNavigationFragment = new RouteParametersFragment();
			fragmentManager.beginTransaction()
					.add(R.id.fragmentContainer, settingsNavigationFragment, RouteParametersFragment.TAG)
					.addToBackStack(RouteParametersFragment.TAG)
					.commitAllowingStateLoss();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	protected void createUI() {
		PreferenceScreen screen = getPreferenceScreen();

		SwitchPreference fastRoute = (SwitchPreference) findAndRegisterPreference(settings.FAST_ROUTE_MODE.getId());
		fastRoute.setIcon(getContentIcon(R.drawable.ic_action_fastest_route));
		if (settings.getApplicationMode().getRouteService() != RouteProvider.RouteService.OSMAND) {
			screen.addPreference(fastRoute);
		} else {
			ApplicationMode am = settings.getApplicationMode();
			GeneralRouter router = getRouter(getMyApplication().getRoutingConfig(), am);
			clearParameters();
			if (router != null) { Map<String, GeneralRouter.RoutingParameter> parameters = router.getParameters();
				if (parameters.containsKey(GeneralRouter.USE_SHORTEST_WAY)) {
					screen.addPreference(fastRoute);
				} else {
					screen.removePreference(fastRoute);
				}
				List<GeneralRouter.RoutingParameter> others = new ArrayList<GeneralRouter.RoutingParameter>();
				for (Map.Entry<String, GeneralRouter.RoutingParameter> e : parameters.entrySet()) {
					String param = e.getKey();
					GeneralRouter.RoutingParameter routingParameter = e.getValue();
					if (param.startsWith("avoid_")) {
						avoidParameters.add(routingParameter);
					} else if (param.startsWith("prefer_")) {
						preferParameters.add(routingParameter);
					} else if ("relief_smoothness_factor".equals(routingParameter.getGroup())) {
						reliefFactorParameters.add(routingParameter);
					} else if (!param.equals(GeneralRouter.USE_SHORTEST_WAY)
							&& !RoutingOptionsHelper.DRIVING_STYLE.equals(routingParameter.getGroup())
							&& !param.equals(GeneralRouter.VEHICLE_WEIGHT)
							&& !param.equals(GeneralRouter.VEHICLE_HEIGHT)) {
						others.add(routingParameter);
					}
				}
				Preference avoidRouting = findAndRegisterPreference("avoid_in_routing");
				if (avoidParameters.size() > 0) {
					avoidRouting.setOnPreferenceClickListener(this);
					avoidRouting.setIcon(getContentIcon(R.drawable.ic_action_alert));
				} else {
					screen.removePreference(avoidRouting);
				}
				Preference preferRouting = findAndRegisterPreference("prefer_in_routing");
				if (preferParameters.size() > 0) {
					preferRouting.setOnPreferenceClickListener(this);
				} else {
					screen.removePreference(preferRouting);
				}
				if (reliefFactorParameters.size() > 0) {
					Preference reliefFactorRouting = new Preference(getContext());
					reliefFactorRouting.setTitle(SettingsBaseActivity.getRoutingStringPropertyName(getContext(), reliefFactorParameters.get(0).getGroup(),
							Algorithms.capitalizeFirstLetterAndLowercase(reliefFactorParameters.get(0).getGroup().replace('_', ' '))));
					reliefFactorRouting.setSummary(R.string.relief_smoothness_factor_descr);
					screen.addPreference(reliefFactorRouting);
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
								SettingsBaseActivity.getRoutingStringPropertyName(getContext(), p.getId(), p.getName()),
								SettingsBaseActivity.getRoutingStringPropertyDescription(getContext(), p.getId(), p.getDescription()));
						((ListPreference) basePref).setEntries(p.getPossibleValueDescriptions());
						((ListPreference) basePref).setEntryValues(svlss);
					}
					basePref.setTitle(SettingsBaseActivity.getRoutingStringPropertyName(getContext(), p.getId(), p.getName()));
					basePref.setSummary(SettingsBaseActivity.getRoutingStringPropertyDescription(getContext(), p.getId(), p.getDescription()));
					screen.addPreference(basePref);
				}
			}
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String key = preference.getKey();
		switch (key) {
			case "avoid_in_routing": {
				AvoidRoadsBottomSheetDialogFragment avoidRoadsFragment = new AvoidRoadsBottomSheetDialogFragment(false);
				avoidRoadsFragment.setTargetFragment(RouteParametersFragment.this, AvoidRoadsBottomSheetDialogFragment.REQUEST_CODE);
				avoidRoadsFragment.show(getActivity().getSupportFragmentManager(), AvoidRoadsBottomSheetDialogFragment.TAG);
				return true;
			}
			case "prefer_in_routing": {
//				AvoidRoadsBottomSheetDialogFragment avoidRoadsFragment = new AvoidRoadsBottomSheetDialogFragment(true);
//				avoidRoadsFragment.setTargetFragment(RouteParametersFragment.this, AvoidRoadsBottomSheetDialogFragment.REQUEST_CODE);
//				avoidRoadsFragment.show(getActivity().getSupportFragmentManager(), AvoidRoadsBottomSheetDialogFragment.TAG);
				Toast.makeText(getContext(), "prefer_in_routing", Toast.LENGTH_LONG).show();
				return true;
			}
		}

		return false;
	}

	private void clearParameters() {
		preferParameters.clear();
		avoidParameters.clear();
		reliefFactorParameters.clear();
	}
}