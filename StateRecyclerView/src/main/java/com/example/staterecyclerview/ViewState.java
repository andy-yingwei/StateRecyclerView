package com.example.staterecyclerview;

import androidx.annotation.NonNull;

public enum ViewState {
    CONTENT,
    EMPTY,
    ERROR,
    LOADING;

    @NonNull
    @Override
    public String toString() {
        return name();
    }
}
