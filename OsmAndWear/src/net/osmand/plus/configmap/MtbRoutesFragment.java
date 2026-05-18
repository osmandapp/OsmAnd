package net.osmand.plus.configmap;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.configmap.routes.MtbClassification;
import net.osmand.plus.configmap.routes.RouteLayersHelper;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.transport.TransportLinesFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MtbRoutesFragment extends BaseOsmAndFragment {

	public static final String TAG = MtbRoutesFragment.class.getSimpleName();

	private RouteLayersHelper routeLayersHelper;
	private final List<View> itemsViews = new ArrayList<>();

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		routeLayersHelper = app.getRouteLayersHelper();
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.fragment_mtb_routes, container, false);

		boolean enabled = routeLayersHelper.isMtbRoutesEnabled();
		setupHeader(view);
		updateScreenMode(view, enabled);
		setupClassifications(view);
		updateClassificationPreferences();

		return view;
	}

	private void setupHeader(@NonNull View view) {
		TransportLinesFragment.setupButton(
				view.findViewById(R.id.main_toggle),
				R.drawable.ic_action_mountain_bike,
				getString(R.string.rendering_attr_showMtbRoutes_name),
				routeLayersHelper.isMtbRoutesEnabled(),
				false,
				v -> {
					routeLayersHelper.toggleMtbRoutes();
					updateClassificationPreferences();
					updateScreenMode(view, routeLayersHelper.isMtbRoutesEnabled());
					refreshMap();
				});
	}

	private void setupClassifications(@NonNull View view) {
		ViewGroup container = view.findViewById(R.id.classification_properties);
		LayoutInflater inflater = UiUtilities.getInflater(getContext(), nightMode);
		itemsViews.clear();

		for (MtbClassification classification : MtbClassification.values()) {
			boolean hasDivider = classification.ordinal() != MtbClassification.values().length - 1;
			View propertyView = createRadioButton(classification, inflater, container, hasDivider);
			container.addView(propertyView);
		}
	}

	private View createRadioButton(@NonNull MtbClassification classification, @NonNull LayoutInflater inflater, @Nullable ViewGroup container, boolean hasDivider) {
		View view = inflater.inflate(R.layout.item_with_radiobutton_and_descr, container, false);
		view.setTag(classification);
		itemsViews.add(view);

		TextView title = view.findViewById(R.id.title);
		TextView description = view.findViewById(R.id.description);

		title.setText(getString(classification.nameId));
		boolean hasDescription = classification.descriptionId != null;
		if (hasDescription) {
			description.setText(classification.descriptionId);
		}
		AndroidUiHelper.updateVisibility(description, hasDescription);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.divider), hasDivider);

		View button = view.findViewById(R.id.button);
		button.setOnClickListener(v -> {
			routeLayersHelper.updateSelectedMtbClassification(classification.attrName);
			updateClassificationPreferences();
			refreshMap();
		});

		Drawable background = UiUtilities.getColoredSelectableDrawable(app, settings.getApplicationMode().getProfileColor(nightMode), 0.3f);
		AndroidUtils.setBackground(button, background);
		return view;
	}

	private void updateClassificationPreferences() {
		for (View itemView : itemsViews) {
			MtbClassification classification = (MtbClassification) itemView.getTag();
			AppCompatRadioButton radioButton = itemView.findViewById(R.id.compound_button);
			boolean selected = Objects.equals(classification.attrName, routeLayersHelper.getSelectedMtbClassificationId());
			radioButton.setChecked(selected);
		}
	}

	private void updateScreenMode(@NonNull View view, boolean enabled) {
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.empty_screen), !enabled);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.normal_screen), enabled);
	}

	private void refreshMap() {
		MapActivity mapActivity = (MapActivity) getMyActivity();
		if (mapActivity != null) {
			mapActivity.refreshMapComplete();
			mapActivity.updateLayers();
		}
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			manager.beginTransaction()
					.replace(R.id.content, new MtbRoutesFragment(), TAG)
					.commitAllowingStateLoss();
		}
	}

}
