package com.cypher.zealth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class WomensGuidance {

    public static String buildGeneralWellnessTips(
            WomensCycleModel.CycleInfo info,
            List<String> symptoms,
            String flow
    ) {
        return buildGuidanceInternal(info, symptoms, flow, false);
    }

   
    public static String buildGuidance(
            WomensCycleModel.CycleInfo info,
            List<String> symptoms,
            String flow
    ) {
        return buildGuidanceInternal(info, symptoms, flow, true);
    }

   
    public static String shortGuideForNotification(WomensCycleModel.CycleInfo info) {
        if (info == null || info.phase == null) {
            return "Daily wellness: hydrate, eat regularly, and take a short walk if you can.";
        }

        switch (info.phase) {
            case MENSTRUAL:
                return "Cycle estimate: period phase — hydrate, gentle movement, rest when needed.";
            case FOLLICULAR:
                return "Cycle estimate: follicular phase — steady meals, strength or skill practice if you feel up to it.";
            case OVULATION:

                return "Cycle estimate: mid-cycle — warm up well, stay hydrated, keep routines balanced.";
            case LUTEAL:
            default:
                return "Cycle estimate: luteal phase — steady meals (protein+fiber), sleep focus, lighter workouts if needed.";
        }
    }

    
    public static String encouragementForNotification(WomensCycleModel.CycleInfo info) {
        if (info == null || info.phase == null) {
            return "Small consistent habits today can support your overall wellbeing.";
        }

        switch (info.phase) {
            case MENSTRUAL:
                return "Be kind to your body today. Rest and gentle movement both count.";
            case FOLLICULAR:
                return "If you feel momentum today, use it—keep it sustainable and simple.";
            case OVULATION:
                return "Show up for yourself today—balance effort with recovery.";
            case LUTEAL:
            default:
                return "Keep it steady today: hydration, regular meals, and a little movement if it helps.";
        }
    }

    
    private static String buildGuidanceInternal(
            WomensCycleModel.CycleInfo info,
            List<String> symptoms,
            String flow,
            boolean includeFlowLine
    ) {
        StringBuilder sb = new StringBuilder();

        WomensCycleModel.Phase phase = (info != null) ? info.phase : null;
        String phaseTitle = (info != null && info.phaseTitle != null) ? info.phaseTitle : "Cycle estimate";

        
        sb.append(phaseTitle).append(" • general wellness\n\n");

        if (phase == null) {
            sb.append("• Hydrate and eat regularly.\n");
            sb.append("• Light movement (like a short walk) can support energy and mood.\n");
            sb.append("• Prioritize sleep and stress-reduction routines.\n");
        } else {
            switch (phase) {
                case MENSTRUAL:
                    sb.append("Focus: comfort + steady routines.\n");
                    sb.append("• Gentle movement (walks, stretching) if it feels good.\n");
                    sb.append("• Hydration, warm shower/heat pack for comfort.\n");
                    sb.append("• Aim for regular meals with iron-rich foods (leafy greens, beans, fish/meat) and vitamin C foods.\n");
                    sb.append("• If you feel low energy, consider lighter workouts and more rest.\n");
                    break;

                case FOLLICULAR:
                    sb.append("Focus: energy building + consistency.\n");
                    sb.append("• If you feel good, try strength training or skill-based workouts.\n");
                    sb.append("• Balanced meals: protein + fiber + carbs to support activity.\n");
                    sb.append("• Great time to plan routines and habits you want to keep.\n");
                    break;

                case OVULATION:
                    sb.append("Focus: balanced performance + recovery.\n");
                    sb.append("• Warm up well and pay attention to form.\n");
                    sb.append("• Hydration and regular meals support stable energy.\n");
                    sb.append("• If you feel achy, choose lower-impact options and mobility work.\n");
                    break;

                case LUTEAL:
                default:
                    sb.append("Focus: steady meals + calmer routines.\n");
                    sb.append("• Prioritize protein + fiber each meal to support steady energy.\n");
                    sb.append("• Consider magnesium-rich foods (nuts, seeds, legumes, dark chocolate).\n");
                    sb.append("• Choose movement that feels doable (lighter cardio, mobility, yoga).\n");
                    sb.append("• Sleep and wind-down routines can help you feel more stable.\n");
                    break;
            }
        }

        List<String> safeSymptoms = (symptoms == null) ? new ArrayList<>() : symptoms;

        List<String> lines = new ArrayList<>();
        for (String s : safeSymptoms) {
            if (s == null) continue;
            String key = s.trim().toLowerCase(Locale.US);

            if (key.isEmpty()) continue;

            if (containsAny(key, "cramp", "cramps")) {
                lines.add("• Cramps: warmth (heat pack), gentle stretching, hydration, and lighter activity if needed.");
            } else if (containsAny(key, "headache")) {
                lines.add("• Headache: hydrate, eat regularly, and take a short screen break.");
            } else if (containsAny(key, "bloat", "bloating")) {
                lines.add("• Bloating: a short walk and smaller/lighter meals may feel better; go easy on very salty foods.");
            } else if (containsAny(key, "acne", "breakout")) {
                lines.add("• Skin: gentle skincare, consistent sleep, and balanced meals can support overall skin comfort.");
            } else if (containsAny(key, "fatigue", "tired", "low energy")) {
                lines.add("• Fatigue: prioritize sleep, get daylight, and choose low-intensity movement if it helps.");
            } else if (containsAny(key, "mood", "anx", "anxiety", "irrit", "irritable", "stress")) {
                lines.add("• Mood: try a quick reset—slow breathing, a short walk, or a calming routine.");
            } else if (containsAny(key, "nausea")) {
                lines.add("• Nausea: sip fluids, try smaller meals, and keep snacks simple.");
            }
            
        }

        if (!lines.isEmpty()) {
            sb.append("\nBased on what you logged:\n");
            
            for (String line : dedupe(lines)) {
                sb.append(line).append("\n");
            }
        }


        if (includeFlowLine && flow != null && !flow.trim().isEmpty()) {
            sb.append("\nFlow today: ").append(flow.trim()).append(".");
            sb.append("\n");
        }

    
        sb.append("\nFor personal tracking only. This is not medical advice. ");
        sb.append("If symptoms are severe, unusual for you, or you’re concerned, consider contacting a qualified clinician.");

        return sb.toString().trim();
    }

    private static boolean containsAny(String hay, String... needles) {
        if (hay == null) return false;
        for (String n : needles) {
            if (n != null && !n.isEmpty() && hay.contains(n)) return true;
        }
        return false;
    }

    private static List<String> dedupe(List<String> xs) {
        List<String> out = new ArrayList<>();
        for (String s : xs) {
            if (s == null) continue;
            if (!out.contains(s)) out.add(s);
        }
        return out;
    }
}
