package ceui.lisa.fragments


import android.content.Intent
import android.net.Uri
import android.view.View
import ceui.lisa.BuildConfig
import ceui.lisa.R
import ceui.lisa.activities.ContainerActivity
import ceui.lisa.databinding.FragmentAboutBinding
import ceui.lisa.update.AppUpdateChecker
import ceui.lisa.update.GitHubRelease
import ceui.lisa.update.UpdateBottomSheet
import ceui.lisa.utils.Common
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import io.reactivex.disposables.Disposable

class FragmentAboutApp : SwipeFragment<FragmentAboutBinding>() {

    private var updateDisposable: Disposable? = null

    override fun initLayout() {
        mLayoutID = R.layout.fragment_about
    }

    override fun getSmartRefreshLayout(): SmartRefreshLayout {
        return baseBind.refreshLayout
    }

    override fun initData() {
        baseBind.toolbar.setNavigationOnClickListener { mActivity.finish() }

        baseBind.appVersion.text = "%s (%s) "
            .format(Common.getAppVersionName(mContext), Common.getAppVersionCode(mContext))

        if (BuildConfig.UPDATE_CHANNEL == "github") {
            baseBind.githubUpdateSection.visibility = View.VISIBLE
            baseBind.checkUpdate.setOnClickListener {
                performUpdateCheck(manual = true)
            }
            baseBind.versionHistory.setOnClickListener {
                val intent = Intent(mContext, ContainerActivity::class.java)
                intent.putExtra(ContainerActivity.EXTRA_FRAGMENT, "版本历史")
                startActivity(intent)
            }
        } else {
            baseBind.githubUpdateSection.visibility = View.GONE
        }

        // Auto-check for github builds
        if (AppUpdateChecker.shouldAutoCheck()) {
            performUpdateCheck(manual = false)
        }

        baseBind.applicationId.text = requireContext().applicationInfo.packageName
        baseBind.projectWebsite.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.data = Uri.parse("https://github.com/huajideshutiao/Pixiv-Shaft")
            startActivity(intent)
        }
    }

    private fun performUpdateCheck(manual: Boolean) {
        baseBind.updateStatus.setText(R.string.update_checking)

        updateDisposable?.dispose()
        updateDisposable = AppUpdateChecker.checkForUpdate()
            .subscribe({ result ->
                AppUpdateChecker.markChecked()
                when (result) {
                    is AppUpdateChecker.UpdateResult.UpdateAvailable -> {
                        val version = result.release.tagName.removePrefix("v").removePrefix("V")
                        baseBind.updateStatus.text = getString(R.string.update_found_new, version)
                        if (!manual && AppUpdateChecker.isVersionSkipped(version)) {
                            return@subscribe
                        }
                        showUpdateDialog(result.release)
                    }

                    is AppUpdateChecker.UpdateResult.NoUpdate -> {
                        baseBind.updateStatus.text =
                            getString(R.string.update_already_latest, result.remoteVersion)
                    }
                }
            }, { _ ->
                baseBind.updateStatus.setText(R.string.update_check_failed)
            })
    }

    private fun showUpdateDialog(release: GitHubRelease) {
        val dialog = UpdateBottomSheet.newInstance(release)
        dialog.show(childFragmentManager, "update_dialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        updateDisposable?.dispose()
    }


    companion object {
    }
}
