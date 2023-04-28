package com.example.oispiot;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.util.Log;
//import android.view.MotionEvent;
import android.view.View;
//import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.github.angads25.toggle.interfaces.OnToggledListener;
import com.github.angads25.toggle.model.ToggleableView;
import com.github.angads25.toggle.widget.LabeledSwitch;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MainActivity extends AppCompatActivity {

    //MQTT Helper Initialization
    //MQTTHelper mqttHelper;

    //Data Manager Initialization
    DataManager dataManager;

    //Text View initialization
    TextView tmpTextView, humidTextView,
            lightTextView, lightlabelTextView, lightStateTextView,
            fanlabelTextView, fanStateTextView, connectionTextView;

    //Image View initialization
    ImageView lightbulbImage, fanImage;

    //Layout initialization
    LinearLayout lightLayout, fanLayout;

    //Transition initialization
    TransitionDrawable lightLayoutTransition, lightbulbImageTransition,
            fanLayoutTransition, fanImageTransition,
            autoButtonTransition;

    //Image View initialization
    ToggleButton autoButton;
    LabeledSwitch lightSwitch;
    LabeledSwitch fanSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dataManager = new DataManager(this);

        //Assign Text View
        tmpTextView = findViewById(R.id.tmpTextView);
        humidTextView = findViewById(R.id.humidTextView);
        lightTextView = findViewById(R.id.lightTextView);

        lightlabelTextView = findViewById(R.id.light_label);
        lightStateTextView = findViewById(R.id.light_state_label);

        fanlabelTextView = findViewById(R.id.fan_label);
        fanStateTextView = findViewById(R.id.fan_state_label);

        connectionTextView = findViewById(R.id.connection_state_textview);

        //Image
        lightbulbImage = findViewById(R.id.lightbulb_image);
        fanImage = findViewById(R.id.fan_image);

        //Button
        autoButton = findViewById(R.id.auto_button);

        //Layout
        lightLayout = findViewById(R.id.light_layout);
        fanLayout = findViewById(R.id.fan_layout);

        //Transition
        lightLayoutTransition = (TransitionDrawable) lightLayout.getBackground();
        lightbulbImageTransition = (TransitionDrawable) lightbulbImage.getBackground();

        fanLayoutTransition = (TransitionDrawable) fanLayout.getBackground();
        fanImageTransition = (TransitionDrawable) fanImage.getBackground();

        autoButtonTransition = (TransitionDrawable) autoButton.getBackground();

        lightSwitch = findViewById(R.id.light_switch);
        lightSwitch.setOnToggledListener(new OnToggledListener() {
            @Override
            public void onSwitched(ToggleableView toggleableView, boolean isOn) {
                // Implement your switching logic here
                if (isOn) {
                    //setUpButtonOn(lightLayoutTransition, lightbulbImageTransition, lightlabelTextView, lightStateTextView);
                    dataManager.publishData(getString(R.string.aio_light_topic), getString(R.string.aio_button_on_state));
                    lightSwitch.setEnabled(false);
                }
                else {
                    //setUpButtonOff(lightLayoutTransition, lightbulbImageTransition, lightlabelTextView, lightStateTextView);
                    dataManager.publishData(getString(R.string.aio_light_topic), getString(R.string.aio_button_off_state));
                    lightSwitch.setEnabled(false);
                }
            }
        });

        fanSwitch = findViewById(R.id.fan_switch);
        fanSwitch.setOnToggledListener(new OnToggledListener() {
            @Override
            public void onSwitched(ToggleableView toggleableView, boolean isOn) {
                // Implement your switching logic here
                if (isOn) {
                    //setUpButtonOn(fanLayoutTransition, fanImageTransition, fanlabelTextView, fanStateTextView);
                    dataManager.publishData(getString(R.string.aio_fan_topic), getString(R.string.aio_button_on_state));
                    fanSwitch.setEnabled(false);
                }
                else {
                    //setUpButtonOff(fanLayoutTransition, fanImageTransition, fanlabelTextView, fanStateTextView);
                    dataManager.publishData(getString(R.string.aio_fan_topic), getString(R.string.aio_button_off_state));
                    fanSwitch.setEnabled(false);
                }
            }
        });

        autoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (autoButton.isChecked()) {
                    disableManualButtons();

                    //set up transition
                    autoButtonTransition.startTransition(200);
                    autoButton.setTextColor(getResources().getColor(R.color.white_fe));

                    //sendMQTT
                    dataManager.publishData(getString(R.string.aio_auto_topic), getString(R.string.aio_button_on_state));
                    autoButton.setEnabled(false);
                }
                else {

                    //set up transition
                    autoButtonTransition.reverseTransition(200);
                    autoButton.setTextColor(getResources().getColor(R.color.dark));

                    //sendMQTT
                    dataManager.publishData(getString(R.string.aio_auto_topic), getString(R.string.aio_button_off_state));
                    autoButton.setEnabled(false);
                }
            }
        });

        dataManager.setCallback(new DataManager.DataManagerListener() {
            @Override
            public void onConnected() {
                connectionTextView.setText(R.string.connection_state_connected);
            }

            @Override
            public void onDisconnected() {
                connectionTextView.setText(R.string.connection_state_disconnected);
            }

            @Override
            public void messageArrive(String topic, String message) {

                if(topic.contains("nutnhan1")) {
                    lightSwitch.setEnabled(true);
                    if(message.contains("1")) {
                        lightSwitch.setOn(true);
                        setUpButtonOn(lightLayoutTransition, lightbulbImageTransition, lightlabelTextView, lightStateTextView);
                    }
                    else {
                        lightSwitch.setOn(false);
                        setUpButtonOff(lightLayoutTransition, lightbulbImageTransition, lightlabelTextView, lightStateTextView);
                    }
                }
                else if(topic.contains("nutnhan2")) {
                    fanSwitch.setEnabled(true);
                    if(message.contains("1")) {
                        fanSwitch.setOn(true);
                        setUpButtonOn(fanLayoutTransition, fanImageTransition, fanlabelTextView, fanStateTextView);
                    }
                    else {
                        fanSwitch.setOn(false);
                        setUpButtonOff(fanLayoutTransition, fanImageTransition, fanlabelTextView, fanStateTextView);
                    }
                }
                else if(topic.contains("nutnhan3")) {
                    autoButton.setEnabled(true);
                    if(message.contains("1")) {
                        //set buttons layouts to be off state
                        lightSwitch.setEnabled(false);
                        fanSwitch.setEnabled(false);
                    }
                    else {
                        lightSwitch.setEnabled(true);
                        lightSwitch.setOn(false);
                        lightStateTextView.setText(R.string.button_label_state_off);
                        fanSwitch.setEnabled(true);
                        fanSwitch.setOn(false);
                        fanStateTextView.setText(R.string.button_label_state_off);
                    }
                }

                else if(topic.contains("cambien1")) {
                    float messageInFloat = Float.parseFloat(message);
                    tmpTextView.setText(String.format("%.2f°C", messageInFloat));
                } else if (topic.contains("cambien2")) {
                    float messageInFloat = Float.parseFloat(message);
                    humidTextView.setText(String.format("%.2f%%", messageInFloat));
                } else if (topic.contains("cambien3")) {
                    float messageInFloat = Float.parseFloat(message);
                    lightTextView.setText(String.format("%.2f%%", messageInFloat));
                }
            }

            @Override
            public void onFailPublishing(String topic) {
                if (topic.contains("nutnhan1")) {
                    lightSwitch.setEnabled(true);
                }
                else if (topic.contains("nutnhan2")) {
                    fanSwitch.setEnabled(true);
                }
                else if (topic.contains("nutnhan3")) {
                    autoButton.setEnabled(true);
                }
            }
        });
    }

    void disableManualButtons() {
        //set up layout of buttons
        lightStateTextView.setText(R.string.button_label_state_auto);
        lightSwitch.setEnabled(false);

        fanStateTextView.setText(R.string.button_label_state_auto);
        fanSwitch.setEnabled(false);

        if (lightSwitch.isOn()) {
            //turn light off
            dataManager.publishData(getString(R.string.aio_light_topic), getString(R.string.aio_button_off_state));
        }
        if (fanSwitch.isOn()) {
            //turn fan off
            dataManager.publishData(getString(R.string.aio_fan_topic), getString(R.string.aio_button_off_state));
        }

    }

    void setUpButtonOn(TransitionDrawable layoutTransition, TransitionDrawable imageTransition, TextView labelTextView, TextView stateTextView) {
        layoutTransition.startTransition(200);
        imageTransition.startTransition(200);
        labelTextView.setTextColor(getResources().getColor(R.color.white_fe));
        stateTextView.setText(R.string.button_label_state_on);
    }

    void setUpButtonOff(TransitionDrawable layoutTransition, TransitionDrawable imageTransition, TextView labelTextView, TextView stateTextView) {
        layoutTransition.reverseTransition(200);
        imageTransition.reverseTransition(200);
        labelTextView.setTextColor(getResources().getColor(R.color.dark));
        stateTextView.setText(R.string.button_label_state_off);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}