/*
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

import com.kroger.gradle.config.MIN_SUPPORTED_GRADLE_VERSION
import com.kroger.gradle.util.KOTLIN_VERSION
import com.kroger.gradle.util.RootTestProjectBuilder
import com.kroger.gradle.util.gradleRunner
import com.kroger.gradle.util.rootProject
import com.kroger.gradle.util.shouldContainAll
import com.kroger.gradle.util.shouldNotContainAny
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class KrogerRootPluginTest {
    @TempDir
    private lateinit var testProjectDir: File
    private lateinit var testProjectBuilder: RootTestProjectBuilder

    @BeforeEach
    fun setup() {
        testProjectBuilder = rootProject(projectDir = testProjectDir) {
            versionCatalogSpec.versions["kgpKotlin"] = "\"$KOTLIN_VERSION\""
            addPlugin("com.kroger.gradle.root", "", true)
        }
    }

    @Test
    fun `GIVEN root plugin WHEN defaults applied THEN expected tasks exist`() {
        testProjectBuilder.build()
        val output = gradleRunner(testProjectDir, arguments = arrayOf("tasks"))
            .build()
            .output

        output.shouldContainAll(
            "clean",
            // tasks from Kotlinter
            "checkKotlinFiles - ",
            "createFormatKotlinPreCommitHook - ",
            "createLintKotlinPreCommitHook - ",
            "formatKotlinFiles - ",
            // tasks from Dependency Management
            "buildHealth - ",
            // tasks from dokka v2
            "dokkaGenerate - ",
            "dokkaGenerateModuleHtml - ",
            "dokkaGeneratePublicationHtml - ",
        )
    }

    @Test
    fun `GIVEN root plugin WHEN plugins disabled THEN related tasks do not exist`() {
        testProjectBuilder.withProperties {
            put("kgp.plugins.autoapply.dokka", "false")
            put("kgp.plugins.autoapply.kotlinter", "false")
            put("kgp.plugins.autoapply.dependencymanagement", "false")
        }
        testProjectBuilder.build()

        val output = gradleRunner(testProjectDir, arguments = arrayOf("tasks"))
            .build()
            .output

        output.shouldNotContainAny(
            // Kotlinter tasks do not exist
            "checkKotlinFiles - ",
            "createFormatKotlinPreCommitHook - ",
            "createLintKotlinPreCommitHook - ",
            "formatKotlinFiles - ",
            // Dependency Management tasks do not exist
            "buildHealth - ",
            // dokka tasks do not exist
            "dokkaGenerate - ",
        )
    }

    @Test
    fun `GIVEN root plugin WHEN multi module build THEN dokka has aggregation task`() {
        testProjectBuilder.versionCatalogSpec.versions["kgpJdk"] = "\"11\""
        testProjectBuilder.addSubproject("kotlin-module") {
            addPlugin("com.kroger.gradle.published-kotlin-library-conventions")
        }
        testProjectBuilder.build()

        val output = gradleRunner(testProjectDir, arguments = arrayOf("tasks"))
            .build()
            .output

        // In v2, aggregation uses dokkaGeneratePublicationHtml (same task but with aggregated content)
        output.shouldContain(
            "dokkaGeneratePublicationHtml - ",
        )

        // Verify that the subproject was detected for aggregation
        output.shouldContain("Dokka: Aggregating documentation from 1 subproject(s)")
        output.shouldContain(":kotlin-module")
    }

    @Test
    fun `GIVEN root plugin WHEN multiple subprojects with dokka THEN aggregates all modules`() {
        testProjectBuilder.versionCatalogSpec.versions["kgpJdk"] = "\"11\""
        testProjectBuilder.addSubproject("kotlin-module-1") {
            addPlugin("com.kroger.gradle.published-kotlin-library-conventions")
        }
        testProjectBuilder.addSubproject("kotlin-module-2") {
            addPlugin("com.kroger.gradle.published-kotlin-library-conventions")
        }
        testProjectBuilder.build()

        val output = gradleRunner(testProjectDir, arguments = arrayOf("tasks"))
            .build()
            .output

        // Verify both subprojects are detected for aggregation
        output.shouldContain("Dokka: Aggregating documentation from 2 subproject(s)")
        output.shouldContain(":kotlin-module-1")
        output.shouldContain(":kotlin-module-2")
    }

    @Test
    fun `GIVEN root plugin WHEN multi module build without dokka in subprojects THEN warning shown`() {
        testProjectBuilder.versionCatalogSpec.versions["kgpJdk"] = "\"11\""
        testProjectBuilder.addSubproject("kotlin-module") {
            addPlugin("com.kroger.gradle.published-kotlin-library-conventions")
            withProperties {
                put("kgp.plugins.autoapply.dokka", "false")
            }
        }
        testProjectBuilder.build()

        val output = gradleRunner(testProjectDir, arguments = arrayOf("tasks"))
            .build()
            .output

        // Verify warning when no subprojects have Dokka
        output.shouldContain("Dokka: No subprojects with Dokka plugin found")
        output.shouldContain("Ensure subprojects use convention plugins that apply Dokka")
    }

    @Test
    fun `GIVEN root plugin WHEN multi module build and root plugin applied to sub module THEN error occurs`() {
        testProjectBuilder.versionCatalogSpec.versions["kgpJdk"] = "\"11\""
        testProjectBuilder.addSubproject("kotlin-module") {
            addPlugin("com.kroger.gradle.published-kotlin-library-conventions")
            addPlugin("com.kroger.gradle.root")
        }
        testProjectBuilder.build()

        val output = gradleRunner(testProjectDir, arguments = arrayOf("tasks"))
            .buildAndFail()
            .output

        output.shouldContain(
            "The KrogerRootPlugin should only be applied once and on the root project.",
        )
    }

    @Test
    fun `GIVEN root plugin WHEN gradle version out of date THEN error occurs`() {
        testProjectBuilder.build()
        val gradleVersion = "9.1.0"
        val result = gradleRunner(testProjectDir, arguments = arrayOf("tasks"))
            .withGradleVersion(gradleVersion)
            .buildAndFail()

        result.output.shouldContain(
            "KGP plugins require Gradle ${MIN_SUPPORTED_GRADLE_VERSION.version} or later. Found Gradle $gradleVersion",
        )
    }

    @Test
    fun `GIVEN root plugin WHEN gradle is min supported version THEN no error`() {
        testProjectBuilder.build()
        val result = gradleRunner(testProjectDir, arguments = arrayOf("tasks"))
            .withGradleVersion(MIN_SUPPORTED_GRADLE_VERSION.version)
            .build()

        result.output.shouldContain("BUILD SUCCESSFUL")
    }
}
