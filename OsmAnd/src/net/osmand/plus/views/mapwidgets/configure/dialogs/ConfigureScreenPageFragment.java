package net.osmand.plus.views.mapwidgets.configure.dialogs;

import static net.osmand.plus.views.mapwidgets.configure.dialogs.ConfigureScreenFragment.SCREEN_LAYOUT_MODE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.StateChangedListener;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.quickaction.MapButtonsHelper.QuickActionUpdatesListener;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.settings.enums.ScreenLayoutMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.InsetTarget.Type;
import net.osmand.plus.utils.InsetTargetsCollection;
import net.osmand.plus.views.mapwidgets.configure.dialogs.cards.ConfigureActionsCard;
import net.osmand.plus.views.mapwidgets.configure.dialogs.cards.ConfigureButtonsCard;
import net.osmand.plus.views.mapwidgets.configure.dialogs.cards.ConfigureOtherCard;
import net.osmand.plus.views.mapwidgets.configure.dialogs.cards.ConfigureWidgetsCard;
import net.osmand.plus.views.mapwidgets.configure.dialogs.cards.MapScreenLayoutCard;

import java.util.ArrayList;
import java.util.List;

public class ConfigureScreenPageFragment extends BaseOsmAndFragment implements QuickActionUpdatesListener {

	@Nullable
	private ScreenLayoutMode layoutMode;
	private final List<BaseCard> cards = new ArrayList<>();
	private LinearLayout cardsContainer;

	private CardListener cardListener;
	private StateChangedListener<Boolean> speedometerListener;
	private StateChangedListener<Boolean> distanceByTapListener;
	private StateChangedListener<Integer> displayPositionListener;

	@Override
	public InsetTargetsCollection getInsetTargets() {
		InsetTargetsCollection collection = super.getInsetTargets();
		collection.removeType(Type.ROOT_INSET);
		return collection;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle args = getArguments();
		if (args != null && args.containsKey(SCREEN_LAYOUT_MODE)) {
			layoutMode = AndroidUtils.getSerializable(args, SCREEN_LAYOUT_MODE, ScreenLayoutMode.class);
		}
		if (getParentFragment() instanceof CardListener) {
			cardListener = (CardListener) getParentFragment();
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		View view = inflate(R.layout.fragment_configure_screen_page, container, false);
		cardsContainer = view.findViewById(R.id.cards_container);
		setupCards();
		return view;
	}

	private void setupCards() {
		MapActivity activity = requireMapActivity();

		cards.clear();
		cardsContainer.removeAllViews();

		if (!settings.MAP_SCREEN_LAYOUT_CARD_DISMISSED.get() && !settings.USE_SEPARATE_LAYOUTS.get()) {
			inflate(R.layout.list_item_divider, cardsContainer);
			addCard(cardsContainer, new MapScreenLayoutCard(activity));
		}

		inflate(R.layout.list_item_divider, cardsContainer);
		addCard(cardsContainer, new ConfigureWidgetsCard(activity, layoutMode));

		inflate(R.layout.list_item_divider, cardsContainer);
		addCard(cardsContainer, new ConfigureButtonsCard(activity));

		inflate(R.layout.list_item_divider, cardsContainer);
		addCard(cardsContainer, new ConfigureOtherCard(activity));

		inflate(R.layout.list_item_divider, cardsContainer);
		addCard(cardsContainer, new ConfigureActionsCard(activity));

		inflate(R.layout.card_bottom_divider, cardsContainer);
	}

	private void addCard(@NonNull ViewGroup container, @NonNull BaseCard card) {
		cards.add(card);
		if (cardListener != null) {
			card.setListener(cardListener);
		}
		container.addView(card.build());
	}

	public void updateCards() {
		for (BaseCard card : cards) {
			card.update();
		}
	}

	private <T extends BaseCard> void updateCard(Class<T> clazz) {
		BaseCard card = getCard(clazz);
		if (card != null) {
			card.update();
		}
	}

	@Nullable
	public <T extends BaseCard> T getCard(Class<T> clazz) {
		for (BaseCard card : cards) {
			if (clazz.isInstance(card)) {
				return clazz.cast(card);
			}
		}
		return null;
	}

	@Override
	public void onResume() {
		super.onResume();
		updateCards();
		app.getMapButtonsHelper().addUpdatesListener(this);
		settings.SHOW_DISTANCE_RULER.addListener(getDistanceByTapListener());
		settings.POSITION_PLACEMENT_ON_MAP.addListener(getDisplayPositionListener());
		settings.SHOW_SPEEDOMETER.addListener(getSpeedometerListener());
	}

	@Override
	public void onPause() {
		super.onPause();
		app.getMapButtonsHelper().removeUpdatesListener(this);
		settings.SHOW_DISTANCE_RULER.removeListener(getDistanceByTapListener());
		settings.POSITION_PLACEMENT_ON_MAP.removeListener(getDisplayPositionListener());
		settings.SHOW_SPEEDOMETER.removeListener(getSpeedometerListener());
	}

	@Override
	public void onActionsUpdated() {
		updateCard(ConfigureButtonsCard.class);
	}

	@NonNull
	private StateChangedListener<Integer> getDisplayPositionListener() {
		if (displayPositionListener == null) {
			displayPositionListener = change -> app.runInUIThread(() -> updateCard(ConfigureOtherCard.class));
		}
		return displayPositionListener;
	}

	@NonNull
	private StateChangedListener<Boolean> getDistanceByTapListener() {
		if (distanceByTapListener == null) {
			distanceByTapListener = change -> app.runInUIThread(() -> updateCard(ConfigureOtherCard.class));
		}
		return distanceByTapListener;
	}

	@NonNull
	private StateChangedListener<Boolean> getSpeedometerListener() {
		if (speedometerListener == null) {
			speedometerListener = change -> app.runInUIThread(() -> updateCard(ConfigureOtherCard.class));
		}
		return speedometerListener;
	}

	@NonNull
	public static ConfigureScreenPageFragment newInstance(@Nullable ScreenLayoutMode layoutMode) {
		Bundle args = new Bundle();
		if (layoutMode != null) {
			args.putSerializable(SCREEN_LAYOUT_MODE, layoutMode);
		}
		ConfigureScreenPageFragment fragment = new ConfigureScreenPageFragment();
		fragment.setArguments(args);
		return fragment;
	}
}