package ceui.lisa.adapters;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Locale;

import ceui.lisa.R;
import ceui.pixiv.route.AppRoute;
import ceui.lisa.activities.SearchActivity;
import ceui.lisa.activities.UActivity;
import ceui.lisa.databinding.RecyNovelMarkersBinding;
import ceui.lisa.models.MarkedNovelItem;
import ceui.lisa.utils.GlideUtil;
import ceui.lisa.utils.Params;
import ceui.lisa.utils.PixivOperate;
import ceui.pixiv.utils.FlexboxUtils;

public class NovelMarkersAdapter extends BaseAdapter<MarkedNovelItem, RecyNovelMarkersBinding> {
    public NovelMarkersAdapter(List<MarkedNovelItem> targetList, Context context) {
        super(targetList, context);
    }

    @Override
    public void initLayout() {
        mLayoutID = R.layout.recy_novel_markers;
    }

    @Override
    public RecyNovelMarkersBinding createViewBinding(LayoutInflater inflater, ViewGroup parent, boolean attachToParent) {
        return RecyNovelMarkersBinding.inflate(inflater, parent, attachToParent);
    }

    @Override
    public void bindData(MarkedNovelItem target, ViewHolder<RecyNovelMarkersBinding> bindView, int position) {
        if (target.getNovel().getSeries() != null && !TextUtils.isEmpty(target.getNovel().getSeries().getTitle())) {
            bindView.baseBind.series.setVisibility(View.VISIBLE);
            bindView.baseBind.series.setText(String.format(mContext.getString(R.string.string_184),
                    target.getNovel().getSeries().getTitle()));
            bindView.baseBind.series.setOnClickListener(v -> {
                new AppRoute.NovelSeriesDetail(target.getNovel().getSeries().getId()).start(mContext);
            });
        } else {
            bindView.baseBind.series.setVisibility(View.GONE);
        }
        bindView.baseBind.title.setText(target.getNovel().getTitle());
        bindView.baseBind.date.setText(target.getNovel().getCreate_date().substring(0, 10));
        FlexboxUtils.populate(
            bindView.baseBind.novelTag,
            target.getNovel().getTags(),
            R.layout.recy_single_line_text_new,
            (view, s, index) -> {
                TextView tv = (TextView) view;
                String tag = s.getName();
                tv.setText(tag);
                view.setOnClickListener(v -> {
                Intent intent = new Intent(mContext, SearchActivity.class);
                    intent.putExtra(
                        Params.KEY_WORD,
                        target.getNovel().getTags().get(index).getName()
                    );
                intent.putExtra(Params.INDEX, 1);
                mContext.startActivity(intent);
                });
        });
        bindView.baseBind.author.setText(target.getNovel().getUser().getName());
        bindView.baseBind.howManyWord.setText(String.format(Locale.getDefault(), "%d字", target.getNovel().getText_length()));
        bindView.baseBind.bookmarkCount.setText(String.valueOf(target.getNovel().getTotal_bookmarks()));
        Glide.with(mContext).load(GlideUtil.getUrl(target.getNovel().getImage_urls().getMaxImage())).into(bindView.baseBind.cover);
        Glide.with(mContext).load(GlideUtil.getHead(target.getNovel().getUser())).into(bindView.baseBind.userHead);

        bindView.baseBind.cover.setOnClickListener(v -> {
            new AppRoute.UrlImage(GlideUtil.getUrl(target.getNovel().getImage_urls().getMaxImage()).toStringUrl(), null).start(mContext);
        });

        bindView.baseBind.userHead.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, UActivity.class);
            intent.putExtra(Params.USER_ID, target.getNovel().getUser().getId());
            mContext.startActivity(intent);
        });

        bindView.baseBind.author.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, UActivity.class);
            intent.putExtra(Params.USER_ID, target.getNovel().getUser().getId());
            mContext.startActivity(intent);
        });

        bindView.itemView.setOnClickListener(v -> {
            new AppRoute.NovelDetail((long) target.getNovel().getId()).start(mContext);
        });

        bindView.baseBind.mark.setOnClickListener(v ->
                PixivOperate.postNovelMarker(target.getNovel_marker(),
                                             target.getNovel().getId(),
                                             bindView.baseBind.mark));

    }
}