package net.osmand.turnScreenOn.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import net.osmand.turnScreenOn.R;

public abstract class PluginStandardDialog {
    private Dialog dialog;
    private Activity activity;
    private LayoutInflater inflater;
    private View dialogView;
    private TextView tvDialogTitle;
    private TextView btnDialogCancel;
    private FrameLayout flSettingsContainer;

    public PluginStandardDialog(Activity activity) {
        this.activity = activity;

        inflater = activity.getLayoutInflater();
        dialogView = inflater.inflate(R.layout.dialog_layout, null, false);

        tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        btnDialogCancel = dialogView.findViewById(R.id.btnDialogCancel);
        flSettingsContainer = dialogView.findViewById(R.id.flDialogContent);

        createDialog();
    }

    private void createDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setView(dialogView);

        ViewGroup content = prepareElements();

        if(content!=null) {
            flSettingsContainer.addView(content);
        }

        btnDialogCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeDialog();
            }
        });

        dialog = builder.create();
    }

    public void setHeader(String header){
        tvDialogTitle.setText(header);
    }

    public void showDialog() {
        dialog.show();
    }

    public void closeDialog() {
        dialog.cancel();
    }

    public abstract ViewGroup prepareElements();
}
