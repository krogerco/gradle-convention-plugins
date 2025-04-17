/**
 * MIT License
 *
 * Copyright (c) 2024 The Kroger Co. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.kroger.gradle.config

import androidx.room.gradle.RoomExtension
import androidx.room.gradle.RoomGradlePlugin
import com.android.build.api.dsl.CommonExtension
import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.plugin.KaptExtension
import java.io.File

// TODO Ideally the extensions on Project would all be on DependencyHandler instead but I could
// not figure out a way to cleanly bring the versions into scope without passing the project.
// Kotlin's context receiver support would work but that is not yet officially supported.
// Maybe there is a better way to do this altogether.

/**
 * Adds core library desugaring to the dependencies so newer Java APIs can be used on older
 * versions of Android.
 * This requires the Version Catalog to have an "androidDesugarJdkLibs" version.
 */
public fun Project.coreLibraryDesugaring() {
    val coreLibraryDesugaringVersion = KgpProperties(project).kgpVersions.kgpAndroidDesugarJdkLibs
    dependencies.add(Configurations.CORE_LIBRARY_DESUGARING, "com.android.tools:desugar_jdk_libs:$coreLibraryDesugaringVersion")
}

/**
 * Adds hilt android and hilt android compiler KSP dependencies.
 * This requires the Version Catalog to have a "dagger" version.
 * An `androidxHiltCompiler` version is required when [androidxHiltCompiler] is true
 * @param androidxHiltCompiler whether or not to also add the androidx hilt compiler
 */
public fun Project.hiltKsp(androidxHiltCompiler: Boolean = false) {
    val versions = KgpProperties(project).kgpVersions
    val daggerVersion = versions.kgpDagger

    dependencies {
        add(Configurations.IMPLEMENTATION, "javax.inject:javax.inject:1")
        add(Configurations.IMPLEMENTATION, "com.google.dagger:hilt-android:$daggerVersion")
        add(Configurations.KSP, "com.google.dagger:hilt-compiler:$daggerVersion")

        add(Configurations.TEST_IMPLEMENTATION, "com.google.dagger:hilt-android-testing:$daggerVersion")
        add(Configurations.KSP_TEST, "com.google.dagger:hilt-compiler:$daggerVersion")

        add(Configurations.ANDROID_TEST_IMPLEMENTATION, "com.google.dagger:hilt-android-testing:$daggerVersion")
        add(Configurations.KSP_ANDROID_TEST, "com.google.dagger:hilt-compiler:$daggerVersion")
        if (androidxHiltCompiler) {
            val androidxHiltCompilerVersion = versions.kgpAndroidxHiltCompiler
            add(Configurations.KSP, "androidx.hilt:hilt-compiler:$androidxHiltCompilerVersion")
        }
    }
}

/**
 * Adds hilt android and hilt android compiler KAPT dependencies.
 * This requires the Version Catalog to have a "dagger" version.
 * An `androidxHiltCompiler` version is required when [androidxHiltCompiler] is true
 * @param androidxHiltCompiler whether or not to also add the androidx hilt compiler
 */
@Deprecated(
    message = "Migrate to ksp by using hiltKsp()",
    replaceWith = ReplaceWith(
        expression = "hiltKsp()",
    ),
)
public fun Project.hilt(
    androidxHiltCompiler: Boolean = false,
) {
    val versions = KgpProperties(project).kgpVersions
    val daggerVersion = versions.kgpDagger

    extensions.configure<KaptExtension> {
        correctErrorTypes = true
    }

    dependencies {
        add(Configurations.IMPLEMENTATION, "javax.inject:javax.inject:1")
        add(Configurations.IMPLEMENTATION, "com.google.dagger:hilt-android:$daggerVersion")
        add(Configurations.KAPT, "com.google.dagger:hilt-android-compiler:$daggerVersion")

        add(Configurations.TEST_IMPLEMENTATION, "com.google.dagger:hilt-android-testing:$daggerVersion")
        add(Configurations.KAPT_TEST, "com.google.dagger:hilt-android-compiler:$daggerVersion")

        add(Configurations.ANDROID_TEST_IMPLEMENTATION, "com.google.dagger:hilt-android-testing:$daggerVersion")
        add(Configurations.KAPT_ANDROID_TEST, "com.google.dagger:hilt-android-compiler:$daggerVersion")
        if (androidxHiltCompiler) {
            val androidxHiltCompilerVersion = versions.kgpAndroidxHiltCompiler
            add(Configurations.KAPT, "androidx.hilt:hilt-compiler:$androidxHiltCompilerVersion")
        }
    }
}

/**
 * Adds dagger KSP dependencies and optionally dagger-android and dagger-android-support.
 * This requires the Version Catalog to have a "dagger" version.
 * @param daggerAndroid whether or not to also add the dagger-android dependencies
 * @param daggerAndroidSupport whether or not to also add the dagger-android-support dependencies. When true
 * this also adds dagger-android dependencies
 */
public fun Project.daggerKsp(daggerAndroid: Boolean = false, daggerAndroidSupport: Boolean = false) {
    val daggerVersion = KgpProperties(project).kgpVersions.kgpDagger

    dependencies {
        add(Configurations.IMPLEMENTATION, "com.google.dagger:dagger:$daggerVersion")
        add(Configurations.IMPLEMENTATION, "javax.inject:javax.inject:1")
        add(Configurations.KSP, "com.google.dagger:dagger-compiler:$daggerVersion")

        if (daggerAndroid || daggerAndroidSupport) {
            add(Configurations.KSP, "com.google.dagger:dagger-android-processor:$daggerVersion")
            add(Configurations.IMPLEMENTATION, "com.google.dagger:dagger-android:$daggerVersion")
            if (daggerAndroidSupport) {
                add(Configurations.IMPLEMENTATION, "com.google.dagger:dagger-android-support:$daggerVersion")
            }
        }
    }
}

/**
 * Adds dagger KAPT dependencies and optionally dagger-android and dagger-android-support.
 * This requires the Version Catalog to have a "dagger" version.
 * @param daggerAndroid whether or not to also add the dagger-android dependencies
 * @param daggerAndroidSupport whether or not to also add the dagger-android-support dependencies. When true
 * this also adds dagger-android dependencies
 */
@Deprecated(
    message = "Migrate to ksp by using daggerKsp()",
    replaceWith = ReplaceWith(
        expression = "daggerKsp()",
    ),
)
public fun Project.dagger(daggerAndroid: Boolean = false, daggerAndroidSupport: Boolean = false) {
    val daggerVersion = KgpProperties(project).kgpVersions.kgpDagger
    extensions.configure<KaptExtension> {
        correctErrorTypes = true
    }

    dependencies {
        add(Configurations.IMPLEMENTATION, "com.google.dagger:dagger:$daggerVersion")
        add(Configurations.IMPLEMENTATION, "javax.inject:javax.inject:1")
        add(Configurations.KAPT, "com.google.dagger:dagger-compiler:$daggerVersion")

        if (daggerAndroid || daggerAndroidSupport) {
            add(Configurations.KAPT, "com.google.dagger:dagger-android-processor:$daggerVersion")
            add(Configurations.IMPLEMENTATION, "com.google.dagger:dagger-android:$daggerVersion")
            if (daggerAndroidSupport) {
                add(Configurations.IMPLEMENTATION, "com.google.dagger:dagger-android-support:$daggerVersion")
            }
        }
    }
}

/**
 * Adds deep link dependencies and ksp processing.
 * This requires the Version Catalog to have a "deepLink" version.
 * @param deepLinkDocFile Where to create the deep link report. Default is {buildDir}/doc/deeplinks.json.
 */
public fun Project.deepLink(deepLinkDocFile: Provider<RegularFile>) {
    val deepLinkVersion = KgpProperties(project).kgpVersions.kgpDeepLink
    extensions.configure<KspExtension> {
        val defaultDocFile = project.layout.buildDirectory.file("doc/deeplinks.json")
        arg(
            "deepLinkDoc.output",
            deepLinkDocFile.orElse(defaultDocFile).get().asFile.absolutePath,
        )
    }

    dependencies {
        add(Configurations.IMPLEMENTATION, "native-platform:deeplink:$deepLinkVersion")
        add(Configurations.KSP, "native-platform:deeplink-processor:$deepLinkVersion")
    }
}

/**
 * Adds the org.jetbrains.kotlin.plugin.serialization plugin and optionally the json serialization dependency.
 * This requires the Version Catalog to have a "kotlinxSerialization" version.
 * @param json when true adds json as a dependency.
 */
public fun Project.kotlinxSerialization(json: Boolean = false) {
    val serializationVersion = KgpProperties(project).kgpVersions.kgpKotlinxSerialization
    pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
    if (json) {
        dependencies.add(Configurations.IMPLEMENTATION, "org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
    }
}

/**
 * Adds Moshi dependencies.
 * This requires the Version Catalog to have a "moshi" version.
 * @param codegen when true adds ksp processor to generate adapters
 * @param moshiAdapters when true adds the moshi-adapters dependency
 * @param moshiKotlinReflect when true allows use of moshi reflection
 */
public fun Project.moshi(
    codegen: Boolean = false,
    moshiAdapters: Boolean = false,
    moshiKotlinReflect: Boolean = false,
) {
    val moshiVersion = KgpProperties(project).kgpVersions.kgpMoshi
    dependencies {
        add(Configurations.IMPLEMENTATION, "com.squareup.moshi:moshi:$moshiVersion")
        if (codegen) {
            add(Configurations.KSP, "com.squareup.moshi:moshi-kotlin-codegen:$moshiVersion")
        }
        if (moshiAdapters) {
            add(Configurations.IMPLEMENTATION, "com.squareup.moshi:moshi-adapters:$moshiVersion")
        }
        if (moshiKotlinReflect) {
            add(Configurations.IMPLEMENTATION, "com.squareup.moshi:moshi-kotlin:$moshiVersion")
        }
    }
}

/**
 * Adds Room dependencies and kapt processor.
 * This requires the Version Catalog to have an "androidxRoom" version.
 * @param schemaDirectory where Room should generate schema json files for the database
 * @param commonExtension used to add [schemaDirectory] to androidTest source set if needed
 */
@Deprecated(
    message = "Migrate to ksp and use room()",
    replaceWith = ReplaceWith(
        expression = "room()",
    ),
)
public fun Project.roomKapt(
    schemaDirectory: Provider<Directory>,
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    val roomVersion = KgpProperties(project).kgpVersions.kgpRoom
    dependencies {
        add(Configurations.IMPLEMENTATION, "androidx.room:room-runtime:$roomVersion")
        add(Configurations.IMPLEMENTATION, "androidx.room:room-ktx:$roomVersion")
        add(Configurations.KAPT, "androidx.room:room-compiler:$roomVersion")
    }

    schemaDirectory.orNull?.let { schemaDir ->
        val schemaFile = schemaDir.asFile
        commonRoomConfig(schemaFile, commonExtension, roomVersion)
        commonExtension.defaultConfig {
            javaCompileOptions {
                annotationProcessorOptions {
                    compilerArgumentProviders(RoomSchemaArgProvider(schemaFile, isKsp = false))
                }
            }
        }
    }
}

private fun Project.commonRoomConfig(
    schemaFile: File,
    commonExtension: CommonExtension<*, *, *, *, *, *>,
    roomVersion: String,
) {
    if (!schemaFile.exists()) {
        schemaFile.mkdirs()
    }

    dependencies.add(Configurations.ANDROID_TEST_IMPLEMENTATION, "androidx.room:room-testing:$roomVersion")

    commonExtension.sourceSets {
        // Adds exported schema location as test app assets.
        named("androidTest").configure {
            assets.srcDir(schemaFile.path)
        }
    }
}

/**
 * Adds Room dependencies and ksp processor.
 * This requires the Version Catalog to have an "androidxRoom" version.
 * @param schemaDirectoryPath where Room should generate schema json files for the database. Default is projectDir/schemas.
 * @param commonExtension used to add [schemaDirectoryPath] to androidTest source set if needed
 */
public fun Project.room(
    schemaDirectoryPath: Provider<String?> = provider { project.layout.projectDirectory.dir("schemas").asFile.absolutePath },
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    val roomVersion = KgpProperties(project).kgpVersions.kgpRoom
    dependencies {
        add(Configurations.IMPLEMENTATION, "androidx.room:room-runtime:$roomVersion")
        add(Configurations.IMPLEMENTATION, "androidx.room:room-ktx:$roomVersion")
        add(Configurations.KSP, "androidx.room:room-compiler:$roomVersion")
        add(Configurations.ANDROID_TEST_IMPLEMENTATION, "androidx.room:room-testing:$roomVersion")
    }

    schemaDirectoryPath.orNull?.let { schemaPath ->
        pluginManager.apply(RoomGradlePlugin::class.java)
        extensions.configure<RoomExtension> {
            schemaDirectory(schemaPath)
        }

        commonExtension.sourceSets {
            // Adds exported schema location as test app assets.
            named("androidTest").configure {
                assets.srcDir(schemaPath)
            }
        }
    }
}

// region testing
/**
 * Adds the required dependencies to use JUnit 5.
 * This requires the Version Catalog to have a `junit5` version.
 */
public fun Project.junit5() {
    val junit5Version = KgpProperties(project).kgpVersions.kgpJunit5
    dependencies {
        add(Configurations.TEST_IMPLEMENTATION, platform("org.junit:junit-bom:$junit5Version"))
        add(Configurations.TEST_IMPLEMENTATION, "org.junit.jupiter:junit-jupiter")
    }
}

/**
 * Adds the required dependencies for JUnit 5. It also enables the Vintage Engine
 * and support for JUnit 4.
 * This requires the Version Catalog to have `junit5` and `junit4` versions.
 */
public fun Project.junitVintage() {
    junit5()
    val junit4Version = KgpProperties(project).kgpVersions.kgpJunit4
    dependencies {
        add(Configurations.TEST_IMPLEMENTATION, "junit:junit:$junit4Version")
        add(Configurations.TEST_RUNTIME_ONLY, "org.junit.vintage:junit-vintage-engine")
    }
}
// endregion

// region compose
/**
 * Imports the compose BOM. This requires the Version Catalog to have an `kgpAndroidxComposeBom` version.
 */
internal fun Project.composeBom() {
    val composeBomVersion = KgpProperties(project).kgpVersions.kgpComposeBom
    dependencies {
        val composeBom = platform("androidx.compose:compose-bom:$composeBomVersion")
        add(Configurations.IMPLEMENTATION, composeBom)
        add(Configurations.ANDROID_TEST_IMPLEMENTATION, composeBom)
    }
}

/**
 * Adds the required dependencies for preview support in Android Studio and testing.
 */
public fun Project.composeBasic() {
    dependencies {
        // Android Studio preview support
        add(Configurations.IMPLEMENTATION, "androidx.compose.ui:ui-tooling-preview")
        add(Configurations.DEBUG_IMPLEMENTATION, "androidx.compose.ui:ui-tooling")

        // UI tests
        add(Configurations.ANDROID_TEST_IMPLEMENTATION, "androidx.compose.ui:ui-test-junit4")
        add(Configurations.DEBUG_IMPLEMENTATION, "androidx.compose.ui:ui-test-manifest")
    }
}

/**
 * Adds compose material3 dependencies and icons (material3, material3-window-size-class, material-icons-core)
 */
public fun DependencyHandler.composeMaterial3() {
    add(Configurations.IMPLEMENTATION, "androidx.compose.material:material-icons-core")
    add(Configurations.IMPLEMENTATION, "androidx.compose.material3:material3")
    add(Configurations.IMPLEMENTATION, "androidx.compose.material3:material3-window-size-class")
}

/**
 * Adds compose material dependency (material)
 */
public fun DependencyHandler.composeMaterial() {
    add(Configurations.IMPLEMENTATION, "androidx.compose.material:material")
}

internal enum class ComposeDependencies {
    BUNDLE,
    MATERIAL,
    MATERIAL3,
    NONE,
}
// endregion

/**
 * Adds a dependency for each project that has the kover plugin applied to the kover configuration.
 * [rootProject.allprojects][Project.allprojects] is used to loop through all projects.
 * This function is currently experimental.
 *
 * @param rootProject the root project in a Gradle multi-project build.
 */
@ExperimentalKrogerPluginApi
public fun DependencyHandler.koverAllProjects(rootProject: Project) {
    rootProject.allprojects {
        pluginManager.withPlugin("org.jetbrains.kotlinx.kover") {
            add("kover", project(path))
        }
    }
}
