/**
 * Copyright (C) 2016 Hyphenate Inc. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.ucai.superwechat.ui;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.hyphenate.EMError;
import com.hyphenate.chat.EMClient;
import com.hyphenate.exceptions.HyphenateException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.ucai.superwechat.I;
import cn.ucai.superwechat.R;
import cn.ucai.superwechat.SuperWeChatHelper;
import cn.ucai.superwechat.domain.Result;
import cn.ucai.superwechat.net.NetDao;
import cn.ucai.superwechat.net.OnCompleteListener;
import cn.ucai.superwechat.utils.CommonUtils;
import cn.ucai.superwechat.utils.L;
import cn.ucai.superwechat.utils.MD5;
import cn.ucai.superwechat.utils.ResultUtils;

/**
 * register screen
 */
public class RegisterActivity extends BaseActivity {
    private static final String TAG = RegisterActivity.class.getSimpleName();
    @BindView(R.id.username)
    EditText mUsername;
    @BindView(R.id.et_userNick)
    EditText etUserNick;
    @BindView(R.id.password)
    EditText password;
    @BindView(R.id.confirm_password)
    EditText ncofirmPassword;
    String username;
    String pwd;
    String usernick;
    ProgressDialog pd;
    @BindView(R.id.txt_title)
    TextView txtTitle;
    @BindView(R.id.img_back)
    ImageView imgBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.em_activity_register);
        ButterKnife.bind(this);
        imgBack.setVisibility(View.VISIBLE);
        txtTitle.setVisibility(View.VISIBLE);
        txtTitle.setText("注册");
    }

    @OnClick({R.id.img_back, R.id.btn_register})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.img_back:
                finish();
                break;
            case R.id.btn_register:
                register();
                break;
        }
    }


    public void register() {
        username = mUsername.getText().toString().trim();
        usernick = etUserNick.getText().toString().trim();
        pwd = password.getText().toString().trim();
        String confirm_pwd = ncofirmPassword.getText().toString().trim();
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, getResources().getString(R.string.User_name_cannot_be_empty), Toast.LENGTH_SHORT).show();
            mUsername.requestFocus();
            return;
        } else if (TextUtils.isEmpty(pwd)) {
            Toast.makeText(this, getResources().getString(R.string.Password_cannot_be_empty), Toast.LENGTH_SHORT).show();
            password.requestFocus();
            return;
        } else if (TextUtils.isEmpty(confirm_pwd)) {
            Toast.makeText(this, getResources().getString(R.string.Confirm_password_cannot_be_empty), Toast.LENGTH_SHORT).show();
            ncofirmPassword.requestFocus();
            return;
        } else if (!pwd.equals(confirm_pwd)) {
            Toast.makeText(this, getResources().getString(R.string.Two_input_password), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(pwd)) {
            pd = new ProgressDialog(this);
            pd.setMessage(getResources().getString(R.string.Is_the_registered));
            pd.show();

            registerAppSever();


        }
    }

    private void registerAppSever() {
        //注册自己的服务器账号
        NetDao.register(this, username, usernick, pwd, new OnCompleteListener<String>() {
            @Override
            public void onSuccess(String s) {
                if (s != null) {
                    Result result = ResultUtils.getResultFromJson(s, null);
                    if (result != null) {
                        if (result.isRetMsg()) {
                            //注册成功后调用环信的注册
                            registerEMServer();
                        } else {
                            pd.dismiss();
                            if (result.getRetCode() == I.MSG_REGISTER_USERNAME_EXISTS) {
                                CommonUtils.showLongToast(R.string.User_already_exists);
                            } else {
                                CommonUtils.showLongToast(R.string.Registration_failed);
                            }
                        }
                    } else {
                        pd.dismiss();
                        CommonUtils.showLongToast(R.string.Registration_failed);
                    }
                } else {
                    pd.dismiss();
                    CommonUtils.showLongToast(R.string.Registration_failed);
                }
            }

            @Override
            public void onError(String error) {
                pd.dismiss();
                CommonUtils.showLongToast(R.string.Registration_failed);
            }
        });

    }

    private void registerEMServer() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    // call method in SDK
                    EMClient.getInstance().createAccount(username, MD5.getMessageDigest(pwd));
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (!RegisterActivity.this.isFinishing())
                                pd.dismiss();
                            // save current user
                            SuperWeChatHelper.getInstance().setCurrentUserName(username);
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.Registered_successfully), Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                } catch (final HyphenateException e) {
                    //取消注册
                    unRegisterAppSever();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (!RegisterActivity.this.isFinishing())
                                pd.dismiss();
                            int errorCode = e.getErrorCode();
                            if (errorCode == EMError.NETWORK_ERROR) {
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.network_anomalies), Toast.LENGTH_SHORT).show();
                            } else if (errorCode == EMError.USER_ALREADY_EXIST) {
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.User_already_exists), Toast.LENGTH_SHORT).show();
                            } else if (errorCode == EMError.USER_AUTHENTICATION_FAILED) {
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.registration_failed_without_permission), Toast.LENGTH_SHORT).show();
                            } else if (errorCode == EMError.USER_ILLEGAL_ARGUMENT) {
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.illegal_user_name), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.Registration_failed), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }


        }).start();
    }

    private void unRegisterAppSever() {
        NetDao.unRegister(this, username, new OnCompleteListener<String>() {
            @Override
            public void onSuccess(String result) {
                L.e(TAG, "result=" + result);
            }

            @Override
            public void onError(String error) {
                L.e(TAG, "error=" + error);
            }
        });
    }

}
