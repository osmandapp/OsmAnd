package net.osmand.plus.quickaction;

import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
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
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

/**
 * Created by okorsun on 20.12.16.
 */

public class QuickActionListFragment extends BaseOsmAndFragment implements QuickActionRegistry.QuickActionUpdatesListener {

    public static final String TAG = QuickActionListFragment.class.getSimpleName();

    private RecyclerView quickActionRV;
    private FloatingActionButton fab;

    private QuickActionAdapter adapter;
    private ItemTouchHelper touchHelper;
    private QuickActionRegistry quickActionRegistry;

    private boolean isLightContent;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        isLightContent = getMyApplication().getSettings().isLightContent();

        View view = UiUtilities.getInflater(getContext(), !isLightContent).inflate(R.layout.quick_action_list, container, false);

        quickActionRV = (RecyclerView) view.findViewById(R.id.recycler_view);
        fab = (FloatingActionButton) view.findViewById(R.id.fabButton);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddQuickActionDialog dialog = new AddQuickActionDialog();
                dialog.show(getFragmentManager(), AddQuickActionDialog.TAG);
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        quickActionRegistry = getMapActivity().getMapLayers().getQuickActionRegistry();

        setUpToolbar(view);
        setUpQuickActionRV();
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
        adapter.addItems(quickActionRegistry.getFilteredQuickActions());

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
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.custom_toolbar);
        Drawable back = getMyApplication().getUIUtilities().getIcon(R.drawable.ic_arrow_back,
                isLightContent ? R.color.active_buttons_and_links_text_light : R.color.active_buttons_and_links_text_dark);
        toolbar.setNavigationIcon(back);
        toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });
        toolbar.setTitle(R.string.configure_screen_quick_action);
        toolbar.setTitleTextColor(ContextCompat.getColor(getContext(),
                isLightContent ? R.color.color_white : R.color.text_color_primary_dark));
    }

    @Override
    public void onResume() {
        super.onResume();

        getMapActivity().disableDrawer();
        quickActionRegistry.setUpdatesListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        getMapActivity().enableDrawer();
        quickActionRegistry.setUpdatesListener(null);
    }

    @Override
    protected boolean isFullScreenAllowed() {
        return false;
    }

    @Override
    public int getStatusBarColorId() {
        return isLightContent ? R.color.status_bar_color_light : R.color.status_bar_color_dark;
    }

    private MapActivity getMapActivity() {
        return (MapActivity) getActivity();
    }

    private void saveQuickActions() {
        quickActionRegistry.updateQuickActions(adapter.getQuickActions());
    }

    void createAndShowDeleteDialog(final int itemPosition, final String itemName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(UiUtilities.getThemedContext(getContext(), !isLightContent));
        builder.setTitle(R.string.quick_actions_delete);
        builder.setMessage(getResources().getString(R.string.quick_actions_delete_text, itemName));
        builder.setIcon(getMyApplication().getUIUtilities().getThemedIcon(R.drawable.ic_action_delete_dark));
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
        AlertDialog dialog = builder.show();
        int activeColorPrimaryResId = isLightContent ? R.color.active_color_primary_light : R.color.active_color_primary_dark;
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(getContext(), activeColorPrimaryResId));
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(getContext(), activeColorPrimaryResId));
    }

    @Override
    public void onActionsUpdated() {
        adapter.addItems(quickActionRegistry.getFilteredQuickActions());
    }

    public class QuickActionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements QuickActionItemTouchHelperCallback.OnItemMoveCallback {
        public static final int SCREEN_ITEM_TYPE = 1;
        public static final int SCREEN_HEADER_TYPE = 2;

        private static final int ITEMS_IN_GROUP = 6;

        private List<QuickAction> itemsList = new ArrayList<>();
        private final OnStartDragListener onStartDragListener;

        public QuickActionAdapter(OnStartDragListener onStartDragListener) {
            this.onStartDragListener = onStartDragListener;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = UiUtilities.getInflater(parent.getContext(), !isLightContent);
            if (viewType == SCREEN_ITEM_TYPE)
                return new QuickActionItemVH(inflater.inflate(R.layout.quick_action_list_item, parent, false));
            else
                return new QuickActionHeaderVH(inflater.inflate(R.layout.quick_action_list_header, parent, false));
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
            int viewType = getItemViewType(position);
            final QuickAction item = QuickActionFactory.produceAction(itemsList.get(position));

            if (viewType == SCREEN_ITEM_TYPE) {
                final QuickActionItemVH itemVH = (QuickActionItemVH) holder;

                itemVH.title.setText(item.getName(getContext()));
                itemVH.subTitle.setText(getResources().getString(R.string.quick_action_item_action, getActionPosition(position)));

                itemVH.icon.setImageDrawable(getMyApplication().getUIUtilities().getThemedIcon(item.getIconRes(getContext())));
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

                itemVH.container.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        CreateEditActionDialog dialog = CreateEditActionDialog.newInstance(item.id);
                        dialog.show(getFragmentManager(), AddQuickActionDialog.TAG);
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
            return itemsList.get(position).type == 0 ? SCREEN_HEADER_TYPE : SCREEN_ITEM_TYPE;
        }

        public void deleteItem(int position) {
            if (position == -1 || position >= itemsList.size())
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

            int lastPosition = itemsList.size() - 1;
            if (getItemViewType(lastPosition) == SCREEN_HEADER_TYPE) {
                itemsList.remove(lastPosition);
                notifyItemRemoved(lastPosition);
            }
        }

        private void showFABIfNotScrollable() {
            LinearLayoutManager layoutManager = (LinearLayoutManager) quickActionRV.getLayoutManager();
            int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
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
                int targetPosition = target.getAdapterPosition();
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
            public TextView title;
            public TextView subTitle;
            public ImageView icon;
            public View divider;
            public ImageView handleView;
            public ImageView closeBtn;
            public View container;

            public QuickActionItemVH(View itemView) {
                super(itemView);
//                AndroidUtils.setListItemBackground(itemView.getContext(), itemView, getMyApplication().getDaynightHelper().isNightMode());
                title = (TextView) itemView.findViewById(R.id.title);
                subTitle = (TextView) itemView.findViewById(R.id.subtitle);
                icon = (ImageView) itemView.findViewById(R.id.imageView);
                divider = itemView.findViewById(R.id.divider);
                handleView = (ImageView) itemView.findViewById(R.id.handle_view);
                closeBtn = (ImageView) itemView.findViewById(R.id.closeImageButton);
                container = itemView.findViewById(R.id.searchListItemLayout);

                handleView.setImageDrawable(getMyApplication().getUIUtilities().getThemedIcon(R.drawable.ic_action_reorder));
                closeBtn.setImageDrawable(getMyApplication().getUIUtilities().getThemedIcon(R.drawable.ic_action_remove_dark));
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
