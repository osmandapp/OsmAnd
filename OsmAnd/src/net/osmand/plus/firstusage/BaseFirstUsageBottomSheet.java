package net.osmand.plus.firstusage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.firstusage.FirstUsageWizardFragment.WizardType;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.TextViewEx;

public abstract class BaseFirstUsageBottomSheet extends BottomSheetDialogFragment {
	private OsmandApplication app;
	private boolean nightMode;
	protected WizardType wizardType;
	protected FirstUsageBottomSheetListener listener;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();

		if (getTargetFragment() != null && getTargetFragment() instanceof FirstUsageWizardFragment) {
			FirstUsageWizardFragment fragment = (FirstUsageWizardFragment) getTargetFragment();
			listener = fragment.getFirstUsageBSListener();
			wizardType = fragment.wizardType;
			nightMode = fragment.deviceNightMode;
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		LayoutInflater layoutInflater = UiUtilities.getInflater(getContext(), nightMode);
		View dialogView = layoutInflater.inflate(R.layout.first_usage_bottom_sheet, container, false);
		TextViewEx bottomSheetTitle = dialogView.findViewById(R.id.title);
		LinearLayout layoutContainer = dialogView.findViewById(R.id.container);

		fillLayout(layoutContainer, layoutInflater);
		bottomSheetTitle.setText(getTitle());

		return dialogView;
	}

	protected abstract void fillLayout(LinearLayout layout, LayoutInflater inflater);

	protected abstract String getTitle();

	protected View createItemView(LayoutInflater layoutInflater, String title, @DrawableRes int iconId, OnClickListener onClickListener) {
		View item = layoutInflater.inflate(R.layout.item_with_left_icon, null);
		TextViewEx titleView = item.findViewById(R.id.title);
		AppCompatImageView iconView = item.findViewById(R.id.icon);
		View button = item.findViewById(R.id.button);

		titleView.setText(title);
		iconView.setImageDrawable(ContextCompat.getDrawable(app, iconId));
		button.setOnClickListener(onClickListener);
		return item;
	}
}
