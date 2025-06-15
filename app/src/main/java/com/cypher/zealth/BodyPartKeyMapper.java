package com.cypher.zealth;

import java.util.Locale;
import java.util.regex.Pattern;

public final class BodyPartKeyMapper {

    private BodyPartKeyMapper() {}

    private static final Pattern P_HEAD     = Pattern.compile("\\b(head|skull|face)\\b");
    private static final Pattern P_NECK     = Pattern.compile("\\b(neck|throat|cervical)\\b");
    private static final Pattern P_CHEST    = Pattern.compile("\\b(chest|thorax|pectoral|breast)\\b");
    private static final Pattern P_ABDOMEN  = Pattern.compile("\\b(abdomen|stomach|belly|navel|umbilical)\\b");
    private static final Pattern P_BACK     = Pattern.compile("\\b(back|spine|lumbar|thoracic_back|dorsal)\\b");
    private static final Pattern P_ARMS     = Pattern.compile("\\b(arm|arms|shoulder|upperarm|forearm|elbow|wrist|hand|hands)\\b");
    private static final Pattern P_LEGS     = Pattern.compile("\\b(leg|legs|hip|thigh|knee|calf|ankle|foot|feet)\\b");
    private static final Pattern P_SKIN     = Pattern.compile("\\b(skin|rash|dermal|dermis)\\b");

    public static String toKey(String nodeName) {
        String n = normalize(nodeName);
        if (n.isEmpty()) return "Other";
        if (matches(P_HEAD, n)) return "Head";
        if (matches(P_NECK, n)) return "Neck";
        if (matches(P_CHEST, n)) return "Chest";
        if (matches(P_ABDOMEN, n)) return "Abdomen";
        if (matches(P_BACK, n)) return "Back";
        if (matches(P_ARMS, n)) return "Arms";
        if (matches(P_LEGS, n)) return "Legs";
        if (matches(P_SKIN, n)) return "Skin";

        return "Other";
    }

   
    private static String normalize(String s) {
        if (s == null) return "";
        String n = s.trim().toLowerCase(Locale.US);

        n = n.replace('_', ' ')
                .replace('-', ' ')
                .replace('/', ' ')
                .replace('\\', ' ');

      
        n = n.replaceAll("\\s+", " ");

        return n;
    }

    private static boolean matches(Pattern p, String text) {
        return p.matcher(text).find();
    }
}
