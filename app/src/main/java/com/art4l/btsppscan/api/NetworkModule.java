package com.art4l.btsppscan.api;


import com.art4l.btsppscan.config.DeviceConfigReader;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;


public class NetworkModule {

    public static ApiService ProvideProjectorService(final String flowengineIp) {
        // todo: outsource this properly to injected configHelper
        Retrofit retrofit = new Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create()) // make sure calls happen properly for the retrofit 2 format
                .addConverterFactory(GsonConverterFactory.create()) // set response converter to Gson to accept Content-Type json

//              .baseUrl("http://192.168.0.177:8080/")
                .baseUrl(flowengineIp) // set the base url for retrofit, this has to prefix "http://" or "https://", and has to postfix "/"
                // for local testing with flowengine, set this to the lan ip of your machine
                .build();

        return retrofit.create(ApiService.class);
    }



}
