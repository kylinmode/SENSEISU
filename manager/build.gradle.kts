import com.android.build.api.dsl.ApplicationDefaultConfig
import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.api.AndroidBasePlugin
import java.io.ByteArrayOutputStream

plugins {
    alias(libs.plugins.agp.app) apply false
    alias(libs.plugins.agp.lib) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.lsplugin.cmaker)
}

cmaker {
    default {
        arguments.addAll(
            arrayOf(
                "-DANDROID_STL=none",
            )
        )
        abiFilters("arm64-v8a", "x86_64", "riscv64")
    }
    buildTypes {
        if (it.name == "release") {
            arguments += "-DDEBUG_SYMBOLS_PATH=${layout.buildDirectory.asFile.get().absolutePath}/symbols"
        }
    }
}

val androidMinSdkVersion = 26
val androidTargetSdkVersion = 36
val androidCompileSdkVersion = 36
val androidBuildToolsVersion = "37.0.0"
extra["androidBuildToolsVersion"] = androidBuildToolsVersion
val androidCompileNdkVersion by extra(libs.versions.ndk.get())
val androidSourceCompatibility = JavaVersion.VERSION_21
val androidTargetCompatibility = JavaVersion.VERSION_21
val managerVersionCode by extra(getVersionCode())
val managerVersionName by extra(getVersionName())

fun getGitCommitCount(): Int {
    val process = Runtime.getRuntime().exec(arrayOf("git", "rev-list", "--count", "HEAD"))
    return process.inputStream.bufferedReader().use { it.readText().trim().toInt() }
}

fun getGitDescribe(): String {
    val process = Runtime.getRuntime().exec(arrayOf("git", "describe", "--tags", "--always"))
    return process.inputStream.bufferedReader().use { it.readText().trim() }
}

fun getVersionCode(): Int {
    // SenseiSU：环境变量 > 根目录 version.txt > 默认值
    System.getenv("SSU_VERSION_CODE")?.toIntOrNull()?.let { return it }
    val vf = rootProject.file("../version.txt")
    if (vf.exists()) vf.readText().trim().toIntOrNull()?.let { return it }
    return 10001
}

fun getVersionName(): String {
    // 版本名单独维护：version-name.txt（如 V1.0.1），与 versionCode 解耦
    System.getenv("SSU_VERSION_NAME")?.takeIf { it.isNotBlank() }?.let { return it }
    val nf = rootProject.file("../version-name.txt")
    if (nf.exists()) nf.readText().trim().takeIf { it.isNotBlank() }?.let { return it }
    val vf = rootProject.file("../version.txt")
    if (vf.exists()) vf.readText().trim().takeIf { it.isNotBlank() }?.let { return it }
    return "V1.0.0"
}

subprojects {
    plugins.withType(AndroidBasePlugin::class.java) {
        extensions.configure(CommonExtension::class.java) {
            compileSdk = androidCompileSdkVersion
            ndkVersion = androidCompileNdkVersion

            defaultConfig {
                minSdk = androidMinSdkVersion
                if (this is ApplicationDefaultConfig) {
                    targetSdk = androidTargetSdkVersion
                    versionCode = managerVersionCode
                    versionName = managerVersionName
                }
                ndk {
                    abiFilters += listOf("arm64-v8a", "x86_64", "riscv64")
                }
            }

            lint {
                abortOnError = true
                checkReleaseBuilds = false
            }

            compileOptions {
                sourceCompatibility = androidSourceCompatibility
                targetCompatibility = androidTargetCompatibility
            }
        }
    }
}
