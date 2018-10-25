package com.art4l.btsppscan.socket;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@StringDef({
        MqttCommand.INITIALIZE,
        MqttCommand.SHOW_SCREENS,
        MqttCommand.ADD_SCREENS,
        MqttCommand.REMOVE_STICKYTILE,
        MqttCommand.SEND_INPUT,
        MqttCommand.SEND_VALUE,
        MqttCommand.SEND_INTERACTION,
        MqttCommand.SET_SCANNER_MODE,
        MqttCommand.END_FLOW,
        MqttCommand.SET_VOICE_INTERACTIONS,     //send all possible voice interactions, workaround for Vuzix Bug
        MqttCommand.ACK,                //acknowledge message send back
        MqttCommand.CALIBRATE,          //Projector Command
        MqttCommand.GRID,               //Projector Command
        MqttCommand.SWITCH_OFF,         //Projector Command
        MqttCommand.REBOOT              //Projector Command


})
public @interface MqttCommand {
    String INITIALIZE = "INITIALIZE";
    String SHOW_SCREENS = "SHOW_SCREENS";
    String ADD_SCREENS = "ADD_SCREENS";
    String REMOVE_STICKYTILE = "REMOVE_STICKYTILE";
    String SEND_INPUT = "SEND_INPUT";
    String SEND_VALUE = "SEND_VALUE";
    String SEND_INTERACTION = "SEND_INTERACTION";
    String SET_SCANNER_MODE = "SET_SCANNER_MODE";
    String CALIBRATE = "CALIBRATE";
    String GRID = "GRID";
    String SWITCH_OFF = "SWITCH_OFF";
    String REBOOT = "REBOOT";
    String END_FLOW = "END_FLOW";
    String SET_VOICE_INTERACTIONS = "SET_VOICE_INTERACTION";
    String ACK = "ACK";

}