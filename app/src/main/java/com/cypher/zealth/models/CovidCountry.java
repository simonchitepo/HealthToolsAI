package com.cypher.zealth.network.models;

import com.google.gson.annotations.SerializedName;

public class CovidCountry {

    @SerializedName("country")
    public String country;

    @SerializedName("updated")
    public Long updated; 

    @SerializedName("deaths")
    public Double deaths;

    @SerializedName("todayDeaths")
    public Double todayDeaths;

    @SerializedName("countryInfo")
    public CountryInfo countryInfo;

    public static class CountryInfo {
        @SerializedName("iso3")
        public String iso3;

        @SerializedName("lat")
        public Double lat;

        @SerializedName("long")
        public Double lon;
    }
}
