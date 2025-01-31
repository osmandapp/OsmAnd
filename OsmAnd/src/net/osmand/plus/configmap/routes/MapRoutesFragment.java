package net.osmand.plus.configmap.routes;

import static net.osmand.osm.OsmRouteType.ALPINE;
import static net.osmand.osm.OsmRouteType.BICYCLE;
import static net.osmand.osm.OsmRouteType.HIKING;
import static net.osmand.osm.OsmRouteType.MTB;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public abstract class MapRoutesFragment extends BaseOsmAndFragment implements CardListener {

	private static final Log log = PlatformUtil.getLog(MapRoutesFragment.class);

	protected RouteLayersHelper routeLayersHelper;

	protected final List<BaseCard> cards = new ArrayList<>();

	protected ViewGroup cardsContainer;
	protected ViewGroup preferenceContainer;
	protected CompoundButton compoundButton;

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	protected abstract boolean isEnabled();

	protected abstract void toggleMainPreference(@NonNull View view);

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		routeLayersHelper = app.getRouteLayersHelper();
	}

	@Override
	@Nullable
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.map_routes_fragment, container, false);

		setupHeader(view);
		setupCards(view);
		updateContent();

		return view;
	}

	protected void setupHeader(@NonNull View view) {
		boolean enabled = isEnabled();

		preferenceContainer = view.findViewById(R.id.preference_container);
		preferenceContainer.setOnClickListener(v -> {
			toggleMainPreference(view);
			setupHeader(view);
			updateContent();
			refreshMap();
		});

		compoundButton = preferenceContainer.findViewById(R.id.toggle_item);
		compoundButton.setClickable(false);
		compoundButton.setFocusable(false);
		compoundButton.setChecked(isEnabled());

		int profileColor = settings.getApplicationMode().getProfileColor(nightMode);
		UiUtilities.setupCompoundButton(nightMode, profileColor, compoundButton);
		AndroidUtils.setBackground(preferenceContainer, UiUtilities.getColoredSelectableDrawable(app, profileColor, 0.3f));

		boolean portrait = AndroidUiHelper.isOrientationPortrait(requireActivity());
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.shadow_on_map), portrait);
		AndroidUiHelper.updateVisibility(preferenceContainer.findViewById(R.id.divider), false);
		AndroidUiHelper.updateVisibility(preferenceContainer.findViewById(R.id.secondary_icon), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.header_divider), !enabled);
	}

	protected void addCard(@NonNull BaseCard card) {
		cards.add(card);
		card.setListener(this);
		cardsContainer.addView(card.build(cardsContainer.getContext()));
	}

	protected void setupCards(@NonNull View view) {
		cards.clear();
		cardsContainer = view.findViewById(R.id.cards_container);
		cardsContainer.removeAllViews();
	}

	protected void updateContent() {
		for (BaseCard card : cards) {
			card.update();
		}
		AndroidUiHelper.updateVisibility(cardsContainer, isEnabled());
	}

	protected void refreshMap() {
		MapActivity mapActivity = (MapActivity) getMyActivity();
		if (mapActivity != null) {
			mapActivity.refreshMapComplete();
			mapActivity.updateLayers();
		}
	}

	@NonNull
	protected View createDivider(@NonNull ViewGroup group, boolean showTop, boolean showBottom) {
		View divider = themedInflater.inflate(R.layout.list_item_divider, group, false);
		AndroidUiHelper.updateVisibility(divider.findViewById(R.id.topShadowView), showTop);
		AndroidUiHelper.updateVisibility(divider.findViewById(R.id.bottomShadowView), showBottom);
		return divider;
	}

	@Override
	public void onCardPressed(@NonNull BaseCard card) {
		refreshMap();

		View view = getView();
		if (view != null) {
			setupHeader(view);
		}
	}

	@Nullable
	protected MapActivity getMapActivity() {
		FragmentActivity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		}
		return null;
	}

	@NonNull
	protected MapActivity requireMapActivity() {
		FragmentActivity activity = getActivity();
		if (!(activity instanceof MapActivity)) {
			throw new IllegalStateException("Fragment " + this + " not attached to an activity.");
		}
		return (MapActivity) activity;
	}

	public static boolean shouldShow(@NonNull OsmandApplication app, @NonNull String attrName) {
		return CollectionUtils.equalsToAny(attrName,
				BICYCLE.getRenderingPropertyAttr(), MTB.getRenderingPropertyAttr(),
				HIKING.getRenderingPropertyAttr(), ALPINE.getRenderingPropertyAttr());
	}

	@Nullable
	public static String getFragmentName(@Nullable String attrName) {
		if (Algorithms.stringsEqual(BICYCLE.getRenderingPropertyAttr(), attrName)) {
			return CycleRoutesFragment.class.getName();
		} else if (Algorithms.stringsEqual(MTB.getRenderingPropertyAttr(), attrName)) {
			return MtbRoutesFragment.class.getName();
		} else if (Algorithms.stringsEqual(HIKING.getRenderingPropertyAttr(), attrName)) {
			return HikingRoutesFragment.class.getName();
		} else if (Algorithms.stringsEqual(ALPINE.getRenderingPropertyAttr(), attrName)) {
			return AlpineHikingScaleFragment.class.getName();
		}
		return null;
	}

	public static void showInstance(@NonNull FragmentActivity activity, @NonNull String attrName) {
		String name = getFragmentName(attrName);
		FragmentManager manager = activity.getSupportFragmentManager();
		if (!Algorithms.isEmpty(name) && AndroidUtils.isFragmentCanBeAdded(manager, name)) {
			try {
				Fragment fragment = Fragment.instantiate(activity, name);
				manager.beginTransaction()
						.replace(R.id.content, fragment, fragment.getTag())
						.commitAllowingStateLoss();
			} catch (Exception e) {
				log.error(e);
			}
		}
	}
}
