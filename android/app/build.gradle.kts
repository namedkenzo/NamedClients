plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "dev.namedkenzo.namedp"
    compileSdk = 35
    defaultConfig {
        applicationId = "dev.namedkenzo.namedp"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
    }
    buildFeatures { buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    packaging { jniLibs { useLegacyPackaging = true; keepDebugSymbols += "**/*.so" } }
    signingConfigs.getByName("debug") {
        System.getenv("NAMEDP_SIGNING_STORE")?.let {
            storeFile = file(it)
            storeType = "PKCS12"
            storePassword = System.getenv("NAMEDP_STORE_PASSWORD")
            keyAlias = System.getenv("NAMEDP_KEY_ALIAS")
            keyPassword = System.getenv("NAMEDP_KEY_PASSWORD")
        }
    }
    // CI supplies the owner identity to both variants so instrumentation can
    // verify an in-place debug-fixture -> non-debuggable release upgrade.
    buildTypes { release { isMinifyEnabled = false; isDebuggable = false; signingConfig = signingConfigs.getByName("debug") } }
}
dependencies {
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
