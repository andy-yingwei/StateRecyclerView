package com.example.mystaterecyclerview;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mystaterecyclerview.R;
import com.example.staterecyclerview.FirstLoadStrategy;
import com.example.staterecyclerview.Resource;
import com.example.staterecyclerview.StateRecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * 纯 LiveData 单路径测试 StateRecyclerView
 *
 * 特性：
 * - 只走 bind(LiveData<Resource<List<FilmStudio>>>, AUTO)
 * - 首次 loading → 骨架屏
 * - 非首次 loading（已有数据）→ 只保留列表 + 下拉动画，不弹骨架
 * - 空数据 → EMPTY 页
 * - 错误 → ERROR 页 + 重试
 * - 不混用手动 beginXxx，token 不串
 */
public class StateStudioTestActivity extends AppCompatActivity {

    private static final String TAG = "StateStudioTest";

    private StateRecyclerView stateRv;
    private FilmStudioAdapter adapter;

    /** 唯一数据源，所有状态都从它发 */
    private final MutableLiveData<Resource<List<FilmStudio>>> liveData = new MutableLiveData<>();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_state_studio_test);

        stateRv = findViewById(R.id.stateRv);

        // 1. 必须 init
        stateRv.init();

        // 2. Adapter
        FilmStudioConfig config = new FilmStudioConfig.Builder()
                .copyFrom(FilmStudioConfig.defaultConfig(this))
                .backgroundRadius(8)
                .coverHeight(160)
                .coverRoundRadius(6)
                .build();

        adapter = new FilmStudioAdapter(new ArrayList<>(), config);
        adapter.enableLog("Test");
        adapter.setOnItemClickListener((item, pos) ->
                Log.d(TAG, "点击:" + item.getName()));

        // 3. 单路径 bind：LiveData + 判空 + AUTO 策略
        // 改成：
        stateRv.bind(
                adapter,
                liveData,
                this,
                list -> list == null || list.isEmpty(),
                FirstLoadStrategy.AUTO,
                list -> adapter.updateData(list)
        );

        // 4. LayoutManager
        stateRv.getRecyclerView().setLayoutManager(new LinearLayoutManager(this));

        // 5. 空态 / 错误态按钮回调
        stateRv.setOnEmptyAction(() -> {
            Log.d(TAG, "空态页-刷新");
            emitLoadingThenSuccess();
        });
        stateRv.setOnErrorAction(() -> {
            Log.d(TAG, "错误页-重试");
            emitLoadingThenSuccess();
        });

        // 6. 下拉刷新（非首次，AUTO 自动判 false）
        stateRv.bindRefresh(() -> {
            Log.d(TAG, "下拉刷新");
            emitLoadingThenSuccess();
        });

        // 7. 测试按钮
        Button btnFirst   = findViewById(R.id.btnLoading);
        Button btnSuccess = findViewById(R.id.btnSuccess);
        Button btnEmpty   = findViewById(R.id.btnEmpty);
        Button btnError   = findViewById(R.id.btnError);
        Button btnRefresh = findViewById(R.id.btnRefresh);

        // 1) 模拟首次加载（当前无内容 → AUTO 判 first=true → 骨架）
        btnFirst.setOnClickListener(v -> {
            Log.d(TAG, "== 触发首次加载 ==");
            liveData.setValue(Resource.loading());
        });

        // 2) 直接出成功数据（若当前是空/首次，会先走骨架再切内容）
        btnSuccess.setOnClickListener(v -> emitLoadingThenSuccess());

        // 3) 空数据
        btnEmpty.setOnClickListener(v -> {
            Log.d(TAG, "== 返回空列表 ==");
            liveData.setValue(Resource.loading());
            handler.postDelayed(() -> liveData.setValue(Resource.success(new ArrayList<>())), 600);
        });

        // 4) 错误
        btnError.setOnClickListener(v -> {
            Log.d(TAG, "== 返回错误 ==");
            liveData.setValue(Resource.loading());
            handler.postDelayed(() -> liveData.setValue(Resource.error()), 600);
        });

        // 5) 非首次刷新（已有数据 → AUTO 判 first=false → 不弹骨架）
        btnRefresh.setOnClickListener(v -> {
            Log.d(TAG, "== 非首次刷新 ==");
            emitLoadingThenSuccess();
        });

        // 8. 进入页面先走一次首次加载
        liveData.setValue(Resource.loading());
        handler.postDelayed(() -> liveData.setValue(Resource.success(buildMockData())), 1000);
    }

    /** loading -> 延迟 -> success(有数据) */
    private void emitLoadingThenSuccess() {
        liveData.setValue(Resource.loading());
        handler.postDelayed(() -> liveData.setValue(Resource.success(buildMockData())), 600);
    }

    private List<FilmStudio> buildMockData() {
        List<FilmStudio> list = new ArrayList<>();
        list.add(new FilmStudio("Studio A",
                "https://picsum.photos/400/300?1",
                "120", "4.8", "TOP 1", true, "https://a"));
        list.add(new FilmStudio("Studio B",
                "https://picsum.photos/400/300?2",
                "88", "4.5", "TOP 3", false, "https://b"));
        list.add(new FilmStudio("Studio C",
                "", "12", "4.1", "", false, "https://c"));
        return list;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stateRv.release();
        handler.removeCallbacksAndMessages(null);
    }
}