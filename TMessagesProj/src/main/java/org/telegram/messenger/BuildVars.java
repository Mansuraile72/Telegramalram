/*
 * This is the source code of Telegram for Android v. 7.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2020.
 */

package org.telegram.messenger;

import android.os.Build;

public class BuildVars {

    // --- आपकी व्यक्तिगत Keys ---
    public static int API_ID = 27041199;
    public static String API_HASH = "7f65bd72b0b483a9eddcfbc487b72420";

    // --- नए एरर्स को ठीक करने के लिए (APP_ID और APP_HASH) ---
    public static int APP_ID = 27041199;
    public static String APP_HASH = "7f65bd72b0b483a9eddcfbc487b72420";

    // --- बिल्ड के लिए ज़रूरी अन्य वैरिएबल ---
    public static boolean DEBUG_VERSION = true;
    public static boolean LOGS_ENABLED = true;
    public static boolean DEBUG_PRIVATE_VERSION = true;
    public static boolean USE_CLOUD_STRINGS = true;
    public static boolean CHECK_UPDATES = true;
    public static boolean NO_SCOPED_STORAGE = Build.VERSION.SDK_INT <= 29;
    public static String BUILD_VERSION_STRING = "10.8.2"; // सैंपल वर्ज़न
    public static boolean IS_BILLING_UNAVAILABLE = true;

    // --- इन वैरिएबल्स को आप खाली छोड़ सकते हैं ---
    public static String HOCKEY_APP_HASH = "";
    public static String HOCKEY_APP_HASH_DEBUG = "";
    public static String BING_SEARCH_KEY = "";
    public static String FOURSQUARE_API_KEY = "";
    public static String FOURSQUARE_API_ID = "";
    public static String FOURSQUARE_API_SECRET = "";
    public static String GCM_SENDER_ID = "762359664522";
    public static String GOOGLE_MAPS_API_KEY = "";
    public static String GOOGLE_API_KEY = "";
    public static String HOCKEY_APP_ID = "";
    public static String APPCENTER_HASH = "";
    public static String APPCENTER_HASH_DEBUG = "";
    public static String SMS_HASH = "";
    public static String TWITTER_TAG = "#telegram";
    public static String TME_URL = "https://t.me/";
    public static String GOOGLE_AUTH_CLIENT_ID = "some_default_client_id";

    // --- इस बार के एरर को ठीक करने के लिए जोड़ी गई लाइनें ---
    public static String HUAWEI_STORE_URL = "https://appgallery.huawei.com/app/C101184875";
    public static String PLAYSTORE_APP_URL = "https://play.google.com/store/apps/details?id=org.telegram.messenger";
    public static String SAFETYNET_KEY = ""; // इसे खाली छोड़ सकते हैं

    // --- पुराने वैरिएबल (अब PLAYSTORE_APP_URL द्वारा प्रतिस्थापित) ---
    public static String PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=org.telegram.messenger";
    public static String PLAY_STORE_URL_BETA = "https://play.google.com/store/apps/details?id=org.telegram.messenger.beta";
    public static String SUPPORT_URL = "https://telegram.org/support";


    // --- एरर को ठीक करने के लिए ज़रूरी मेथड्स ---
    public static String getSmsHash() {
        return "your_sms_hash"; // एक सैंपल वैल्यू
    }

    public static boolean useInvoiceBilling() {
        return false;
    }

    public static boolean isBetaApp() {
        return false;
    }

    public static boolean isHuaweiStoreApp() {
        return false;
    }

    // --- प्रोजेक्ट के लिए अन्य ज़रूरी वैरिएबल ---
    public static boolean isStandalone = false;
    public static boolean isBeta = false;
    public static boolean isInternal = false;
    public static boolean isFoss = false;
}