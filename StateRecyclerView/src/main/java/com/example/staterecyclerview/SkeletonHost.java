package com.example.staterecyclerview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.shimmer.ShimmerFrameLayout;

/**
 * 骨架屏宿主，挂到父 FrameLayout 上并叠层铺满。
 * 自动识别 ShimmerFrameLayout 根布局，show/hide 联动扫光启停。
 */
public class SkeletonHost {

    @NonNull
    private final View skeletonView;
    @Nullable
    private final ShimmerFrameLayout shimmerLayout;
    private boolean showing = false;

    public SkeletonHost(@NonNull FrameLayout parent) {
        this(parent, R.layout.layout_srv_skeleton, parent.getContext());
    }

    public SkeletonHost(@NonNull FrameLayout parent, @LayoutRes int layoutRes) {
        this(parent, layoutRes, parent.getContext());
    }

    /**
     * @param themeCtx 仅用于 inflate 默认骨架布局（如需要 M3 主题）
     */
    public SkeletonHost(@NonNull FrameLayout parent,
                        @LayoutRes int layoutRes,
                        @NonNull Context themeCtx) {
        if (layoutRes == 0) {
            throw new IllegalArgumentException("skeleton layoutRes must not be 0");
        }
        this.skeletonView = LayoutInflater.from(themeCtx)
                .inflate(layoutRes, parent, false);

        if (skeletonView instanceof ShimmerFrameLayout) {
            this.shimmerLayout = (ShimmerFrameLayout) skeletonView;
        } else {
            this.shimmerLayout = null;
        }

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        if (skeletonView.getParent() == null) {
            parent.addView(skeletonView, lp);
        }
        skeletonView.setVisibility(View.GONE);
    }

    public void show() {
        if (showing) return;
        showing = true;
        skeletonView.setVisibility(View.VISIBLE);
        if (shimmerLayout != null) {
            shimmerLayout.startShimmer();
        }
    }

    public void hide() {
        if (!showing) return;
        showing = false;
        skeletonView.setVisibility(View.GONE);
        if (shimmerLayout != null) {
            shimmerLayout.stopShimmer();
        }
    }

    public boolean isShowing() {
        return showing;
    }

    @NonNull
    public View getView() {
        return skeletonView;
    }
}