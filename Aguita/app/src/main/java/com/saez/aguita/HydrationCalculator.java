package com.saez.aguita;

public final class HydrationCalculator {
    private HydrationCalculator() {}

    /**
     * Estimación orientativa para adultos sanos, no prescripción médica.
     * Base simple: 30 ml/kg/día. La actividad añade una reserva pequeña y configurable.
     * Se limita el resultado de la estimación automática para evitar recomendaciones extremas.
     */
    public static int estimateMl(float weightKg, String activity) {
        float safeWeight = Math.max(35f, Math.min(weightKg, 140f));
        int base = Math.round(safeWeight * 30f);
        int extra = 0;
        if ("Moderada".equals(activity)) extra = 300;
        if ("Alta".equals(activity)) extra = 600;
        int total = base + extra;
        return Math.max(1500, Math.min(total, 3500));
    }

    public static int bmiCategoryValue(float weightKg, int heightCm) {
        if (heightCm <= 0) return 0;
        double h = heightCm / 100.0;
        return (int)Math.round((weightKg / (h * h)) * 10.0);
    }
}
