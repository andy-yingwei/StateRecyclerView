package com.example.mystaterecyclerview.helper;

import android.content.Context;
import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;

public final class Colors {

    private Colors() {}

    /** R.color.xxx → 颜色 */
    @ColorInt
    public static int of(Context ctx, int colorRes) {
        return ContextCompat.getColor(ctx, colorRes);
    }

    /** "#FFFFFF" / "#FF4081" → 颜色 */
    @ColorInt
    public static int of(String hex) {
        return Color.parseColor(hex);
    }

    /** 0xFFFFFFFF 原样返回 */
    @ColorInt
    public static int of(int colorInt) {
        return colorInt;
    }
}
