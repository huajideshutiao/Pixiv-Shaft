package ceui.lisa.dialogs;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ceui.lisa.R;
import ceui.lisa.activities.ContainerActivity;
import ceui.lisa.databinding.DialogMuteTagBinding;
import ceui.lisa.helper.IllustNovelFilter;
import ceui.lisa.models.IllustsBean;
import ceui.lisa.models.TagsBean;
import ceui.lisa.utils.Common;
import ceui.lisa.utils.Params;
import ceui.lisa.utils.PixivOperate;
import ceui.pixiv.utils.FlexboxExtKt;

public class MuteDialog extends BaseDialog<DialogMuteTagBinding> {

    private IllustsBean mIllust;
    private final List<TagsBean> selected = new ArrayList<>();
    private final List<Boolean> muteNotEffect = new ArrayList<>();

    public static MuteDialog newInstance(IllustsBean illustsBean) {
        Bundle args = new Bundle();
        args.putSerializable(Params.CONTENT, illustsBean);
        MuteDialog fragment = new MuteDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    void initLayout() {
        mLayoutID = R.layout.dialog_mute_tag;
    }

    @Override
    void initView(View v) {
        List<TagsBean> muted = IllustNovelFilter.getMutedTags();
        List<TagsBean> illustTags = mIllust.getTags();
        Set<Integer> selectedIndex = new HashSet<>();
        for (int i = 0; i < illustTags.size(); i++) {
            TagsBean tagsBean = illustTags.get(i);
            muteNotEffect.add(i, false);
            for (TagsBean mutedBean : muted) {
                if (tagsBean.getName().equals(mutedBean.getName())) {
                    if (mutedBean.isEffective()) {
                        selectedIndex.add(i);
                    } else {
                        muteNotEffect.set(i, true);
                    }
                    break;
                }
            }
        }

        FlexboxExtKt.populate(
            baseBind.tagLayout, mIllust.getTags(), R.layout.recy_single_tag_text, (view, o) -> {
                TextView tag = view.findViewById(R.id.tag_title);
                tag.setText(o.getName());
                int position = mIllust.getTags().indexOf(o);

                if (muteNotEffect.get(position)) {
                    view.setBackgroundResource(R.drawable.tag_stroke_checked_not_enable_bg);
                } else if (selectedIndex.contains(position)) {
                    tag.setTextColor(Common.resolveThemeAttribute(
                        mContext,
                        androidx.appcompat.R.attr.colorPrimary
                    ));
                view.setBackgroundResource(R.drawable.tag_stroke_checked_bg);
                    if (!selected.contains(o)) {
                        selected.add(o);
                    }
                } else {
                    view.setBackgroundResource(R.drawable.tag_stroke_bg);
            }

                view.setOnClickListener(v1 -> {
                    if (selected.contains(o)) {
                        selected.remove(o);
                        if (muteNotEffect.get(position)) {
                            view.setBackgroundResource(R.drawable.tag_stroke_checked_not_enable_bg);
                        } else {
                            view.setBackgroundResource(R.drawable.tag_stroke_bg);
                        }
                        tag.setTextColor(ContextCompat.getColor(
                            mContext,
                            R.color.tag_text_unselect
                        ));
                    } else {
                        selected.add(o);
                        tag.setTextColor(Common.resolveThemeAttribute(
                            mContext,
                            androidx.appcompat.R.attr.colorPrimary
                        ));
                        view.setBackgroundResource(R.drawable.tag_stroke_checked_bg);
                    }
                });
            }
        );

        baseBind.cancel.setOnClickListener(v1 -> dismiss());
        baseBind.sure.setOnClickListener(v1 -> {
            if (selected.size() != 0) {
                PixivOperate.muteTags(selected);
                Common.showToast(mContext.getResources().getString(R.string.operate_success));
                dismiss();
            } else {
                Common.showToast(getString(R.string.string_165));
            }
        });
        baseBind.other.setOnClickListener(v1 -> {
            Intent intent = new Intent(mContext, ContainerActivity.class);
            intent.putExtra(ContainerActivity.EXTRA_FRAGMENT, "标签屏蔽记录");
            mContext.startActivity(intent);
            dismiss();
        });
    }

    @Override
    void initData() {

    }

    @Override
    @SuppressWarnings("deprecation")
    public void initBundle(Bundle bundle) {
        mIllust = ((IllustsBean) bundle.getSerializable(Params.CONTENT));
    }

    @Override
    protected DialogMuteTagBinding onCreateBinding(
        LayoutInflater inflater,
        ViewGroup container,
        boolean attachToParent
    ) {
        return DialogMuteTagBinding.inflate(inflater, container, false);
    }
}
