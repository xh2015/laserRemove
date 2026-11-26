package com.topsky.laserremove.ui;

import android.content.Intent;
import android.view.LayoutInflater;

import com.blankj.utilcode.util.ToastUtils;
import com.topsky.laserremove.base.BaseActivity;
import com.topsky.laserremove.databinding.ActivityLoginBinding;

public class LoginActivity extends BaseActivity<ActivityLoginBinding> {
    private String name = "admin";
    private String pwd = "admin";


    @Override
    protected ActivityLoginBinding initViewBinding(LayoutInflater inflater) {
        return ActivityLoginBinding.inflate(inflater);
    }

    @Override
    protected void init() {
        binding.btnLogin.setOnClickListener(v -> {
            String account = binding.edAccount.getText().toString();
            String pwd = binding.edPwd.getText().toString();
            if (account.isEmpty() || pwd.isEmpty()) {
                ToastUtils.showLong("账号或密码为空");
                return;
            }
            if (check(account, pwd)) {
                login();
            } else {
                ToastUtils.showLong("账号或者密码错误,请校验后重试");
            }
        });
    }

    private boolean check(String account, String pwd) {
        if (account.equals(name) && pwd.equals(this.pwd)) {
            return true;
        } else {
            return false;
        }
    }

    private void login() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}