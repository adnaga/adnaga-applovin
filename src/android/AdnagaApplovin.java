package com.adnaga;

import org.apache.cordova.*;
import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.applovin.adview.AppLovinInterstitialAd;
import com.applovin.adview.AppLovinInterstitialAdDialog;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinSdk;

public class AdnagaApplovin implements IPlugin {
    private static final String LOG_TAG = "Adnaga-Applovin";

    private AppLovinInterstitialAdDialog _adDialog;
    private volatile boolean _applovinReady = false;
    private Adnaga _adnaga;

    public String getNetworkName() {
        return "applovin";
    }

    public void init(String pid, Adnaga adnaga) {
        _adnaga = adnaga;
        Log.w(LOG_TAG, "applovin ads is enabled. initing...");
        Activity act = _adnaga.getActivity();
        _adDialog = AppLovinInterstitialAd.create(AppLovinSdk.getInstance(act), act);
        MyAppLovinListener myAppLovinListener = new MyAppLovinListener();
        _adDialog.setAdDisplayListener(myAppLovinListener);
        _adDialog.setAdLoadListener(myAppLovinListener);
        _adDialog.setAdClickListener(myAppLovinListener);
        AppLovinSdk.initializeSdk(act);
        // start a polling thread to check if ads is ready to show
        Thread checkAppLovinThread = new Thread(new Runnable() { public void run() {
            while (true) {
                if (_applovinReady == false) {
                    boolean result = _adDialog.isAdReadyToDisplay();
                    Log.d(LOG_TAG, "checking applovin ready state = " + result);
                    if (result) {
                        _adnaga.sendAdsEventToJs("applovin", "READY", "");
                        _applovinReady = true;
                    }
                }
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) { e.printStackTrace(); }
            }
        }});
        checkAppLovinThread.start();
        Log.i(LOG_TAG, "admaga-applovin inited");
    }

    public void loadAds(final String pid) {
        Log.e(LOG_TAG, "admaga-applovin is autoReload, so loadAds should not be called");
    }

    public void showAds(final CallbackContext callbackContext) {
        _adnaga.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (_adDialog.isAdReadyToDisplay()) {
                    Log.i(LOG_TAG, "Trying to show applovin ads");
                    // NOTE: only after we call show, it would trigger loaded event
                    //       this is stupid, makes the logic hard to implement
                    _adDialog.show();
                } else {
                    Log.e(LOG_TAG, "applovin ads not ready, cannot show");
                    PluginResult result = new PluginResult(PluginResult.Status.ERROR, "applovin ads not ready, cannot show");
                    callbackContext.sendPluginResult(result);
                }
            }
        });
    }

    private class MyAppLovinListener implements AppLovinAdDisplayListener, AppLovinAdLoadListener, AppLovinAdClickListener {
        @Override
        public void adDisplayed(AppLovinAd appLovinAd) {
            _adnaga.sendAdsEventToJs("applovin", "START", "");
            _applovinReady = false;
        }
        @Override
        public void adHidden(AppLovinAd appLovinAd) {
            _adnaga.sendAdsEventToJs("applovin", "FINISH", "");
        }
        @Override
        public void adReceived(AppLovinAd appLovinAd) {
            // This will actually happen after ads is shown, so not useful
            Log.i(LOG_TAG, "applovin got adReceived event");
            // sendAdsEventToJs("applovin", "READY", String.valueOf(appLovinAd.getAdIdNumber()));
        }
        @Override
        public void failedToReceiveAd(int errorCode) {
            _adnaga.sendAdsEventToJs("applovin", "LOADERROR", String.valueOf(errorCode));
        }
        @Override
        public void adClicked(AppLovinAd appLovinAd) {
            _adnaga.sendAdsEventToJs("applovin", "CLICK", "");
        }
    }
}