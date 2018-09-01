package nweave.com.uberclient.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import nweave.com.uberclient.R;



public class BaseActivity extends AppCompatActivity {
    protected Context mContext = null;
    protected Activity mActivity = null;
    protected CoordinatorLayout mCoordinatorLayout = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        mActivity = this;
    }

    protected void showNetworkError() {
        //showSnackBar(getString(R.string.network_error));
    }

    protected void showSnackBar(String text) {
        showSnackBar(text, true);
    }

    protected void showSnackBar(String text, boolean isError) {
        if(mCoordinatorLayout != null) {
            Snackbar snackbar = Snackbar.make(
                    mCoordinatorLayout,
                    text,
                    Snackbar.LENGTH_LONG);
            View snackbarView = snackbar.getView();

            if(isError) {
                snackbarView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.error_color));
            } else {
                snackbarView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.primary_text));
            }

            snackbar.show();

        } else {
            Toast.makeText(mContext, text, Toast.LENGTH_LONG).show();
        }
    }

    protected void launchActivity(Class activityClass) {
        Intent i = new Intent(mContext, activityClass);
        i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isInternetConnected();
    }

    protected boolean isInternetConnected() {
        if (mContext != null) {
            ConnectivityManager connectivityMgr = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityMgr != null) {
                NetworkInfo networkInfo = connectivityMgr.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    return true;
                }
            }
        }
        return false;
    }

    protected String getFormattedTime(long millis) {
        Date date = new Date(millis);
        DateFormat formatter = new SimpleDateFormat("hh:mm:a");
        String dateFormatted = formatter.format(date);
        return dateFormatted;
    }
}
