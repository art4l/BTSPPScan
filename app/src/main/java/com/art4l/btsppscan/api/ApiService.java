package com.art4l.btsppscan.api;



import com.art4l.btsppscan.api.model.DeviceInitializationDto;
import com.art4l.btsppscan.api.model.Template;

import java.util.List;

import io.reactivex.Flowable;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface ApiService {
    @Headers("Content-Type: application/json")
    @POST("smartpickv2/devices")
    Flowable<List<Template>> getInitializationData(@Body DeviceInitializationDto body);
}