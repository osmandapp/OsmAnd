package net.osmand.plus.configmap.routes;

import static net.osmand.osm.OsmRouteType.ALPINE;
import static net.osmand.osm.OsmRouteType.BICYCLE;
import static net.osmand.osm.OsmRouteType.HIKING;
import static net.osmand.osm.OsmRouteType.MTB;
import static net.osmand.osm.OsmRouteType.SKI;

import android.os.Bundle;
import android.util.Pair;
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
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.configmap.ConfigureMapUtils;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.InsetsUtils.InsetSide;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.render.RenderingClass;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class MapRoutesFragment extends BaseFullScreenFragment implements CardListener {

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
		View view = inflate(R.layout.map_routes_fragment, container, false);

		setupHeader(view);
		setupContent(view);

		return view;
	}

	protected void setupHeader(@NonNull View view) {
		boolean enabled = isEnabled();

		preferenceContainer = view.findViewById(R.id.preference_container);
		preferenceContainer.setOnClickListener(v -> {
			toggleMainPreference(view);
			setupHeader(view);
			setupContent(view);
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

	protected void setupContent(@NonNull View view) {
		createCards(view);
		inflateCards(view);
		AndroidUiHelper.updateVisibility(cardsContainer, isEnabled());
	}

	protected void createCards(@NonNull View view) {
		cards.clear();
	}

	protected void inflateCards(@NonNull View view) {
		cardsContainer = view.findViewById(R.id.cards_container);
		cardsContainer.removeAllViews();

		for (int i = 0; i < cards.size(); i++) {
			BaseCard card = cards.get(i);

			if (i == 0) {
				cardsContainer.addView(createDivider(cardsContainer, true, true));
			}
			cardsContainer.addView(card.build(cardsContainer.getContext()));

			boolean lastItem = i == cards.size() - 1;
			cardsContainer.addView(createDivider(cardsContainer, !lastItem, true));
		}
	}

	protected void addCard(@NonNull BaseCard card) {
		cards.add(card);
		card.setListener(this);
	}

	protected void addRenderingClassCard(@NonNull String attrName) {
		if (PluginsHelper.isDevelopment()) {
			BaseCard card = createRenderingClassCard(attrName);
			if (card != null) {
				addCard(card);
			}
		}
	}

	@Nullable
	protected BaseCard createRenderingClassCard(@NonNull String attrName) {
		Pair<RenderingClass, List<RenderingClass>> pair = ConfigureMapUtils.getRenderingClassWithChildren(app, attrName);
		if (pair != null) {
			return new RenderingClassesCard(getMapActivity(), pair.first, pair.second);
		}
		return null;
	}

	@NonNull
	protected View createDivider(@NonNull ViewGroup group, boolean showTop, boolean showBottom) {
		View divider = inflate(R.layout.list_item_divider, group, false);
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
			setupContent(view);
		}
	}

	protected void refreshMap() {
		MapActivity mapActivity = (MapActivity) getMyActivity();
		if (mapActivity != null) {
			mapActivity.refreshMapComplete();
			mapActivity.updateLayers();
		}
	}

	public static boolean shouldShow(@NonNull OsmandApplication app, @NonNull String attrName) {
		boolean defaultScreens = CollectionUtils.equalsToAny(attrName,
				BICYCLE.getRenderingPropertyAttr(), MTB.getRenderingPropertyAttr(),
				HIKING.getRenderingPropertyAttr(), ALPINE.getRenderingPropertyAttr(), SKI.getRenderingPropertyAttr());

		Pair<RenderingClass, List<RenderingClass>> pair = ConfigureMapUtils.getRenderingClassWithChildren(app, attrName);
		return defaultScreens || pair != null;
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
		} else if (Algorithms.stringsEqual(SKI.getRenderingPropertyAttr(), attrName)) {
			return SkiRoutesFragment.class.getName();
		}
		return CustomRoutesFragment.class.getName();
	}

	@Nullable
	public Set<InsetSide> getRootInsetSides() {
		return null;
	}

	@Nullable
	@Override
	public List<Integer> getBottomContainersIds() {
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
