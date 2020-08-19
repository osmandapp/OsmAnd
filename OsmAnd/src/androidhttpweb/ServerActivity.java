package androidhttpweb;

import android.net.TrafficStats;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import net.osmand.plus.R;

public class ServerActivity extends AppCompatActivity {
    private boolean initialized = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.server_activity);
        findViewById(R.id.Button01).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!initialized) {
                    ((TextView) findViewById(R.id.TextView02)).setText("Click second button to deactivate server");
                    initServer();
                }
            }
        });
        findViewById(R.id.Button03).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (initialized) {
                    ((TextView) findViewById(R.id.TextView02)).setText("Click first button to activate server");
                    deInitServer();
                }
            }
        });
    }

    private void initServer() {
        final String ip = "localhost";
        final int port = 9000;
        final int THREAD_ID = 10000;
        TrafficStats.setThreadStatsTag(THREAD_ID);
        TinyWebServer.startServer(ip, port, "/web/public_html");
        TinyWebServer.object = ServerActivity.this;
    }

    private void deInitServer() {
        TinyWebServer.stopServer();
        initialized = false;
    }

    @Override
    protected void onDestroy() {
        deInitServer();
        super.onDestroy();
    }
}
