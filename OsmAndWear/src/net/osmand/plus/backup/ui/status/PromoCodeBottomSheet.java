package net.osmand.plus.backup.ui.status;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.inapp.InAppPurchaseHelper;

public class PromoCodeBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = PromoCodeBottomSheet.class.getSimpleName();

	private static final String PROMOCODE_KEY = "promocode_key";

	private EditText editText;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		OsmandApplication app = requiredMyApplication();

		items.add(new TitleItem(getString(R.string.backup_promocode)));

		String promoCode;
		if (savedInstanceState == null) {
			promoCode = app.getSettings().BACKUP_PROMOCODE.get();
		} else {
			promoCode = savedInstanceState.getString(PROMOCODE_KEY);
		}
		Context ctx = requireContext();
		View view = UiUtilities.getInflater(ctx, nightMode).inflate(R.layout.preference_edit_text_box, null);
		editText = view.findViewById(R.id.edit_text);
		editText.setText(promoCode);

		ViewGroup editTextLayout = view.findViewById(R.id.text_field_boxes_editTextLayout);
		if (editTextLayout != null && editTextLayout.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
			ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) editTextLayout.getLayoutParams();
			params.setMargins(params.leftMargin, AndroidUtils.dpToPx(ctx, 9), params.rightMargin, params.bottomMargin);
		}

		items.add(new SimpleBottomSheetItem.Builder().setCustomView(view).create());
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(PROMOCODE_KEY, editText.getText().toString());
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Override
	protected void onRightBottomButtonClick() {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			String promoCode = editText.getText().toString();
			app.getSettings().BACKUP_PROMOCODE.set(promoCode);

			InAppPurchaseHelper purchaseHelper = app.getInAppPurchaseHelper();
			if (purchaseHelper != null) {
				CallbackWithObject<Boolean> listener = result -> {
					app.runInUIThread(() -> app.getBackupHelper().prepareBackup());
					return true;
				};
				purchaseHelper.checkPromoAsync(listener);
			}
		}
		dismiss();
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			PromoCodeBottomSheet fragment = new PromoCodeBottomSheet();
			fragment.show(fragmentManager, TAG);
		}
	}
}