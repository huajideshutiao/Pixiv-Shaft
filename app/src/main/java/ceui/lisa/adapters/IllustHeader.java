package ceui.lisa.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;

import ceui.pixiv.route.AppRoute;
import ceui.lisa.activities.RankActivity;
import ceui.lisa.core.Container;
import ceui.lisa.core.PageData;
import ceui.lisa.databinding.RecyRecmdHeaderBinding;
import ceui.lisa.models.IllustsBean;
import ceui.lisa.utils.DensityUtil;
import ceui.lisa.utils.Params;
import ceui.lisa.view.LinearItemHorizontalDecoration;

public class IllustHeader extends ViewHolder<RecyRecmdHeaderBinding> {

    private String type = "";

    public IllustHeader(RecyRecmdHeaderBinding bindView, String type) {
        super(bindView);
        this.type = type;
    }

    public void show(Context context, List<IllustsBean> illustsBeans) {
        baseBind.topRela.setVisibility(View.VISIBLE);
        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(800L);
        baseBind.topRela.startAnimation(animation);
        RAdapter adapter = new RAdapter(illustsBeans, context);
        adapter.setOnItemClickListener((v, position, viewType) -> {
            final PageData pageData = new PageData(illustsBeans);
            Container.get().addPageToMap(pageData);

            new AppRoute.FullScreen(pageData.getUUID(), position, null).start(context);
        });
        baseBind.ranking.setAdapter(adapter);
    }

    public void initView(Context context) {
        baseBind.topRela.setVisibility(View.GONE);
        baseBind.seeMore.setOnClickListener(v -> {
            Intent intent = new Intent(context, RankActivity.class);
            intent.putExtra("dataType", type);
            context.startActivity(intent);
        });
        baseBind.ranking.addItemDecoration(new LinearItemHorizontalDecoration(DensityUtil.dp2px(8.0f)));
        LinearLayoutManager manager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        baseBind.ranking.setLayoutManager(manager);
        baseBind.ranking.setHasFixedSize(true);
    }
}
