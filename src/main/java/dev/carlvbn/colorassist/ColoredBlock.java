package dev.carlvbn.colorassist;

import net.minecraft.block.Block;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class ColoredBlock implements Comparable<ColoredBlock> {
    private Block block;
    private Color averageColor;
    private Color dominantHalf;
    private Color dominantThird;
    private Color dominantQuarter;

    public static ColorAttributionMode colorAttributionMode = ColorAttributionMode.DOMINANT_HALF;

    public ColoredBlock(Block block, Color averageColor, Color dominantHalf, Color dominantThird, Color dominantQuarter) {
        this.block = block;
        this.averageColor = averageColor;
        this.dominantHalf = dominantHalf;
        this.dominantThird = dominantThird;
        this.dominantQuarter = dominantQuarter;
    }

    public Block getBlock() {
        return block;
    }

    public Color getColor() {
        switch (colorAttributionMode) {
            case AVERAGE -> {return averageColor;}
            case DOMINANT_HALF -> {return dominantHalf;}
            case DOMINANT_THIRD -> {return dominantThird;}
            case DOMINANT_QUARTER -> {return dominantQuarter;}
        }

        return averageColor;
    }

    private int[] stepKey(Color col, int repetitions) {
        float r = col.getRed() / 255.0F;
        float g = col.getGreen() / 255.0F;
        float b = col.getBlue() / 255.0F;
        float lum = (float) Math.sqrt(.241 * r + .691 * g + .068 * b);
        float[] hsv = new float[3];
        Color.RGBtoHSB(col.getRed(), col.getGreen(), col.getBlue(), hsv);
        int h2 = (int)(hsv[0] * repetitions);
        int lum2 = (int)(lum * repetitions);
        int v2 = (int)(hsv[2] * repetitions);

        int primary = 2;
        if (hsv[1] < 0.1) {
            primary = hsv[2] > 0.5 ? 1 : 0;
        }

        return new int[]{primary, h2,lum2, v2};
    }

    private int compareKey(int[] a, int[] b) {
        for (int i = 0; i<a.length; i++) {
            int comp = Integer.compare(a[i], b[i]);
            if (comp != 0) return comp;
        }

        return 0;
    }

    @Override
    public int compareTo(@NotNull ColoredBlock o) {
        return compareKey(stepKey(this.getColor(), 8), stepKey(o.getColor(), 8));
    }

    public float[] getHSB() {
        Color color = getColor();
        return Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
    }

    public static int getColorError(Color col, Color target) {
        int redDiff = col.getRed() - target.getRed();
        int greenDiff = col.getGreen() - target.getGreen();
        int blueDiff = col.getBlue() - target.getBlue();

        return redDiff*redDiff + greenDiff*greenDiff + blueDiff*blueDiff;
    }
}

