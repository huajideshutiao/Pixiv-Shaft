package ceui.lisa.fragments;

import android.os.Bundle;
import android.text.TextUtils;

import com.bumptech.glide.Glide;

import java.io.File;

import ceui.lisa.R;
import ceui.lisa.databinding.FragmentImageDetailLocalBinding;
import ceui.lisa.utils.Params;

public class FragmentLocalImageDetail extends BaseFragment<FragmentImageDetailLocalBinding> {

    private String filePath;

    public static FragmentLocalImageDetail newInstance(String filePath) {
        Bundle args = new Bundle();
        args.putString(Params.FILE_PATH, filePath);
        FragmentLocalImageDetail fragment = new FragmentLocalImageDetail();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void initBundle(Bundle bundle) {
        filePath = bundle.getString(Params.FILE_PATH);
    }

    @Override
    public void initLayout() {
        mLayoutID = R.layout.fragment_image_detail_local;
    }

    @Override
    public void initView() {
        if (!TextUtils.isEmpty(filePath)) {
            if (filePath.contains(".zip")) {
                baseBind.illustImage.setImageResource(R.mipmap.zip);
            } else {
                Glide.with(this)
                        .load(new File(filePath))
                        .into(baseBind.illustImage);
            }
        }
    }
}
