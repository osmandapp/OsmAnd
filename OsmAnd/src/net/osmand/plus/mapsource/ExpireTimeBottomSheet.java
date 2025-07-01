package net.osmand.plus.mapsource;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import static net.osmand.plus.mapsource.EditMapSourceDialogFragment.EXPIRE_TIME_NEVER;

public class ExpireTimeBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = ExpireTimeBottomSheet.class.getName();
	private static final Log LOG = PlatformUtil.getLog(ExpireTimeBottomSheet.class);
	private static final String EXPIRE_VALUE_KEY = "expire_value_key";
	private int expireValue;
	private TextInputEditText editText;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			expireValue = savedInstanceState.getInt(EXPIRE_VALUE_KEY, EXPIRE_TIME_NEVER);
		}
		TitleItem titleItem = new TitleItem(getString(R.string.expire_time));
		items.add(titleItem);
		View inputValueLayout = inflate(R.layout.edit_text_with_descr);
		((TextView) inputValueLayout.findViewById(R.id.dialog_descr)).setText(R.string.expire_time_descr);
		editText = inputValueLayout.findViewById(R.id.value_edit_text);
		if (expireValue > 0) {
			editText.setText(String.valueOf(expireValue));
		}
		int boxStrokeColor = ColorUtilities.getOsmandIconColor(app, nightMode);
		TextInputLayout textInputLayout = inputValueLayout.findViewById(R.id.value_input_layout);
		textInputLayout.setBoxStrokeColor(boxStrokeColor);
		SimpleBottomSheetItem editTextItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
				.setCustomView(inputValueLayout)
				.create();
		items.add(editTextItem);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putInt(EXPIRE_VALUE_KEY, getExpireValue());
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRightBottomButtonClick() {
		AndroidUtils.hideSoftKeyboard(requireActivity(), editText);
		if (getTargetFragment() instanceof OnExpireValueSetListener listener) {
			listener.onExpireValueSet(getExpireValue());
		}
		dismiss();
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	private int getExpireValue() {
		int expireValue = EXPIRE_TIME_NEVER;
		if (editText.getText() != null) {
			String value = editText.getText().toString();
			if (!Algorithms.isEmpty(value)) {
				try {
					expireValue = Integer.parseInt(value);
				} catch (RuntimeException e) {
					LOG.error("Error parsing expire value: " + expireValue + " " + e);
				}
			}
		}
		return expireValue > 0 ? expireValue : EXPIRE_TIME_NEVER;
	}

	private void setExpireValue(int expireValue) {
		this.expireValue = expireValue;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager,
	                                @Nullable Fragment targetFragment, int expireValue) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			ExpireTimeBottomSheet bottomSheet = new ExpireTimeBottomSheet();
			bottomSheet.setTargetFragment(targetFragment, 0);
			bottomSheet.setExpireValue(expireValue);
			bottomSheet.show(fragmentManager, TAG);
		}
	}

	public interface OnExpireValueSetListener {
		void onExpireValueSet(int expireValue);
	}
}
