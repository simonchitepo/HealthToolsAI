package com.cypher.zealth.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GhoModels {

    public static class GhoResponse {

        @SerializedName("value")
        public List<GhoValue> value;
        @SerializedName("@odata.nextLink")
        public String nextLink;
    }

    public static class GhoValue {

        @SerializedName("IndicatorCode")
        public String indicatorCode;

        @SerializedName("IndicatorName")
        public String indicatorName;

        @SerializedName("IndicatorShortName")
        public String indicatorShortName;

        @SerializedName("SpatialLocationCode")
        public String spatialLocationCode;

        @SerializedName("SpatialDimType")
        public String spatialDimType;

        @SerializedName("TimeDim")
        public Integer timeDim;

        @SerializedName("NumericValue")
        public Double numericValue;

        // Optional dimensions used by some GHO indicators
        @SerializedName("Dim1")
        public String dim1;

        @SerializedName("Dim2")
        public String dim2;
    }
}
