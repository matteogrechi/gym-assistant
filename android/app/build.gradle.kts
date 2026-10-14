plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}
val releaseCode = providers.gradleProperty("releaseVersionCode").orNull?.let {
    requireNotNull(it.toIntOrNull()?.takeIf { code -> code in 1001..2100000000 }) {
        "releaseVersionCode must be an integer between 1001 and 2100000000"
    }
}
val releaseKeystore = providers.environmentVariable("ANDROID_RELEASE_KEYSTORE").orNull
val releasePassword = providers.environmentVariable("ANDROID_RELEASE_KEYSTORE_PASSWORD").orNull
require((releaseKeystore == null) == (releasePassword == null)) {
    "Set both ANDROID_RELEASE_KEYSTORE and ANDROID_RELEASE_KEYSTORE_PASSWORD"
}
android {
    namespace = "it.matteogrechi.gymassystant"
    compileSdk = 36
    buildToolsVersion = "35.0.0"
    defaultConfig {
        applicationId = "it.matteogrechi.gymassystant"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        if (releaseKeystore != null) {
            create("production") {
                storeFile = file(releaseKeystore)
                storeType = "PKCS12"
                storePassword = releasePassword
                keyAlias = "gym-assystant"
                keyPassword = releasePassword
            }
        }
    }
    buildTypes {
        getByName("release") {
            if (releaseKeystore != null) signingConfig = signingConfigs.getByName("production")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    // Keep bundled fonts memory-mappable instead of blocking on compressed asset reads.
    androidResources { noCompress += "ttf" }
    testOptions { unitTests.isReturnDefaultValues = true }
}
androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        if (releaseCode != null) variant.outputs.forEach {
            it.versionCode.set(releaseCode)
            it.versionName.set("0.1.$releaseCode")
        }
    }
}
kotlin { jvmToolchain(17) }
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
    implementation(project(":domain"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material)
    implementation(libs.compose.ui)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.compose)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.appcompat)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.serialization)
    implementation(libs.coroutines)
    implementation(libs.work)
    implementation(libs.media.player)
    implementation(libs.media.ui)
    implementation(libs.documentfile)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test)
    androidTestImplementation(libs.android.test.runner)
    androidTestImplementation(libs.android.test.junit)
}
