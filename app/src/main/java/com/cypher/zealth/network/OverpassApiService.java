package com.cypher.zealth.network;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface OverpassApiService {

    @GET("api/interpreter")
    Call<OverpassModels.OverpassResponse> interpreter(
            @Query("data") String data
    );
}
