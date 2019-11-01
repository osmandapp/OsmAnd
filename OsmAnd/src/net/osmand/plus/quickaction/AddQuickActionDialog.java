package net.osmand.plus.quickaction;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;

import java.util.List;

/**
 * Created by rosty on 12/22/16.
 */

public class AddQuickActionDialog extends DialogFragment {

    public static final String TAG = AddQuickActionDialog.class.getSimpleName();
    
    private boolean isLightContent;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        OsmandApplication application = (OsmandApplication) getActivity().getApplication();
        isLightContent = application.getSettings().isLightContent() && !application.getDaynightHelper().isNightMode();

        return new Dialog(UiUtilities.getThemedContext(getActivity(), !isLightContent, R.style.Dialog90Light, R.style.Dialog90Dark), getTheme());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        List<QuickAction> active = ((MapActivity) getActivity())
                .getMapLayers()
                .getQuickActionRegistry()
                .getQuickActions();

        View root = inflater.inflate(R.layout.quick_action_add_dialog, container, false);
        Adapter adapter = new Adapter(QuickActionFactory.produceTypeActionsListWithHeaders(active));

        TextView tvTitle = root.findViewById(R.id.tvTitle);
        RecyclerView recyclerView = (RecyclerView) root.findViewById(R.id.recycler_view);
        Button btnDismiss = (Button) root.findViewById(R.id.btnDismiss);

        tvTitle.setTextColor(ContextCompat.getColor(getContext(),
                isLightContent ? R.color.text_color_primary_light : R.color.text_color_primary_dark));
        
        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        recyclerView.setAdapter(adapter);

        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    public class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int HEADER = 1;
        private static final int ITEM = 2;

        private List<QuickAction> data;

        public class ItemViewHolder extends RecyclerView.ViewHolder {

            private TextView title;
            private ImageView icon;

            public ItemViewHolder(View v) {
                super(v);

                title = (TextView) v.findViewById(R.id.title);
                icon = (ImageView) v.findViewById(R.id.image);
            }
        }

        public class HeaderViewHolder extends RecyclerView.ViewHolder {

            private TextView header;
            private View divider;

            public HeaderViewHolder(View v) {
                super(v);

                header = (TextView) v.findViewById(R.id.header);
                divider = v.findViewById(R.id.divider);
            }
        }

        public Adapter(List<QuickAction> data) {
            this.data = data;
        }


        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            if (viewType == HEADER) {

                return new HeaderViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.quick_action_add_dialog_header, parent, false));

            } else {

                return new ItemViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.quick_action_add_dialog_item, parent, false));
            }
        }


        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

            final QuickAction action = data.get(position);

            if (getItemViewType(position) == HEADER) {

                HeaderViewHolder headerHolder = (HeaderViewHolder) holder;

                headerHolder.header.setText(action.getNameRes());
                if (position == 0) headerHolder.divider.setVisibility(View.GONE);
                else headerHolder.divider.setVisibility(View.VISIBLE);

            } else {

                ItemViewHolder itemHolder = (ItemViewHolder) holder;

                itemHolder.title.setText(action.getNameRes());
                itemHolder.title.setTextColor(ContextCompat.getColor(getContext(), 
                        isLightContent ? R.color.text_color_primary_light : R.color.text_color_primary_dark));
                itemHolder.icon.setImageResource(action.getIconRes());

                itemHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        CreateEditActionDialog dialog = CreateEditActionDialog.newInstance(action.type);
                        dialog.show(getFragmentManager(), CreateEditActionDialog.TAG);

                        dismiss();
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return data.size();

        }

        @Override
        public int getItemViewType(int position) {

            if (data.get(position).type == 0)
                return HEADER;

            return ITEM;
        }
    }
}
