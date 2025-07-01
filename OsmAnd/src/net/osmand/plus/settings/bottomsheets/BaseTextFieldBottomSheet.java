package net.osmand.plus.settings.bottomsheets;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputLayout;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.widgets.chips.ChipItem;
import net.osmand.plus.widgets.chips.HorizontalChipsView;

public abstract class BaseTextFieldBottomSheet extends BasePreferenceBottomSheet {

	protected TextView title;
	protected ImageView ivImage;

	protected TextView tvDescription;
	protected TextView tvMetric;
	protected HorizontalChipsView chipsView;
	protected TextInputLayout tilCaption;
	protected ScrollView scrollView;
	protected EditText etText;

	protected int shadowHeight;
	protected int buttonsHeight;
	protected int contentHeightPrevious;

	protected float currentValue;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, parent, savedInstanceState);
		if (view != null) {
			view.getViewTreeObserver().addOnGlobalLayoutListener(getOnGlobalLayoutListener());
		}
		return view;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(setupContent(app));
	}

	@SuppressLint("ClickableViewAccessibility")
	private BaseBottomSheetItem setupContent(@NonNull OsmandApplication app) {
		View mainView = inflate(R.layout.bottom_sheet_item_edit_with_chips_view);

		title = mainView.findViewById(R.id.title);
		ivImage = mainView.findViewById(R.id.image_view);
		tvDescription = mainView.findViewById(R.id.description);
		tvMetric = mainView.findViewById(R.id.metric);

		chipsView = mainView.findViewById(R.id.chips_view);
		etText = mainView.findViewById(R.id.text_edit);
		tilCaption = mainView.findViewById(R.id.text_caption);

		chipsView.setOnSelectChipListener(chip -> {
			currentValue = (float) chip.tag;
			etText.setText(formatInputValue(currentValue));
			if (etText.hasFocus()) {
				etText.setSelection(etText.getText().length());
			}
			return true;
		});
		return createBottomSheetItem(app, mainView);
	}

	@NonNull
	protected abstract BaseBottomSheetItem createBottomSheetItem(@NonNull OsmandApplication app, @NonNull View mainView);

	private ViewTreeObserver.OnGlobalLayoutListener getOnGlobalLayoutListener() {
		return () -> {
			Rect visibleDisplayFrame = new Rect();
			buttonsHeight = getResources().getDimensionPixelSize(R.dimen.dialog_button_ex_height);
			shadowHeight = getResources().getDimensionPixelSize(R.dimen.bottom_sheet_top_shadow_height);
			scrollView = requireView().findViewById(R.id.scroll_view);
			scrollView.getWindowVisibleDisplayFrame(visibleDisplayFrame);
			int contentHeight = visibleDisplayFrame.bottom - visibleDisplayFrame.top - buttonsHeight;
			if (contentHeightPrevious != contentHeight) {
				boolean showTopShadow;
				if (scrollView.getHeight() + shadowHeight > contentHeight) {
					scrollView.getLayoutParams().height = contentHeight;
					showTopShadow = false;
				} else {
					scrollView.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
					showTopShadow = true;
				}
				scrollView.requestLayout();
				scrollView.postDelayed(() -> scrollView.scrollTo(0, scrollView.getHeight()), 300);
				contentHeightPrevious = contentHeight;
				drawTopShadow(showTopShadow);
			}
		};
	}

	protected void updateChips() {
		ChipItem selected = chipsView.findChipByTag(currentValue);
		chipsView.setSelected(selected);
		if (selected != null) {
			chipsView.notifyDataSetChanged();
			chipsView.smoothScrollTo(selected);
		}
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	protected abstract String formatInputValue(float input);
}
