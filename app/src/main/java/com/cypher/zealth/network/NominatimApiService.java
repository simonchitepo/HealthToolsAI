package com.cypher.zealth.network;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public interface NominatimApiService {

    @Headers({
            "Accept: application/json",
            "User-Agent: Zealth/1.0 (contact: example@invalid.local)"
    })
    @GET("search")
    Call<List<NominatimModels.Place>> search(
            @Query("q") String q,
            @Query("format") String format,
            @Query("limit") int limit
    );
}
