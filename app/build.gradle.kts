import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-android-extensions")
}

val local = Properties().apply {
    FileInputStream(rootProject.file("local.properties"))
        .reader(Charsets.UTF_8)
        .use(this::load)
}

fun getTimeNow(): String {
    val date = Date()
    val format = SimpleDateFormat("yyyyMMdd")
    return format.format(date)
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
        resValue("string", "geoip_version", getTimeNow())

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

    applicationVariants.all {
        val variant = this
        variant.outputs
                .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
                .forEach { output ->
                    output.outputFileName = output.outputFileName
                            .replace("app-", "geoip-cn-")
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
        //URL(url).openStream()
        (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            useCaches = false
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.114 Safari/537.36")
        }.inputStream.use { input ->
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


dependencies {
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${rootProject.extra["kotlin_version"]}")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")
}

fun Properties.requireProperty(key: String): String {
    return getProperty(key)
            ?: throw GradleScriptException(
                    "property \"$key\" not found in local.properties",
                    FileNotFoundException()
            )
}