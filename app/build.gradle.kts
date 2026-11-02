plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.downloadanything"
    // Compose 1.12(BOM 2026.09)가 compileSdk 37 이상을 요구한다. targetSdk는 36 유지.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.downloadanything"
        // MediaStore RELATIVE_PATH(API 29)로 저장해 구버전 저장 권한 분기를 없앤다.
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        // youtubedl-android는 ABI마다 Python·ffmpeg를 넣는다. 개인용이라 arm64만 빌드한다.
        ndk { abiFilters += "arm64-v8a" }
    }

    buildTypes {
        debug {
            // PC 에뮬레이터(x86_64)에서 실제 다운로드를 확인하기 위해 디버그 빌드에만 추가한다.
            ndk { abiFilters += "x86_64" }
        }
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // youtubedl-android가 실행 파일을 lib 폴더에서 꺼내 쓰므로 압축 해제 상태로 설치해야 한다.
    packaging {
        jniLibs { useLegacyPackaging = true }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.work.runtime)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.youtubedl.library)
    implementation(libs.youtubedl.ffmpeg)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
