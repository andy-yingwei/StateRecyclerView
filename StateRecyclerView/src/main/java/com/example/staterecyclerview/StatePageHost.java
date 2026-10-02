package com.example.staterecyclerview;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 空态/错误态页面宿主，挂到父 FrameLayout 上并叠层铺满。
 * 支持传入 View 或 layoutRes，布局中可不写默认 id，业务自行通过 getEmptyView/getErrorView 绑定。
 */
public class StatePageHost {

    @NonNull
    private final View emptyView;
    @NonNull
    private final View errorView;

    @Nullable
    private TextView tvEmpty;
    @Nullable
    private TextView tvError;
    @Nullable
    private Button btnEmpty;
    @Nullable
    private Button btnError;

    /** 传入已有 View（自定义页推荐） */
    public StatePageHost(@NonNull FrameLayout parent,
                         @NonNull View empty,
                         @NonNull View error) {
        this.emptyView = empty;
        this.errorView = error;
        bindIds();
        attach(parent);
    }

    /** 传入 layoutRes，内部 inflate 后绑定默认 id */
    public StatePageHost(@NonNull FrameLayout parent,
                         @LayoutRes int emptyLayoutRes,
                         @LayoutRes int errorLayoutRes) {
        if (emptyLayoutRes == 0 || errorLayoutRes == 0) {
            throw new IllegalArgumentException("empty/error layoutRes must not be 0");
        }
        this.emptyView = LayoutInflater.from(parent.getContext())
                .inflate(emptyLayoutRes, parent, false);
        this.errorView = LayoutInflater.from(parent.getContext())
                .inflate(errorLayoutRes, parent, false);
        bindIds();
        attach(parent);
    }

    private void bindIds() {
        // 自定义页可能不含这些 id，故全部可空
        this.tvEmpty = emptyView.findViewById(R.id.tv_empty_title);
        this.tvError = errorView.findViewById(R.id.tv_error_title);
        this.btnEmpty = emptyView.findViewById(R.id.btn_empty_refresh);
        this.btnError = errorView.findViewById(R.id.btn_error_retry);
    }

    private void attach(@NonNull FrameLayout parent) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        if (emptyView.getParent() == null) parent.addView(emptyView, lp);
        if (errorView.getParent() == null) parent.addView(errorView, lp);
        emptyView.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
    }

    public void setEmptyText(@NonNull String t) {
        if (tvEmpty != null) tvEmpty.setText(t);
    }

    public void setErrorText(@NonNull String t) {
        if (tvError != null) tvError.setText(t);
    }

    public void setEmptyAction(@NonNull Runnable r) {
        if (btnEmpty != null) btnEmpty.setOnClickListener(v -> r.run());
    }

    public void setErrorAction(@NonNull Runnable r) {
        if (btnError != null) btnError.setOnClickListener(v -> r.run());
    }

    @NonNull
    public View getEmptyView() {
        return emptyView;
    }

    @NonNull
    public View getErrorView() {
        return errorView;
    }

    public void apply(@NonNull ViewState state) {
        emptyView.setVisibility(state == ViewState.EMPTY ? View.VISIBLE : View.GONE);
        errorView.setVisibility(state == ViewState.ERROR ? View.VISIBLE : View.GONE);
    }
}