package net.osmand.plus;

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
import androidx.appcompat.view.ContextThemeWrapper;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

public class DialogListItemAdapter extends BaseAdapter {

	public static final int INVALID_ID = -1;

	private final CharSequence[] mData;
	private final boolean multiChoice;
	private final boolean nightMode;
	private int selected = INVALID_ID;
	private final boolean[] checkedItems;
	@ColorInt
	private int controlsColor = INVALID_ID;

	private AlertDialog dialog;
	private final View.OnClickListener listener;
	private final LayoutInflater inflater;

	@NonNull
	public static DialogListItemAdapter createSingleChoiceAdapter(@NonNull CharSequence[] mData, boolean nightMode, int selected, @NonNull OsmandApplication app,
	                                                              @ColorInt int controlsColor, int themeRes, @Nullable View.OnClickListener listener) {
		return new DialogListItemAdapter(mData, selected, null, nightMode, app, controlsColor, themeRes, listener, false);
	}

	@NonNull
	public static DialogListItemAdapter createMultiChoiceAdapter(@NonNull CharSequence[] mData, boolean nightMode, @Nullable boolean[] checkedItems,
	                                                             @NonNull OsmandApplication app, @ColorInt int controlsColor, int themeRes,
	                                                             @Nullable View.OnClickListener listener) {
		return new DialogListItemAdapter(mData, INVALID_ID, checkedItems, nightMode, app, controlsColor, themeRes, listener, true);
	}

	private DialogListItemAdapter(@NonNull CharSequence[] mData, int selected, @NonNull boolean[] checkedItems, boolean nightMode,
	                              @NonNull OsmandApplication app, int controlsColor, int themeRes,
	                              @Nullable View.OnClickListener listener, boolean multiChoice) {
		this.mData = mData;
		this.selected = selected;
		this.checkedItems = checkedItems;
		this.nightMode = nightMode;
		this.multiChoice = multiChoice;
		this.controlsColor = controlsColor;
		this.listener = listener;
		inflater = LayoutInflater.from(new ContextThemeWrapper(app, themeRes));
	}

	@Override
	public int getCount() {
		return mData.length;
	}

	@NonNull
	@Override
	public Object getItem(int position) {
		return mData[position];
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
		if (controlsColor != INVALID_ID) {
			UiUtilities.setupCompoundButton(nightMode, controlsColor, cb);
			Drawable selectable = UiUtilities.getColoredSelectableDrawable(ctx, controlsColor, 0.3f);
			AndroidUtils.setBackground(button, selectable);
		}
		TextView text = view.findViewById(R.id.text);
		text.setText(mData[position]);
		return view;
	}
}
