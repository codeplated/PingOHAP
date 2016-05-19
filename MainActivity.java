package codeplated.pingohap;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements PingConnection.PingObserver{

    private PingConnection pingConnection;
    public static int counter;
    private static final String TAG = "Main Activity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        counter=0;

        pingConnection=PingConnection.getInstance();
        try {
            Log.d(TAG, "Trying to initializing pingConnection Object");
            pingConnection.initialize(new URL("http://10.0.2.2:18001"), this );
            if(pingConnection.isRunning()==true){
                counter ++;
                Toast.makeText(this, "Ping "+ counter, Toast.LENGTH_LONG ).show();
            }
        } catch (MalformedURLException e) {
            Log.d(TAG, "Error in initializing pingConnection Object");
            e.printStackTrace();
            counter=0;

        }


    }

    @Override
    public void handlePingResponse(IncomingMessage incomingMessage) {
        TextView textView=(TextView) findViewById(R.id.ping);
        Log.d(TAG,"Getting response from server");
        if (null != incomingMessage) {
            int messageType = incomingMessage.integer8();
            switch (messageType) {
                case 0x03:
                    long pong_id = incomingMessage.integer32();
                    textView.append("\npong-message-arrived: " + pong_id);
                    Log.d(TAG,"pong response");
                    break;
                case 0x08:
                    textView.append("\nmessage-type-container");

                    long itemIdentifier = incomingMessage.integer32();
                    long itemDataParentIdentifier = incomingMessage.integer32();
                    String itemDataName = incomingMessage.text();
                    String itemDataDescription = incomingMessage.text();
                    boolean itemDataInternal = incomingMessage.binary8();
                    Log.d(TAG,"pong response1");
                    textView.append("item-identifier: " + itemIdentifier + "\n");
                    textView.append("item-data-parent-identifier: " + itemDataParentIdentifier + "\n");
                    textView.append("item-data-name: " + itemDataName + "\n");
                    textView.append("item-data-description: " + itemDataDescription + "\n");
                    textView.append("item-data-internal: " + itemDataInternal + "\n");
                    break;
                default:
                    textView.append("Unrecognised message type");
                    Log.d(TAG,"pong response3");
                    break;
            }
        } else {
            textView.setText("No incoming msgs yet.");
            Log.d(TAG,"pong response3");
        }
    }
    @Override
    public void onStart(){
        super.onStart();
        Log.d(TAG, "Starting Ping connect from onStart");
        Toast.makeText(this, "Connecting with server", Toast.LENGTH_SHORT).show();
        pingConnection.start(this);
    }

    @Override
    protected void onStop() {

        super.onStop();


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "stopping Ping connect from onDestroy");
        counter=0;
        pingConnection.stop();

    }

    public void pingButtonClick(View view){
        Log.d(TAG, "Trying to ping on click");

        if(pingConnection.isRunning()==true){
            counter ++;
            Toast.makeText(this, "Ping "+ counter, Toast.LENGTH_LONG ).show();
        }
        pingConnection.doPing();
        if(pingConnection.isRunning()==false) {
            Toast.makeText(this, "Server not conencted", Toast.LENGTH_SHORT).show();

        }

    }
}
