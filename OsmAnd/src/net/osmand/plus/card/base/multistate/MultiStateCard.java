package net.osmand.plus.card.base.multistate;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.popup.OnPopUpMenuItemClickListener;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;

import java.util.List;

public class MultiStateCard extends BaseCard {

	private final IMultiStateCardController cardController;

	public MultiStateCard(@NonNull FragmentActivity activity,
	                      @NonNull IMultiStateCardController cardController) {
		super(activity);
		this.cardController = cardController;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.card_multi_state;
	}

	@Override
	protected void updateContent() {
		updateTitle();
		updateSelector();
		updateContentView();
	}

	private void updateTitle() {
		TextView tvTitle = view.findViewById(R.id.card_title);
		tvTitle.setText(cardController.getCardTitle());
	}

	private void updateSelector() {
		View selector = view.findViewById(R.id.card_selector);
		if (!cardController.shouldShowMenuButton()) {
			selector.setVisibility(View.GONE);
			return;
		}
		selector.setVisibility(View.VISIBLE);
		selector.setOnClickListener(v -> showSelectionMenu());

		int colorId = ColorUtilities.getActiveColor(app, nightMode);
		Drawable selectableBackground = UiUtilities.getColoredSelectableDrawable(app, colorId);
		selector.setBackground(selectableBackground);

		updateSelectorTitle();
	}

	private void updateSelectorTitle() {
		View selector = view.findViewById(R.id.card_selector);
		TextView tvTitle = selector.findViewById(R.id.title);
		tvTitle.setText(cardController.getMenuButtonTitle());
	}

	private void updateContentView() {
		ViewGroup contentContainer = view.findViewById(R.id.content);
		contentContainer.removeAllViews();
		cardController.onBindContentView(themedInflater, contentContainer);
	}

	private void showSelectionMenu() {
		View selector = view.findViewById(R.id.card_selector);
		List<PopUpMenuItem> menuItems = cardController.getMenuItems();

		OnPopUpMenuItemClickListener onItemClickListener = item -> {
			if (cardController.onMenuItemSelected(item)) {
				updateSelector();
				updateContentView();
			}
		};
		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = selector;
		displayData.menuItems = menuItems;
		displayData.nightMode = nightMode;
		displayData.onItemClickListener = onItemClickListener;
		PopUpMenu.show(displayData);
	}
}