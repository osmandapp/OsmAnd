package net.osmand.plus.track;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.GPXUtilities;
import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.widgets.EditTextEx;

import org.apache.commons.logging.Log;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class GpxEditDescriptionDialogFragment extends BaseOsmAndDialogFragment {

    public static final String TAG = GpxEditDescriptionDialogFragment.class.getSimpleName();
    private static final Log log = PlatformUtil.getLog(GpxEditDescriptionDialogFragment.class);

    public static final String CONTENT_KEY = "content_key";

    private EditTextEx editableHtml;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_edit_gpx_description, container, false);
        editableHtml = view.findViewById(R.id.description);

        view.findViewById(R.id.btn_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        view.findViewById(R.id.btn_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!saveGpx(editableHtml.getText().toString())) {
                    dismiss();
                }
            }
        });

        Bundle args = getArguments();
        if (args != null) {
            String html = args.getString(CONTENT_KEY);
            if (html != null) {
                editableHtml.setText(html);
            }
        }

        return view;
    }

    private boolean saveGpx(final String html) {
        if (html == null) {
            return false;
        }
        final FragmentManager manager = getFragmentManager();
        if (manager == null) {
            return false;
        }

        final Fragment readGpxFragment = manager.findFragmentByTag(GpxReadDescriptionDialogFragment.TAG);
        final TrackMenuFragment trackMenuFragment = (TrackMenuFragment) manager.findFragmentByTag(TrackMenuFragment.TAG);
        if (trackMenuFragment == null) {
            return false;
        }

        File file = trackMenuFragment.getDisplayHelper().getFile();
        GPXUtilities.GPXFile gpx = trackMenuFragment.getGpx();
        gpx.metadata.getExtensionsToWrite().put("desc", html);

        new SaveGpxAsyncTask(file, gpx, new SaveGpxAsyncTask.SaveGpxListener() {
            @Override
            public void gpxSavingStarted() {
            }

            @Override
            public void gpxSavingFinished(Exception errorMessage) {
                if (errorMessage != null) {
                    log.error(errorMessage);
                }

                trackMenuFragment.onGpxChanged();
                if (readGpxFragment != null) {
                    Bundle args = readGpxFragment.getArguments();
                    if (args != null) {
                        args.putString(GpxReadDescriptionDialogFragment.CONTENT_KEY, html);
                        readGpxFragment.onResume();
                    }
                }
                dismiss();

            }
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return true;
    }

    public static void showInstance(AppCompatActivity activity, String description) {
        Bundle args = new Bundle();
        args.putString(GpxEditDescriptionDialogFragment.CONTENT_KEY, description);
        GpxEditDescriptionDialogFragment fragment = new GpxEditDescriptionDialogFragment();
        fragment.setArguments(args);
        fragment.show(activity.getSupportFragmentManager(), GpxEditDescriptionDialogFragment.TAG);
    }

}
