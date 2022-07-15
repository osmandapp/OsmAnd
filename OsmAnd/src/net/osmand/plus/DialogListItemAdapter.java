package net.osmand.plus;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;

import net.osmand.plus.utils.UiUtilities;

public class DialogListItemAdapter extends BaseAdapter {

	public static final int INVALID_ID = -1;

	private final String[] mData;
	private final boolean multiChoice;
	private final boolean nightMode;
	private int selected = INVALID_ID;
	private final boolean[] checkedItems;
	@ColorInt
	private int compoundButtonColor = INVALID_ID;

	private AlertDialog dialog;
	private final View.OnClickListener listener;
	private final LayoutInflater inflater;

	public static DialogListItemAdapter createSingleChoiceAdapter(String[] mData, boolean nightMode, int selected, OsmandApplication app,
	                                                              @ColorInt int compoundButtonColor, int themeRes, View.OnClickListener listener) {

		return new DialogListItemAdapter(mData, selected, null, nightMode, app, compoundButtonColor, themeRes, listener, false);
	}

	public static DialogListItemAdapter createMultiChoiceAdapter(String[] mData, boolean nightMode, boolean[] checkedItems, OsmandApplication app,
	                                                             @ColorInt int compoundButtonColor, int themeRes, View.OnClickListener listener) {

		return new DialogListItemAdapter(mData, INVALID_ID, checkedItems, nightMode, app, compoundButtonColor, themeRes, listener, true);
	}

	private DialogListItemAdapter(String[] mData, int selected, boolean[] checkedItems, boolean nightMode, OsmandApplication app,
	                              int compoundButtonColor, int themeRes, View.OnClickListener listener, boolean multiChoice) {
		this.mData = mData;
		this.selected = selected;
		this.checkedItems = checkedItems;
		this.nightMode = nightMode;
		this.multiChoice = multiChoice;
		this.compoundButtonColor = compoundButtonColor;
		this.listener = listener;
		inflater = LayoutInflater.from(new ContextThemeWrapper(app, themeRes));
	}

	@Override
	public int getCount() {
		return mData.length;
	}

	@Override
	public Object getItem(int position) {
		return mData[position];
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	public void setDialog(AlertDialog dialog) {
		this.dialog = dialog;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view;
		if (convertView == null) {
			view = inflater.inflate(R.layout.dialog_list_item_with_compound_button, null);
		} else {
			view = convertView;
		}
		View button = view.findViewById(R.id.button);
		button.setTag(position);
		CompoundButton cb;
		if (multiChoice) {
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
			cb.setChecked(position == selected);
			button.setOnClickListener(v -> {
				listener.onClick(v);
				dialog.dismiss();
			});
		}
		cb.setVisibility(View.VISIBLE);
		if (compoundButtonColor != INVALID_ID) {
			UiUtilities.setupCompoundButton(nightMode, compoundButtonColor, cb);
		}
		TextView text = view.findViewById(R.id.text);
		text.setText(mData[position]);
		return view;
	}
}
