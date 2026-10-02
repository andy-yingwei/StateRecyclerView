package com.example.staterecyclerview;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Resource<T> {
    public enum Status { LOADING, SUCCESS, ERROR }

    @NonNull
    public final Status status;
    @Nullable
    public final T data;

    private Resource(@NonNull Status status, @Nullable T data) {
        this.status = status;
        this.data = data;
    }

    @NonNull
    public static <T> Resource<T> loading() {
        return new Resource<>(Status.LOADING, null);
    }

    @NonNull
    public static <T> Resource<T> success(@NonNull T data) {
        return new Resource<>(Status.SUCCESS, data);
    }

    @NonNull
    public static <T> Resource<T> error() {
        return new Resource<>(Status.ERROR, null);
    }

    public boolean isLoading() { return status == Status.LOADING; }
    public boolean isSuccess() { return status == Status.SUCCESS; }
    public boolean isError() { return status == Status.ERROR; }
}