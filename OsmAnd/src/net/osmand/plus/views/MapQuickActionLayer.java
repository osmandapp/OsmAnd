package net.osmand.plus.views;

import android.graphics.Canvas;
import android.view.View;
import android.widget.ImageButton;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionFactory;
import net.osmand.plus.quickaction.QuickActionRegistry;
import net.osmand.plus.quickaction.QuickActionsWidget;

/**
 * Created by okorsun on 23.12.16.
 */

public class MapQuickActionLayer extends OsmandMapLayer implements QuickActionRegistry.QuickActionUpdatesListener, QuickAction.QuickActionSelectionListener{

    private final MapActivity       mapActivity;
    private final OsmandApplication app;
    private final OsmandSettings    settings;
    private final QuickActionRegistry quickActionRegistry;

    private ImageButton quickActionButton;
    private QuickActionsWidget quickActionsWidget;

    public MapQuickActionLayer(MapActivity activity) {
        this.mapActivity = activity;
        app = activity.getMyApplication();
        settings = activity.getMyApplication().getSettings();
        quickActionRegistry = activity.getMapLayers().getQuickActionRegistry();
    }


    @Override
    public void initLayer(OsmandMapTileView view) {
        quickActionsWidget = (QuickActionsWidget) mapActivity.findViewById(R.id.quick_action_widget);
        quickActionButton = (ImageButton) mapActivity.findViewById(R.id.map_quick_actions_button);
        quickActionButton.setVisibility(settings.QUICK_ACTION.get() ? View.VISIBLE : View.GONE);
        quickActionButton.setImageResource(R.drawable.ic_action_quit_dark);
        quickActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (quickActionsWidget.getVisibility() == View.VISIBLE){

                    quickActionsWidget.setVisibility(View.GONE);
                    quickActionRegistry.setUpdatesListener(null);
                    quickActionsWidget.setSelectionListener(null);

                }else {

                    quickActionsWidget.setActions(quickActionRegistry.getQuickActions());
                    quickActionsWidget.setVisibility(View.VISIBLE);
                    quickActionRegistry.setUpdatesListener(MapQuickActionLayer.this);
                    quickActionsWidget.setSelectionListener(MapQuickActionLayer.this);
                }
            }
        });
    }

    public void refreshLayer() {
        quickActionButton.setVisibility(settings.QUICK_ACTION.get() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
        // do nothing
    }

    @Override
    public void destroyLayer() {

    }

    @Override
    public boolean drawInScreenPixels() {
        return true;
    }


    @Override
    public void onActionsUpdated() {
        quickActionsWidget.setActions(quickActionRegistry.getQuickActions());
    }

    @Override
    public void onActionSelected(QuickAction action) {
        action.execute(mapActivity);
    }
}
