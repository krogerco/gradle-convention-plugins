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
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class AndroidLibraryConventionPluginTest {
    @TempDir
    private lateinit var testProjectDir: File
    private lateinit var testProjectBuilder: RootTestProjectBuilder

    @BeforeEach
    fun init() {
        testProjectBuilder = rootProject(projectDir = testProjectDir) {
            versionCatalogSpec.versions.apply {
                put("kgpAndroidxComposeBom", "\"2026-08-00\"")
                put("kgpCompileSdk", "\"37\"")
                put("kgpDokka", "\"2.0.0\"")
                put("kgpKotlin", "\"$KOTLIN_VERSION\"")
                put("kgpJdk", "\"$JDK_VERSION\"")
                put("kgpMinSdk", "\"26\"")
                put("kgpTargetSdk", "\"32\"")
            }
            addPlugin("com.kroger.gradle.root")
            addPlugin("com.kroger.gradle.android-library-conventions", apply = false)
            addSubproject("android-library") {
                addPlugin("com.kroger.gradle.android-library-conventions")
                appendBuildFile(
                    """
                    android {
                        namespace = "com.kroger.kgp.testlibrary"
                    }

                    afterEvaluate {
                        val hasKotlinAndroidPlugin = pluginManager.hasPlugin("org.jetbrains.kotlin.android")
                        val hasKotlinBaseApiPlugin = plugins.findPlugin(org.jetbrains.kotlin.gradle.plugin.KotlinBaseApiPlugin::class.java) != null
                        println("hasKotlinAndroidPlugin: ${'$'}hasKotlinAndroidPlugin")
                        println("hasKotlinBaseApiPlugin: ${'$'}hasKotlinBaseApiPlugin")
                    
                        val hasHiltPlugin = pluginManager.hasPlugin("com.google.dagger.hilt.android")
                        println("hasHiltPlugin: ${"$"}hasHiltPlugin")
                    }
                    """.trimIndent(),
                )
            }
        }
    }

    @Test
    fun `WHEN android library plugin applied THEN expected default tasks and configuration exist`() {
        testProjectBuilder.build()

        val output = gradleRunner(testProjectDir, ":android-library:tasks")
            .build()
            .output

        output.shouldContainAll(
            // default tasks
            "assemble - ",
            "lintKotlin - ",
            "koverHtmlReportDebug",
            "installDebugAndroidTest - ",
            "dokkaGenerate - ",
            // hilt configuration
            "hasHiltPlugin: false",
        )
    }

    @Test
    fun `WHEN android library plugin applied THEN targetSdk is set from kgpTargetSdk`() {
        testProjectBuilder.configureSubproject("android-library") {
            appendBuildFile(
                """
                afterEvaluate {
                    android {
                        println("testOptionsTargetSdk: ${"$"}{testOptions.targetSdk}")
                        println("lintTargetSdk: ${"$"}{lint.targetSdk}")
                        println("minSdk: ${"$"}{defaultConfig.minSdk}")
                        println("compileSdk: ${"$"}{compileSdk}")
                    }
                }
                """.trimIndent(),
            )
        }
        testProjectBuilder.build()

        val output = gradleRunner(testProjectDir, ":android-library:tasks")
            .build()
            .output

        output.shouldContainAll(
            "testOptionsTargetSdk: 32",
            "lintTargetSdk: 32",
            "minSdk: 26",
            "compileSdk: 37",
        )
    }

    @Test
    fun `WHEN android library plugin applied with hilt configuration on THEN hilt plugin applied`() {
        testProjectBuilder.versionCatalogSpec.versions["kgpDagger"] = "\"1.0.0\""
        testProjectBuilder.configureSubproject("android-library") {
            withProperties {
                put("kgp.android.autoconfigure.hilt.library", "true")
            }
        }
        testProjectBuilder.build()

        val output = gradleRunner(testProjectDir, ":android-library:tasks")
            .build()
            .output

        output.shouldContainAll(
            // hilt configuration
            "hasHiltPlugin: true",
        )
    }

    @Test
    fun `WHEN android library plugin applied and kover disabled THEN no kover tasks exist`() {
        testProjectBuilder.configureSubproject("android-library") {
            withProperties { put("kgp.plugins.autoapply.kover", "false") }
        }
        testProjectBuilder.build()

        val output = gradleRunner(testProjectDir, ":android-library:tasks")
            .build()
            .output

        output.shouldNotContain("kover")
    }

    @Test
    fun `WHEN android library plugin applied and dokka disabled THEN no dokka tasks exist`() {
        testProjectBuilder.configureSubproject("android-library") {
            withProperties { put("kgp.plugins.autoapply.dokka", "false") }
        }
        testProjectBuilder.build()

        val output = gradleRunner(testProjectDir, ":android-library:tasks")
            .build()
            .output

        output
            .substringAfter("Task :android-library:tasks")
            .shouldNotContain("dokka")
    }

    @Test
    fun `WHEN android library plugin applied and dependency guard disabled THEN no dependency guard tasks exist`() {
        testProjectBuilder.configureSubproject("android-library") {
            withProperties { put("kgp.plugins.autoapply.dependencyguard", "false") }
        }
        testProjectBuilder.build()

        val output = gradleRunner(testProjectDir, ":android-library:tasks")
            .build()
            .output

        output.shouldNotContain("dependencyGuard")
    }

    @Test
    fun `WHEN android library plugin applied without built in kotlin THEN ABI validation tasks exist`() {
        testProjectBuilder.withProperties {
            put("android.builtInKotlin", "false")
            put("android.newDsl", "false")
        }
        testProjectBuilder.build()

        val output = gradleRunner(testProjectDir, arguments = arrayOf(":android-library:tasks", "--all"))
            .build()
            .output

        output.shouldContainAll(
            "apiCheck",
            "apiDump",
        )
    }

    @Test
    fun `WHEN android library plugin applied using built in kotlin THEN ABI validation tasks exist under different names`() {
        testProjectBuilder.withProperties {
            put("android.builtInKotlin", "true")
        }
        testProjectBuilder.build()

        val output = gradleRunner(testProjectDir, arguments = arrayOf(":android-library:tasks", "--all"))
            .build()
            .output

        output.shouldContainAll(
            "releaseApiCheck",
            "releaseApiDump",
        )
    }

    @Test
    fun `GIVEN android library plugin applied WHEN version catalog missing THEN error occurs`() {
        rootProject(projectDir = testProjectDir) {
            addPlugin("com.kroger.gradle.android-library-conventions", "", true)
        }.build()

        val output = gradleRunner(testProjectDir, arguments = arrayOf("tasks"))
            .buildAndFail()
            .output

        output.shouldContain("Missing version catalog with name: libs")
    }

    @Test
    fun `WHEN android library plugin applied with built-in Kotlin enabled THEN Kotlin is available via KotlinBaseApiPlugin`() {
        testProjectBuilder.build()

        val output = gradleRunner(testProjectDir, ":android-library:tasks")
            .build()
            .output

        // With built-in Kotlin enabled, AGP applies KotlinBaseApiPlugin but not the standalone Kotlin Android plugin
        output.shouldContainAll(
            "hasKotlinBaseApiPlugin: true",
            "hasKotlinAndroidPlugin: false",
        )
    }

    @Test
    fun `WHEN android library plugin applied with built-in Kotlin disabled THEN Kotlin Android plugin is explicitly applied`() {
        testProjectBuilder.withProperties {
            put("android.builtInKotlin", "false")
            put("android.newDsl", "false")
        }
        testProjectBuilder.build()

        val output = gradleRunner(testProjectDir, ":android-library:tasks")
            .build()
            .output

        // With built-in Kotlin disabled, our plugin should explicitly apply the Kotlin Android plugin
        output.shouldContainAll(
            "hasKotlinAndroidPlugin: true",
        )
    }
}
