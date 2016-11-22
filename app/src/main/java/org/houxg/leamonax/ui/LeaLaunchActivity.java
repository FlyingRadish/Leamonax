package org.houxg.leamonax.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.houxg.leamonax.model.Account;
import org.houxg.leamonax.network.ApiProvider;
import org.houxg.leamonax.service.AccountService;

public class LeaLaunchActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent;
        if (AccountService.isSignedIn()) {
            Account account = AccountService.getCurrent();
            ApiProvider.getInstance().init(account.getHost());
            intent = new Intent(this, MainActivity.class);
        } else {
            intent = new Intent(this, SignInActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
