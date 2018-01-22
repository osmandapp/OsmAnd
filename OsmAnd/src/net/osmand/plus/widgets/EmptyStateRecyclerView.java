package net.osmand.plus.widgets;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

public class EmptyStateRecyclerView extends RecyclerView {
	private View emptyView;

	public EmptyStateRecyclerView(Context context) {
		super(context);
	}

	public EmptyStateRecyclerView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	public EmptyStateRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	private AdapterDataObserver emptyStateObserver = new AdapterDataObserver() {
		@Override
		public void onChanged() {
			checkIfEmpty();
		}

		@Override
		public void onItemRangeInserted(int positionStart, int itemCount) {
			checkIfEmpty();
		}

		@Override
		public void onItemRangeRemoved(int positionStart, int itemCount) {
			checkIfEmpty();
		}
	};

	@Override
	public void setAdapter(Adapter adapter) {
		final Adapter oldAdapter = getAdapter();
		if (oldAdapter != null) {
			oldAdapter.unregisterAdapterDataObserver(emptyStateObserver);
		}
		super.setAdapter(adapter);
		if (adapter != null) {
			adapter.registerAdapterDataObserver(emptyStateObserver);
		}
		checkIfEmpty();
	}

	public void setEmptyView(View emptyView) {
		this.emptyView = emptyView;
		checkIfEmpty();
	}

	private void checkIfEmpty() {
		if (emptyView != null && getAdapter() != null) {
			if (getAdapter().getItemCount() == 0) {
				setVisibility(View.GONE);
				emptyView.setVisibility(View.VISIBLE);
			} else {
				emptyView.setVisibility(View.GONE);
				setVisibility(View.VISIBLE);
			}
		}
	}
}
