plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kover)
}

val releaseVersionName: String =
    (project.findProperty("versionName") as String?)?.takeIf { it.isNotBlank() } ?: "1.0.0-dev"
val releaseVersionCode: Int =
    (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 1

fun readSigningValue(envName: String, propName: String): String? =
    System.getenv(envName)?.takeIf { it.isNotBlank() }
        ?: (project.findProperty(propName) as String?)?.takeIf { it.isNotBlank() }

val releaseKeystorePath = readSigningValue("RELEASE_KEYSTORE_PATH", "releaseKeystorePath")
val releaseKeystorePassword = readSigningValue("RELEASE_KEYSTORE_PASSWORD", "releaseKeystorePassword")
val releaseKeyAlias = readSigningValue("RELEASE_KEY_ALIAS", "releaseKeyAlias")
val releaseKeyPassword = readSigningValue("RELEASE_KEY_PASSWORD", "releaseKeyPassword")
val hasReleaseSigning = listOf(
    releaseKeystorePath,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { it != null }

android {
    namespace = "com.gymslut.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gymslut.app"
        minSdk = 26
        targetSdk = 36
        versionCode = releaseVersionCode
        versionName = releaseVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

kover {
    reports {
        filters {
            excludes {
                androidGeneratedClasses()
                annotatedBy("androidx.compose.runtime.Composable")
                packages(
                    "com.gymslut.app.di",
                    "com.gymslut.app.core.designsystem",
                    "hilt_aggregated_deps",
                    "dagger.hilt.internal"
                )
                classes(
                    "com.gymslut.app.GymSlutApplication",
                    "com.gymslut.app.MainActivity",
                    "*ComposableSingletons*",
                    "*$\$serializer",
                    "*.Hilt_*",
                    "*_HiltModules*",
                    "*_Factory*",
                    "*_MembersInjector*",
                    "*_GeneratedInjector",
                    "*_Impl",
                    "*_Impl\$*"
                )
            }
        }

        verify {
            rule {
                minBound(90)
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.room.testing)
}
