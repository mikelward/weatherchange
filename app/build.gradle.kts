plugins {
    id("com.android.application")
}

android {
    namespace = "com.mikelward.weatherchange"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mikelward.weatherchange"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }
}
