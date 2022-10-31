package net.osmand.plus.plugins.openseamaps;

import static net.osmand.plus.plugins.openseamaps.NauticalMapsPlugin.DEPTH_CONTOURS;
import static net.osmand.plus.transport.TransportLinesMenu.RENDERING_CATEGORY_TRANSPORT;
import static net.osmand.render.RenderingRuleStorageProperties.UI_CATEGORY_HIDDEN;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.DialogListItemAdapter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.configmap.ConfigureMapUtils;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.transport.TransportLinesMenu;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class NauticalDepthContourFragment extends BaseOsmAndFragment {

	public static final String TAG = NauticalDepthContourFragment.class.getSimpleName();
	private static final String DEPTH_CONTOUR = "depthContour";
	public static final String DEPTH_CONTOUR_WIDTH = "depthContourWidth";
	public static final String DEPTH_CONTOUR_COLOR_SCHEME = "depthContourColorScheme";

	private OsmandApplication app;
	private MapActivity mapActivity;
	private OsmandSettings settings;
	private ApplicationMode appMode;

	private View view;
	private LayoutInflater themedInflater;
	private boolean nightMode;

	private CommonPreference<Boolean> pref;
	private final List<RenderingRuleProperty> rules = new ArrayList<>();

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		settings = app.getSettings();
		mapActivity = (MapActivity) requireMyActivity();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		appMode = settings.getApplicationMode();
		nightMode = app.getDaynightHelper().isNightModeForMapControls();
		themedInflater = UiUtilities.getInflater(getContext(), nightMode);
		view = themedInflater.inflate(R.layout.fragment_nautical_depth_contours, container, false);

		List<RenderingRuleProperty> customRules = ConfigureMapUtils.getCustomRules(app,
				UI_CATEGORY_HIDDEN, RENDERING_CATEGORY_TRANSPORT);
		Iterator<RenderingRuleProperty> iterator = customRules.iterator();
		while (iterator.hasNext()) {
			RenderingRuleProperty property = iterator.next();
			if (DEPTH_CONTOURS.equals(property.getAttrName())) {
				pref = settings.getCustomRenderBooleanProperty(property.getAttrName());

				iterator.remove();
			}else if (property.getAttrName().startsWith(DEPTH_CONTOUR)) {
				rules.add(property);
			}
		}
		setupMainToggle();
		setupDepthContourLinesToggles();

		updateScreenMode(pref.get());
		return view;
	}

	private void setupMainToggle() {
		setupButton(
				view.findViewById(R.id.main_toggle),
				R.drawable.ic_action_nautical_depth,
				getString(R.string.rendering_attr_depthContours_name),
				pref.get(),
				false,
				v -> {
					pref.set(!pref.get());
					updateScreenMode(pref.get());
					mapActivity.refreshMapComplete();
				});
	}

	private void setupDepthContourLinesToggles() {
		View container = view.findViewById(R.id.routes_container);
		if (Algorithms.isEmpty(rules)) {
			container.setVisibility(View.GONE);
			return;
		}

		ViewGroup list = view.findViewById(R.id.nautical_toggles_list);
		for (RenderingRuleProperty property: rules) {
			String attrName = property.getAttrName();
			CommonPreference<String> pref = app.getSettings().getCustomRenderProperty(property.getAttrName());
			View view = themedInflater.inflate(R.layout.configure_screen_list_item, null);

			int iconId = 0;
			String title = null;
			String descr;
			if(attrName.equals(DEPTH_CONTOUR_WIDTH)){
				iconId = R.drawable.circle_background_dark;
				title = getString(R.string.shared_string_lines_width);
			} else if(attrName.equals(DEPTH_CONTOUR_COLOR_SCHEME)){
				iconId = R.drawable.ic_action_appearance;
				title = getString(R.string.shared_string_lines_color_scheme);
			}

			if (!Algorithms.isEmpty(pref.get())) {
				descr = AndroidUtils.getRenderingStringPropertyValue(app, pref.get());
			} else {
				descr = AndroidUtils.getRenderingStringPropertyValue(app, property.getDefaultValueDescription());
			}
			Drawable icon = getIcon(iconId, ColorUtilities.getDefaultIconColorId(nightMode));
			ImageView ivIcon = view.findViewById(R.id.icon);
			ivIcon.setImageDrawable(icon);

			TextView tvTitle = view.findViewById(R.id.title);
			tvTitle.setText(title);

			TextView tvDesc = view.findViewById(R.id.description);
			tvDesc.setText(descr);
			AndroidUiHelper.updateVisibility(tvDesc, true);

			View button = view.findViewById(R.id.button_container);
			String finalTitle = title;
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {

					dialogOnClick(finalTitle, property, tvDesc, pref);
				}
			});

			int color = settings.getApplicationMode().getProfileColor(nightMode);
			Drawable background = UiUtilities.getColoredSelectableDrawable(app, color, 0.3f);
			AndroidUtils.setBackground(view.findViewById(R.id.button_container), background);

			list.addView(view);

		}
	}

	private void dialogOnClick(String title, RenderingRuleProperty property, TextView tvDesc, CommonPreference<String> pref){
		int currentProfileColor = settings.APPLICATION_MODE.get().getProfileColor(nightMode);
		int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		AlertDialog.Builder b = new AlertDialog.Builder(new ContextThemeWrapper(mapActivity, themeRes));
		b.setTitle(title);

		String[] possibleValuesString = new String[property.getPossibleValues().length];

		for (int j = 0; j < property.getPossibleValues().length; j++) {
			possibleValuesString[j] = AndroidUtils.getRenderingStringPropertyValue(app, property.getPossibleValues()[j]);
		}
		DialogListItemAdapter dialogAdapter = DialogListItemAdapter.createSingleChoiceAdapter(
				possibleValuesString, nightMode, Arrays.asList(property.getPossibleValues()).indexOf(pref.get()), getMyApplication(), currentProfileColor, themeRes, v -> {
					int which = (int) v.getTag();

					pref.set(property.getPossibleValues()[which]);
					mapActivity.refreshMapComplete();
					String description = AndroidUtils.getRenderingStringPropertyValue(mapActivity, pref.get());
					tvDesc.setText(description);
				}
		);
		b.setAdapter(dialogAdapter, null);
		dialogAdapter.setDialog(b.show());
	}

	private void updateScreenMode(boolean enabled) {
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.empty_screen), !enabled);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.normal_screen), enabled);
	}

	private void setupButton(@NonNull View view, int iconId, @NonNull String title, boolean enabled,
	                         boolean showDivider, @Nullable OnClickListener listener) {
		int activeColor = appMode.getProfileColor(nightMode);
		int defColor = ColorUtilities.getDefaultIconColor(app, nightMode);
		int iconColor = enabled ? activeColor : defColor;

		Drawable icon = getPaintedContentIcon(iconId, iconColor);
		ImageView ivIcon = view.findViewById(R.id.icon);
		ivIcon.setImageDrawable(icon);
		ivIcon.setColorFilter(enabled ? activeColor : defColor);

		TextView tvTitle = view.findViewById(R.id.title);
		tvTitle.setText(title);

		CompoundButton cb = view.findViewById(R.id.compound_button);
		cb.setChecked(enabled);
		cb.setVisibility(View.VISIBLE);
		UiUtilities.setupCompoundButton(nightMode, activeColor, cb);

		cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
			ivIcon.setColorFilter(isChecked ? activeColor : defColor);
			if (listener != null) {
				listener.onClick(buttonView);
			}
		});

		view.setOnClickListener(v -> {
			boolean newState = !cb.isChecked();
			cb.setChecked(newState);
		});

		View divider = view.findViewById(R.id.bottom_divider);
		if (divider != null) {
			AndroidUiHelper.updateVisibility(divider, showDivider);
		}

		Drawable background = UiUtilities.getColoredSelectableDrawable(app, activeColor, 0.3f);
		AndroidUtils.setBackground(view, background);
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			fragmentManager.beginTransaction()
					.replace(R.id.content, new NauticalDepthContourFragment(), TAG)
					.commitAllowingStateLoss();
		}
	}
}
