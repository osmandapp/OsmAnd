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

import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;

public abstract class BaseFirstUsageBottomSheet extends BottomSheetDialogFragment {

	protected WizardType wizardType;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getTargetFragment() instanceof FirstUsageWizardFragment fragment) {
			wizardType = fragment.wizardType;
			nightMode = fragment.deviceNightMode;
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.first_usage_bottom_sheet, container, false);
		TextView titleTv = view.findViewById(R.id.title);
		ViewGroup itemsContainer = view.findViewById(R.id.container);

		setupItems(itemsContainer);
		titleTv.setText(getTitle());

		return view;
	}

	protected abstract String getTitle();

	protected abstract void setupItems(@NonNull ViewGroup container);

	@NonNull
	protected View createItemView(@Nullable String title,
	                              @DrawableRes int iconId, @Nullable OnClickListener listener) {
		View item = inflate(R.layout.item_with_left_icon);
		TextView titleView = item.findViewById(R.id.title);
		ImageView iconView = item.findViewById(R.id.icon);

		titleView.setText(title);
		iconView.setImageDrawable(getContentIcon(iconId));
		item.findViewById(R.id.button).setOnClickListener(listener);
		return item;
	}

	protected void processActionClick(@NonNull FirstUsageAction action) {
		if (getTargetFragment() instanceof FirstUsageActionsListener firstUsageActionsListener) {
			firstUsageActionsListener.processActionClick(action);
		}
	}
}
