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
import com.kroger.gradle.util.TestProjectBuilder
import com.kroger.gradle.util.gradleRunner
import com.kroger.gradle.util.rootProject
import com.kroger.gradle.util.shouldContainAll
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class PublishedAndroidLibraryConventionPluginTest {
    @TempDir
    private lateinit var testProjectDir: File
    private lateinit var testProjectBuilder: RootTestProjectBuilder

    @BeforeEach
    fun init() {
        testProjectBuilder = rootProject(projectDir = testProjectDir) {
            versionCatalogSpec.versions.apply {
                put("kgpAndroidDesugarJdkLibs", "\"1.0.0\"")
                put("kgpAndroidxComposeBom", "\"2022-12-00\"")
                put("kgpCompileSdk", "\"32\"")
                put("kgpDokka", "\"1.8.20\"")
                put("kgpKotlin", "\"$KOTLIN_VERSION\"")
                put("kgpJdk", "\"$JDK_VERSION\"")
                put("kgpMinSdk", "\"26\"")
            }
            addPlugin("com.kroger.gradle.root")
            addPlugin("com.kroger.gradle.published-android-library-conventions", apply = false)
            addSubproject("android-library-module") {
                addPlugin("com.kroger.gradle.published-android-library-conventions")
                appendBuildFile(
                    """
                    android {
                        namespace = "com.kroger.kgp.testmodule"
                    }
     
                    afterEvaluate {
                        val hasHiltPlugin = pluginManager.hasPlugin("com.google.dagger.hilt.android")
                        val hasKaptPlugin = pluginManager.hasPlugin("org.jetbrains.kotlin.kapt")
                        println("hasHiltPlugin: ${"$"}hasHiltPlugin")
                        println("hasKaptPlugin: ${"$"}hasKaptPlugin")
                    }
                    """.trimIndent(),
                )
            }
        }
    }

    @Test
    fun `WHEN published android library plugin applied with repo url property set THEN expected default tasks and configuration exist`() {
        testProjectBuilder.configureSubproject("android-library-module") {
            withProperties {
                put("kgp.repository.url", "https://artifactory")
            }
        }
        testProjectBuilder.build()

        val output = gradleRunner(testProjectDir, ":android-library-module:tasks")
            .build()
            .output

        output.shouldContainAll(
            // default tasks
            "assemble - ",
            "dokkaHtml - ",
            "installDebugAndroidTest - ",
            "koverHtmlReportDebug",
            "lintKotlin - ",
            "publishMavenPublicationToArtifactoryRepository - ",
            // hilt configuration
            "hasHiltPlugin: false",
            "hasKaptPlugin: false",
        )
    }

    @Test
    fun `WHEN published android library plugin applied with hilt configuration on THEN kapt and hilt plugins applied`() {
        testProjectBuilder.versionCatalogSpec.versions["kgpDagger"] = "\"1.0.0\""
        testProjectBuilder.configureSubproject("android-library-module") {
            withProperties {
                put("kgp.android.autoconfigure.hilt.library", "true")
                put("kgp.android.autoconfigure.corelibrarydesugaring", "true")
            }
        }
        testProjectBuilder.build()

        val output = gradleRunner(testProjectDir, ":android-library-module:tasks")
            .build()
            .output

        output.shouldContainAll(
            // hilt configuration
            "hasHiltPlugin: true",
            "hasKaptPlugin: true",
        )
    }

    @Test
    fun `WHEN published android library plugin applied and kover disabled THEN no kover tasks exist`() {
        testProjectBuilder.configureSubproject("android-library-module") {
            withProperties { put("kgp.plugins.autoapply.kover", "false") }
        }
        testProjectBuilder.build()

        val output = gradleRunner(testProjectDir, ":android-library-module:tasks")
            .build()
            .output

        output.shouldNotContain("kover")
    }

    @Test
    fun `WHEN published android library plugin applied and dokka disabled THEN no dokka tasks exist`() {
        testProjectBuilder.configureSubproject("android-library-module") {
            withProperties { put("kgp.plugins.autoapply.dokka", "false") }
        }
        testProjectBuilder.build()

        val output = gradleRunner(testProjectDir, ":android-library-module:tasks")
            .build()
            .output

        output
            .substringAfter("Task :android-library-module:tasks") // in the current version of dokka there are warnings printed
            .shouldNotContain("dokka")
    }

    @Test
    fun `GIVEN published android library plugin applied WHEN version catalog missing THEN error occurs`() {
        rootProject(projectDir = testProjectDir) {
            addPlugin("com.kroger.gradle.android-library-conventions", "", true)
        }.build()

        val output = gradleRunner(testProjectDir, arguments = arrayOf("tasks"))
            .buildAndFail()
            .output

        output
            .shouldContain("Missing version catalog with name: libs")
    }

    @Test
    fun `WHEN published android library plugin applied with no java or kotlin overrides set THEN expected defaults used`() {
        testProjectBuilder.configureSubproject("android-library-module") {
            printJavaAndKotlinVersions()
        }
        testProjectBuilder.build()

        val output = gradleRunner(testProjectDir, ":android-library-module:tasks")
            .build()
            .output

        output.shouldContainAll(
            "Kotlin API Version: null",
            "Kotlin Language Version: null",
            "Java Source Compatibility: $JDK_VERSION",
            "Java Target Compatibility: $JDK_VERSION",
        )
    }

    @Test
    fun `WHEN published android library plugin applied with java and kotlin overrides set THEN override values are used`() {
        val jvmTarget = "11"
        val kotlinVersion = "1.9"
        testProjectBuilder.versionCatalogSpec.versions.apply {
            put("kgpJvmTarget", "\"$jvmTarget\"")
            put("kgpKotlinApiVersion", "\"$kotlinVersion\"")
            put("kgpKotlinLanguageVersion", "\"$kotlinVersion\"")
        }
        testProjectBuilder.configureSubproject("android-library-module") {
            printJavaAndKotlinVersions()
        }
        testProjectBuilder.build()

        val output = gradleRunner(testProjectDir, ":android-library-module:tasks")
            .build()
            .output

        output.shouldContainAll(
            "Kotlin API Version: $kotlinVersion",
            "Kotlin Language Version: $kotlinVersion",
            "Java Source Compatibility: $jvmTarget",
            "Java Target Compatibility: $jvmTarget",
        )
    }

    private fun TestProjectBuilder.printJavaAndKotlinVersions() {
        appendBuildFile(
            """
                afterEvaluate {
                    kotlin {
                        compilerOptions {
                            println("Kotlin API Version: ${"$"}{apiVersion.orNull?.version}")
                            println("Kotlin Language Version: ${"$"}{languageVersion.orNull?.version}")
                        }
                    }
                    android {
                        compileOptions {
                            println("Java Source Compatibility: ${"$"}{sourceCompatibility}")
                            println("Java Target Compatibility: ${"$"}{targetCompatibility}")
                        }
                    }
                }    
            """.trimIndent(),
        )
    }
}
