package com.secure.letschat;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.security.SecureRandom;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;


public class MainActivity extends AppCompatActivity {

    private TextInputLayout chatIdInputLayout;

    private String clientId;

    private MqttAndroidClient client;

    private static final String TAG = MainActivity.class.getSimpleName();

    Button generate_unique_key, copy, share;
    ImageButton paste;
    TextView generate_key_txt;
    TextInputEditText unique_key;
    String key_str = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        generate_unique_key.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String key = generateRandomString(16);
                generate_key_txt.setText(key);
            }
        });
        copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("label", generate_key_txt.getText().toString());
                clipboard.setPrimaryClip(clip);
            }
        });
        paste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                String pasteData = "";
                if (!(clipboard.hasPrimaryClip())) {

                }
                else if (!(clipboard.getPrimaryClipDescription().hasMimeType(MIMETYPE_TEXT_PLAIN))) {

                }
                else {
                    ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                    pasteData = item.getText().toString();
                    unique_key.setText(pasteData);
                }
            }
        });
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                key_str = generate_key_txt.getText().toString();
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                //shareIntent.putExtra(Intent.EXTRA_SUBJECT, key_str);
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.secure.letschat");
                shareIntent.putExtra(Intent.EXTRA_TEXT, key_str);
                //String app_url = "https://play.google.com/store/apps/details?id=com.secure.exammaterialt";
                //shareIntent.putExtra(Intent.EXTRA_TEXT,app_url);
                startActivity(Intent.createChooser(shareIntent, "Share via"));
            }
        });

        SplashScreen.flag = true;

        if (isOnline()){
            connect();
        } else {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
        }

        chatIdInputLayout = findViewById(R.id.chatIdTextInputLayout);

        Button chatButton = findViewById(R.id.chatButton);

        chatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean noError = true;
                String editTextString = chatIdInputLayout.getEditText().getText().toString();
                if (editTextString.isEmpty()) {
                    chatIdInputLayout.setError(getResources().getString(R.string.error_string));
                    noError = false;
                } else {
                    chatIdInputLayout.setError(null);
                }

                // Text input Field is not empty and there is internet connection
                if (noError && isOnline()) {
                    //Subscribe to the topic
                    subscribe(editTextString.toLowerCase().trim());
                    Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                    //Pass the Unique ID through an intent to the Chat Activity
                    intent.putExtra("subscribeTo", editTextString.toLowerCase());

                    startActivity(intent);

                } else if(noError && !isOnline())  {
                    Toast.makeText(MainActivity.this, "No internet connection", Toast.LENGTH_SHORT).show();
                }
            }

        });


    }

    private void init() {
        generate_unique_key = findViewById(R.id.generate_key);
        copy = findViewById(R.id.copy);
        share = findViewById(R.id.share);
        paste = findViewById(R.id.paste);
        generate_key_txt = findViewById(R.id.generate_key_txt);
        unique_key = findViewById(R.id.unique_key);
    }

    //Method to check for internet connectivity
    private boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null &&
                cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    //Method to connect our broker service
    private void connect(){

        clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), "tcp://broker.hivemq.com:1883",
                clientId);

        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Log.d(TAG, "onSuccess");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.d(TAG, "onFailure");

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    //Method to subscribe to a specific topic in this case the Unique ID
    private void subscribe(final String topic) {
        int qos = 1;
        try {
            IMqttToken subToken = client.subscribe(topic, qos);
            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // The message was published

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    // The subscription could not be performed, maybe the user was not
                    // authorized to subscribe on the specified topic e.g. using wildcards

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    boolean doubleBackToExitPressedOnce = false;
    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            MainActivity.this.finish();
            moveTaskToBack(true);
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
    }

    public static String generateRandomString(int length){
        String CHAR_LOWER = "abcdefghijklmnopqrstuvwxyz";
        String CHAR_UPPER = CHAR_LOWER.toUpperCase();
        String NUMBER = "0123456789";
        String DATA_FOR_RANDOM_STRING = CHAR_LOWER + CHAR_UPPER + NUMBER;
        SecureRandom random = new SecureRandom();
        if (length < 1) throw new IllegalArgumentException();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int rndCharAt = random.nextInt(DATA_FOR_RANDOM_STRING.length());
            char rndChar = DATA_FOR_RANDOM_STRING.charAt(rndCharAt);
            sb.append(rndChar);
        }
        return sb.toString();
    }
}