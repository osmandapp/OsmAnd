package net.osmand.plus.base.bottomsheetmenu;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;

public class HorizontalRecyclerBottomSheetItem extends BaseBottomSheetItem {

	protected RecyclerView.Adapter adapter;

	private RecyclerView recyclerView;

	public HorizontalRecyclerBottomSheetItem(View customView,
	                                         @LayoutRes int layoutId,
	                                         Object tag,
	                                         boolean disabled,
	                                         View.OnClickListener onClickListener,
	                                         int position,
	                                         RecyclerView.Adapter adapter) {
		super(customView, layoutId, tag, disabled, onClickListener, position);
		this.adapter = adapter;
	}

	public void setAdapter(RecyclerView.Adapter adapter) {
		this.adapter = adapter;
		recyclerView.setAdapter(adapter);
	}

	@Override
	public void inflate(Context context, ViewGroup container, boolean nightMode) {
		super.inflate(context, container, nightMode);
		recyclerView = view.findViewById(R.id.recycler_view);
		if (recyclerView != null && adapter != null) {
			recyclerView.setAdapter(adapter);
		}
	}

	public static class Builder extends BaseBottomSheetItem.Builder {

		protected RecyclerView.Adapter adapter;

		public HorizontalRecyclerBottomSheetItem.Builder setAdapter(RecyclerView.Adapter adapter) {
			this.adapter = adapter;
			return this;
		}

		public HorizontalRecyclerBottomSheetItem create() {
			return new HorizontalRecyclerBottomSheetItem(customView,
					layoutId,
					tag,
					disabled,
					onClickListener,
					position,
					adapter);
		}
	}
}
