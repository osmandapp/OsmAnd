package net.osmand.plus.plugins.srtm;

import static net.osmand.plus.plugins.srtm.SRTMPlugin.MAX_VERTICAL_EXAGGERATION;
import static net.osmand.plus.plugins.srtm.SRTMPlugin.MIN_VERTICAL_EXAGGERATION;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.MapOptionSliderFragment;
import net.osmand.plus.dashboard.DashboardType;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.utils.AndroidUtils;

public class Relief3DExaggerationFragment extends MapOptionSliderFragment {

	private static final String RETURN_DASHBOARD_KEY = "return_dashboard";

	@Nullable
	@Override
	protected String getToolbarTitle() {
		return getString(R.string.vertical_exaggeration);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SRTMPlugin plugin = PluginsHelper.getPlugin(SRTMPlugin.class);
		if (savedInstanceState == null && plugin != null) {
			value = originalValue = plugin.getVerticalExaggerationScale();
		}
	}

	@Override
	protected void setupMainContent(@NonNull ViewGroup container) {
		super.setupMainContent(container);

		View view = container.findViewById(R.id.slider_container);
		TextView description = view.findViewById(R.id.description);
		description.setText(R.string.vertical_exaggeration_description);
	}

	@Override
	protected void setupSlider(@NonNull View view) {
		super.setupSlider(view);

		slider.setValueTo(MAX_VERTICAL_EXAGGERATION);
		slider.setValueFrom(MIN_VERTICAL_EXAGGERATION);
		slider.setStepSize(0.1f);
	}

	@Override
	protected void onValueChanged(float value) {
		SRTMPlugin plugin = PluginsHelper.getPlugin(SRTMPlugin.class);
		if (plugin != null) {
			plugin.setVerticalExaggerationScale(value);
			refreshMap();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		MapActivity activity = getMapActivity();
		if (activity != null && !activity.isChangingConfigurations()) {
			DashboardType returnTo = DashboardType.valueOf(requireArguments().getString(RETURN_DASHBOARD_KEY));
			activity.getDashboard().setDashboardVisibility(true, returnTo, false);
		}
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull DashboardType returnTo) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle args = new Bundle();
			args.putString(RETURN_DASHBOARD_KEY, returnTo.name());
			Relief3DExaggerationFragment fragment = new Relief3DExaggerationFragment();
			fragment.setArguments(args);
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss();
		}
	}

	@Override
	public String getAnalyticsScreen() {
		return "relief_3d_exaggeration";
	}
}
