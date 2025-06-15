package com.cypher.zealth.network;
import com.google.gson.annotations.SerializedName;

public class NominatimModels {

    public static class Place {
        @SerializedName("display_name")
        public String displayName;

        @SerializedName("lat")
        public String lat;

        @SerializedName("lon")
        public String lon;
    }
}
