package net.osmand.plus.widgets.alert;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

class SelectionDialogAdapter extends BaseAdapter {

	public static final int INVALID_ID = -1;

	private final CharSequence[] items;
	private final boolean useMultiSelection;
	private final boolean nightMode;
	private int selectedIndex = INVALID_ID;
	private final boolean[] checkedItems;
	@ColorInt
	private final Integer controlsColor;

	private AlertDialog dialog;
	private final View.OnClickListener listener;
	private final LayoutInflater inflater;

	public SelectionDialogAdapter(
			@NonNull Context ctx, @NonNull CharSequence[] items, int selected,
			@Nullable boolean[] checkedItems, @ColorInt @Nullable Integer controlsColor,
			boolean nightMode, @Nullable View.OnClickListener listener,
			boolean useMultiSelection
	) {
		this.items = items;
		this.selectedIndex = selected;
		this.checkedItems = checkedItems;
		this.nightMode = nightMode;
		this.useMultiSelection = useMultiSelection;
		this.controlsColor = controlsColor;
		this.listener = listener;
		inflater = UiUtilities.getInflater(ctx, nightMode);
	}

	@Override
	public int getCount() {
		return items.length;
	}

	@NonNull
	@Override
	public Object getItem(int position) {
		return items[position];
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	public void setDialog(@NonNull AlertDialog dialog) {
		this.dialog = dialog;
	}

	@NonNull
	@Override
	public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		View view;
		if (convertView == null) {
			view = inflater.inflate(R.layout.dialog_list_item_with_compound_button, parent, false);
		} else {
			view = convertView;
		}
		Context ctx = view.getContext();
		View button = view.findViewById(R.id.button);
		button.setTag(position);
		CompoundButton cb;
		if (useMultiSelection) {
			cb = view.findViewById(R.id.checkbox);
			view.findViewById(R.id.radio).setVisibility(View.INVISIBLE);
			cb.setChecked(checkedItems[position]);
			button.setOnClickListener(v -> {
				listener.onClick(v);
				cb.setChecked(!cb.isChecked());
			});
		} else {
			cb = view.findViewById(R.id.radio);
			view.findViewById(R.id.checkbox).setVisibility(View.INVISIBLE);
			cb.setChecked(position == selectedIndex);
			button.setOnClickListener(v -> {
				listener.onClick(v);
				dialog.dismiss();
			});
		}
		cb.setVisibility(View.VISIBLE);
		if (controlsColor != null) {
			UiUtilities.setupCompoundButton(nightMode, controlsColor, cb);
			Drawable selectable = UiUtilities.getColoredSelectableDrawable(ctx, controlsColor, 0.3f);
			AndroidUtils.setBackground(button, selectable);
		}
		TextView text = view.findViewById(R.id.text);
		text.setText(items[position]);
		return view;
	}
}
