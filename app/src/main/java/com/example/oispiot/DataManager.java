package com.example.oispiot;

import android.content.Context;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.HashMap;
import java.util.Map;

public class DataManager {
    private MQTTHelper mqttHelper;
    private Context context;
    private DataManagerListener listener;
    private static final int RETRY_TIME_OUT = 3;
    private static final int MAX_RETIRES = 3;
    private volatile boolean[] isStopMap = new boolean[3];
    private volatile boolean[] isTwoHopResponded = new boolean[3];

    public DataManager(Context context) {
        this.context = context;
        for (int i = 0; i < 3; i++) {
            isStopMap[i] = false;
            isTwoHopResponded[i] = false;
        }
        startMQTT();
    }



    public interface DataManagerListener {
        void onConnected();
        void onDisconnected();
        void messageArrive(String topic, String message);
        void onFailPublishing(String topic);
    }

    public void setCallback(DataManagerListener listener) {
        this.listener = listener;
    }

    public int convertTopictoIndex(String topic) {
        if(topic.contains("nutnhan1")) {
            return 0;
        }
        else if(topic.contains("nutnhan2")) {
            return 1;
        }
        else if(topic.contains("nutnhan3")) {
            return 2;
        }
        return -1;
    }

    public void publishData(String topic, String value) {
        Thread thread = new Thread(() -> {
            int counter = 0;
            int retries = 0;
            mqttHelper.sendMQTT(topic, value);
            boolean isStop;
            Log.d("PUBLISH", "1HOP: " + topic + "***" + value);
            while (true) {
                isStop = isStopMap[convertTopictoIndex(topic)];
                if(isStop) {
                    break;
                }
                counter += 1;
                if (retries >= MAX_RETIRES) {
                    break;
                }
                if (counter >= RETRY_TIME_OUT) {
                    counter = 0;
                    retries ++;
                    mqttHelper.sendMQTT(topic, value);
                }
                try {
                    Thread.sleep(1000); // wait for 1 second
                } catch (InterruptedException e) {
                    // handle the InterruptedException if necessary
                }
            }

            Log.d("PUBLISH", "Wait 2HOP: " + topic + "***" + value);
            //Wait for 2 hop response
            counter = 0;
            while (true) {
                counter ++;
                isStop = isTwoHopResponded[convertTopictoIndex(topic)];
                if(isStop) {
                    break;
                }
                if (counter > RETRY_TIME_OUT ) {
                    break;
                }
                try {
                    Thread.sleep(1000); // wait for 1 second
                } catch (InterruptedException e) {
                    // handle the InterruptedException if necessary
                }
            }

            if (!isStop) {
                Log.d("PUBLISH", "Failed: " + topic + "***" + value);
                listener.onFailPublishing(topic);
                return;
            }
            //reset stop var
            isStopMap[convertTopictoIndex(topic)] = false;
            isTwoHopResponded[convertTopictoIndex(topic)] = false;
            Log.d("PUBLISH", "Success: " + topic + "***" + value);
        });
        thread.start();
    }


    public void startMQTT() {
        mqttHelper = new MQTTHelper(context);
        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                listener.onConnected();
            }

            @Override
            public void connectionLost(Throwable cause) {
                listener.onDisconnected();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                Log.d("TEST", topic + "***" + message.toString());

                if (topic.contains("nutnhan1") || topic.contains("nutnhan2") ||topic.contains("nutnhan3")) {
                    isStopMap[convertTopictoIndex(topic)] = true;
                }
                else if (topic.contains("twohop")) {
                    String newString = message.toString().replace(String.valueOf("#"), "");
                    newString = newString.replace(String.valueOf("!"), "");
                    String[] parts = newString.split(":");
                    Log.d("PUBLISH", "2HOP: " + topic + " got " + parts[1]);
                    isTwoHopResponded[convertTopictoIndex(parts[1])] = true;
                }
                listener.messageArrive(topic, message.toString());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });
    }
}
