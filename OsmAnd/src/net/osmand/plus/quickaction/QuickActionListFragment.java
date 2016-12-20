package net.osmand.plus.quickaction;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;

import java.util.ArrayList;
import java.util.List;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

/**
 * Created by okorsun on 20.12.16.
 */

public class QuickActionListFragment extends BaseOsmAndFragment {
    public static final String TAG = QuickActionListFragment.class.getSimpleName();

    RecyclerView quickActionRV;
    QuickActionAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.quick_action_list, container, false);

        quickActionRV = (RecyclerView) view.findViewById(R.id.recycler_view);
//        quickActionRV.setBackgroundColor(
//                getResources().getColor(
//                        getMyApplication().getSettings().isLightContent() ? R.color.bg_color_light
//                                : R.color.bg_color_dark));


        adapter = new QuickActionAdapter();
        quickActionRV.setAdapter(adapter);
        quickActionRV.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter.addItems(createMockDada());

        return view;
    }

    private List<QuickActionItem> createMockDada() {
        List<QuickActionItem> result = new ArrayList<>();
        for (int i = 0; i < 15; i ++){
            result.add(new QuickActionItem(R.string.favorite, R.drawable.ic_action_flag_dark));
        }

        return result;
    }


    public class QuickActionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int SCREEN_ITEM_TYPE  = 1;
        private static final int SCREEN_TITLE_TYPE = 2;

        private static final int ITEMS_IN_GROUP = 6;

        private List<QuickActionItem> itemsList = new ArrayList<>();

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
                QuickActionItemVH itemVH = (QuickActionItemVH) holder;
                itemVH.icon.setImageResource(item.getDrawableRes());
                itemVH.title.setText(item.getNameRes());
                itemVH.subTitle.setText("Action " + getActionPosition(position));      //TODO: get proper string

                LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                dividerParams.setMargins(isShortDivider(position) ? dpToPx(56f) : 0, 0, 0, 0);
            } else {
                QuickActionHeaderVH headerVH = (QuickActionHeaderVH) holder;
                headerVH.headerName.setText("Screen " + (position/(ITEMS_IN_GROUP + 1) + 1));   //TODO: get proper string
            }
        }

        @Override
        public int getItemCount() {
            return itemsList.size();
        }

        @Override
        public int getItemViewType(int position) {
            return itemsList.get(position).isHeader() ? SCREEN_TITLE_TYPE : SCREEN_ITEM_TYPE;
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

        public class QuickActionItemVH extends RecyclerView.ViewHolder {
            public TextView  title;
            public TextView  subTitle;
            public ImageView icon;
            public View divider;

            public QuickActionItemVH(View itemView) {
                super(itemView);
                title = (TextView) itemView.findViewById(R.id.title);
                subTitle = (TextView) itemView.findViewById(R.id.subtitle);
                icon = (ImageView) itemView.findViewById(R.id.imageView);
                divider = itemView.findViewById(R.id.divider);

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
}
