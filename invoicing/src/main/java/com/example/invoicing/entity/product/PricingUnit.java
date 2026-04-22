package com.example.invoicing.entity.product;

import java.util.Map;

public enum PricingUnit {
    PCS, KG, TON, M3, LITER, METER, HOUR;

    private static final Map<PricingUnit, Map<String, String>> LABELS = Map.ofEntries(
        Map.entry(PCS,   Map.of("FI", "kpl", "SV", "st",  "EN", "pcs")),
        Map.entry(KG,    Map.of("FI", "kg",  "SV", "kg",  "EN", "kg")),
        Map.entry(TON,   Map.of("FI", "t",   "SV", "t",   "EN", "t")),
        Map.entry(M3,    Map.of("FI", "m3",  "SV", "m3",  "EN", "m3")),
        Map.entry(LITER, Map.of("FI", "l",   "SV", "l",   "EN", "l")),
        Map.entry(METER, Map.of("FI", "m",   "SV", "m",   "EN", "m")),
        Map.entry(HOUR,  Map.of("FI", "h",   "SV", "h",   "EN", "h"))
    );

    public String getLabel(String language) {
        String lang = language != null ? language.toUpperCase() : "FI";
        return LABELS.getOrDefault(this, Map.of()).getOrDefault(lang, "pcs");
    }
}
