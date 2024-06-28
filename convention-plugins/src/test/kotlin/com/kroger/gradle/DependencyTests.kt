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
package com.kroger.gradle

import com.kroger.gradle.util.JDK_VERSION
import com.kroger.gradle.util.KOTLIN_VERSION
import com.kroger.gradle.util.RootTestProjectBuilder
import com.kroger.gradle.util.gradleRunner
import com.kroger.gradle.util.rootProject
import com.kroger.gradle.util.shouldContainAll
import com.kroger.gradle.util.shouldNotContainAny
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DependencyTests {
    @TempDir
    private lateinit var testProjectDir: File
    private lateinit var testProjectBuilder: RootTestProjectBuilder

    @BeforeEach
    fun init() {
        testProjectBuilder = rootProject(projectDir = testProjectDir) {
            versionCatalogSpec.versions.apply {
                put("kgpCompileSdk", "\"32\"")
                put("kgpAndroidxComposeCompiler", "\"1.3.2\"")
                put("kgpAndroidxComposeBom", "\"2022-12-00\"")
                put("kgpAndroidxHiltCompiler", "\"1.0.0\"")
                put("kgpAndroidxRoom", "\"2.6.1\"")
                put("kgpDeepLink", "\"3.0.0\"")
                put("kgpDokka", "\"1.8.20\"")
                put("kgpDagger", "\"1.0.0\"")
                put("kgpKotlin", "\"$KOTLIN_VERSION\"")
                put("kgpKotlinxSerialization", "\"1.0.0\"")
                put("kgpJdk", "\"$JDK_VERSION\"")
                put("kgpMinSdk", "\"26\"")
                put("kgpMoshi", "\"1.0.0\"")
            }
            addPlugin("com.kroger.gradle.root")
            addPlugin("com.kroger.gradle.published-android-library-conventions", apply = false)
            addPlugin("com.android.library", apply = false)
            addSubproject("android-library-module") {
                addPlugin("com.kroger.gradle.published-android-library-conventions")
                addPlugin("com.android.library")
                addPlugin("org.jetbrains.kotlin.kapt")
                addPlugin("com.google.devtools.ksp")
                appendBuildFile(
                    """
                    android {
                        namespace = "com.kroger.kgp.testmodule"
                    }

                    afterEvaluate {
                        listOf("implementation", "debugImplementation", "androidTestImplementation", 
                               "ksp", "kapt", "kaptTest", "kaptAndroidTest").forEach { configurationName ->
                            configurations.named(configurationName).configure {
                                println("CONFIGURATION NAME: ${"$"}name")
                                dependencies.forEach { println("\t${"$"}name(${"$"}{it.group}:${"$"}{it.name}:${"$"}{it.version})") }
                                println()
                            }
                        }
                        val hasSerializationPlugin = pluginManager.hasPlugin("org.jetbrains.kotlin.plugin.serialization")
                        println("Has serialization plugin = ${"$"}hasSerializationPlugin")
                        
                        val hasRoomPlugin = pluginManager.hasPlugin("androidx.room")
                        println("Has room plugin = ${"$"}hasRoomPlugin")
                    }
                    """.trimIndent(),
                )
                withImportStatements {
                    add("import com.kroger.gradle.config.*")
                    add("import com.android.build.gradle.LibraryExtension")
                }
            }
        }
    }

    @Test
    fun `GIVEN room called THEN expected dependencies added`() {
        testProjectBuilder.configureSubproject("android-library-module") {
            appendBuildFile("room(layout.projectDirectory.dir(provider { \"schemas\" }).map { it.asFile.absolutePath }, extensions.getByType(LibraryExtension::class))")
        }
        testProjectBuilder.build()
        val output = gradleRunner(testProjectDir, ":android-library-module:tasks")
            .build()
            .output

        output.shouldContainAll(
            "implementation(androidx.room:room-runtime:2.6.1)",
            "implementation(androidx.room:room-ktx:2.6.1)",
            "androidTestImplementation(androidx.room:room-testing:2.6.1)",
            "ksp(androidx.room:room-compiler:2.6.1)",
            "Has room plugin = true",
        )
    }

    @Test
    fun `GIVEN room called with null schemaDir THEN expected dependencies added`() {
        testProjectBuilder.configureSubproject("android-library-module") {
            appendBuildFile("room(provider { null }, extensions.getByType(LibraryExtension::class))")
        }
        testProjectBuilder.build()
        val output = gradleRunner(testProjectDir, ":android-library-module:tasks")
            .build()
            .output

        // null schemaDir means room plugin will not be applied
        output.shouldContainAll(
            "implementation(androidx.room:room-runtime:2.6.1)",
            "implementation(androidx.room:room-ktx:2.6.1)",
            "ksp(androidx.room:room-compiler:2.6.1)",
            "Has room plugin = false",
        )
    }

    @Test
    fun `GIVEN roomKapt called THEN expected dependencies added`() {
        testProjectBuilder.configureSubproject("android-library-module") {
            appendBuildFile("roomKapt(layout.projectDirectory.dir(provider { \"schemas\" }), extensions.getByType(LibraryExtension::class))")
        }
        testProjectBuilder.build()
        val output = gradleRunner(testProjectDir, ":android-library-module:tasks")
            .build()
            .output

        output.shouldContainAll(
            "implementation(androidx.room:room-runtime:2.6.1)",
            "implementation(androidx.room:room-ktx:2.6.1)",
            "kapt(androidx.room:room-compiler:2.6.1)",
            "Has room plugin = false",
        )
    }

    @Test
    fun `GIVEN moshi called THEN expected dependencies added`() {
        testProjectBuilder.configureSubproject("android-library-module") {
            appendBuildFile("moshi()")
        }
        testProjectBuilder.build()
        val output = gradleRunner(testProjectDir, ":android-library-module:tasks")
            .build()
            .output

        output.shouldContainAll(
            "implementation(com.squareup.moshi:moshi:1.0.0",
        )

        output.shouldNotContainAny(
            "implementation(com.squareup.moshi:moshi-kotlin-codegen:1.0.0",
            "implementation(com.squareup.moshi:moshi-adapters:1.0.0",
            "implementation(com.squareup.moshi:moshi-kotlin:1.0.0",
        )
    }

    @Test
    fun `GIVEN moshi called with all dependencies true THEN expected dependencies added`() {
        testProjectBuilder.configureSubproject("android-library-module") {
            appendBuildFile("moshi(codegen = true, moshiAdapters = true, moshiKotlinReflect = true)")
        }
        testProjectBuilder.build()
        val output = gradleRunner(testProjectDir, ":android-library-module:tasks")
            .build()
            .output

        output.shouldContainAll(
            "implementation(com.squareup.moshi:moshi:1.0.0)",
            "ksp(com.squareup.moshi:moshi-kotlin-codegen:1.0.0)",
            "implementation(com.squareup.moshi:moshi-adapters:1.0.0)",
            "implementation(com.squareup.moshi:moshi-kotlin:1.0.0)",
        )
    }

    @Test
    fun `GIVEN hiltKsp called THEN expected dependencies added`() {
        testProjectBuilder.configureSubproject("android-library-module") {
            appendBuildFile("hiltKsp()")
        }
        testProjectBuilder.build()
        val output = gradleRunner(testProjectDir, ":android-library-module:tasks")
            .build()
            .output

        output.shouldContainAll(
            "implementation(javax.inject:javax.inject:1)",
            "implementation(com.google.dagger:hilt-android:1.0.0)",
            "ksp(com.google.dagger:hilt-compiler:1.0.0)",
            "androidTestImplementation(com.google.dagger:hilt-android-testing:1.0.0)",
        )

        output.shouldNotContainAny(
            "ksp(androidx.hilt:hilt-compiler:1.0.0)",
        )
    }

    @Test
    fun `GIVEN hiltKsp called with all dependencies true THEN expected dependencies added`() {
        testProjectBuilder.configureSubproject("android-library-module") {
            appendBuildFile("hiltKsp(androidxHiltCompiler = true)")
        }
        testProjectBuilder.build()
        val output = gradleRunner(testProjectDir, ":android-library-module:tasks")
            .build()
            .output

        output.shouldContainAll(
            "implementation(javax.inject:javax.inject:1)",
            "implementation(com.google.dagger:hilt-android:1.0.0)",
            "ksp(com.google.dagger:hilt-compiler:1.0.0)",
            "androidTestImplementation(com.google.dagger:hilt-android-testing:1.0.0)",
            "ksp(androidx.hilt:hilt-compiler:1.0.0)",
        )
    }

    @Test
    fun `GIVEN hilt called THEN expected dependencies added`() {
        testProjectBuilder.configureSubproject("android-library-module") {
            appendBuildFile("hilt()")
        }
        testProjectBuilder.build()
        val output = gradleRunner(testProjectDir, ":android-library-module:tasks")
            .build()
            .output

        output.shouldContainAll(
            "implementation(javax.inject:javax.inject:1)",
            "implementation(com.google.dagger:hilt-android:1.0.0)",
            "kapt(com.google.dagger:hilt-android-compiler:1.0.0)",
            "androidTestImplementation(com.google.dagger:hilt-android-testing:1.0.0)",
        )

        output.shouldNotContainAny(
            "kapt(androidx.hilt:hilt-compiler:1.0.0)",
        )
    }

    @Test
    fun `GIVEN hilt called with all dependencies true THEN expected dependencies added`() {
        testProjectBuilder.configureSubproject("android-library-module") {
            appendBuildFile("hilt(androidxHiltCompiler = true)")
        }
        testProjectBuilder.build()
        val output = gradleRunner(testProjectDir, ":android-library-module:tasks")
            .build()
            .output

        output.shouldContainAll(
            "implementation(javax.inject:javax.inject:1)",
            "implementation(com.google.dagger:hilt-android:1.0.0)",
            "kapt(com.google.dagger:hilt-android-compiler:1.0.0)",
            "androidTestImplementation(com.google.dagger:hilt-android-testing:1.0.0)",
            "kapt(androidx.hilt:hilt-compiler:1.0.0)",
        )
    }

    @Test
    fun `GIVEN daggerKsp called THEN expected dependencies added`() {
        testProjectBuilder.configureSubproject("android-library-module") {
            appendBuildFile("daggerKsp()")
        }
        testProjectBuilder.build()
        val output = gradleRunner(testProjectDir, ":android-library-module:tasks")
            .build()
            .output

        output.shouldContainAll(
            "implementation(com.google.dagger:dagger:1.0.0)",
            "implementation(javax.inject:javax.inject:1)",
            "ksp(com.google.dagger:dagger-compiler:1.0.0)",
        )

        output.shouldNotContainAny(
            "ksp(com.google.dagger:dagger-android-processor:1.0.0)",
            "implementation(com.google.dagger:dagger-android:1.0.0)",
            "implementation(com.google.dagger:dagger-android-support:1.0.0)",
        )
    }

    @Test
    fun `GIVEN daggerKsp called with all dependencies true THEN expected dependencies added`() {
        testProjectBuilder.configureSubproject("android-library-module") {
            appendBuildFile("daggerKsp(daggerAndroid = true, daggerAndroidSupport = true)")
        }
        testProjectBuilder.build()
        val output = gradleRunner(testProjectDir, ":android-library-module:tasks")
            .build()
            .output

        output.shouldContainAll(
            "implementation(com.google.dagger:dagger:1.0.0)",
            "implementation(javax.inject:javax.inject:1)",
            "ksp(com.google.dagger:dagger-compiler:1.0.0)",
            "ksp(com.google.dagger:dagger-android-processor:1.0.0)",
            "implementation(com.google.dagger:dagger-android:1.0.0)",
            "implementation(com.google.dagger:dagger-android-support:1.0.0)",
        )
    }

    @Test
    fun `GIVEN dagger called THEN expected dependencies added`() {
        testProjectBuilder.configureSubproject("android-library-module") {
            appendBuildFile("dagger()")
        }
        testProjectBuilder.build()
        val output = gradleRunner(testProjectDir, ":android-library-module:tasks")
            .build()
            .output

        output.shouldContainAll(
            "implementation(com.google.dagger:dagger:1.0.0)",
            "implementation(javax.inject:javax.inject:1)",
            "kapt(com.google.dagger:dagger-compiler:1.0.0)",
        )

        output.shouldNotContainAny(
            "kapt(com.google.dagger:dagger-android-processor:1.0.0)",
            "implementation(com.google.dagger:dagger-android:1.0.0)",
            "implementation(com.google.dagger:dagger-android-support:1.0.0)",
        )
    }

    @Test
    fun `GIVEN dagger called with all dependencies true THEN expected dependencies added`() {
        testProjectBuilder.configureSubproject("android-library-module") {
            appendBuildFile("dagger(daggerAndroid = true, daggerAndroidSupport = true)")
        }
        testProjectBuilder.build()
        val output = gradleRunner(testProjectDir, ":android-library-module:tasks")
            .build()
            .output

        output.shouldContainAll(
            "implementation(com.google.dagger:dagger:1.0.0)",
            "implementation(javax.inject:javax.inject:1)",
            "kapt(com.google.dagger:dagger-compiler:1.0.0)",
            "kapt(com.google.dagger:dagger-android-processor:1.0.0)",
            "implementation(com.google.dagger:dagger-android:1.0.0)",
            "implementation(com.google.dagger:dagger-android-support:1.0.0)",
        )
    }

    @Test
    fun `GIVEN deepLink called THEN expected dependencies added`() {
        testProjectBuilder.configureSubproject("android-library-module") {
            appendBuildFile("deepLink(objects.fileProperty())")
        }
        testProjectBuilder.build()
        val output = gradleRunner(testProjectDir, ":android-library-module:tasks")
            .build()
            .output

        output.shouldContainAll(
            "implementation(native-platform:deeplink:3.0.0",
            "ksp(native-platform:deeplink-processor:3.0.0",
        )
    }

    @Test
    fun `GIVEN kotlinxSerialization called THEN expected dependencies added`() {
        testProjectBuilder.configureSubproject("android-library-module") {
            appendBuildFile("kotlinxSerialization(json = true)")
        }
        testProjectBuilder.build()
        val output = gradleRunner(testProjectDir, ":android-library-module:tasks")
            .build()
            .output

        output.shouldContainAll(
            "implementation(org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.0)",
            "Has serialization plugin = true",
        )
    }
}
