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
import androidx.recyclerview.widget.RecyclerView;

import com.scwang.smart.refresh.header.ClassicsHeader;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class StateRecyclerView extends FrameLayout {

    @NonNull
    private final Context appContext;
    @NonNull
    private final Context themedForDefault;

    @Nullable
    private SmartRefreshLayout refreshLayout;
    @Nullable
    private RecyclerView recyclerView;
    @Nullable
    private SkeletonHost skeletonHost;
    @Nullable
    private StatePageHost pageHost;

    @NonNull
    private final ListStateMachine stateMachine = new ListStateMachine();

    @LayoutRes
    private int skeletonLayoutRes = R.layout.layout_srv_skeleton;
    @Nullable
    private View customEmpty;
    @Nullable
    private View customError;

    private boolean inited = false;

    public StateRecyclerView(@NonNull Context context) {
        this(context, null);
    }

    public StateRecyclerView(@NonNull Context context, @Nullable android.util.AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StateRecyclerView(@NonNull Context context, @Nullable android.util.AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.appContext = context.getApplicationContext();
        this.themedForDefault = new android.view.ContextThemeWrapper(
                context,
                com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar
        );
    }

    // ===================== 初始化 =====================

    public void init() {
        if (inited) {
            throw new IllegalStateException("StateRecyclerView 已经初始化过了");
        }
        inited = true;

        LayoutInflater.from(appContext).inflate(R.layout.layout_srv_content, this, true);

        refreshLayout = findView(R.id.srv_refresh);
        recyclerView = findView(R.id.srv_rv);

        // 库自带布局资源异常（覆盖/混淆/裁剪）时，release 静默降级
        if (refreshLayout == null || recyclerView == null) {
            inited = false;
            return;
        }

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

    @Nullable
    @SuppressWarnings("unchecked")
    private <V extends View> V findView(@IdRes int id) {
        return (V) findViewById(id);
    }

    // ===================== 自定义入口 =====================

    @NonNull
    public StateRecyclerView setSkeleton(@LayoutRes int layoutRes) {
        checkNotInited();
        this.skeletonLayoutRes = layoutRes;
        return this;
    }

    @NonNull
    public StateRecyclerView setEmptyView(@NonNull View v) {
        checkNotInited();
        this.customEmpty = v;
        return this;
    }

    @NonNull
    public StateRecyclerView setErrorView(@NonNull View v) {
        checkNotInited();
        this.customError = v;
        return this;
    }

    @NonNull
    public StateRecyclerView setEmptyLayout(@LayoutRes int layoutRes) {
        checkNotInited();
        this.customEmpty = LayoutInflater.from(themedForDefault).inflate(layoutRes, this, false);
        return this;
    }

    @NonNull
    public StateRecyclerView setErrorLayout(@LayoutRes int layoutRes) {
        checkNotInited();
        this.customError = LayoutInflater.from(themedForDefault).inflate(layoutRes, this, false);
        return this;
    }

    private void checkNotInited() {
        if (inited) {
            throw new IllegalStateException("init() 之后不能再设置自定义视图");
        }
    }

    // ===================== 基础 API =====================

    @NonNull
    public RecyclerView getRecyclerView() {
        checkInited();
        if (recyclerView == null) throw new IllegalStateException("recyclerView 为空");
        return recyclerView;
    }

    @NonNull
    public SmartRefreshLayout getRefreshLayout() {
        checkInited();
        if (refreshLayout == null) throw new IllegalStateException("refreshLayout 为空");
        return refreshLayout;
    }

    @NonNull
    public StatePageHost getPageHost() {
        checkInited();
        if (pageHost == null) throw new IllegalStateException("pageHost 为空");
        return pageHost;
    }

    @NonNull
    public SkeletonHost getSkeletonHost() {
        checkInited();
        if (skeletonHost == null) throw new IllegalStateException("skeletonHost 为空");
        return skeletonHost;
    }

    public void setAdapter(@NonNull RecyclerView.Adapter<?> adapter) {
        checkInited();
        if (recyclerView == null) throw new IllegalStateException("recyclerView 为空");
        recyclerView.setAdapter(adapter);
    }

    @NonNull
    public ViewState getCurrentState() {
        checkInited();
        return stateMachine.getCurrent();
    }

    // ===================== 状态入口 =====================

    public void beginFirstLoad() {
        checkInited();
        stateMachine.beginLoad(true);
    }

    public void beginLoad() {
        checkInited();
        stateMachine.beginLoad(false);
    }

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

    public void submitError() {
        checkInited();
        stateMachine.submitError(stateMachine.currentToken());
    }

    public void finishRefresh() {
        checkInited();
        if (refreshLayout == null) return;
        refreshLayout.finishRefresh();
        refreshLayout.finishLoadMore();
    }

    // ===================== OnDataCallback =====================

    public interface OnDataCallback<T> {
        void onData(T data);
    }

    // ===================== bind（主入口） =====================

    /**
     * 无侵入 bind：adapter 不需要 implements 任何接口
     * 数据通过 onData 回调交给业务方
     */
    public <T> void bind(@NonNull RecyclerView.Adapter<?> adapter,
                         @NonNull LiveData<Resource<T>> source,
                         @NonNull LifecycleOwner owner,
                         @NonNull Mapper<T, Boolean> isEmpty,
                         @NonNull FirstLoadStrategy strategy,
                         @Nullable OnDataCallback<T> onData) {
        setAdapter(adapter);

        final AtomicInteger token = new AtomicInteger(-1);

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
                token.set(stateMachine.beginLoad(first));
            } else if (res.isSuccess()) {
                T data = res.data;
                if (onData != null) {
                    onData.onData(data);
                }
                boolean empty = data == null || isEmpty.apply(data);
                int cur = token.getAndSet(-1);
                if (cur != -1) {
                    stateMachine.submitData(empty, cur);
                }
                finishRefresh();
            } else if (res.isError()) {
                int cur = token.getAndSet(-1);
                if (cur != -1) {
                    stateMachine.submitError(cur);
                }
                finishRefresh();
            }
        });
    }

    /** 兼容旧调用：无 onData，只切状态 */
    public <T> void bind(@NonNull RecyclerView.Adapter<?> adapter,
                         @NonNull LiveData<Resource<T>> source,
                         @NonNull LifecycleOwner owner,
                         @NonNull Mapper<T, Boolean> isEmpty,
                         @NonNull FirstLoadStrategy strategy) {
        bind(adapter, source, owner, isEmpty, strategy, null);
    }

    public <T> void bind(@NonNull RecyclerView.Adapter<?> adapter,
                         @NonNull LiveData<Resource<T>> source,
                         @NonNull LifecycleOwner owner,
                         @NonNull Mapper<T, Boolean> isEmpty) {
        bind(adapter, source, owner, isEmpty, FirstLoadStrategy.getDefault(), null);
    }

    public <T extends List<?>> void bind(@NonNull RecyclerView.Adapter<?> adapter,
                                         @NonNull LiveData<Resource<T>> source,
                                         @NonNull LifecycleOwner owner) {
        bind(adapter, source, owner, c -> c == null || c.isEmpty(), FirstLoadStrategy.getDefault(), null);
    }

    // ===================== 交互回调 =====================

    public void setOnEmptyAction(@NonNull Runnable r) {
        checkInited();
        if (pageHost == null) throw new IllegalStateException("pageHost 为空");
        pageHost.setEmptyAction(r);
    }

    public void setOnErrorAction(@NonNull Runnable r) {
        checkInited();
        if (pageHost == null) throw new IllegalStateException("pageHost 为空");
        pageHost.setErrorAction(r);
    }

    public void bindRefresh(@NonNull Runnable r) {
        checkInited();
        if (refreshLayout == null) throw new IllegalStateException("refreshLayout 为空");
        refreshLayout.setEnableRefresh(true);
        refreshLayout.setOnRefreshListener(refresh -> r.run());
    }

    // ===================== 渲染 =====================

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

    // ===================== 生命周期 =====================

    public void release() {
        if (!inited) return;
        stateMachine.setOnStateChangeListener((n, o) -> {});
        if (skeletonHost != null) skeletonHost.hide();
        stateMachine.invalidate();
        stateMachine.forceState();
        if (recyclerView != null) {
            recyclerView.setAdapter(null);
        }
    }

    private void checkInited() {
        if (!inited) {
            throw new IllegalStateException("必须先调用 init() 才能使用 StateRecyclerView");
        }
    }

    // ===================== ListAwareAdapter 已删除 =====================
}
