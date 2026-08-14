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
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class AndroidApplicationConventionPluginTest {
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
                put("kgpDagger", "\"2.59.2\"")
                put("kgpTargetSdk", "\"37\"")
            }
            addPlugin("com.kroger.gradle.root")
            addPlugin("com.kroger.gradle.android-application-conventions", apply = false)
            addSubproject("android-app") {
                addPlugin("com.kroger.gradle.android-application-conventions")
                addPlugin("org.jetbrains.kotlin.plugin.compose")
                appendBuildFile(
                    $$"""
                    android {
                        namespace = "com.kroger.kgp.testapp"
                    }
                    
                    afterEvaluate {
                        val hasHiltPlugin = pluginManager.hasPlugin("com.google.dagger.hilt.android")
                        val hasKspPlugin = pluginManager.hasPlugin("com.google.devtools.ksp")
                        val hasKotlinAndroidPlugin = pluginManager.hasPlugin("org.jetbrains.kotlin.android")
                        val hasKotlinBaseApiPlugin = plugins.findPlugin(org.jetbrains.kotlin.gradle.plugin.KotlinBaseApiPlugin::class.java) != null
                        println("hasHiltPlugin: $hasHiltPlugin")
                        println("hasKspPlugin: $hasKspPlugin")
                        println("hasKotlinAndroidPlugin: $hasKotlinAndroidPlugin")
                        println("hasKotlinBaseApiPlugin: $hasKotlinBaseApiPlugin")
                    }
                    """.trimIndent(),
                )
            }
        }
    }

    @Test
    fun `WHEN android application plugin applied THEN expected default tasks and configuration exist`() {
        testProjectBuilder.build()

        val output = gradleRunner(testProjectDir, ":android-app:tasks")
            .build()
            .output

        output.shouldContainAll(
            // default tasks
            "installDebug - ",
            "lintKotlin - ",
            "koverHtmlReportDebug",
            // hilt configuration
            "hasHiltPlugin: true",
            "hasKspPlugin: true",
        )
    }

    @Test
    fun `WHEN android application plugin applied with hilt configuration off THEN kapt and hilt plugins not applied`() {
        testProjectBuilder.configureSubproject("android-app") {
            withProperties { put("kgp.android.autoconfigure.hilt.application", "false") }
        }
        testProjectBuilder.build()

        val output = gradleRunner(testProjectDir, ":android-app:tasks")
            .build()
            .output

        output.shouldContainAll(
            // hilt configuration
            "hasHiltPlugin: false",
            "hasKspPlugin: false",
        )
    }

    @Test
    fun `WHEN android application plugin applied and kover disabled THEN no kover tasks exist`() {
        testProjectBuilder.configureSubproject("android-app") {
            withProperties { put("kgp.plugins.autoapply.kover", "false") }
        }
        testProjectBuilder.build()

        val output = gradleRunner(testProjectDir, ":android-app:tasks")
            .build()
            .output

        output.shouldNotContain("kover")
    }

    @Test
    fun `WHEN android application plugin applied with built-in Kotlin enabled THEN Kotlin is available via KotlinBaseApiPlugin`() {
        // Built-in Kotlin is enabled by default in AGP 9
        testProjectBuilder.build()

        val output = gradleRunner(testProjectDir, ":android-app:tasks")
            .build()
            .output

        // With built-in Kotlin enabled, AGP applies KotlinBaseApiPlugin but not the standalone Kotlin Android plugin
        output.shouldContainAll(
            "hasKotlinBaseApiPlugin: true",
            "hasKotlinAndroidPlugin: false",
        )
    }

    @Test
    fun `WHEN android application plugin applied with built-in Kotlin disabled THEN Kotlin Android plugin is explicitly applied`() {
        testProjectBuilder.withProperties {
            put("android.builtInKotlin", "false")
            put("android.newDsl", "false")
        }
        testProjectBuilder.build()

        val output = gradleRunner(testProjectDir, ":android-app:tasks")
            .build()
            .output

        // With built-in Kotlin disabled, our plugin should explicitly apply the Kotlin Android plugin
        output.shouldContainAll(
            "hasKotlinAndroidPlugin: true",
        )
    }
}
