package com.cypher.zealth.network;

import com.cypher.zealth.network.models.CovidCountry;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface CovidApiService {

  
    @GET("v3/covid-19/countries")
    Call<List<CovidCountry>> getCovidByCountry(
            @Query("allowNull") boolean allowNull
    );
}
