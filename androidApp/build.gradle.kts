import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import javax.inject.Inject

abstract class PrepareLicenseAssetsTask : DefaultTask() {
    @get:InputDirectory
    abstract val inputDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @TaskAction
    fun prepare() {
        fileSystemOperations.sync {
            from(inputDirectory)
            include("NOTICE.md", "APACHE-2.0.txt")
            into(outputDirectory)
        }
    }
}

val configuredVersionCode = providers.environmentVariable("DND_VERSION_CODE")
    .orNull
    ?.toIntOrNull()
    ?.takeIf { it in 1..2_100_000_000 }
    ?: 1
val requireInternalSigning = providers.environmentVariable("DND_REQUIRE_INTERNAL_SIGNING").orNull == "true"
val requireReleaseSigning = providers.environmentVariable("DND_REQUIRE_RELEASE_SIGNING").orNull == "true"
val configuredVersionName = providers.environmentVariable("DND_RELEASE_TAG").orNull
    ?.removePrefix("v")
    ?: if (requireInternalSigning) "0.1.0-dev.$configuredVersionCode" else "0.1.0"

fun signingEnvironment(prefix: String): Map<String, String?> = mapOf(
    "path" to providers.environmentVariable("${prefix}_KEYSTORE_PATH").orNull,
    "storePassword" to providers.environmentVariable("${prefix}_KEYSTORE_PASSWORD").orNull,
    "alias" to providers.environmentVariable("${prefix}_KEY_ALIAS").orNull,
    "keyPassword" to providers.environmentVariable("${prefix}_KEY_PASSWORD").orNull,
)

val prepareLicenseAssets = tasks.register<PrepareLicenseAssetsTask>("prepareLicenseAssets") {
    inputDirectory.set(rootProject.layout.projectDirectory.dir("licenses"))
    outputDirectory.set(layout.buildDirectory.dir("generated/licenseAssets/licenses"))
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

android {
    namespace = "app.dulliesanddungeons.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.dulliesanddungeons"
        minSdk = 23
        targetSdk = 36
        versionCode = configuredVersionCode
        versionName = configuredVersionName
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "/META-INF/DEPENDENCIES",
        )
    }

    signingConfigs {
        listOf("releaseCi" to signingEnvironment("ANDROID")).forEach { (name, values) ->
            if (values.values.all { !it.isNullOrBlank() }) {
                create(name) {
                    storeFile = file(values.getValue("path")!!)
                    storePassword = values.getValue("storePassword")
                    keyAlias = values.getValue("alias")
                    keyPassword = values.getValue("keyPassword")
                    enableV1Signing = true
                    enableV2Signing = true
                    enableV3Signing = true
                    enableV4Signing = true
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("releaseCi")
            if (requireReleaseSigning && signingConfig == null) {
                error("Release signing is required but ANDROID_* signing variables are incomplete")
            }
        }
        create("internal") {
            initWith(getByName("release"))
            isDebuggable = false
            isMinifyEnabled = false
            isShrinkResources = false
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.findByName("releaseCi")
            if (requireInternalSigning && signingConfig == null) {
                error("Stable main signing is required but ANDROID_* signing variables are incomplete")
            }
        }
    }
}

androidComponents {
    onVariants(selector().all()) { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(
            prepareLicenseAssets,
            PrepareLicenseAssetsTask::outputDirectory,
        )
    }
}

dependencies {
    implementation(projects.shared)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.pdfbox.android)
    implementation(compose.runtime)
    implementation(compose.animation)
    implementation(compose.foundation)
    implementation(compose.materialIconsExtended)
    implementation(compose.material3)
    implementation(compose.ui)
    testImplementation(libs.kotlin.test)
}

tasks.register("verifyReleaseTag") {
    group = "verification"
    doLast {
        val tag = providers.environmentVariable("DND_RELEASE_TAG").orNull
            ?: error("DND_RELEASE_TAG is required")
        check(tag == "v$configuredVersionName") {
            "Release tag $tag does not match configured versionName v$configuredVersionName"
        }
    }
}
