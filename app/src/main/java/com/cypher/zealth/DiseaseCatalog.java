package com.cypher.zealth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DiseaseCatalog {

    private DiseaseCatalog() {}

    public enum SourceType {
        WHO_GHO,
        DISEASE_SH_COVID,
        CLIMATE_PROXY
    }

    public enum MetricType {
        MORTALITY,
        CASES,
        INCIDENCE,
        PREVALENCE,
        RISK_INDEX
    }

    public enum UpdateCadence {
        REALTIME,
        DAILY,
        WEEKLY,
        MONTHLY,
        ANNUAL
    }

    public static final class Capabilities {
        public final boolean supportsRealtime;
        public final boolean supportsSpreadIndex;
        public final boolean supportsClimateNowcast;

        public Capabilities(boolean r, boolean s, boolean c) {
            supportsRealtime = r;
            supportsSpreadIndex = s;
            supportsClimateNowcast = c;
        }

        public static Capabilities chronic() {
            return new Capabilities(false, false, false);
        }

        public static Capabilities infectious() {
            return new Capabilities(false, true, false);
        }

        public static Capabilities climateLinkedInfectious() {
            return new Capabilities(false, true, true);
        }

        public static Capabilities realtimeInfectious() {
            return new Capabilities(true, true, false);
        }
    }

    public static final class DiseaseOption {
        public final String id;
        public final String displayName;
        public final SourceType sourceType;
        public final MetricType metricType;
        public final UpdateCadence cadence;
        public final Capabilities caps;
        public final String unitLabel;
        public final String ghoKeyword;
        public final String[] requiredTokens;
        public final String pinnedIndicatorCode;

        private DiseaseOption(
                String id,
                String displayName,
                SourceType sourceType,
                MetricType metricType,
                UpdateCadence cadence,
                Capabilities caps,
                String unitLabel,
                String ghoKeyword,
                String[] requiredTokens,
                String pinnedIndicatorCode
        ) {
            this.id = id;
            this.displayName = displayName;
            this.sourceType = sourceType;
            this.metricType = metricType;
            this.cadence = cadence;
            this.caps = caps;
            this.unitLabel = unitLabel;
            this.ghoKeyword = ghoKeyword;
            this.requiredTokens = requiredTokens;
            this.pinnedIndicatorCode = pinnedIndicatorCode;
        }

        public static DiseaseOption who(
                String id,
                String displayName,
                String keyword,
                MetricType metricType,
                String unitLabel,
                Capabilities caps,
                String pinnedIndicatorCode,
                String... tokens
        ) {
            return new DiseaseOption(
                    id, displayName,
                    SourceType.WHO_GHO,
                    metricType,
                    UpdateCadence.ANNUAL,
                    caps,
                    unitLabel,
                    keyword,
                    tokens,
                    pinnedIndicatorCode
            );
        }

        public static DiseaseOption covid() {
            return new DiseaseOption(
                    "covid19",
                    "COVID-19",
                    SourceType.DISEASE_SH_COVID,
                    MetricType.CASES,
                    UpdateCadence.REALTIME,
                    Capabilities.realtimeInfectious(),
                    "cases / deaths",
                    null, null, null
            );
        }

        public static DiseaseOption climateProxy(
                String id,
                String displayName,
                Capabilities caps
        ) {
            return new DiseaseOption(
                    id, displayName,
                    SourceType.CLIMATE_PROXY,
                    MetricType.RISK_INDEX,
                    UpdateCadence.DAILY,
                    caps,
                    "risk index",
                    null, null, null
            );
        }
    }

    public static List<DiseaseOption> defaultList() {
        List<DiseaseOption> items = new ArrayList<>();

        items.add(DiseaseOption.who("ihd","Ischaemic heart disease",
                "ischaemic heart disease",MetricType.MORTALITY,
                "deaths",Capabilities.chronic(),null,
                "ischaemic","heart","death"));

        items.add(DiseaseOption.who("stroke","Stroke",
                "stroke",MetricType.MORTALITY,
                "deaths",Capabilities.chronic(),null,
                "stroke","death"));

        items.add(DiseaseOption.who("copd","COPD",
                "chronic obstructive pulmonary disease",MetricType.MORTALITY,
                "deaths",Capabilities.chronic(),null,
                "chronic","obstructive","pulmonary"));

        items.add(DiseaseOption.who("tb","Tuberculosis",
                "tuberculosis deaths",MetricType.MORTALITY,
                "deaths",Capabilities.infectious(),null,
                "tuberculosis","tb"));

        items.add(DiseaseOption.who("malaria","Malaria",
                "malaria deaths",MetricType.MORTALITY,
                "deaths",Capabilities.climateLinkedInfectious(),null,
                "malaria"));

        items.add(DiseaseOption.who("hiv","HIV/AIDS",
                "hiv aids deaths",MetricType.MORTALITY,
                "deaths",Capabilities.infectious(),null,
                "hiv","aids"));

        items.add(DiseaseOption.climateProxy(
                "cholera_proxy","Cholera (Climate Risk)",
                Capabilities.climateLinkedInfectious()));

        items.add(DiseaseOption.covid());

        return Collections.unmodifiableList(items);
    }
}
