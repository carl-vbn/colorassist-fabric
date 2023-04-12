package dev.carlvbn.colorassist;

import net.minecraft.util.TranslatableOption;

enum ColorAttributionMode implements TranslatableOption {
    AVERAGE, DOMINANT_HALF, DOMINANT_THIRD, DOMINANT_QUARTER;

    @Override
    public int getId() {
        return this == AVERAGE ? 0 : (this == DOMINANT_HALF ? 1 : (this == DOMINANT_THIRD ? 2 : 3));
    }

    @Override
    public String getTranslationKey() {
        return "colorassist.colorAttributionMode."+name().toLowerCase();
    }

    public static ColorAttributionMode byId(Integer id) {
        return id == 0 ? AVERAGE : (id == 1 ? DOMINANT_HALF : (id == 2 ? DOMINANT_THIRD : (id == 3 ? DOMINANT_QUARTER : null)));
    }
}