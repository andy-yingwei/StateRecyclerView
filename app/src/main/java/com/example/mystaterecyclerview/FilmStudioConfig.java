package com.example.mystaterecyclerview;

import android.content.Context;
import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.mystaterecyclerview.helper.*;

/**
 * 同原注释结构，仅调整 defaultConfig 配色为浅色内容区
 */
public final class FilmStudioConfig {

    /* ===================== 卡片 ===================== */
    @ColorInt
    public final int backgroundColor;
    public final int backgroundRadius;

    /* ===================== 封面 ===================== */
    public final int coverHeight;
    public final int coverRoundRadius;
    @DrawableRes
    public final int coverPlaceholder;
    @DrawableRes
    public final int coverError;
    public final boolean skipMemoryCache;
    public final DiskCacheStrategy diskCacheStrategy;

    /* ===================== 标题 ===================== */
    public final int nameTextSize;
    @ColorInt
    public final int nameTextColor;

    /* ===================== 信息 ===================== */
    public final int infoTextSize;
    @ColorInt
    public final int infoTextColor;

    /* ===================== 收藏 ===================== */
    public final int favoriteIconSize;
    @DrawableRes
    public final int favoriteIcon;

    private FilmStudioConfig(Builder b) {
        this.backgroundColor = b.backgroundColor;
        this.backgroundRadius = b.backgroundRadius;
        this.coverHeight = b.coverHeight;
        this.coverRoundRadius = b.coverRoundRadius;
        this.coverPlaceholder = b.coverPlaceholder;
        this.coverError = b.coverError;
        this.skipMemoryCache = b.skipMemoryCache;
        this.diskCacheStrategy = b.diskCacheStrategy;
        this.nameTextSize = b.nameTextSize;
        this.nameTextColor = b.nameTextColor;
        this.infoTextSize = b.infoTextSize;
        this.infoTextColor = b.infoTextColor;
        this.favoriteIconSize = b.favoriteIconSize;
        this.favoriteIcon = b.favoriteIcon;
    }

    /* =========================================================
     * 默认配置（浅色内容区 · 适配 StateRecyclerView CONTENT 态）
     * 之前是黑底白字，在 StateRecyclerView 里会“看不见数据”
     * 现在改为：白卡片 + 深灰标题 + 中灰信息 + 金色收藏
     * ========================================================= */
    public static FilmStudioConfig defaultConfig(Context ctx) {
        return new Builder()
                /* 卡片 */
                .backgroundColor(Color.parseColor("#FFFFFF"))
                .backgroundRadius(8)

                /* 封面 */
                .coverHeight(180)
                .coverRoundRadius(6)
                .coverPlaceholder(R.drawable.placeholder_video)
                .coverError(R.drawable.placeholder_video)
                .skipMemoryCache(false)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)

                /* 标题 */
                .nameTextSize(16)
                .nameTextColor(Color.parseColor("#1A1A1A"))

                /* 信息 */
                .infoTextSize(12)
                .infoTextColor(Color.parseColor("#666666"))

                /* 收藏 */
                .favoriteIconSize(20)
                .favoriteIcon(R.drawable.ic_star_fill)

                .build();
    }

    /* =========================================================
     * Builder（保持原结构，默认值也同步浅色化）
     * ========================================================= */
    public static final class Builder {

        private int backgroundColor = Color.parseColor("#FFFFFF");
        private int backgroundRadius = 8;

        private int coverHeight = 180;
        private int coverRoundRadius = 6;
        private int coverPlaceholder = R.drawable.placeholder_video;
        private int coverError = R.drawable.placeholder_video;
        private boolean skipMemoryCache = false;
        private DiskCacheStrategy diskCacheStrategy = DiskCacheStrategy.AUTOMATIC;

        private int nameTextSize = 16;
        private int nameTextColor = Color.parseColor("#1A1A1A");

        private int infoTextSize = 12;
        private int infoTextColor = Color.parseColor("#666666");

        private int favoriteIconSize = 20;
        private int favoriteIcon = R.drawable.ic_star_fill;

        public Builder backgroundColor(@ColorInt int color) {
            this.backgroundColor = color; return this;
        }
        public Builder backgroundRadius(int dp) {
            this.backgroundRadius = dp; return this;
        }
        public Builder coverHeight(int dp) {
            this.coverHeight = dp; return this;
        }
        public Builder coverRoundRadius(int dp) {
            this.coverRoundRadius = dp; return this;
        }
        public Builder coverPlaceholder(@DrawableRes int res) {
            this.coverPlaceholder = (res == 0) ? R.drawable.placeholder_video : res; return this;
        }
        public Builder coverError(@DrawableRes int res) {
            this.coverError = (res == 0) ? R.drawable.placeholder_video : res; return this;
        }
        public Builder skipMemoryCache(boolean skip) {
            this.skipMemoryCache = skip; return this;
        }
        public Builder diskCacheStrategy(DiskCacheStrategy strategy) {
            this.diskCacheStrategy = strategy; return this;
        }
        public Builder nameTextSize(int sp) {
            this.nameTextSize = sp; return this;
        }
        public Builder nameTextColor(@ColorInt int color) {
            this.nameTextColor = color; return this;
        }
        public Builder infoTextSize(int sp) {
            this.infoTextSize = sp; return this;
        }
        public Builder infoTextColor(@ColorInt int color) {
            this.infoTextColor = color; return this;
        }
        public Builder favoriteIconSize(int dp) {
            this.favoriteIconSize = dp; return this;
        }
        public Builder favoriteIcon(@DrawableRes int res) {
            this.favoriteIcon = res; return this;
        }

        public Builder copyFrom(FilmStudioConfig old) {
            if (old == null) return this;
            this.backgroundColor = old.backgroundColor;
            this.backgroundRadius = old.backgroundRadius;
            this.coverHeight = old.coverHeight;
            this.coverRoundRadius = old.coverRoundRadius;
            this.coverPlaceholder = old.coverPlaceholder;
            this.coverError = old.coverError;
            this.skipMemoryCache = old.skipMemoryCache;
            this.diskCacheStrategy = old.diskCacheStrategy;
            this.nameTextSize = old.nameTextSize;
            this.nameTextColor = old.nameTextColor;
            this.infoTextSize = old.infoTextSize;
            this.infoTextColor = old.infoTextColor;
            this.favoriteIconSize = old.favoriteIconSize;
            this.favoriteIcon = old.favoriteIcon;
            return this;
        }

        public FilmStudioConfig build() {
            return new FilmStudioConfig(this);
        }
    }
}
