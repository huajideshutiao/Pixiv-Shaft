package ceui.lisa.fragments

import android.view.LayoutInflater
import android.view.ViewGroup
import ceui.lisa.R
import ceui.lisa.databinding.FragmentUserInfoBinding
import ceui.lisa.interfaces.Display
import ceui.lisa.models.UserDetailResponse
import ceui.lisa.utils.Common
import ceui.lisa.utils.Params
import com.scwang.smart.refresh.header.FalsifyFooter
import com.scwang.smart.refresh.header.FalsifyHeader

class FragmentUserInfo : BaseFragment<FragmentUserInfoBinding>(), Display<UserDetailResponse> {

    override fun initLayout() {
        mLayoutID = R.layout.fragment_user_info
    }

    public override fun initData() {
        baseBind.toolbar.setNavigationOnClickListener {
            mActivity.finish()
        }
        val user = mActivity.intent.getSerializableExtra(Params.CONTENT) as UserDetailResponse
        invoke(user)
    }

    override fun invoke(response: UserDetailResponse) {
        val profile = response.profile!!
        val user = response.user!!
        val workspace = response.workspace!!
        baseBind.mainPage.setHtml(Common.checkEmpty(profile.webpage))
        baseBind.twitter.setHtml(Common.checkEmpty(profile.twitter_url))
        baseBind.description.setHtml(Common.checkEmpty(user.comment))
        baseBind.pawoo.setHtml(Common.checkEmpty(profile.pawoo_url))
        baseBind.computer.text = Common.checkEmpty(workspace.pc)
        baseBind.monitor.text = Common.checkEmpty(workspace.monitor)
        baseBind.app.text = Common.checkEmpty(workspace.tool)
        baseBind.scanner.text = Common.checkEmpty(workspace.scanner)
        baseBind.drawBoard.text = Common.checkEmpty(workspace.tablet)
        baseBind.mouse.text = Common.checkEmpty(workspace.mouse)
        baseBind.printer.text = Common.checkEmpty(workspace.printer)
        baseBind.tableObjects.text = Common.checkEmpty(workspace.desktop)
        baseBind.likeMusic.text = Common.checkEmpty(workspace.music)
        baseBind.table.text = Common.checkEmpty(workspace.desk)
        baseBind.chair.text = Common.checkEmpty(workspace.chair)
        baseBind.otherText.text = Common.checkEmpty(workspace.comment)
    }

    override fun initView() {
        baseBind.refreshLayout.setRefreshHeader(FalsifyHeader(mContext))
        baseBind.refreshLayout.setRefreshFooter(FalsifyFooter(mContext))
    }

    override fun onCreateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        attachToParent: Boolean
    ): FragmentUserInfoBinding {
        return FragmentUserInfoBinding.inflate(inflater, container, false)
    }
}
