package net.osmand.plus.quickaction;

import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

/**
 * Created by okorsun on 20.12.16.
 */

public class QuickActionListFragment extends BaseOsmAndFragment implements QuickAction.QuickActionSelectionListener {
    public static final String TAG = QuickActionListFragment.class.getSimpleName();

    RecyclerView         quickActionRV;
    FloatingActionButton fab;

    QuickActionFactory quickActionFactory = new QuickActionFactory();
    QuickActionAdapter adapter;
    ItemTouchHelper    touchHelper;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.quick_action_list, container, false);

        quickActionRV = (RecyclerView) view.findViewById(R.id.recycler_view);
        fab = (FloatingActionButton) view.findViewById(R.id.fabButton);

        setUpQuickActionRV();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddQuickActionDialog dialog = new AddQuickActionDialog();
                dialog.show(getFragmentManager(), AddQuickActionDialog.TAG);
                dialog.selectionListener = QuickActionListFragment.this;
            }
        });

        setUpToolbar(view);

        Fragment dialog = getFragmentManager().findFragmentByTag(AddQuickActionDialog.TAG);

        if (dialog != null && dialog instanceof AddQuickActionDialog)
            ((AddQuickActionDialog) dialog).selectionListener  = this;

        return view;
    }

    private void setUpQuickActionRV() {
        adapter = new QuickActionAdapter(new OnStartDragListener() {
            @Override
            public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
                touchHelper.startDrag(viewHolder);
            }
        });
        quickActionRV.setAdapter(adapter);
        quickActionRV.setLayoutManager(new LinearLayoutManager(getContext()));


        ItemTouchHelper.Callback touchHelperCallback = new QuickActionItemTouchHelperCallback(adapter);
        touchHelper = new ItemTouchHelper(touchHelperCallback);
        touchHelper.attachToRecyclerView(quickActionRV);
        adapter.addItems(getSavedActions());

        quickActionRV.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 && fab.getVisibility() == View.VISIBLE)
                    fab.hide();
                else if (dy < 0 && fab.getVisibility() != View.VISIBLE)
                    fab.show();
            }
        });
    }

    private void setUpToolbar(View view) {
        Toolbar  toolbar = (Toolbar) view.findViewById(R.id.custom_toolbar);
        Drawable back    = getMyApplication().getIconsCache().getIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        back.setColorFilter(ContextCompat.getColor(getContext(), R.color.color_white), PorterDuff.Mode.MULTIPLY);
        toolbar.setNavigationIcon(back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });
        toolbar.setTitle(R.string.configure_screen_quick_action);
        toolbar.setTitleTextColor(ContextCompat.getColor(getContext(), R.color.color_white));
    }

    @Override
    public void onResume() {
        super.onResume();
        getMapActivity().disableDrawer();
    }

    @Override
    public void onPause() {
        super.onPause();
        getMapActivity().enableDrawer();
    }

    private List<QuickAction> getSavedActions() {
        String actionsJson = getMyApplication().getSettings().QUICK_ACTION_LIST.get();

        return quickActionFactory.parseActiveActionsList(actionsJson);
    }

    private MapActivity getMapActivity() {
        return (MapActivity) getActivity();
    }

    private void saveQuickActions(){
        String json = quickActionFactory.quickActionListToString((ArrayList<QuickAction>) adapter.getQuickActions());
        getMyApplication().getSettings().QUICK_ACTION_LIST.set(json);
    }

    void createAndShowDeleteDialog(final int itemPosition, final String itemName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.quick_actions_delete);
        builder.setMessage(getResources().getString(R.string.quick_actions_delete_text, itemName));
        builder.setIcon(getMyApplication().getIconsCache().getThemedIcon(R.drawable.ic_action_delete_dark));
        builder.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                adapter.deleteItem(itemPosition);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(R.string.shared_string_no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onActionSelected(QuickAction action) {
        adapter.addItem(action);
        saveQuickActions();
    }

    public class QuickActionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements QuickActionItemTouchHelperCallback.OnItemMoveCallback {
        public static final int SCREEN_ITEM_TYPE   = 1;
        public static final int SCREEN_HEADER_TYPE = 2;

        private static final int ITEMS_IN_GROUP = 6;

        private List<QuickAction> itemsList = new ArrayList<>();
        private final OnStartDragListener onStartDragListener;

        public QuickActionAdapter(OnStartDragListener onStartDragListener) {
            this.onStartDragListener = onStartDragListener;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == SCREEN_ITEM_TYPE)
                return new QuickActionItemVH(inflater.inflate(R.layout.quick_action_list_item, parent, false));
            else
                return new QuickActionHeaderVH(inflater.inflate(R.layout.quick_action_list_header, parent, false));
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
            int               viewType = getItemViewType(position);
            final QuickAction item     = itemsList.get(position);

            if (viewType == SCREEN_ITEM_TYPE) {
                final QuickActionItemVH itemVH = (QuickActionItemVH) holder;

                itemVH.title.setText(item.getNameRes());
                itemVH.subTitle.setText(getResources().getString(R.string.quick_action_item_action, getActionPosition(position)));

                itemVH.icon.setImageDrawable(getMyApplication().getIconsCache().getThemedIcon(item.getIconRes()));
                itemVH.handleView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (MotionEventCompat.getActionMasked(event) ==
                                MotionEvent.ACTION_DOWN) {
                            onStartDragListener.onStartDrag(itemVH);
                        }
                        return false;
                    }
                });
                itemVH.closeBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        createAndShowDeleteDialog(holder.getAdapterPosition(), getResources().getString(item.getNameRes()));
                    }
                });

                LinearLayout.LayoutParams dividerParams = (LinearLayout.LayoutParams) itemVH.divider.getLayoutParams();
                //noinspection ResourceType
                dividerParams.setMargins(!isLongDivider(position) ? dpToPx(56f) : 0, 0, 0, 0);
                itemVH.divider.setLayoutParams(dividerParams);
            } else {
                QuickActionHeaderVH headerVH = (QuickActionHeaderVH) holder;
                headerVH.headerName.setText(getResources().getString(R.string.quick_action_item_screen, position / (ITEMS_IN_GROUP + 1) + 1));
            }
        }

        @Override
        public int getItemCount() {
            return itemsList.size();
        }

        @Override
        public int getItemViewType(int position) {
            return itemsList.get(position).getId() == 0 ? SCREEN_HEADER_TYPE : SCREEN_ITEM_TYPE;
        }

        public void deleteItem(int position) {
            if (position == -1)
                return;

            itemsList.remove(position);
            notifyItemRemoved(position);

            moveHeaders(position);
            showFABIfNotScrollable();
            saveQuickActions();
        }

        private void moveHeaders(int position) {
            for (int i = position; i < itemsList.size(); i++) {
                if (getItemViewType(i) == SCREEN_HEADER_TYPE) {
                    if (i != itemsList.size() - 2) {
                        Collections.swap(itemsList, i, i + 1);
                        notifyItemMoved(i, i + 1);
                        i++;
                    } else {
                        itemsList.remove(i);
                        notifyItemRemoved(i);
                    }
                }
            }
            notifyItemRangeChanged(position, itemsList.size() - position);

            if (itemsList.size() == 1){
                itemsList.remove(0);
                notifyItemRemoved(0);
            }
        }

        private void showFABIfNotScrollable() {
            LinearLayoutManager layoutManager           = (LinearLayoutManager) quickActionRV.getLayoutManager();
            int                 lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
            if ((lastVisibleItemPosition == itemsList.size() - 1 || lastVisibleItemPosition == itemsList.size()) &&
                    layoutManager.findFirstVisibleItemPosition() == 0 &&
                    fab.getVisibility() != View.VISIBLE ||
                    itemsList.size() == 0)
                fab.show();
        }

        public List<QuickAction> getQuickActions() {
            List<QuickAction> result = new ArrayList<>();
            for (int i = 0; i < itemsList.size(); i++) {
                if (getItemViewType(i) == SCREEN_ITEM_TYPE)
                    result.add(itemsList.get(i));
            }

            return result;
        }

        public void addItems(List<QuickAction> data) {
            List<QuickAction> resultList = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                if (i % ITEMS_IN_GROUP == 0)
                    resultList.add(createHeader());

                resultList.add(data.get(i));
            }

            itemsList = resultList;
            notifyDataSetChanged();
        }

        public void addItem(QuickAction item) {
            int oldSize = itemsList.size();
            if (oldSize % (ITEMS_IN_GROUP + 1) == 0)
                itemsList.add(createHeader());
            itemsList.add(item);
            notifyItemRangeInserted(oldSize, itemsList.size() - oldSize);
        }

        private QuickAction createHeader() {
            return new QuickAction();
        }

        private int getActionPosition(int globalPosition) {
            return globalPosition % (ITEMS_IN_GROUP + 1);
        }

        private boolean isLongDivider(int globalPosition) {
            return getActionPosition(globalPosition) == ITEMS_IN_GROUP || globalPosition == getItemCount() - 1;
        }

        private int dpToPx(float dp) {
            Resources r = getActivity().getResources();
            return (int) TypedValue.applyDimension(
                    COMPLEX_UNIT_DIP,
                    dp,
                    r.getDisplayMetrics()
            );
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            if (viewHolder.getItemViewType() == SCREEN_HEADER_TYPE || target.getItemViewType() == SCREEN_HEADER_TYPE)
                return false;
            else {
                int selectedPosition = viewHolder.getAdapterPosition();
                int targetPosition   = target.getAdapterPosition();
                Log.v(TAG, "selected: " + selectedPosition + ", target: " + targetPosition);

                if (selectedPosition < 0 || targetPosition < 0)
                    return false;

                Collections.swap(itemsList, selectedPosition, targetPosition);
                if (selectedPosition - targetPosition < -1) {
                    notifyItemMoved(selectedPosition, targetPosition);
                    notifyItemMoved(targetPosition - 1, selectedPosition);
                } else if (selectedPosition - targetPosition > 1) {
                    notifyItemMoved(selectedPosition, targetPosition);
                    notifyItemMoved(targetPosition + 1, selectedPosition);
                } else {
                    notifyItemMoved(selectedPosition, targetPosition);
                }
                notifyItemChanged(selectedPosition);
                notifyItemChanged(targetPosition);
                return true;
            }
        }

        @Override
        public void onViewDropped(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            saveQuickActions();
        }

        public class QuickActionItemVH extends RecyclerView.ViewHolder {
            public TextView  title;
            public TextView  subTitle;
            public ImageView icon;
            public View      divider;
            public ImageView handleView;
            public ImageView closeBtn;

            public QuickActionItemVH(View itemView) {
                super(itemView);
//                AndroidUtils.setListItemBackground(itemView.getContext(), itemView, getMyApplication().getDaynightHelper().isNightMode());
                title = (TextView) itemView.findViewById(R.id.title);
                subTitle = (TextView) itemView.findViewById(R.id.subtitle);
                icon = (ImageView) itemView.findViewById(R.id.imageView);
                divider = itemView.findViewById(R.id.divider);
                handleView = (ImageView) itemView.findViewById(R.id.handle_view);
                closeBtn = (ImageView) itemView.findViewById(R.id.closeImageButton);

                handleView.setImageDrawable(getMyApplication().getIconsCache().getThemedIcon(R.drawable.ic_action_reorder));
                closeBtn.setImageDrawable(getMyApplication().getIconsCache().getThemedIcon(R.drawable.ic_action_remove_dark));
            }
        }

        public class QuickActionHeaderVH extends RecyclerView.ViewHolder {
            public TextView headerName;

            public QuickActionHeaderVH(View itemView) {
                super(itemView);
                headerName = (TextView) itemView.findViewById(R.id.header);
            }
        }
    }

    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }
}
