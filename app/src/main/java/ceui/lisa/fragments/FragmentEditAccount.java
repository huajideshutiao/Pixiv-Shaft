package ceui.lisa.fragments;

import android.text.TextUtils;
import android.view.View;

import ceui.lisa.R;
import ceui.lisa.activities.Shaft;

import static ceui.lisa.core.CallExtKt.executeCall;
import ceui.lisa.database.AppDatabase;
import ceui.lisa.database.UserEntity;
import ceui.lisa.databinding.FragmentEditAccountBinding;
import ceui.lisa.http.NullCtrl;
import ceui.lisa.http.Retro;
import ceui.lisa.models.AccountEditResponse;
import ceui.lisa.models.UserState;
import ceui.lisa.models.UserModel;
import ceui.lisa.utils.Common;
import ceui.lisa.utils.Local;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import java.util.function.Function;

import ceui.pixiv.session.SessionManager;

public class FragmentEditAccount extends BaseFragment<FragmentEditAccountBinding> {

    private boolean canChangePixivID = false;
    private boolean hasPassword = false;

    @Override
    public void initLayout() {
        mLayoutID = R.layout.fragment_edit_account;
    }

    @Override
    protected void initData() {
        if (!SessionManager.INSTANCE.isLoggedIn()) {
            Common.showToast("你还没有登录");
            mActivity.finish();
            return;
        }
        baseBind.toolbar.toolbarTitle.setText(R.string.string_250);
        baseBind.toolbar.toolbar.setNavigationOnClickListener(v -> finish());
        executeCall(
            Retro.getAppApi().getAccountState(), Function.identity(),
            new NullCtrl<UserState>() {
                    @Override
                    public void success(UserState userState) {
                        if (userState.getUser_state() != null) {
                            canChangePixivID = userState.getUser_state().getCan_change_pixiv_id();
                            baseBind.pixivId.setEnabled(canChangePixivID);
                            hasPassword = userState.getUser_state().getHas_password();
                            baseBind.userOldPasswordLayout.setVisibility(hasPassword ? View.VISIBLE : View.GONE);
                        }
                    }
                });
        if (!TextUtils.isEmpty(SessionManager.INSTANCE.getMailAddress())) {
            baseBind.emailAddress.setText(SessionManager.INSTANCE.getMailAddress());
        }
        baseBind.pixivId.setText(SessionManager.INSTANCE.getAccountName());
        baseBind.pixivId.setEnabled(false);
        baseBind.submit.setOnClickListener(v -> submit());
    }

    @Override
    protected FragmentEditAccountBinding onCreateBinding(
        @NonNull LayoutInflater inflater,
        ViewGroup container,
        boolean attachToParent
    ) {
        return FragmentEditAccountBinding.inflate(inflater, container, false);
    }

    private void submit() {
        if(hasPassword && TextUtils.isEmpty(baseBind.userOldPassword.getText().toString())){
            Common.showToast("更新账号信息需要输入当前密码");
            return;
        }
        String currentPassword = baseBind.userOldPassword.getText().toString();
        if (canChangePixivID) {
            if (TextUtils.isEmpty(baseBind.pixivId.getText().toString())) {
                Common.showToast("pixiv ID不能为空");
                return;
            }
            if (TextUtils.isEmpty(baseBind.userNewPassword.getText().toString())) {
                Common.showToast("新密码不能为空");
                return;
            }
            boolean isPixivIdNotChanged = baseBind.pixivId.getText().toString().equals(SessionManager.INSTANCE.getAccountName());
            boolean isPasswordNotChanged = baseBind.userNewPassword.getText().toString().equals(currentPassword);
            if (TextUtils.isEmpty(baseBind.emailAddress.getText().toString())) {
                if (isPixivIdNotChanged && isPasswordNotChanged) {
                    Common.showToast("你还没有做任何修改");
                } else if (isPixivIdNotChanged && !isPasswordNotChanged) {
                    Common.showToast("正在修改密码");
                    executeCall(
                        Retro.getSignApi().changePassword(
                            SessionManager.INSTANCE.getBearerToken(),
                            currentPassword,
                            baseBind.userNewPassword.getText().toString()
                        ),
                        Function.identity(),
                        new NullCtrl<AccountEditResponse>() {
                                @Override
                                public void success(AccountEditResponse accountEditResponse) {
                                    Local.getUser().getUser().setPassword(baseBind.userNewPassword.getText().toString());
                                    saveUser();
                                    mActivity.finish();
                                    Common.showToast("密码修改成功");
                                }
                            });
                } else if (!isPixivIdNotChanged && isPasswordNotChanged) {
                    Common.showToast("正在修改PixivID");
                    executeCall(
                        Retro.getSignApi().changePixivID(
                            SessionManager.INSTANCE.getBearerToken(),
                            baseBind.pixivId.getText().toString(),
                            currentPassword
                        ),
                        Function.identity(),
                        new NullCtrl<AccountEditResponse>() {
                                @Override
                                public void success(AccountEditResponse accountEditResponse) {
                                    Local.getUser().getUser().setAccount(baseBind.pixivId.getText().toString());
                                    saveUser();
                                    mActivity.finish();
                                    Common.showToast("PixivID修改成功");
                                }
                            });
                } else if (!isPixivIdNotChanged && !isPasswordNotChanged) {
                    Common.showToast("正在修改PixivID 和密码");
                    executeCall(
                        Retro.getSignApi().changePasswordPixivID(
                            SessionManager.INSTANCE.getBearerToken(),
                            baseBind.pixivId.getText().toString(),
                            currentPassword,
                            baseBind.userNewPassword.getText().toString()
                        ),
                        Function.identity(),
                        new NullCtrl<AccountEditResponse>() {
                                @Override
                                public void success(AccountEditResponse accountEditResponse) {
                                    Local.getUser().getUser().setAccount(baseBind.pixivId.getText().toString());
                                    Local.getUser().getUser().setPassword(baseBind.userNewPassword.getText().toString());
                                    saveUser();
                                    mActivity.finish();
                                    Common.showToast("PixivID 和密码修改成功");
                                }
                            });
                }
            } else {
                if (TextUtils.isEmpty(baseBind.pixivId.getText().toString())) {
                    Common.showToast("pixiv ID不能为空");
                    return;
                }
                if (TextUtils.isEmpty(baseBind.userNewPassword.getText().toString())) {
                    Common.showToast("新密码不能为空");
                    return;
                }

                if (isPixivIdNotChanged && isPasswordNotChanged) {
                    executeCall(
                        Retro.getSignApi().changeEmail(
                            SessionManager.INSTANCE.getBearerToken(),
                            baseBind.emailAddress.getText().toString(),
                            currentPassword
                        ),
                        Function.identity(),
                        new NullCtrl<AccountEditResponse>() {
                                @Override
                                public void success(AccountEditResponse accountEditResponse) {
                                    mActivity.finish();
                                    Common.showToast("验证邮件发送成功！", true);
                                }
                            });
                } else if (!isPixivIdNotChanged && isPasswordNotChanged) {
                    executeCall(
                        Retro.getSignApi().changeEmailAndPixivID(
                            SessionManager.INSTANCE.getBearerToken(),
                            baseBind.emailAddress.getText().toString(),
                            baseBind.pixivId.getText().toString(),
                            currentPassword
                        ),
                        Function.identity(),
                        new NullCtrl<AccountEditResponse>() {
                                @Override
                                public void success(AccountEditResponse accountEditResponse) {
                                    Local.getUser().getUser().setAccount(baseBind.pixivId.getText().toString());
                                    saveUser();
                                    mActivity.finish();
                                    Common.showToast("验证邮件发送成功！", true);
                                }
                            });
                } else if (isPixivIdNotChanged && !isPasswordNotChanged) {
                    executeCall(
                        Retro.getSignApi().changeEmailAndPassword(
                            SessionManager.INSTANCE.getBearerToken(),
                            baseBind.emailAddress.getText().toString(),
                            currentPassword,
                            baseBind.userNewPassword.getText().toString()
                        ),
                        Function.identity(),
                        new NullCtrl<AccountEditResponse>() {
                                @Override
                                public void success(AccountEditResponse accountEditResponse) {
                                    Local.getUser().getUser().setPassword(baseBind.userNewPassword.getText().toString());
                                    saveUser();
                                    mActivity.finish();
                                    Common.showToast("验证邮件发送成功！", true);
                                }
                            });
                } else if (!isPixivIdNotChanged && !isPasswordNotChanged) {
                    executeCall(
                        Retro.getSignApi().edit(
                            SessionManager.INSTANCE.getBearerToken(),
                            baseBind.emailAddress.getText().toString(),
                            baseBind.pixivId.getText().toString(),
                            currentPassword,
                            baseBind.userNewPassword.getText().toString()
                        ),
                        Function.identity(),
                        new NullCtrl<AccountEditResponse>() {
                                @Override
                                public void success(AccountEditResponse accountEditResponse) {
                                    Local.getUser().getUser().setPassword(baseBind.userNewPassword.getText().toString());
                                    Local.getUser().getUser().setAccount(baseBind.pixivId.getText().toString());
                                    saveUser();
                                    mActivity.finish();
                                    Common.showToast("验证邮件发送成功！", true);
                                }
                            });
                }
            }
        } else {
            if (TextUtils.isEmpty(baseBind.userNewPassword.getText().toString())) {
                Common.showToast("新密码不能为空");
                return;
            }
            boolean isPasswordNotChanged = baseBind.userNewPassword.getText().toString().equals(currentPassword);
            if (TextUtils.isEmpty(baseBind.emailAddress.getText().toString())) {
                if (isPasswordNotChanged) {
                    Common.showToast("你还没有做任何修改");
                } else {
                    Common.showToast("正在修改密码");
                    executeCall(
                        Retro.getSignApi().changePassword(
                            SessionManager.INSTANCE.getBearerToken(),
                            currentPassword,
                            baseBind.userNewPassword.getText().toString()
                        ),
                        Function.identity(),
                        new NullCtrl<AccountEditResponse>() {
                                @Override
                                public void success(AccountEditResponse accountEditResponse) {
                                    Local.getUser().getUser().setPassword(baseBind.userNewPassword.getText().toString());
                                    saveUser();
                                    mActivity.finish();
                                    Common.showToast("密码修改成功");
                                }
                            });
                }
            } else {
                boolean isEmailNotChanged = baseBind.emailAddress.getText().toString().equals(Local.getUser().getUser().getMail_address());
                if (isEmailNotChanged) {
                    if (isPasswordNotChanged) {
                        Common.showToast("你还没有做任何修改");
                    } else {
                        Common.showToast("正在修改密码");
                        executeCall(
                            Retro.getSignApi().changePassword(
                                SessionManager.INSTANCE.getBearerToken(),
                                currentPassword,
                                baseBind.userNewPassword.getText().toString()
                            ),
                            Function.identity(),
                            new NullCtrl<AccountEditResponse>() {
                                    @Override
                                    public void success(AccountEditResponse accountEditResponse) {
                                        Local.getUser().getUser().setPassword(baseBind.userNewPassword.getText().toString());
                                        saveUser();
                                        mActivity.finish();
                                        Common.showToast("密码修改成功");
                                    }
                                });
                    }
                } else {
                    if (isPasswordNotChanged) {
                        executeCall(
                            Retro.getSignApi().changeEmail(
                                SessionManager.INSTANCE.getBearerToken(),
                                baseBind.emailAddress.getText().toString(),
                                currentPassword
                            ),
                            Function.identity(),
                            new NullCtrl<AccountEditResponse>() {
                                    @Override
                                    public void success(AccountEditResponse accountEditResponse) {
                                        mActivity.finish();
                                        Common.showToast("验证邮件发送成功！", true);
                                    }
                                });
                    } else {
                        executeCall(
                            Retro.getSignApi().changeEmailAndPassword(
                                SessionManager.INSTANCE.getBearerToken(),
                                baseBind.emailAddress.getText().toString(),
                                currentPassword,
                                baseBind.userNewPassword.getText().toString()
                            ),
                            Function.identity(),
                            new NullCtrl<AccountEditResponse>() {
                                    @Override
                                    public void success(AccountEditResponse accountEditResponse) {
                                        Local.getUser().getUser().setPassword(baseBind.userNewPassword.getText().toString());
                                        saveUser();
                                        mActivity.finish();
                                        Common.showToast("验证邮件发送成功！", true);
                                    }
                                });
                    }
                }
            }
        }
    }

    private void saveUser() {
        UserModel currentUser = Local.getUser();
        Local.saveUser(currentUser);
        UserEntity userEntity = new UserEntity();
        userEntity.setLoginTime(System.currentTimeMillis());
        userEntity.setUserID(currentUser.getUser().getId());
        userEntity.setUserGson(Shaft.sGson.toJson(currentUser));
        AppDatabase.getAppDatabase(mContext).downloadDao().insertUser(userEntity);
    }
}