package net.osmand.plus.views.mapwidgets.configure.panel;

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnScrollChangedListener;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.CompoundButtonType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.WidgetsRegister;
import net.osmand.plus.views.mapwidgets.configure.WidgetItem;

import java.util.List;

public class WidgetsListFragment extends Fragment implements OnScrollChangedListener {

	private static final String SELECTED_GROUP_ATTR = "selected_group_key";

	private OsmandApplication app;
	private ApplicationMode appMode;
	private LayoutInflater inflater;
	private WidgetsPanel panel;
	private boolean nightMode;

	private View view;
	private LinearLayout widgetsList;
	private NestedScrollView scrollView;
	private View listBtnChangeOrder;
	private View stickBtnChangeOrder;
	private ConfigureWidgetsFragment parentFragment;
	private int lastListBtnPos;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (OsmandApplication) requireContext().getApplicationContext();
		appMode = app.getSettings().getApplicationMode();
		nightMode = !app.getSettings().isLightContent();
		parentFragment = (ConfigureWidgetsFragment) getParentFragment();
		if (savedInstanceState != null) {
			restoreData(savedInstanceState);
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
	                         @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		this.inflater = inflater = UiUtilities.getInflater(getContext(), nightMode);
		view = inflater.inflate(R.layout.fragment_widgets_list, container, false);
		widgetsList = view.findViewById(R.id.widgets_list);
		scrollView = view.findViewById(R.id.scroll_view);
		listBtnChangeOrder = view.findViewById(R.id.change_order_button_in_list);
		scrollView.getViewTreeObserver().addOnScrollChangedListener(this);
		updateContent();
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		parentFragment.setCurrentListFragment(this);
		setupBtnChangeOrder();
	}

	private void setupBtnChangeOrder() {
		stickBtnChangeOrder = parentFragment.getBtnChangeOrder();
		if (stickBtnChangeOrder != null) {
			parentFragment.setupReorderButton(stickBtnChangeOrder);
		}
		parentFragment.setupReorderButton(listBtnChangeOrder);
	}

	public void updateContent() {
		updateCardTitle();
		updateWidgetsList();
	}

	private void updateCardTitle() {
		TextView tvPanelTitle = view.findViewById(R.id.panel_title);
		tvPanelTitle.setText(getString(panel.getTitleId()));
	}

	private void updateWidgetsList() {
		widgetsList.removeAllViews();
		List<WidgetItem> widgets = WidgetsRegister.getSortedWidgets(appMode, panel, false);
		for (WidgetItem widget : widgets) {
			View view = inflater.inflate(R.layout.configure_screen_widget_item, widgetsList, false);

			boolean isEnabled = widget.isActive;

			ImageView ivIcon = view.findViewById(R.id.icon);
			ivIcon.setImageResource(widget.iconId);
			setImageFilter(ivIcon, !isEnabled);

			TextView tvTitle = view.findViewById(R.id.title);
			tvTitle.setText(widget.title);

			TextView tvDesc = view.findViewById(R.id.description);
			tvDesc.setVisibility(View.GONE);

			ImageView ivSecondaryIcon = view.findViewById(R.id.secondary_icon);
			ivSecondaryIcon.setImageResource(R.drawable.ic_action_additional_option);

			CompoundButton cb = view.findViewById(R.id.compound_button);
			cb.setChecked(isEnabled);
			cb.setVisibility(View.VISIBLE);
			UiUtilities.setupCompoundButton(cb, nightMode, CompoundButtonType.GLOBAL);

			OnClickListener listener = v -> {
				boolean isChecked = cb.isChecked();
				setImageFilter(ivIcon, !isChecked);
				widget.isActive = isChecked;
			};

			cb.setOnCheckedChangeListener((buttonView, isChecked) -> listener.onClick(buttonView));

			view.setOnClickListener(v -> {
				cb.setChecked(!cb.isChecked());
				listener.onClick(v);
			});

			setupListItemBackground(view);

			widgetsList.addView(view);
		}
	}

	private void setupListItemBackground(@NonNull View view) {
		View button = view.findViewById(R.id.button_container);
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		Drawable background = UiUtilities.getColoredSelectableDrawable(app, activeColor, 0.3f);
		AndroidUtils.setBackground(button, background);
	}

	private void setImageFilter(@NonNull ImageView ivImage, boolean applyFilter) {
		if (applyFilter) {
			ColorMatrix matrix = new ColorMatrix();
			matrix.setSaturation(0);
			ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
			ivImage.setColorFilter(filter);
		} else {
			ivImage.clearColorFilter();
		}
	}

	private void updateReorderButtons() {
		if (stickBtnChangeOrder != null && isCurrentTab()) {
			int y1 = AndroidUtils.getViewOnScreenY(listBtnChangeOrder);
			int y2 = AndroidUtils.getViewOnScreenY(stickBtnChangeOrder);
			stickBtnChangeOrder.setVisibility(y1 <= y2 ? View.GONE : View.VISIBLE);
		}
	}

	private boolean isCurrentTab() {
		return parentFragment.getSelectedPanel() == panel;
	}

	@Nullable
	public NestedScrollView getScrollView() {
		return scrollView;
	}

	@Override
	public void onScrollChanged() {
		updateReorderButtons();
	}

	public void setPanel(@NonNull WidgetsPanel panel) {
		this.panel = panel;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(SELECTED_GROUP_ATTR, panel.name());
	}

	private void restoreData(@NonNull Bundle savedInstanceState) {
		String groupName = savedInstanceState.getString(SELECTED_GROUP_ATTR);
		panel = WidgetsPanel.valueOf(groupName);
	}

}
