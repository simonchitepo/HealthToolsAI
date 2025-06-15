package com.cypher.zealth;

import java.util.List;
import java.util.Map;

public class Facility {
    public final String id;
    public final String name;
    public final String type; 
    public final double lat;
    public final double lon;
    public final Double distanceKm;
    public final String address;
    public final String phone;
    public final String website;
    public final Integer capacityBeds;          
    public final Integer equipmentIndex;        
    public final Integer stockIndex;            
    public final List<String> majorDrugs;      
    public final Map<String, String> tags;
    public final boolean isFeatured;

    public Facility(
            String id,
            String name,
            String type,
            double lat,
            double lon,
            Double distanceKm,
            String address,
            String phone,
            String website,
            Integer capacityBeds,
            Integer equipmentIndex,
            Integer stockIndex,
            List<String> majorDrugs,
            Map<String, String> tags,
            boolean isFeatured
    ) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.lat = lat;
        this.lon = lon;
        this.distanceKm = distanceKm;
        this.address = address;
        this.phone = phone;
        this.website = website;
        this.capacityBeds = capacityBeds;
        this.equipmentIndex = equipmentIndex;
        this.stockIndex = stockIndex;
        this.majorDrugs = majorDrugs;
        this.tags = tags;
        this.isFeatured = isFeatured;
    }
}
