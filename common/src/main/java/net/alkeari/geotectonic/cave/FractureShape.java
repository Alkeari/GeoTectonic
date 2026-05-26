package net.alkeari.geotectonic.cave;

import net.minecraft.util.Mth;

public class FractureShape {

    public static float widthRadius(int y) {
        if (y > 50)   return 1.5f;
        if (y >= 20)  return lerp(1.5f, 3.5f, (50f - y) / 30f);
        if (y >= 0)   return lerp(3.5f, 5.0f, (20f - y) / 20f);
        if (y >= -30) return lerp(5.0f, 7.0f, -y / 30f);
        if (y >= -60) return lerp(7.0f, 9.0f, (-30 - y) / 30f);
        return 9.0f;
    }

    public static float heightRadius(int y) {
        if (y > 50)   return 3.5f;
        if (y >= 20)  return lerp(4.0f, 5.5f, (50f - y) / 30f);
        if (y >= 0)   return lerp(5.5f, 6.5f, (20f - y) / 20f);
        if (y >= -30) return lerp(6.5f, 7.5f, -y / 30f);
        if (y >= -60) return lerp(7.5f, 8.5f, (-30 - y) / 30f);
        return 8.5f;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * Mth.clamp(t, 0f, 1f);
    }
}
