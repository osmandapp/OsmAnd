package net.osmand.plus.quickaction;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

/**
 * Created by okorsun on 21.12.16.
 */

public class QuickActionItemTouchHelperCallback extends ItemTouchHelper.Callback {

    OnItemMoveCallback itemMoveCallback;

    public QuickActionItemTouchHelperCallback(OnItemMoveCallback itemMoveCallback) {
        this.itemMoveCallback = itemMoveCallback;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int swipeFlags = 0;
        return !isaHeaderType(viewHolder) ? makeMovementFlags(dragFlags, swipeFlags) : 0;

    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return itemMoveCallback.onMove(recyclerView, viewHolder, target);
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

    }

    private boolean isaHeaderType(RecyclerView.ViewHolder viewHolder) {
        return viewHolder.getItemViewType() == QuickActionListFragment.QuickActionAdapter.SCREEN_HEADER_TYPE;
    }

    interface OnItemMoveCallback {
        boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target);
    }
}
