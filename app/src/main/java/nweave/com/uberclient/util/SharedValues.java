package nweave.com.uberclient.util;

import android.content.Context;
import android.content.SharedPreferences;

    public class SharedValues {

        private static final String SHARED_PREFS = "driver_app_shared_values";


        public static String getValue(Context context, String key) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
            String value = sharedPreferences.getString(key, null);
            return value;
        }

        public static void saveValue(Context context, String key, String value) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(key, value);
            editor.apply();
        }

        public static void resetAllValues(Context context) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();
        }

}
