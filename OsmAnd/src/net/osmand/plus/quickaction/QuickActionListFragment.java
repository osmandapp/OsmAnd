package net.osmand.plus.quickaction;

import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static net.osmand.plus.R.id.toolbar;

/**
 * Created by okorsun on 20.12.16.
 */

public class QuickActionListFragment extends BaseOsmAndFragment {
    public static final String TAG = QuickActionListFragment.class.getSimpleName();

    RecyclerView quickActionRV;
    QuickActionAdapter adapter;
    ItemTouchHelper touchHelper;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.quick_action_list, container, false);

        quickActionRV = (RecyclerView) view.findViewById(R.id.recycler_view);
//        quickActionRV.setBackgroundColor(
//                getResources().getColor(
//                        getMyApplication().getSettings().isLightContent() ? R.color.bg_color_light
//                                : R.color.bg_color_dark));


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


        adapter.addItems(createMockDada());
        Toolbar          toolbar = (Toolbar) view.findViewById(R.id.custom_toolbar);
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

        return view;
    }

    private List<QuickActionItem> createMockDada() {
        List<QuickActionItem> result = new ArrayList<>();
        for (int i = 0; i < 5; i ++){
            result.add(new QuickActionItem(R.string.favorite, R.drawable.ic_action_flag_dark));
            result.add(new QuickActionItem(R.string.poi, R.drawable.ic_action_flag_dark));
            result.add(new QuickActionItem(R.string.map_marker, R.drawable.ic_action_flag_dark));
        }

        return result;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    public class QuickActionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements QuickActionItemTouchHelperCallback.OnItemMoveCallback {
        public static final int SCREEN_ITEM_TYPE   = 1;
        public static final int SCREEN_HEADER_TYPE = 2;

        private static final int ITEMS_IN_GROUP = 6;

        private List<QuickActionItem> itemsList = new ArrayList<>();
        private final OnStartDragListener onStartDragListener;

        public QuickActionAdapter(OnStartDragListener onStartDragListener){
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
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            int             viewType = getItemViewType(position);
            QuickActionItem item     = itemsList.get(position);

            if (viewType == SCREEN_ITEM_TYPE) {
                final QuickActionItemVH itemVH = (QuickActionItemVH) holder;

                itemVH.title.setText(item.getNameRes());
                itemVH.subTitle.setText(getResources().getString(R.string.quick_action_item_action, getActionPosition(position)));

                itemVH.icon.setImageDrawable(getMyApplication().getIconsCache().getThemedIcon(item.getDrawableRes()));
                itemVH.handleView.setImageDrawable(getMyApplication().getIconsCache().getThemedIcon(R.drawable.ic_action_reorder));
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

                LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                dividerParams.setMargins(isShortDivider(position) ? dpToPx(56f) : 0, 0, 0, 0);
            } else {
                QuickActionHeaderVH headerVH = (QuickActionHeaderVH) holder;
                headerVH.headerName.setText(getResources().getString(R.string.quick_action_item_action, position/(ITEMS_IN_GROUP + 1) + 1));
            }
        }

        @Override
        public int getItemCount() {
            return itemsList.size();
        }

        @Override
        public int getItemViewType(int position) {
            return itemsList.get(position).isHeader() ? SCREEN_HEADER_TYPE : SCREEN_ITEM_TYPE;
        }

        public void deleteItem(int position) {
            itemsList.remove(position);

            notifyItemRemoved(position);
        }

        public void addItems(List<QuickActionItem> data) {
            List<QuickActionItem> resultList = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                if (i % ITEMS_IN_GROUP == 0)
                    resultList.add(QuickActionItem.createHeaderItem());

                resultList.add(data.get(i));
            }

            itemsList = resultList;
            notifyDataSetChanged();
        }


        private int getActionPosition(int globalPosition) {
            return globalPosition % (ITEMS_IN_GROUP + 1);
        }

        private boolean isShortDivider(int globalPosition) {
            return getActionPosition(globalPosition) == ITEMS_IN_GROUP || (globalPosition + 1) == getItemCount();
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
                int targetPosition  = target.getAdapterPosition();

                Collections.swap(itemsList, selectedPosition, targetPosition);
                if (selectedPosition - targetPosition < -1){
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

        public class QuickActionItemVH extends RecyclerView.ViewHolder {
            public TextView  title;
            public TextView  subTitle;
            public ImageView icon;
            public View divider;
            public ImageView handleView;

            public QuickActionItemVH(View itemView) {
                super(itemView);
                title = (TextView) itemView.findViewById(R.id.title);
                subTitle = (TextView) itemView.findViewById(R.id.subtitle);
                icon = (ImageView) itemView.findViewById(R.id.imageView);
                divider = itemView.findViewById(R.id.divider);
                handleView = (ImageView) itemView.findViewById(R.id.handle_view);

                itemView.findViewById(R.id.closeImageButton).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        QuickActionAdapter.this.deleteItem(getAdapterPosition());
                    }
                });

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
