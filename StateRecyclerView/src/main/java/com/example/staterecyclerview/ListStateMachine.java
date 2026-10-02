package com.example.staterecyclerview;

import androidx.annotation.NonNull;

class ListStateMachine {
    @NonNull
    private ViewState current = ViewState.CONTENT;
    private boolean firstLoading = false;
    private int tokenSeq = 0;

    @NonNull
    private OnStateChangeListener listener = (n, o) -> {};

    interface OnStateChangeListener {
        void onChanged(@NonNull ViewState newState, @NonNull ViewState oldState);
    }

    void setOnStateChangeListener(@NonNull OnStateChangeListener l) {
        this.listener = l;
    }

    @NonNull
    ViewState getCurrent() { return current; }

    boolean isFirstLoading() { return firstLoading; }

    int currentToken() { return tokenSeq; }

    int beginLoad(boolean first) {
        int token = ++tokenSeq;
        this.firstLoading = first;
        transit(ViewState.LOADING);
        return token;
    }

    void submitData(boolean empty, int token) {
        if (token != tokenSeq) return;
        this.firstLoading = false;
        transit(empty ? ViewState.EMPTY : ViewState.CONTENT);
    }

    void submitError(int token) {
        if (token != tokenSeq) return;
        this.firstLoading = false;
        transit(ViewState.ERROR);
    }

    void forceState() {
        this.firstLoading = false;
        transit(ViewState.CONTENT);
    }

    void invalidate() {
        ++tokenSeq;
    }

    private void transit(@NonNull ViewState next) {
        if (next == current) {
            return;
        }
        ViewState old = current;
        current = next;
        listener.onChanged(current, old);
    }
}