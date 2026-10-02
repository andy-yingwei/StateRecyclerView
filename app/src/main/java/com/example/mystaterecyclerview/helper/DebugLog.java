package com.example.mystaterecyclerview.helper;

import android.util.Log;

/**
 * ============================================================
 * DebugLog —— 通用调试日志工具类
 * ============================================================

 * 【设计原则】
 * 1. 全局总开关是 static：DebugLog.enableLog() / DebugLog.disableLog()
 * 2. tag 是实例级：new DebugLog("AppDAO") 只写一次，后面所有方法都不用再传 tag
 * 3. 本类不绑定任何业务、不保存“模块开关”
 * 4. 是否打印 = 全局总开关开 && 调用方自己愿意调

 * 【调用方标准写法】
 * ------------------------------------------------------------
 * // 1）Application / 调试入口：开总闸（一次）
 * DebugLog.enableLog();

 * // 2）在某个 DAO / Adapter / Activity 里：
 * private static final DebugLog log = new DebugLog("AppDAO");
 * private static boolean enabled = false;

 * public static void enableLog() { enabled = true; }
 * public static void disableLog() { enabled = false; }

 * // 3）业务方法里：
 * if (enabled) log.d("getUrl=xxx");

 * // 4）上线前关总闸：
 * DebugLog.disableLog();
 * ------------------------------------------------------------

 * 【日志输出条件】
 *     DebugLog.isLogEnabled() == true
 *     && 调用方 enabled == true
 *     && log.d / log.e 被调用
 * ============================================================
 */
public class DebugLog {

    // ==================== 全局总开关 ====================

    /**
     * 全局日志总开关。
     * true = 允许打印
     * false = 任何 DebugLog 实例都不会输出日志（默认 false）
     */
    private static boolean logEnabled = false;

    /**
     * 打开全局日志总开关。
     * 一般在 Application 的 DEBUG 环境下调用一次。
     */
    public static void enableLog() {
        logEnabled = true;
    }

    /**
     * 关闭全局日志总开关。
     * 一般在上线 / Release 包里调用，或调试结束后调用。
     */
    public static void disableLog() {
        logEnabled = false;
    }

    /**
     * 判断全局日志总开关是否打开
     *
     * @return true=总开关已开，false=总开关已关
     */
    public static boolean isLogEnabled() {
        return logEnabled;
    }

    // ==================== 实例级 tag ====================

    /**
     * 当前日志器的 tag（日志前缀）。
     * 在构造时一次性传入，例如 "AppDAO" / "VideoAdapter" / "Net"。
     */
    private final String tag;

    /**
     * 构造一个绑定 tag 的日志器。
     *
     * @param tag 日志标签；
     *            - 为 null / 空 / 全空格 时，自动使用 "DebugLog"
     *            - 一般传类名，如 "AppDAO"
     */
    public DebugLog(String tag) {
        this.tag = (tag != null && !tag.trim().isEmpty()) ? tag : "DebugLog";
    }

    // ==================== 基础打印方法 ====================

    /**
     * 打印 Debug 级别日志
     *
     * @param msg 日志内容；为 null 时输出友好提示，避免 Logcat 显示 (msg=null)
     */
    public void d(String msg) {
        if (!logEnabled) return;
        Log.d(tag, msg != null ? msg : "msg=null");
    }

    /**
     * 打印 Warn 级别日志
     *
     * @param msg 警告信息；为 null 时输出友好提示
     */
    public void w(String msg) {
        if (!logEnabled) return;
        Log.w(tag, msg != null ? msg : "msg=null");
    }

    /**
     * 打印 Error 级别日志
     *
     * @param msg 错误描述（可为 null）
     * @param t   异常对象（可为 null），最终打印 t.getMessage()
     */
    public void e(String msg, Throwable t) {
        if (!logEnabled) return;
        Log.e(tag, (msg != null ? msg : "msg=null") + " 异常=" + (t != null ? t.getMessage() : "null"));
    }
}