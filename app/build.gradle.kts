import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileNotFoundException
import java.util.*
import java.net.URL

plugins {
    id("com.android.application")
}

val local = Properties().apply {
    FileInputStream(rootProject.file("local.properties")).use(this::load)
}

android {
    compileSdk = 30

    defaultConfig {
        applicationId = local.requireProperty("project.package_name")

        minSdk = 21
        targetSdk = 30

        versionCode = local.requireProperty("project.versionCode").toInt()
        versionName = local.requireProperty("project.versionName")

        resValue("string", "package_label", local.requireProperty("project.package_label"))

        val iconId = if (local.getProperty("project.package_icon_url") != null)
            "@mipmap/ic_icon"
        else
            "@android:drawable/sym_def_app_icon"

        manifestPlaceholders(
            mapOf("applicationIcon" to iconId)
        )
    }

    signingConfigs {
        maybeCreate("release").apply {
            storeFile(rootProject.file(local.requireProperty("keystore.file")))
            storePassword(local.requireProperty("keystore.password"))
            keyAlias(local.requireProperty("keystore.key_alias"))
            keyPassword(local.requireProperty("keystore.key_password"))
        }
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs["release"]
        }
    }

    sourceSets {
        named("main") {
            assets {
                srcDir(buildDir.resolve("mmdb"))
            }
            res {
                srcDir(buildDir.resolve("icon"))
            }
        }
    }
}

afterEvaluate {
    android.applicationVariants.forEach {
        it.mergeResourcesProvider.get().dependsOn(tasks["fetchIcon"])
        it.mergeAssetsProvider.get().dependsOn(tasks["fetchMMDB"])
    }
}

task("fetchMMDB") {
    val url = local.requireProperty("project.geoip_mmdb_url")
    val outputDir = buildDir.resolve("mmdb").apply { mkdirs() }

    doLast {
        URL(url).openStream().use { input ->
            FileOutputStream(outputDir.resolve("Country.mmdb")).use { output ->
                input.copyTo(output)
            }
        }
    }
}

task("fetchIcon") {
    val url = local.getProperty("project.package_icon_url") ?: return@task
    val outputDir = buildDir.resolve("icon/mipmap").apply { mkdirs() }

    require(url.endsWith(".png")) {
        throw IllegalArgumentException("icon must be .png file")
    }

    doLast {
        URL(url).openStream().use { input ->
            FileOutputStream(outputDir.resolve("ic_icon.png")).use { output ->
                input.copyTo(output)
            }
        }
    }
}

fun Properties.requireProperty(key: String): String {
    return getProperty(key)
        ?: throw GradleScriptException(
            "property \"$key\" not found in local.properties",
            FileNotFoundException()
        )
}