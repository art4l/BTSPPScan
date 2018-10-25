package com.art4l.btsppscan.config;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import timber.log.Timber;

public class DeviceConfigReader {

    private static final String KEY_SCANNERNAME1 = "SCANNERNAME1";
    private static final String KEY_SCANNERNAME2 = "SCANNERNAME2";
    private static final String KEY_SCANNERNAME3 = "SCANNERNAME3";
    private static final String KEY_SCANNERNAME4 = "SCANNERNAME4";
    private static final String KEY_SCANNER_MAC_ADDRESS_COLOR1 = "SCANNER_MAC_ADDRESS_COLOR1";
    private static final String KEY_SCANNER_MAC_ADDRESS_COLOR2 = "SCANNER_MAC_ADDRESS_COLOR2";
    private static final String KEY_SCANNER_MAC_ADDRESS_COLOR3 = "SCANNER_MAC_ADDRESS_COLOR3";
    private static final String KEY_SCANNER_MAC_ADDRESS_COLOR4 = "SCANNER_MAC_ADDRESS_COLOR4";
    private static final String KEY_COLOR1 = "COLOR1";
    private static final String KEY_COLOR2 = "COLOR2";
    private static final String KEY_COLOR3 = "COLOR3";
    private static final String KEY_COLOR4 = "COLOR4";

    private static final String KEY_MQTT_HOST = "MQTT_HOST";
    private static final String KEY_MQTT_PORT = "MQTT_PORT";
    private static final String KEY_DEVICE_ID = "DEVICE_ID";
    private static final String KEY_DEVICE_TYPE = "DEVICE_TYPE";
    private static final String KEY_DEVICE_SCANTO = "DEVICE_SCANTO";
    private static final String KEY_LOCATION = "LOCATION";
    private static final String KEY_CAMUNDAFLOW = "CAMUNDAFLOW";
    private static final String KEY_FLOWENGINE_IP = "FLOWENGINE_IP";


    public static DeviceConfig readFromConfig(String filename) {
        // get config
        File file = Environment.getExternalStoragePublicDirectory("Config");

        File[] configs = file.listFiles();

        if (configs != null) {
            // search for file
            for (File f : configs) {
                if (f.getName().equals(filename)) {
                    try {
                        // try to parse result
                        InputStream input = new FileInputStream(f);
                        // parse stream
                        DeviceConfig config = parseInputStream(input);
                        // close stream
                        input.close();
                        // return
                        return config;
                    } catch (Exception e) {
                        Timber.e(e);
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
        return new DeviceConfig();
    }

    private static DeviceConfig parseInputStream(InputStream input) {
        DeviceConfig device = new DeviceConfig();
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(input, null);
            parser.nextTag();
            parser.require(XmlPullParser.START_TAG, null, "properties");
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String name = parser.getName();

                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if (!TextUtils.isEmpty(name) && name.equals("entry")) {
                            // create device from XML
                            switch (parser.getAttributeValue(null,"key")) {
                                case KEY_SCANNERNAME1:
                                    device.scannerName1 = parser.nextText();
                                    break;
                                case KEY_SCANNERNAME2:
                                    device.scannerName2 = parser.nextText();
                                    break;
                                case KEY_SCANNERNAME3:
                                    device.scannerName3 = parser.nextText();
                                    break;
                                case KEY_SCANNERNAME4:
                                    device.scannerName4 = parser.nextText();
                                    break;
                                case KEY_SCANNER_MAC_ADDRESS_COLOR1:
                                    device.scannerMacAddress_Color1 = parser.nextText();
                                    break;
                                case KEY_SCANNER_MAC_ADDRESS_COLOR2:
                                    device.scannerMacAddress_Color2 = parser.nextText();
                                    break;
                                case KEY_SCANNER_MAC_ADDRESS_COLOR3:
                                    device.scannerMacAddress_Color3 = parser.nextText();
                                    break;
                                case KEY_SCANNER_MAC_ADDRESS_COLOR4:
                                    device.scannerMacAddress_Color4 = parser.nextText();
                                    break;
                                case KEY_COLOR1:
                                    device.color1 = parser.nextText();
                                    break;
                                case KEY_COLOR2:
                                    device.color2 = parser.nextText();
                                    break;
                                case KEY_COLOR3:
                                    device.color3 = parser.nextText();
                                    break;
                                case KEY_COLOR4:
                                    device.color4 = parser.nextText();
                                    break;
                                case KEY_MQTT_HOST:
                                    device.mqttHost = parser.nextText();
                                    break;
                                case KEY_MQTT_PORT:
                                    device.mqqtPort = parser.nextText();
                                    break;
                                case KEY_DEVICE_ID:
                                    device.deviceId = parser.nextText();
                                    break;
                                case KEY_DEVICE_TYPE:
                                    device.deviceType = parser.nextText();
                                    break;
                                case KEY_DEVICE_SCANTO:
                                    device.deviceScanTo = parser.nextText();
                                    break;
                                case KEY_LOCATION:
                                    device.location = parser.nextText();
                                    break;
                                case KEY_CAMUNDAFLOW:
                                    device.camundaFlow = parser.nextText();
                                    break;
                                case KEY_FLOWENGINE_IP:
                                    device.flowengineIp = parser.nextText();
                                    break;
                            }
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        break;
                }
                eventType = parser.next();
            }
            // return the device we created
            return device;
        } catch (XmlPullParserException | IOException e) {
            Timber.e(e);
            e.printStackTrace();
        }
        return new DeviceConfig();
    }

    public static String readFromConfigNetwork(String filename) {
        // get config
        File file = Environment.getExternalStoragePublicDirectory("Config");

        File[] configs = file.listFiles();

        if (configs != null) {
            // search for file
            for (File f : configs) {
                if (f.getName().equals(filename)) {
                    try {
                        // try to parse result
                        InputStream input = new FileInputStream(f);
                        // parse stream
                        String config = parseInputStreamNetwork(input);
                        // close stream
                        input.close();
                        // return
                        return config;
                    } catch (Exception e) {
                        Timber.e(e);
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
        return null;
    }

    private static String parseInputStreamNetwork(InputStream input) {
        String flowengineIp = null;
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(input, null);
            parser.nextTag();
            parser.require(XmlPullParser.START_TAG, null, "properties");
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String name = parser.getName();

                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if (!TextUtils.isEmpty(name) && name.equals("entry")) {
                            // create device from XML
                            switch (parser.getAttributeValue(null,"key")) {
                                case KEY_FLOWENGINE_IP:
                                    flowengineIp = parser.nextText();
                                    break;

                            }
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        break;
                }
                eventType = parser.next();
            }
            // return the device we created
            return flowengineIp;
        } catch (XmlPullParserException | IOException e) {
            Timber.e(e);
            e.printStackTrace();
        }
        return null;
    }

}
