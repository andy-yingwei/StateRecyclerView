package com.example.mystaterecyclerview;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.glidehelper.AesOption;
import com.example.glidehelper.ImageLoader;
import com.example.glidehelper.LoadOption;
import com.example.mystaterecyclerview.helper.DebugLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ============================================================
 * FilmStudioMergedAdapter（融合版）
 * ============================================================
 *
 * <p><b>功能简介：</b></p>
 * <ul>
 *     <li>用于展示 {@link FilmStudio} 列表的 RecyclerView Adapter</li>
 *     <li>支持封面图加载（普通 / AES 解密 / Cookie + Header 鉴权）</li>
 *     <li>支持收藏状态展示（favoriteView 位于 infoContainer 内）</li>
 *     <li>支持动态信息行（videoCount、rating、rank）</li>
 *     <li>支持通过 {@link FilmStudioConfig} 统一配置样式与行为</li>
 *     <li>基于 {@link ListAdapter} + DiffUtil，
 *         实现高效列表更新</li>
 * </ul>
 *
 * <p><b>适用场景：</b></p>
 * <ul>
 *     <li>片商列表页</li>
 *     <li>需要鉴权 / 加密封面图的列表</li>
 *     <li>需要动态展示片商元信息的 RecyclerView</li>
 * </ul>
 *
 * <p><b>完整使用范例：</b></p>
 *
 * <pre>
 * // 1. 准备数据
 * List&lt;FilmStudio&gt; list = new ArrayList&lt;&gt;();
 * list.add(new FilmStudio(
 *         "片商名称",
 *         "httpS://example.com/cover.jpg",
 *         "120",
 *         "4.5",
 *         "TOP 10",
 *         false,
 *         "httpS://example.com/studio"
 * ));
 *
 * // 2. 创建 Adapter（可传入 FilmStudioConfig）
 * FilmStudioConfig config = FilmStudioConfig.defaultConfig(context);
 * config.coverHeight = 220;
 * config.nameTextSize = 16;
 *
 * FilmStudioMergedAdapter adapter = new FilmStudioMergedAdapter(list, config);
 *
 * // 3. 设置点击事件
 * adapter.setOnItemClickListener((item, position) -> {
 *     android.util.Log.d("FilmStudio", "点击了：" + item.getName());
 * });
 *
 * // 4. 设置 RecyclerView
 * recyclerView.setLayoutManager(new LinearLayoutManager(context));
 * recyclerView.setAdapter(adapter);
 *
 * // 5. AES 加密封面（可选）
 * adapter.setAesKeyIv("aes_key", "aes_iv");
 *
 * // 6. Cookie + Header 鉴权（可选）
 * Map&lt;String, String&gt; cookies = new HashMap&lt;&gt;();
 * cookies.put("sessionid", "abc123");
 *
 * Map&lt;String, String&gt; headers = new HashMap&lt;&gt;();
 * headers.put("Authorization", "Bearer token");
 *
 * adapter.setCookiesHeaders(cookies, headers);
 *
 * // 7. 动态更新数据
 * List&lt;FilmStudio&gt; newList = loadMoreData();
 * adapter.updateData(newList);
 *
 * // 8. 开启调试日志（可选）
 * adapter.enableLog("FilmStudioMergedAdapter");
 * </pre>
 *
 * <p><b>注意事项：</b></p>
 * <ul>
 *     <li>封面加载模式优先级：AES > Cookie/Header > 普通</li>
 *     <li>调用 {@link #clearAuthParams()} 可清除所有鉴权参数</li>
 *     <li>infoContainer 内的 TextView 会根据数据动态复用</li>
 * </ul>
 *
 * <p><b>作者：</b>TODO</p>
 * <p><b>创建时间：</b>TODO</p>
 *
 * @see FilmStudio
 * @see FilmStudioConfig
 */
public class FilmStudioAdapter extends ListAdapter<FilmStudio, FilmStudioAdapter.ViewHolder> {

    /* ==================== DiffUtil ==================== */
    private static final DiffUtil.ItemCallback<FilmStudio> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<FilmStudio>() {
                @Override
                public boolean areItemsTheSame(@NonNull FilmStudio oldItem, @NonNull FilmStudio newItem) {
                    String n1 = oldItem.getName();
                    String n2 = newItem.getName();
                    if (n1 != null && n2 != null) {
                        if (oldItem.getUrl() != null && newItem.getUrl() != null) {
                            return n1.equals(n2) && oldItem.getUrl().equals(newItem.getUrl());
                        }
                        return n1.equals(n2);
                    }
                    return oldItem == newItem;
                }

                @Override
                public boolean areContentsTheSame(@NonNull FilmStudio oldItem, @NonNull FilmStudio newItem) {
                    return oldItem.isContentSame(newItem);
                }
            };

    /* ==================== 日志 ==================== */
    private final String DEFAULT_TAG = "FilmStudioMergedAdapter";
    private DebugLog debugLog = new DebugLog(DEFAULT_TAG);
    private boolean enabled = false;

    public void enableLog(String tag) {
        this.enabled = true;
        DebugLog.enableLog();
        debugLog = new DebugLog((tag != null && !tag.trim().isEmpty()) ? tag : DEFAULT_TAG);
    }

    public void disableLog() {
        this.enabled = false;
    }

    /* ==================== 鉴权参数 ==================== */
    private String aesKey;
    private String aesIv;
    private Map<String, String> cookies;
    private Map<String, String> headers;

    public void setAesKeyIv(String key, String iv) {
        this.aesKey = key;
        this.aesIv = iv;
        this.cookies = null;
        this.headers = null;
    }

    public void setCookiesHeaders(Map<String, String> cookies, Map<String, String> headers) {
        this.cookies = cookies;
        this.headers = headers;
        this.aesKey = null;
        this.aesIv = null;
    }

    public void clearAuthParams() {
        this.aesKey = null;
        this.aesIv = null;
        this.cookies = null;
        this.headers = null;
    }

    /* ==================== Config ==================== */
    private FilmStudioConfig config;

    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(FilmStudio item, int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    /* ==================== 构造 ==================== */

    public FilmStudioAdapter(@Nullable List<FilmStudio> list, @Nullable FilmStudioConfig config) {
        super(DIFF_CALLBACK);
        this.config = config;
        this.debugLog = new DebugLog(DEFAULT_TAG);
        submitList(list == null ? new ArrayList<>() : new ArrayList<>(list));
    }

    public FilmStudioAdapter(@Nullable List<FilmStudio> list) {
        this(list, null);
    }

    /* ==================== 数据更新 ==================== */
    public void updateData(@Nullable List<FilmStudio> list) {
        int size = (list != null) ? list.size() : 0;
        if (enabled) debugLog.d(getClass().getSimpleName() + "更新数据 listSize=" + size);

        List<FilmStudio> newList;
        if (list == null || list.isEmpty()) {
            newList = new ArrayList<>();
        } else {
            newList = new ArrayList<>(list);
        }

        submitList(newList, () -> {
        });
    }

    /* ==================== ViewHolder 创建 ==================== */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_film_studio, parent, false);

        ViewHolder holder = new ViewHolder(view);
        // 标记收藏图标，让 bindInfoContainer 跳过它
        holder.favoriteView.setTag("favorite");

        return holder;
    }

    /* ==================== 绑定 ==================== */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Context ctx = holder.itemView.getContext();
        FilmStudio item = getItem(position);

        // config 延迟初始化
        if (config == null) {
            config = FilmStudioConfig.defaultConfig(ctx);
            if (enabled) debugLog.d("config 为 null，延迟初始化默认配置");
        }

        if (item == null) return;

        /* ---- 卡片 ---- */
        holder.card.setCardBackgroundColor(config.backgroundColor);
        if (config.backgroundRadius > 0) {
            holder.card.setRadius(dp2px(ctx, config.backgroundRadius));
        }

        /* ---- 名称 ---- */
        holder.nameView.setText(item.getName());
        holder.nameView.setTextColor(config.nameTextColor);
        holder.nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, config.nameTextSize);

        /* ---- 封面 ---- */
        loadCover(holder, item, ctx);

        /* ---- 信息区（收藏图标 + 动态信息行 统一在这里管理） ---- */
        holder.infoContainer.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        bindInfoContainer(holder.infoContainer, ctx, item);

        /* ---- 点击 ---- */
        holder.card.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos < 0) return;
            if (enabled) debugLog.d(getClass().getSimpleName() + "点击了 位置=" + pos + "  数据=" + item.toLogString());
            if (listener != null) {
                listener.onItemClick(getItem(pos), pos);
            }
        });
    }

    /* ==================== 信息区（收藏图标 + 动态 TextView） ==================== */
    /* ==================== 信息区（收藏图标 + 动态 TextView） ==================== */
    private void bindInfoContainer(LinearLayout container, Context ctx, FilmStudio item) {
        List<String> infoList = new ArrayList<>();

        if (!TextUtils.isEmpty(item.getVideoCount())) infoList.add(item.getVideoCount());
        if (!TextUtils.isEmpty(item.getRating())) infoList.add(item.getRating());
        if (!TextUtils.isEmpty(item.getRank())) infoList.add(item.getRank());

        // 第一步：处理收藏图标显隐（在 infoContainer 内的带 "favorite" tag 的 View）
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child.getTag() != null && "favorite".equals(child.getTag())) {
                if (item.isFavorite()) {
                    child.setVisibility(View.VISIBLE);
                    if (config != null) {
                        ((ImageView) child).setImageResource(config.favoriteIcon);
                        if (config.favoriteIconSize > 0) {
                            ViewGroup.LayoutParams lp = child.getLayoutParams();
                            lp.width = dp2px(ctx, config.favoriteIconSize);
                            lp.height = dp2px(ctx, config.favoriteIconSize);
                            child.setLayoutParams(lp);
                        }
                    }
                } else {
                    child.setVisibility(View.GONE);
                }
                break;
            }
        }

        // 第二步：收集已有的动态 TextView（排除 favoriteView）
        List<TextView> existingTextViews = new ArrayList<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child.getTag() != null && "favorite".equals(child.getTag())) continue;
            if (child instanceof TextView) {
                existingTextViews.add((TextView) child);
            }
        }

        // 第三步：复用已有 TextView
        int reuseCount = Math.min(existingTextViews.size(), infoList.size());
        // 第三步：复用已有 TextView
        for (int i = 0; i < reuseCount; i++) {
            TextView tv = existingTextViews.get(i);
            tv.setText(infoList.get(i));
            tv.setTextColor(config.infoTextColor);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, config.infoTextSize);
            tv.setGravity(android.view.Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tv.getLayoutParams();
            if (lp != null) {
                lp.gravity = android.view.Gravity.CENTER_VERTICAL;
            }
            tv.setVisibility(View.VISIBLE);
        }

        // 第四步：不够就新建
        for (int i = reuseCount; i < infoList.size(); i++) {
            TextView tv = createInfoTextView(ctx, container);
            tv.setText(infoList.get(i));
            tv.setTextColor(config.infoTextColor);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, config.infoTextSize);
        }

        // 第五步：多余的 TextView 隐藏
        for (int i = infoList.size(); i < existingTextViews.size(); i++) {
            existingTextViews.get(i).setVisibility(View.GONE);
        }
    }

    private TextView createInfoTextView(Context ctx, LinearLayout container) {
        TextView tv = new TextView(ctx);
        tv.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(dp2px(ctx, 8));
        lp.gravity = android.view.Gravity.CENTER_VERTICAL; // ← 补这一行
        tv.setLayoutParams(lp);
        tv.setMaxLines(1);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        container.addView(tv);
        return tv;
    }

    /* ==================== 封面加载（三路分支） ==================== */
    private void loadCover(@NonNull ViewHolder holder, FilmStudio item, Context ctx) {
        String coverUrl = item.getLogo();

        if (TextUtils.isEmpty(coverUrl)) {
            holder.coverView.setVisibility(View.GONE);
            if (enabled) {
                debugLog.w(getClass().getSimpleName() + " 加载封面跳过(url无效) position="
                        + holder.getBindingAdapterPosition());
            }
            return;
        }

        holder.coverView.setVisibility(View.VISIBLE);

        if (config.coverHeight > 0) {
            ViewGroup.LayoutParams lp = holder.coverView.getLayoutParams();
            lp.height = dp2px(ctx, config.coverHeight);
            holder.coverView.setLayoutParams(lp);
        }

        /* ===== 1. AES ===== */
        if (!TextUtils.isEmpty(aesKey) && !TextUtils.isEmpty(aesIv)) {

            if (enabled) debugLog.d(getClass().getSimpleName() + " 封面加载模式: AES");
            try {
                LoadOption option = new LoadOption()
                        .forList()
                        .placeholder(config.coverPlaceholder)
                        .error(config.coverError)
                        .round(config.coverRoundRadius);
                option.skipMemoryCache(config.skipMemoryCache);
                option.diskCacheStrategy(config.diskCacheStrategy);

                ImageLoader.with(ctx).loadAesBitmap(
                        coverUrl, holder.coverView, option,
                        AesOption.create(aesKey, aesIv));
            } catch (Exception e) {
                if (enabled) debugLog.e(getClass().getSimpleName() + " AES加载封面错误", e);
                holder.coverView.setImageResource(config.coverError);
            }

            /* ===== 2. Cookie + Header ===== */
        } else if (cookies != null && headers != null) {

            if (enabled) debugLog.d(getClass().getSimpleName() + " 封面加载模式: Cookie/Header");
            if (cookies.isEmpty() || headers.isEmpty()) {
                holder.coverView.setImageResource(config.coverError);
                return;
            }

            try {
                LoadOption option = new LoadOption()
                        .forList()
                        .addCookies(cookies)
                        .addHeaders(headers)
                        .placeholder(config.coverPlaceholder)
                        .error(config.coverError)
                        .round(config.coverRoundRadius);
                option.skipMemoryCache(config.skipMemoryCache);
                option.diskCacheStrategy(config.diskCacheStrategy);

                ImageLoader.with(ctx).load(coverUrl, holder.coverView, option);
            } catch (Exception e) {
                if (enabled) debugLog.e(getClass().getSimpleName() + " Cookie/Header加载封面错误", e);
                holder.coverView.setImageResource(config.coverError);
            }

            /* ===== 3. 普通 ===== */
        } else {

            if (enabled) debugLog.d(getClass().getSimpleName() + " 封面加载模式: 普通");
            try {
                RequestOptions options = new RequestOptions().transform(new CenterCrop());
                if (config.coverRoundRadius > 0) {
                    options = options.transform(
                            new CenterCrop(),
                            new RoundedCorners(dp2px(ctx, config.coverRoundRadius)));
                }

                Glide.with(ctx)
                        .load(coverUrl)
                        .apply(options)
                        .placeholder(config.coverPlaceholder)
                        .error(config.coverError)
                        .skipMemoryCache(config.skipMemoryCache)
                        .diskCacheStrategy(config.diskCacheStrategy)
                        .into(holder.coverView);
            } catch (Exception e) {
                if (enabled) debugLog.e(getClass().getSimpleName() + " 普通加载封面错误", e);
                holder.coverView.setImageResource(config.coverError);
            }
        }
    }

    /* ==================== ViewHolder ==================== */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        CardView card;
        ImageView coverView;
        ImageView favoriteView;
        TextView nameView;
        LinearLayout infoContainer;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.item);
            coverView = itemView.findViewById(R.id.coverView);
            favoriteView = itemView.findViewById(R.id.favoriteView);
            nameView = itemView.findViewById(R.id.nameView);
            infoContainer = itemView.findViewById(R.id.infoContainer);
        }
    }

    /* ==================== 工具 ==================== */
    private int dp2px(Context ctx, int dp) {
        return (int) (dp * ctx.getResources().getDisplayMetrics().density + 0.5f);
    }
}