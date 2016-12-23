package net.osmand.plus.quickaction;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;

import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.R;

import java.util.List;

/**
 * Created by rosty on 12/22/16.
 */

public class AddQuickActionDialog extends DialogFragment {

    public static final String TAG = AddQuickActionDialog.class.getSimpleName();

    protected QuickAction.QuickActionSelectionListener selectionListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new Dialog(new ContextThemeWrapper(getActivity(), R.style.Dialog90), getTheme());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        View root = inflater.inflate(R.layout.quick_action_add_dialog, container, false);
        Adapter adapter = new Adapter(QuickActionFactory.produceTypeActionsList());

        RecyclerView recyclerView = (RecyclerView) root.findViewById(R.id.recycler_view);
        Button btnDismiss = (Button) root.findViewById(R.id.btnDismiss);

        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        recyclerView.setAdapter(adapter);

        return root;
    }

    public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

        private List<QuickAction> data;

        public class ViewHolder extends RecyclerView.ViewHolder {

            private TextView title;
            private ImageView icon;

            public ViewHolder(View v) {
                super(v);

                title = (TextView) v.findViewById(R.id.title);
                icon = (ImageView) v.findViewById(R.id.image);
            }
        }

        public Adapter(List<QuickAction> data) {
            this.data = data;
        }


        @Override
        public Adapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.quick_action_add_dialog_item, parent, false);

            return new ViewHolder(v);
        }


        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

            final QuickAction action = data.get(position);

            holder.title.setText(action.nameRes);
            holder.icon.setImageResource(action.iconRes);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (selectionListener != null)
                        selectionListener.onActionSelected(action);

                    dismiss();
                }
            });
        }

        @Override
        public int getItemCount() {
            return data.size();

        }
    }
}
