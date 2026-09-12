plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.wangxilab.yixi"
    compileSdk = 36

    val releaseKeystoreFile = providers.gradleProperty("YIXI_KEYSTORE_FILE").orNull
    val releaseKeystorePassword = providers.gradleProperty("YIXI_KEYSTORE_PASSWORD").orNull
    val releaseKeyAlias = providers.gradleProperty("YIXI_KEY_ALIAS").orNull
    val releaseKeyPassword = providers.gradleProperty("YIXI_KEY_PASSWORD").orNull

    defaultConfig {
        applicationId = "com.wangxilab.yixi"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.3.0-alpha.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        create("release") {
            if (
                releaseKeystoreFile != null &&
                releaseKeystorePassword != null &&
                releaseKeyAlias != null &&
                releaseKeyPassword != null
            ) {
                storeFile = file(releaseKeystoreFile)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            if (releaseKeystoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions.jvmTarget = "17"

    buildFeatures.compose = true
    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.05.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}

val forbiddenPermissions = listOf(
    "android.permission.INTERNET",
    "android.permission.ACCESS_NETWORK_STATE",
    "android.permission.PACKAGE_USAGE_STATS",
    "android.permission.QUERY_ALL_PACKAGES",
    "android.permission.SYSTEM_ALERT_WINDOW",
    "android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE",
)

tasks.register("checkPrivacyManifest") {
    group = "verification"
    description = "Fails if the source manifest requests a forbidden permission."
    val sourceManifest = layout.projectDirectory.file("src/main/AndroidManifest.xml")
    inputs.file(sourceManifest)
    doLast {
        val manifestText = sourceManifest.asFile.readText()
        forbiddenPermissions.forEach { permission ->
            check(permission !in manifestText) { "Forbidden permission declared: $permission" }
        }
    }
}

tasks.named("check").configure { dependsOn("checkPrivacyManifest") }
