@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
}

val androidCompileSdkVersion = rootProject.extra["androidCompileSdkVersion"] as Int
val androidCompileSdkVersionMinor = rootProject.extra["androidCompileSdkVersionMinor"] as Int
val androidBuildToolsVersion = rootProject.extra["androidBuildToolsVersion"] as String
val androidMinSdkVersion = rootProject.extra["androidMinSdkVersion"] as Int
val androidTargetSdkVersion = rootProject.extra["androidTargetSdkVersion"] as Int
val androidSourceCompatibility = rootProject.extra["androidSourceCompatibility"] as JavaVersion
val androidTargetCompatibility = rootProject.extra["androidTargetCompatibility"] as JavaVersion

android {
    namespace = "com.material3tim"

    signingConfigs {
        // Real signing: credentials are read from ~/.gradle/gradle.properties (TM_KEYSTORE etc.).
        // Falls back to the debug signature when no key is configured, so CI / fresh clones still build.
        create("release") {
            if (project.hasProperty("TM_KEYSTORE")) {
                storeFile = file(project.property("TM_KEYSTORE") as String)
                storePassword = project.property("TM_KEYSTORE_PASS") as String
                keyAlias = project.property("TM_KEY_ALIAS") as String
                keyPassword = project.property("TM_KEY_PASS") as String
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            vcsInfo.include = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (project.hasProperty("TM_KEYSTORE")) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    buildFeatures {
        buildConfig = true
        resValues = true
        compose = true
    }

    packaging {
        dex {
            useLegacyPackaging = true
        }
        jniLibs {
            useLegacyPackaging = true
            excludes += "lib/*/libandroidx.graphics.path.so"
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    compileSdk {
        version =
            release(androidCompileSdkVersion) {
                minorApiLevel = androidCompileSdkVersionMinor
            }
    }
    buildToolsVersion = androidBuildToolsVersion

    defaultConfig {
        minSdk = androidMinSdkVersion
        targetSdk = androidTargetSdkVersion
        // 开发者可在本机 Gradle 属性中配置 my.telegram.org 的应用凭据。
        val telegramId = providers.gradleProperty("TELEGRAM_API_ID").orNull?.toIntOrNull() ?: 0
        val telegramHash = providers.gradleProperty("TELEGRAM_API_HASH").orNull.orEmpty()
        require(telegramHash.isEmpty() || telegramHash.matches(Regex("[a-fA-F0-9]{32}")))
        buildConfigField("int", "TELEGRAM_API_ID", telegramId.toString())
        buildConfigField("String", "TELEGRAM_API_HASH", "\"$telegramHash\"")
        versionCode = 1
        versionName = "0.1.0"
        applicationId = "io.github.linx3141.material3tim"

        resValue("string", "app_name", "Material3 TIM")
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = androidSourceCompatibility
        targetCompatibility = androidTargetCompatibility
    }
}

androidComponents {
    onVariants(selector().withBuildType("release")) {
        it.packaging.resources.excludes.addAll(
            listOf("META-INF/*.version", "META-INF/LICENSE*", "META-INF/NOTICE*", "kotlin/**", "**.bin")
        )
    }
}

dependencies {
    // Xposed 模块 API（仅编译期；运行时由 LSPosed 注入）
    compileOnly("io.github.libxposed:api:102.0.0")

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)

    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.navigationevent.compose)

    // 扫一扫：应用内 CameraX 预览 + ZXing 解码（不再拉起系统相机）
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.miuix.ui)
    implementation(libs.miuix.icons)
    implementation(libs.miuix.nav)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.blur)
    implementation(libs.miuix.glass)
    implementation(libs.miuix.glass.icons)

    implementation(libs.material.kolor.utilities)
    implementation(libs.material.kolor.core)
    implementation(libs.material.kolor.material3)
    implementation("androidx.browser:browser:1.8.0")
    implementation("com.google.zxing:core:3.5.3")
    implementation("io.github.tdlib-android:core:0.1.1")
    implementation("com.airbnb.android:lottie:6.6.7")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2") // Okio 的可空注解，供 R8 解析。
    implementation("com.squareup:gifencoder:0.10.1")
    testImplementation("junit:junit:4.13.2")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
        )
    }
}
