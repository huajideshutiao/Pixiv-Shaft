package ceui.pixiv.ui.detail


import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import ceui.lisa.R
import ceui.lisa.core.ArtworksMap
import ceui.lisa.databinding.FragmentArtworkViewpagerBinding
import ceui.loxia.threadSafeArgs
import ceui.pixiv.ui.common.PixivFragment
import ceui.pixiv.ui.common.viewBinding
import ceui.pixiv.ui.novel.NovelTextFragment

class ArtworkViewPagerFragment : PixivFragment(R.layout.fragment_artwork_viewpager) {

    private val binding by viewBinding(FragmentArtworkViewpagerBinding::bind)
    private val safeArgs by threadSafeArgs<ArtworkViewPagerFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ids = ArtworksMap.store[safeArgs.seed]
        if (ids?.isNotEmpty() == true) {
            binding.artworkViewpager.adapter = object : FragmentStateAdapter(this) {
                override fun createFragment(position: Int): Fragment {
                    return NovelTextFragment.newInstance(ids[position])
                }

                override fun getItemCount(): Int {
                    return ids.size
                }
            }
            val index = ids.indexOf(safeArgs.objectId)
            if (index > 0) {
                binding.artworkViewpager.setCurrentItem(index, false)
            }
        } else {
            binding.artworkViewpager.adapter = object : FragmentStateAdapter(this) {
                override fun createFragment(position: Int): Fragment {
                    return NovelTextFragment.newInstance(safeArgs.objectId)
                }

                override fun getItemCount(): Int {
                    return 1
                }
            }
        }
    }

    override fun handleVolumeKey(keyCode: Int): Boolean {
        val viewPager = view?.findViewById<ViewPager2>(R.id.artwork_viewpager) ?: return false
        val adapter = viewPager.adapter ?: return false
        val currentItem = viewPager.currentItem
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (currentItem < adapter.itemCount - 1) {
                viewPager.setCurrentItem(currentItem + 1, true)
            } else {
                ceui.lisa.utils.Common.showToast("已到达最后一页")
            }
            return true
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (currentItem > 0) {
                viewPager.setCurrentItem(currentItem - 1, true)
            } else {
                ceui.lisa.utils.Common.showToast("已到达第一页")
            }
            return true
        }
        return false
    }
}
