package net.osmand.plus.firstusage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.firstusage.FirstUsageWizardFragment.WizardType;
import net.osmand.plus.utils.UiUtilities;

public abstract class BaseFirstUsageBottomSheet extends BottomSheetDialogFragment {

	protected OsmandApplication app;

	protected WizardType wizardType;

	private boolean nightMode;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requiredMyApplication();

		Fragment targetFragment = getTargetFragment();
		if (targetFragment instanceof FirstUsageWizardFragment) {
			FirstUsageWizardFragment fragment = (FirstUsageWizardFragment) targetFragment;
			wizardType = fragment.wizardType;
			nightMode = fragment.deviceNightMode;
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(getContext(), nightMode);
		View view = themedInflater.inflate(R.layout.first_usage_bottom_sheet, container, false);
		TextView titleTv = view.findViewById(R.id.title);
		ViewGroup itemsContainer = view.findViewById(R.id.container);

		setupItems(itemsContainer, themedInflater);
		titleTv.setText(getTitle());

		return view;
	}

	protected abstract String getTitle();

	protected abstract void setupItems(@NonNull ViewGroup container, @NonNull LayoutInflater inflater);

	protected View createItemView(@NonNull LayoutInflater inflater, @Nullable String title,
	                              @DrawableRes int iconId, @Nullable OnClickListener listener) {
		View item = inflater.inflate(R.layout.item_with_left_icon, null);
		TextView titleView = item.findViewById(R.id.title);
		ImageView iconView = item.findViewById(R.id.icon);

		titleView.setText(title);
		iconView.setImageDrawable(app.getUIUtilities().getThemedIcon(iconId));
		item.findViewById(R.id.button).setOnClickListener(listener);
		return item;
	}

	protected void processActionClick(@NonNull FirstUsageAction action) {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof FirstUsageActionsListener) {
			((FirstUsageActionsListener) fragment).processActionClick(action);
		}
	}
}
