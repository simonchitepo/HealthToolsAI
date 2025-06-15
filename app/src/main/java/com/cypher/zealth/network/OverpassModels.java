package com.cypher.zealth.network;
import java.util.List;
import java.util.Map;

public class OverpassModels {

    public static class OverpassResponse {
        public List<Element> elements;
    }

    public static class Element {
        public String type;
        public long id;
        public double lat;
        public double lon;

        public Center center;

        public Map<String, String> tags;

        public double getLat() {
            return center != null ? center.lat : lat;
        }

        public double getLon() {
            return center != null ? center.lon : lon;
        }
    }

    public static class Center {
        public double lat;
        public double lon;
    }
}
