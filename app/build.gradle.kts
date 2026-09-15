plugins {
    id("com.android.application")
}

/**
 * 二级分类图标的资源根目录，按一级分类分组存放（一个分组 = 一个 res 根目录）。
 *
 * 为什么不用 res/drawable/<子目录>/ ？
 *   1. AGP 不采集 res/drawable/ 下的子目录，放在里面的资源不会被 R 类识别（报 Unresolved reference）。
 *   2. drawable-<组名>/ 的写法会被当成「资源限定符」，非法名称直接报 Invalid resource directory name；
 *      就算换成合法限定符（hdpi、v21 之类）也会污染密度/平台语义，不可取。
 * 所以走官方支持的「多资源根目录」：每个分组一个 res-cat-<组名>/，内含 drawable/。
 * 注意 addStaticSourceDirectory 要传 **res 根目录**（不含 drawable/），传 .../drawable 不会被采集。
 *
 * 资源名在 R 类里依然是扁平的 ic_sub_*，@drawable/xxx 的引用方式完全不变，
 * 只是磁盘上按餐饮/购物/交通…分开，翻目录时清爽很多。
 *
 * 新增分组时，这里和 .workbuddy/tools/gen_sub_icons.py 的 GROUPS 两处同步登记。
 */
val catResDirs = listOf(
    "res-cat-food",
    "res-cat-shopping",
    "res-cat-transport",
    "res-cat-housing",
    "res-cat-daily",
    "res-cat-study",
    "res-cat-social",
    "res-cat-entertainment",
    "res-cat-beauty",
    "res-cat-travel",
    "res-cat-medical",
    "res-cat-membership",
    "res-cat-telecom",
).map { "src/main/$it" }

android {
    namespace = "com.localbill"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.localbill"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// AGP 9 起 android.sourceSets 的 res.srcDirs(...) 已废弃且会静默失效，
// 追加资源根目录要用 Variant API 的 addStaticSourceDirectory。
androidComponents {
    onVariants { variant ->
        catResDirs.forEach { variant.sources.res?.addStaticSourceDirectory(it) }
    }
}

dependencies {
}
