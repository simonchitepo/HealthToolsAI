package com.cypher.zealth.network;

import com.cypher.zealth.models.GhoModels;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface GhoApiService {

    @Headers("Accept: application/json")
    @GET("api/Indicator")
    Call<GhoModels.GhoResponse> searchIndicators(
            @Query("$filter") String filter,
            @Query("$select") String select,
            @Query("$top") int top
    );

    @Headers("Accept: application/json")
    @GET("api/{indicatorCode}")
    Call<GhoModels.GhoResponse> getIndicatorData(
            @Path(value = "indicatorCode", encoded = true) String indicatorCode,
            @Query("$filter") String filter
    );

    @Headers("Accept: application/json")
    @GET("api/{indicatorCode}")
    Call<GhoModels.GhoResponse> getIndicatorData(
            @Path(value = "indicatorCode", encoded = true) String indicatorCode,
            @Query("$filter") String filter,
            @Query("$select") String select,
            @Query("$top") Integer top
    );
}
