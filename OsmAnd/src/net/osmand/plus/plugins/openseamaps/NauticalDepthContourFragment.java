package net.osmand.plus.plugins.openseamaps;

import static net.osmand.plus.plugins.openseamaps.NauticalMapsPlugin.DEPTH_CONTOURS;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.DialogListItemAdapter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.configmap.ConfigureMapUtils;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.transport.TransportLinesFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NauticalDepthContourFragment extends BaseOsmAndFragment {

	public static final String TAG = NauticalDepthContourFragment.class.getSimpleName();

	public static final String DEPTH_CONTOUR_WIDTH = "depthContourWidth";
	public static final String DEPTH_CONTOUR_COLOR_SCHEME = "depthContourColorScheme";

	private OsmandApplication app;
	private OsmandSettings settings;

	private CommonPreference<Boolean> preference;
	private final List<RenderingRuleProperty> properties = new ArrayList<>();

	private boolean nightMode;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		settings = app.getSettings();
		nightMode = app.getDaynightHelper().isNightModeForMapControls();

		List<RenderingRuleProperty> customRules = ConfigureMapUtils.getCustomRules(app);
		for (RenderingRuleProperty property : customRules) {
			String attrName = property.getAttrName();
			if (DEPTH_CONTOURS.equals(attrName)) {
				preference = settings.getCustomRenderBooleanProperty(attrName);
			} else if (DEPTH_CONTOUR_WIDTH.equals(attrName) || DEPTH_CONTOUR_COLOR_SCHEME.equals(attrName)) {
				properties.add(property);
			}
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(requireContext(), nightMode);
		View view = themedInflater.inflate(R.layout.fragment_nautical_depth_contours, container, false);

		setupHeader(view);
		setupPropertyPreferences(view);
		updateScreenMode(view, preference.get());

		return view;
	}

	private void setupHeader(@NonNull View view) {
		TransportLinesFragment.setupButton(
				view.findViewById(R.id.main_toggle),
				R.drawable.ic_action_nautical_depth,
				getString(R.string.nautical_depth),
				preference.get(),
				false,
				v -> {
					boolean enabled = !preference.get();
					preference.set(enabled);
					updateScreenMode(view, enabled);
					refreshMap();
				});
	}

	private void updateScreenMode(@NonNull View view, boolean enabled) {
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.empty_screen), !enabled);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.normal_screen), enabled);
	}

	private void setupPropertyPreferences(@NonNull View view) {
		boolean hasProperties = !Algorithms.isEmpty(properties);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.properties_container), hasProperties);
		if (!hasProperties) {
			return;
		}
		ViewGroup container = view.findViewById(R.id.nautical_properties);
		LayoutInflater inflater = UiUtilities.getInflater(getContext(), nightMode);
		for (RenderingRuleProperty property : properties) {
			View propertyView = createPropertyView(property, inflater, container);
			container.addView(propertyView);
		}
	}

	private View createPropertyView(@NonNull RenderingRuleProperty property, @NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
		String attrName = property.getAttrName();
		View view = inflater.inflate(R.layout.configure_screen_list_item, container, false);

		ImageView icon = view.findViewById(R.id.icon);
		TextView title = view.findViewById(R.id.title);
		TextView description = view.findViewById(R.id.description);
		AndroidUiHelper.updateVisibility(description, true);

		CommonPreference<String> pref = settings.getCustomRenderProperty(attrName);
		String propertyValue = Algorithms.isEmpty(pref.get()) ? property.getDefaultValueDescription() : pref.get();

		icon.setImageDrawable(getPropertyIcon(attrName));
		title.setText(AndroidUtils.getRenderingStringPropertyName(app, attrName, property.getName()));
		description.setText(AndroidUtils.getRenderingStringPropertyValue(app, propertyValue));

		View button = view.findViewById(R.id.button_container);
		button.setOnClickListener(v -> showPreferenceDialog(property, pref, description));

		Drawable background = UiUtilities.getColoredSelectableDrawable(app, getProfileColor(), 0.3f);
		AndroidUtils.setBackground(button, background);
		return view;
	}

	@ColorInt
	private int getProfileColor() {
		return settings.getApplicationMode().getProfileColor(nightMode);
	}

	private void showPreferenceDialog(@NonNull RenderingRuleProperty property,
	                                  @NonNull CommonPreference<String> pref,
	                                  @Nullable TextView description) {
		int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		AlertDialog.Builder builder = new AlertDialog.Builder(UiUtilities.getThemedContext(requireContext(), nightMode));
		builder.setTitle(AndroidUtils.getRenderingStringPropertyName(app, property.getAttrName(), property.getName()));

		String[] possibleValues = property.getPossibleValues();
		String[] possibleValuesString = new String[possibleValues.length];

		for (int i = 0; i < possibleValues.length; i++) {
			possibleValuesString[i] = AndroidUtils.getRenderingStringPropertyValue(app, possibleValues[i]);
		}
		DialogListItemAdapter adapter = DialogListItemAdapter.createSingleChoiceAdapter(possibleValuesString,
				nightMode, Arrays.asList(possibleValues).indexOf(pref.get()), app, getProfileColor(), themeRes, v -> {
					int which = (int) v.getTag();
					pref.set(possibleValues[which]);
					refreshMap();

					if (description != null) {
						description.setText(AndroidUtils.getRenderingStringPropertyValue(app, pref.get()));
					}
				}
		);
		builder.setAdapter(adapter, null);
		adapter.setDialog(builder.show());
	}

	private void refreshMap() {
		MapActivity mapActivity = (MapActivity) getMyActivity();
		if (mapActivity != null) {
			mapActivity.refreshMapComplete();
			mapActivity.updateLayers();
		}
	}

	private Drawable getPropertyIcon(@NonNull String attrName) {
		switch (attrName) {
			case DEPTH_CONTOUR_WIDTH:
				return getContentIcon(R.drawable.ic_action_width_limit);
			case DEPTH_CONTOUR_COLOR_SCHEME:
				return getContentIcon(R.drawable.ic_action_appearance);
			default:
				return null;
		}
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			manager.beginTransaction()
					.replace(R.id.content, new NauticalDepthContourFragment(), TAG)
					.commitAllowingStateLoss();
		}
	}
}
