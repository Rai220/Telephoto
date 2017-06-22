package com.rai220.securityalarmbot;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.rai220.securityalarmbot.prefs.PrefsController;
import com.rai220.securityalarmbot.utils.L;

/**
 * Created by rai220 on 10/29/16.
 */
public class BuyActivity extends Activity implements BillingProcessor.IBillingHandler {
    public static final String ANSWER_STEP_BUY = "BUY";

    private static final String SKU_PRO = "pro";
    //private static final String SKU_PRO = "android.test.purchased";

    private static final String PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhq+vpXQHkLy4LezWN+H+r3ZU9IOTIfLbU1no2i6JmCsmPJYXCfOQrIgzzsPg4XwIo8Joln8u7ptsSbpp6IBAJDohCWsoG8OXs2L/sX3NF02dZ8zNCNHbIyWaThcdIiS3kNbWXanO4MxSjJyNXrOi8HuwKOUTViv3LWcNMHCi3+odWWsAYIh/OVAkR3XFZffzoN4JMmnHUjuVb24WZfoPzefMPWjhAwzuPX4W/i2YgXc3IsiIbpGHDO7O6a5zT68dw0+EXXT6neJJcMKRKHNtdYAMSqblM8sz992Or435sDW6EJVQA6wRIFt+F3UWM+xy+5qgRM7fpAm3acQ9RNcd/wIDAQAB";

    private BillingProcessor bp = null;
    private ProgressDialog progressDialog = null;
    private volatile Thread checkingThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.buy_pro_activity);

        Answers.getInstance().logCustom(new CustomEvent(ANSWER_STEP_BUY).putCustomAttribute("step", "Activity open"));

        progressDialog = ProgressDialog.show(this, "", getString(R.string.loading_payments), true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bp = new BillingProcessor(this, PUBLIC_KEY, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (checkingThread != null) {
                checkingThread.interrupt();
                checkingThread.join();
                checkingThread = null;
            }
        } catch (InterruptedException ignore) {
        }
        if (bp != null) {
            bp.release();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Answers.getInstance().logCustom(new CustomEvent(ANSWER_STEP_BUY).putCustomAttribute("step", "Cancel - back"));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void onBuyCancel(View view) {
        Answers.getInstance().logCustom(new CustomEvent(ANSWER_STEP_BUY).putCustomAttribute("step", "Cancel"));
        finish();
    }

    public void onBuyClick(View view) {
        try {
            Answers.getInstance().logCustom(new CustomEvent(ANSWER_STEP_BUY).putCustomAttribute("step", "Buy!"));
            boolean purchaseResult = bp.purchase(this, SKU_PRO);
            L.i(purchaseResult);
        } catch (Throwable ex) {
            L.e(ex);
        }
    }

    public void onRatingClick(View view) {
        Answers.getInstance().logCustom(new CustomEvent(ANSWER_STEP_BUY).putCustomAttribute("step", "Rating"));
        final String appPackageName = getPackageName();
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }

    private void afterPayment() {
        PrefsController.instance.makePro();
        Answers.getInstance().logCustom(new CustomEvent(ANSWER_STEP_BUY).putCustomAttribute("step", "Go Pro!"));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(BuyActivity.this)
                        .setTitle(R.string.warning)
                        .setMessage(R.string.buy_thank_you_for_bought)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                BuyActivity.this.finish();
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });
    }

    private void hideLoadingDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
            checkAlreadyBought();
        }
    }

    private void checkAlreadyBought() {
        if (bp.listOwnedProducts().contains(SKU_PRO)) {
            afterPayment();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!bp.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onProductPurchased(String productId, TransactionDetails details) {
        L.i("onProductPurchased");
        if (productId.equals(SKU_PRO)) {
            afterPayment();
        }
    }

    @Override
    public void onPurchaseHistoryRestored() {
        checkAlreadyBought();
    }

    @Override
    public void onBillingError(int errorCode, Throwable error) {
        L.e("onBillingError " + errorCode);
        hideLoadingDialog();
    }

    @Override
    public void onBillingInitialized() {
        hideLoadingDialog();
        //checkAlreadyBought();
    }
}