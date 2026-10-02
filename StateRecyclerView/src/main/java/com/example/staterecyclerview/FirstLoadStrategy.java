package com.example.staterecyclerview;

import androidx.annotation.NonNull;

/**
 * 首次加载视觉策略
 * <p>
 * AUTO  —— 旧逻辑：按当前状态 + itemCount 自动判定
 * ALWAYS —— 强制走骨架屏（冷启动首次加载）
 * NEVER —— 永远走内容保留 + 刷新动画（下拉刷新、错误页重试）
 */
public enum FirstLoadStrategy {
    AUTO,
    ALWAYS,
    NEVER;

    @NonNull
    public static FirstLoadStrategy getDefault() {
        return AUTO;
    }
}