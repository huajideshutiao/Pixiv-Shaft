package ceui.lisa.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;

import ceui.lisa.activities.RankActivity;
import ceui.lisa.databinding.RecyRecmdHeaderBinding;
import ceui.lisa.models.NovelBean;
import ceui.lisa.utils.DensityUtil;
import ceui.lisa.view.LinearItemHorizontalDecoration;
import ceui.pixiv.route.AppRoute;

public class NovelHeader extends ViewHolder<RecyRecmdHeaderBinding> {

    public NovelHeader(RecyRecmdHeaderBinding bindView) {
        super(bindView);
    }

    public void show(Context context, List<NovelBean> illustsBeans) {
        baseBind.topRela.setVisibility(View.VISIBLE);
        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(800L);
        baseBind.topRela.startAnimation(animation);
        NHAdapter adapter = new NHAdapter(illustsBeans, context);
        adapter.setOnItemClickListener((v, position, viewType) -> {
            new AppRoute.NovelDetail(Long.valueOf(illustsBeans.get(position).getId())).start(context);
        });
        baseBind.ranking.setAdapter(adapter);
    }

    public void initView(Context context) {
        baseBind.topRela.setVisibility(View.GONE);
        baseBind.seeMore.setOnClickListener(v -> {
            Intent intent = new Intent(context, RankActivity.class);
            intent.putExtra("dataType", "小说");
            context.startActivity(intent);
        });
        baseBind.ranking.addItemDecoration(new LinearItemHorizontalDecoration(DensityUtil.dp2px(8.0f)));
        LinearLayoutManager manager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        baseBind.ranking.setLayoutManager(manager);
        baseBind.ranking.setHasFixedSize(true);
    }
}
