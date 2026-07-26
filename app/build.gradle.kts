import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val localProperties = Properties().apply {
    val localPropertiesFile =
        rootProject.file("local.properties")

    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { input ->
            load(input)
        }
    }
}

fun localProperty(
    name: String
): String {
    return localProperties.getProperty(
        name,
        ""
    )
}

fun buildConfigString(
    value: String
): String {
    return "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
}

android {
    namespace = "com.example.padelboardarena"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.padelboardarena"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "ARENA_API_BASE_URL",
            buildConfigString(
                localProperty("ARENA_API_BASE_URL")
            )
        )

        buildConfigField(
            "String",
            "ARENA_COURT_ID",
            buildConfigString(
                localProperty("ARENA_COURT_ID")
            )
        )

        buildConfigField(
            "String",
            "SUPABASE_URL",
            buildConfigString(
                localProperty("SUPABASE_URL")
            )
        )

        buildConfigField(
            "String",
            "SUPABASE_PUBLISHABLE_KEY",
            buildConfigString(
                localProperty("SUPABASE_PUBLISHABLE_KEY")
            )
        )

        buildConfigField(
            "String",
            "ARENA_EMAIL",
            buildConfigString(
                localProperty("ARENA_EMAIL")
            )
        )
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
