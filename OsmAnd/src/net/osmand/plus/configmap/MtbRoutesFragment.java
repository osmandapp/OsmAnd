package net.osmand.plus.configmap;

import static net.osmand.osm.OsmRouteType.MTB;
import static net.osmand.plus.configmap.ConfigureMapMenu.SHOW_MTB_SCALE_IMBA_TRAILS;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.transport.TransportLinesFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class MtbRoutesFragment extends BaseOsmAndFragment {

	public static final String TAG = MtbRoutesFragment.class.getSimpleName();

	private CommonPreference<Boolean> showMtbRoutesPref;
	private final List<View> itemsViews = new ArrayList<>();

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		showMtbRoutesPref = settings.getCustomRenderBooleanProperty(MTB.getRenderingPropertyAttr());
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.fragment_mtb_routes, container, false);

		setupHeader(view);
		updateScreenMode(view, showMtbRoutesPref.get());
		setupClassifications(view);
		setupClassificationPreference(showMtbRoutesPref.get());

		return view;
	}

	private void setupHeader(@NonNull View view) {
		TransportLinesFragment.setupButton(
				view.findViewById(R.id.main_toggle),
				R.drawable.ic_action_mountain_bike,
				getString(R.string.rendering_attr_showMtbRoutes_name),
				showMtbRoutesPref.get(),
				false,
				v -> {
					boolean enabled = !showMtbRoutesPref.get();
					setupClassificationPreference(enabled);
					showMtbRoutesPref.set(enabled);
					updateScreenMode(view, enabled);
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
			updateClassificationPreferences(classification);
			refreshMap();
		});

		Drawable background = UiUtilities.getColoredSelectableDrawable(app, settings.getApplicationMode().getProfileColor(nightMode), 0.3f);
		AndroidUtils.setBackground(button, background);
		return view;
	}

	private void updateClassificationPreferences(@Nullable MtbClassification selectedClassification) {
		for (View itemView : itemsViews) {
			MtbClassification classification = (MtbClassification) itemView.getTag();
			AppCompatRadioButton radioButton = itemView.findViewById(R.id.compound_button);

			boolean selected = classification == selectedClassification;
			radioButton.setChecked(selected);
			settings.getCustomRenderBooleanProperty(classification.attrName).set(selected);
		}
	}

	private void setupClassificationPreference(boolean mtbRoutesEnabled) {
		updateClassificationPreferences(mtbRoutesEnabled ? getSelectedClassification(settings) : null);
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

	@NonNull
	public static MtbClassification getSelectedClassification(@NonNull OsmandSettings settings) {
		MtbClassification classification = null;
		for (MtbClassification mtbClassification : MtbClassification.values()) {
			if (settings.getCustomRenderBooleanProperty(mtbClassification.attrName).get()) {
				classification = mtbClassification;
			}
		}
		return classification != null ? classification : MtbClassification.SCALE;
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			manager.beginTransaction()
					.replace(R.id.content, new MtbRoutesFragment(), TAG)
					.commitAllowingStateLoss();
		}
	}

	public enum MtbClassification {

		SCALE("showMtbScale", R.string.mtb_scale, null),
		IMBA(SHOW_MTB_SCALE_IMBA_TRAILS, R.string.mtb_imba, R.string.mtb_imba_full);

		public final String attrName;
		@StringRes
		public final int nameId;
		@Nullable
		@StringRes
		public final Integer descriptionId;

		MtbClassification(String attrName, int nameId, @Nullable Integer descriptionId) {
			this.attrName = attrName;
			this.nameId = nameId;
			this.descriptionId = descriptionId;
		}
	}
}
