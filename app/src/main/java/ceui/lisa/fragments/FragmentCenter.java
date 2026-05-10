package ceui.lisa.fragments;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.FragmentTransaction;

import com.scwang.smart.refresh.layout.SmartRefreshLayout;

import ceui.lisa.R;
import ceui.lisa.activities.ContainerActivity;
import ceui.lisa.activities.MainActivity;
import ceui.lisa.activities.Shaft;
import ceui.lisa.databinding.FragmentNewCenterBinding;
import ceui.lisa.interfaces.VolumeKeyHandler;
import ceui.lisa.utils.Dev;

public class FragmentCenter extends SwipeFragment<FragmentNewCenterBinding> implements
    VolumeKeyHandler {

    private FragmentPivisionHorizontal pivisionFragment = null;
    
    @Override
    public void initLayout() {
        mLayoutID = R.layout.fragment_new_center;
    }

    @Override
    protected void initView() {
        if (Dev.hideMainActivityStatus) {
            ViewGroup.LayoutParams headParams = baseBind.head.getLayoutParams();
            headParams.height = Shaft.statusHeight;
            baseBind.head.setLayoutParams(headParams);
        }

        baseBind.toolbar.inflateMenu(R.menu.fragment_left);
        baseBind.toolbar.setNavigationOnClickListener(v -> {
            if (mActivity instanceof MainActivity) {
                ((MainActivity) mActivity).getDrawer().openDrawer(GravityCompat.START, true);
            }
        });
        baseBind.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_search) {
                Intent intent = new Intent(mContext, ceui.lisa.activities.SearchActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });

        baseBind.latestWork.setClipToOutline(true);
        baseBind.manga.setClipToOutline(true);
        baseBind.novel.setClipToOutline(true);
        baseBind.walkThrough.setClipToOutline(true);
        baseBind.followNovels.setClipToOutline(true);

        baseBind.latestWork.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, ContainerActivity.class);
            intent.putExtra(ContainerActivity.EXTRA_FRAGMENT, "最新作品");
            intent.putExtra("hideStatusBar", false);
            startActivity(intent);
        });
        baseBind.manga.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, ContainerActivity.class);
            intent.putExtra(ContainerActivity.EXTRA_FRAGMENT, "推荐漫画");
            startActivity(intent);
        });
        baseBind.novel.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, ContainerActivity.class);
            intent.putExtra(ContainerActivity.EXTRA_FRAGMENT, "推荐小说");
            intent.putExtra("hideStatusBar", false);
            startActivity(intent);
        });

        baseBind.walkThrough.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, ContainerActivity.class);
            intent.putExtra(ContainerActivity.EXTRA_FRAGMENT, "画廊");
            startActivity(intent);
        });
        baseBind.followNovels.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, ContainerActivity.class);
            intent.putExtra(ContainerActivity.EXTRA_FRAGMENT, "关注者的小说");
            startActivity(intent);
        });
    }

    @Override
    public void lazyData() {
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();

        pivisionFragment = new FragmentPivisionHorizontal();
        transaction.add(R.id.fragment_pivision, pivisionFragment, "FragmentPivisionHorizontal");
        transaction.commitNowAllowingStateLoss();
    }

    @Override
    public boolean handleVolumeKey(int keyCode) {
        if (baseBind != null) {
            int scrollDistance = baseBind.refreshLayout.getHeight() / 2;
            if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN) {
                baseBind.refreshLayout.getLayout().scrollBy(0, scrollDistance);
            } else if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP) {
                baseBind.refreshLayout.getLayout().scrollBy(0, -scrollDistance);
            }
            return true;
        }
        return false;
    }

    @Override
    public SmartRefreshLayout getSmartRefreshLayout() {
        return baseBind.refreshLayout;
    }

    public void forceRefresh(){
        if(pivisionFragment != null){
            pivisionFragment.forceRefresh();
        }
    }

    @Override
    protected FragmentNewCenterBinding onCreateBinding(
        @NonNull LayoutInflater inflater,
        ViewGroup container,
        boolean attachToParent
    ) {
        return FragmentNewCenterBinding.inflate(inflater, container, false);
    }
}
