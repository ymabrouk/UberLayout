package nweave.com.uberclient.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.text.TextUtils;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import nweave.com.uberclient.BuildConfig;
import nweave.com.uberclient.R;
import nweave.com.uberclient.util.Constants;
import nweave.com.uberclient.util.SharedValues;

public class SplashActivity extends BaseActivity {


    private static final int RC_SIGN_IN = 65535;
    private Handler mWaitHandler = new Handler();


    @BindView(R.id.splash_root_view)
    ConstraintLayout rootFrame;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        ButterKnife.bind(this);
        FirebaseApp.initializeApp(this);

        // sign-out
//        AuthUI.getInstance()
//                .signOut(this)
//                .addOnCompleteListener(new OnCompleteListener<Void>() {
//                    public void onComplete(@NonNull Task<Void> task) {
//                        SharedValues.resetAllValues(getApplicationContext());
//                    }
//                });

        // Adjust color the status bar in android splash screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        }


        mWaitHandler.postDelayed(new Runnable() {

            @Override
            public void run() {

                //The following code will execute after the 5 seconds.
                try {
                    checkUserAuth();
                } catch (Exception ignored) {
                    ignored.printStackTrace();
                }
            }
        }, 2000);  // Give a 2 seconds delay.


    }

    private void checkUserAuth() {

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser == null || TextUtils.isEmpty(firebaseUser.getUid())) {
            attemptUserLoginViaFirebase();
        } else {
            launchMainActivity();
            finish();
        }
    }

    private void attemptUserLoginViaFirebase() {
        if (isInternetConnected()) {

            // We used it to block some countries
            List<String> blacklistedCountries = new ArrayList<>();
            blacklistedCountries.add("+1");
            blacklistedCountries.add("gr");

            List<AuthUI.IdpConfig> providers = Arrays.asList(

                    // Email
                    new AuthUI.IdpConfig.EmailBuilder().build(),

                     // Phone
                    new AuthUI.IdpConfig.PhoneBuilder()
                           //  .setDefaultNumber("ca", "23456789")
                          //   .setBlacklistedCountries(blacklistedCountries)
                            .build(),
                    // Google
                    new AuthUI.IdpConfig.GoogleBuilder()
                            .setScopes(Arrays.asList(Scopes.GAMES))
                            .build(),

                    // Facebook
                    new AuthUI.IdpConfig.FacebookBuilder()
                            .setPermissions(Arrays.asList("user_friends"))
                            .build()

            );

            // https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md
            // Create and launch sign-in intent
            // https://gist.github.com/cutiko/9942f76504cbb67c8d04ee6632286dbc
            // https://github.com/firebase/FirebaseUI-Android/issues/229
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(providers)
                            .setTheme(R.style.LoginTheme)
                            .setIsSmartLockEnabled(true)
                           //  .setIsSmartLockEnabled(!BuildConfig.DEBUG /* credentials */, true /* hints */)
                            .setLogo(R.mipmap.ic_launcher)
                             //  PrivacyPolicyUrls
                            .setTosAndPrivacyPolicyUrls("https://superapp.example.com/terms-of-service.html", "https://superapp.example.com/terms-of-service.html")
                            .build(),
                    RC_SIGN_IN);

        } else {
           // showNetworkError();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RC_SIGN_IN:
                if (resultCode == RESULT_OK) {
                    String uid = FirebaseAuth.getInstance().getUid();
                    SharedValues.saveValue(this, Constants.USER_UID, uid);
                    launchMainActivity();
                } else {
                    // showSnackBar(getString(R.string.firebase_auth_failed));
                }
                break;
        }
    }

    @Override
    protected void setUpPolyLine() {

    }

    private void launchMainActivity() {
        rootFrame.setAlpha(0.8f);
        launchActivity(MainActivity.class);
        finish();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        //Remove all the callbacks otherwise navigation will execute even after activity is killed or closed.
        mWaitHandler.removeCallbacksAndMessages(null);
    }

}
