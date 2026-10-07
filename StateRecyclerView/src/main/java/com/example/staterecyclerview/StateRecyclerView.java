/*
 * ============================================================
 *  StateRecyclerView  最终定稿版
 * ------------------------------------------------------------
 *  功能：自带状态管理的 RecyclerView 封装（loading / content / empty / error）
 *        + 下拉刷新(SmartRefresh) + 骨架屏(SkeletonHost) + 空/错页(StatePageHost)
 *
 *  列数支持（三入口）：
 *   1) xml 属性      app:srv_spanCount="2"        —— 构造读取，init 时生效
 *   2) init() 前     setSpanCount(2)              —— 必须在 init() 前调用
 *   3) init() 后     updateSpanCount(3)           —— 运行时改列，带滚动位保持
 *   默认列数 = 1（即单列纵向）
 *
 *  配套 attrs.xml（缺则编译不过）：
 *    <declare-styleable name="StateRecyclerView">
 *        <attr name="srv_spanCount" format="integer"/>
 *    </declare-styleable>
 *
 *  状态驱动方式：
 *   - 推荐：bind(adapter, LiveData<Resource<T>>, owner, isEmpty, strategy, onData)
 *   - 手动：beginFirstLoad / beginLoad / submitData / submitError / finishRefresh
 *
 *  生命周期：务必在 onDestroyView 中调用 release()，防监听/回调悬挂
 *
 *  已知取舍：
 *   - updateSpanCount 滚动为“item 级”对齐（非像素级）
 *  - bind(source, owner) 内部会调用 source.removeObservers(owner) 后重新 observe；
      若同一 owner 下曾对同一个 source 挂过其他观察者，会被移除。
 * ============================================================
 */
package com.example.staterecyclerview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.scwang.smart.refresh.header.ClassicsHeader;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;

import java.util.List;

public class StateRecyclerView extends FrameLayout {

    /* ===================== 字段 ===================== */

    @NonNull private final Context appContext;          // 应用级 Context，避免内存泄漏
    @NonNull private final Context themedForDefault;     // 空/错页默认 M3 主题 Context

    @Nullable private SmartRefreshLayout refreshLayout;  // 下拉刷新容器
    @Nullable private RecyclerView recyclerView;         // 内部列表
    @Nullable private SkeletonHost skeletonHost;         // 骨架屏宿主
    @Nullable private StatePageHost pageHost;            // 空/错状态页宿主

    @NonNull private final ListStateMachine stateMachine = new ListStateMachine(); // 状态机（唯一发号源）

    @LayoutRes private int skeletonLayoutRes = R.layout.layout_srv_skeleton; // 骨架布局
    @Nullable private View customEmpty;                  // 自定义空页
    @Nullable private View customError;                  // 自定义错页

    private boolean inited = false;                     // 是否已初始化（init 幂等保护）
    private int spanCount = 1;                          // 列数，默认 1

    /* ===================== 构造 ===================== */

    /** 代码 new 时走这里 */
    public StateRecyclerView(@NonNull Context context) {
        this(context, null);
    }

    /** xml 解析属性时走这里 */
    public StateRecyclerView(@NonNull Context context, @Nullable android.util.AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /** 完整构造：解析 srv_spanCount 属性（无属性默认 1 列） */
    public StateRecyclerView(@NonNull Context context, @Nullable android.util.AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.appContext = context.getApplicationContext();
        this.themedForDefault = new android.view.ContextThemeWrapper(
                context, com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar);

        if (attrs != null) {
            android.content.res.TypedArray a = context.obtainStyledAttributes(
                    attrs, R.styleable.StateRecyclerView, defStyle, 0);
            this.spanCount = a.getInt(R.styleable.StateRecyclerView_srv_spanCount, 1);
            a.recycle();
        }
    }

    /* ===================== 初始化 ===================== */

    /**
     * 初始化：inflate 布局、挂 GridLayoutManager(spanCount)、配刷新/空错页/骨架、渲染初始状态
     * 幂等：二次调用抛异常；关键 id 缺失抛异常（与二次调用风格统一）
     */
    public void init() {
        if (inited) {
            throw new IllegalStateException("StateRecyclerView 已经初始化过了");
        }
        inited = true;

        LayoutInflater.from(appContext).inflate(R.layout.layout_srv_content, this, true);
        refreshLayout = findView(R.id.srv_refresh);
        recyclerView = findView(R.id.srv_rv);

        if (refreshLayout == null || recyclerView == null) {
            inited = false;
            throw new IllegalStateException(
                    "StateRecyclerView 初始化失败：未找到 R.id.srv_refresh 或 R.id.srv_rv，请检查 layout_srv_content.xml");
        }

        // 关键：给内部 RV 真正挂上 LM，否则卡片不绘制（空屏根因）
        recyclerView.setLayoutManager(
                new GridLayoutManager(appContext, spanCount, RecyclerView.VERTICAL, false));
        recyclerView.setItemAnimator(null); // 关默认动画，防首帧吞行

        refreshLayout.setRefreshHeader(new ClassicsHeader(themedForDefault));
        refreshLayout.setEnableLoadMore(false);

        View empty = customEmpty != null
                ? customEmpty
                : LayoutInflater.from(themedForDefault).inflate(R.layout.layout_srv_empty, this, false);
        View error = customError != null
                ? customError
                : LayoutInflater.from(themedForDefault).inflate(R.layout.layout_srv_error, this, false);

        pageHost = new StatePageHost(this, empty, error);
        skeletonHost = new SkeletonHost(this, skeletonLayoutRes, themedForDefault);

        stateMachine.setOnStateChangeListener((next, old) -> renderState(next));
        renderState(stateMachine.getCurrent());
        refreshLayout.setEnableRefresh(false);
    }

    /** 内部泛型 findViewById，避免各处强转 */
    @Nullable
    @SuppressWarnings("unchecked")
    private <V extends View> V findView(@IdRes int id) {
        return (V) findViewById(id);
    }

    /* ===================== 自定义入口（init 前调用） ===================== */

    /** 设置骨架屏布局，init 前 */
    @NonNull public StateRecyclerView setSkeleton(@LayoutRes int layoutRes) {
        checkNotInited();
        this.skeletonLayoutRes = layoutRes;
        return this;
    }

    /** 设置自定义空页 View，init 前 */
    @NonNull public StateRecyclerView setEmptyView(@NonNull View v) {
        checkNotInited();
        this.customEmpty = v;
        return this;
    }

    /** 设置自定义错页 View，init 前 */
    @NonNull public StateRecyclerView setErrorView(@NonNull View v) {
        checkNotInited();
        this.customError = v;
        return this;
    }

    /** 通过 layoutRes 设置空页，init 前 */
    @NonNull public StateRecyclerView setEmptyLayout(@LayoutRes int layoutRes) {
        checkNotInited();
        this.customEmpty = LayoutInflater.from(themedForDefault).inflate(layoutRes, this, false);
        return this;
    }

    /** 通过 layoutRes 设置错页，init 前 */
    @NonNull public StateRecyclerView setErrorLayout(@LayoutRes int layoutRes) {
        checkNotInited();
        this.customError = LayoutInflater.from(themedForDefault).inflate(layoutRes, this, false);
        return this;
    }

    /** 校验：init 后禁止改自定义视图相关配置 */
    private void checkNotInited() {
        if (inited) {
            throw new IllegalStateException("init() 之后不能再设置自定义视图");
        }
    }

    /* ===================== 列数控制 ===================== */

    /** init 前动态设列数（1 起），init 后调用抛异常 */
    @NonNull
    public StateRecyclerView setSpanCount(int span) {
        if (inited) {
            throw new IllegalStateException("setSpanCount 必须在 init() 前调用");
        }
        this.spanCount = Math.max(1, span);
        return this;
    }

    /**
     * init 后运行时改列（横竖屏/折叠屏）：重建 GridLayoutManager 并恢复首个可见位置
     * 注意：滚动为 item 级对齐（非像素级）
     */
    public void updateSpanCount(int span) {
        checkInited();
        this.spanCount = Math.max(1, span);
        if (recyclerView == null) return;

        // 记录当前首个可见位置
        int firstPos = 0;
        RecyclerView.LayoutManager lm = recyclerView.getLayoutManager();
        if (lm instanceof GridLayoutManager) {
            firstPos = ((GridLayoutManager) lm).findFirstVisibleItemPosition();
        } else if (lm instanceof LinearLayoutManager) {
            firstPos = ((LinearLayoutManager) lm).findFirstVisibleItemPosition();
        }
        if (firstPos < 0) firstPos = 0;

        // 重建 LM
        recyclerView.setLayoutManager(
                new GridLayoutManager(appContext, this.spanCount, RecyclerView.VERTICAL, false));
        recyclerView.setItemAnimator(null);

        // 新 LM 布局完再滚，避免白滚；lambda 捕获 final
        final int targetPos = firstPos;
        recyclerView.post(() -> recyclerView.scrollToPosition(targetPos));
    }

    /** 读取当前列数 */
    public int getSpanCount() {
        return spanCount;
    }

    /* ===================== 基础 API ===================== */

    /** 获取内部 RecyclerView（必须先 init） */
    @NonNull public RecyclerView getRecyclerView() {
        checkInited();
        if (recyclerView == null) throw new IllegalStateException("recyclerView 为空");
        return recyclerView;
    }

    /** 获取下拉刷新容器（必须先 init） */
    @NonNull public SmartRefreshLayout getRefreshLayout() {
        checkInited();
        if (refreshLayout == null) throw new IllegalStateException("refreshLayout 为空");
        return refreshLayout;
    }

    /** 获取空/错状态页宿主（必须先 init） */
    @NonNull public StatePageHost getPageHost() {
        checkInited();
        if (pageHost == null) throw new IllegalStateException("pageHost 为空");
        return pageHost;
    }

    /** 获取骨架屏宿主（必须先 init） */
    @NonNull public SkeletonHost getSkeletonHost() {
        checkInited();
        if (skeletonHost == null) throw new IllegalStateException("skeletonHost 为空");
        return skeletonHost;
    }

    /** 给内部 RV 设置 Adapter */
    public void setAdapter(@NonNull RecyclerView.Adapter<?> adapter) {
        checkInited();
        if (recyclerView == null) throw new IllegalStateException("recyclerView 为空");
        recyclerView.setAdapter(adapter);
    }

    /** 当前视图状态 */
    @NonNull public ViewState getCurrentState() {
        checkInited();
        return stateMachine.getCurrent();
    }

    /* ===================== 状态入口（手动驱动） ===================== */

    /** 标记首次加载开始（显示骨架） */
    public void beginFirstLoad() {
        checkInited();
        stateMachine.beginLoad(true);
    }

    /** 标记加载开始（非首次，不显示骨架） */
    public void beginLoad() {
        checkInited();
        stateMachine.beginLoad(false);
    }

    /** 提交数据：通知状态机内容/空态（已过时，建议走 bind/onData） */
    @SuppressLint("NotifyDataSetChanged")
    @Deprecated
    public void submitData(@NonNull List<?> data) {
        checkInited();
        if (stateMachine.currentToken() == 0) return;
        stateMachine.submitData(data.isEmpty(), stateMachine.currentToken());
        if (stateMachine.getCurrent() == ViewState.CONTENT) {
            if (recyclerView != null && recyclerView.getAdapter() != null) {
                recyclerView.getAdapter().notifyDataSetChanged();
            }
        }
    }

    /** 标记加载失败（走错页） */
    public void submitError() {
        checkInited();
        stateMachine.submitError(stateMachine.currentToken());
    }

    /** 结束下拉刷新动画 */
    public void finishRefresh() {
        checkInited();
        if (refreshLayout == null) return;
        refreshLayout.finishRefresh();
        refreshLayout.finishLoadMore();
    }

    /* ===================== OnDataCallback ===================== */

    /** bind 成功回调 */
    public interface OnDataCallback<T> {
        void onData(T data);
    }

    /* ===================== bind（主入口） ===================== */

    /**
     * 主入口：将 LiveData<Resource<T>> 绑定到 Adapter，自动驱动状态切换
     *  token 统一由 ListStateMachine 发号，本地只存副本（单线程 int[]，非原子）
     *  removeObservers(owner) 会清空该 owner 下同 source 的观察者，注意顺序
     */
    public <T> void bind(@NonNull RecyclerView.Adapter<?> adapter,
                         @NonNull LiveData<Resource<T>> source,
                         @NonNull LifecycleOwner owner,
                         @NonNull Mapper<T, Boolean> isEmpty,
                         @NonNull FirstLoadStrategy strategy,
                         @Nullable OnDataCallback<T> onData) {
        setAdapter(adapter);

        final int[] tokenHolder = new int[]{-1}; // 本地 token 副本，非 Atomic

        source.removeObservers(owner);

        source.observe(owner, res -> {
            if (res.isLoading()) {
                boolean first;
                if (strategy == FirstLoadStrategy.ALWAYS) {
                    first = true;
                } else if (strategy == FirstLoadStrategy.NEVER) {
                    first = false;
                } else {
                    first = stateMachine.getCurrent() != ViewState.CONTENT
                            || adapter.getItemCount() == 0;
                }
                tokenHolder[0] = stateMachine.beginLoad(first);
            } else if (res.isSuccess()) {
                T data = res.data;
                if (onData != null) {
                    onData.onData(data);
                }
                boolean empty = data == null || isEmpty.apply(data);
                int cur = tokenHolder[0];
                tokenHolder[0] = -1;
                if (cur != -1) {
                    stateMachine.submitData(empty, cur);
                }
                finishRefresh();
            } else if (res.isError()) {
                int cur = tokenHolder[0];
                tokenHolder[0] = -1;
                if (cur != -1) {
                    stateMachine.submitError(cur);
                }
                finishRefresh();
            }
        });
    }

    /** bind：不传 onData */
    public <T> void bind(@NonNull RecyclerView.Adapter<?> adapter,
                         @NonNull LiveData<Resource<T>> source,
                         @NonNull LifecycleOwner owner,
                         @NonNull Mapper<T, Boolean> isEmpty,
                         @NonNull FirstLoadStrategy strategy) {
        bind(adapter, source, owner, isEmpty, strategy, null);
    }

    /** bind：用默认策略 */
    public <T> void bind(@NonNull RecyclerView.Adapter<?> adapter,
                         @NonNull LiveData<Resource<T>> source,
                         @NonNull LifecycleOwner owner,
                         @NonNull Mapper<T, Boolean> isEmpty) {
        bind(adapter, source, owner, isEmpty, FirstLoadStrategy.getDefault(), null);
    }

    /** bind：默认策略 + 默认 isEmpty（判 null/空集合） */
    public <T extends List<?>> void bind(@NonNull RecyclerView.Adapter<?> adapter,
                                         @NonNull LiveData<Resource<T>> source,
                                         @NonNull LifecycleOwner owner) {
        bind(adapter, source, owner, c -> c == null || c.isEmpty(), FirstLoadStrategy.getDefault(), null);
    }

    /* ===================== 交互回调 ===================== */

    /** 设置空页点击重试回调 */
    public void setOnEmptyAction(@NonNull Runnable r) {
        checkInited();
        if (pageHost == null) throw new IllegalStateException("pageHost 为空");
        pageHost.setEmptyAction(r);
    }

    /** 设置错页点击重试回调 */
    public void setOnErrorAction(@NonNull Runnable r) {
        checkInited();
        if (pageHost == null) throw new IllegalStateException("pageHost 为空");
        pageHost.setErrorAction(r);
    }

    /** 绑定下拉刷新回调 */
    public void bindRefresh(@NonNull Runnable r) {
        checkInited();
        if (refreshLayout == null) throw new IllegalStateException("refreshLayout 为空");
        refreshLayout.setEnableRefresh(true);
        refreshLayout.setOnRefreshListener(refresh -> r.run());
    }

    /* ===================== 渲染 ===================== */

    /** 按状态渲染：骨架/列表/空页/错页的可见性与刷新开关 */
    private void renderState(@NonNull ViewState state) {
        if (skeletonHost == null || recyclerView == null || pageHost == null || refreshLayout == null) {
            return;
        }
        switch (state) {
            case LOADING:
                if (stateMachine.isFirstLoading()) {
                    skeletonHost.show();
                    recyclerView.setVisibility(View.GONE);
                    refreshLayout.setEnableRefresh(false);
                } else {
                    skeletonHost.hide();
                    recyclerView.setVisibility(View.VISIBLE);
                    refreshLayout.setEnableRefresh(true);
                }
                pageHost.apply(ViewState.CONTENT);
                break;
            case CONTENT:
                skeletonHost.hide();
                recyclerView.setVisibility(View.VISIBLE);
                pageHost.apply(ViewState.CONTENT);
                refreshLayout.setEnableRefresh(true);
                break;
            case EMPTY:
                skeletonHost.hide();
                recyclerView.setVisibility(View.GONE);
                pageHost.apply(ViewState.EMPTY);
                refreshLayout.setEnableRefresh(false);
                break;
            case ERROR:
                skeletonHost.hide();
                recyclerView.setVisibility(View.GONE);
                pageHost.apply(ViewState.ERROR);
                refreshLayout.setEnableRefresh(false);
                break;
        }
    }

    /* ===================== 生命周期 ===================== */

    /**
     * 释放资源：清状态监听、藏骨架、置空 Adapter、清刷新监听
     * 务必在 Fragment.onDestroyView / Activity.onDestroy 中调用
     */
    public void release() {
        if (!inited) return;
        stateMachine.setOnStateChangeListener((n, o) -> {});
        if (skeletonHost != null) skeletonHost.hide();
        stateMachine.invalidate();
        stateMachine.forceState();
        if (recyclerView != null) {
            recyclerView.setAdapter(null);
        }
        if (refreshLayout != null) {
            refreshLayout.setOnRefreshListener(null);
            refreshLayout.setEnableRefresh(false);
        }
    }

    /** 校验：必须先 init 才能使用相关 API */
    private void checkInited() {
        if (!inited) {
            throw new IllegalStateException("必须先调用 init() 才能使用 StateRecyclerView");
        }
    }
}
