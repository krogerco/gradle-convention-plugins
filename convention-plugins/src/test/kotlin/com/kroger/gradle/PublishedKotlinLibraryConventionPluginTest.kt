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

class PublishedKotlinLibraryConventionPluginTest {
    @TempDir
    private lateinit var testProjectDir: File
    private lateinit var testProjectBuilder: RootTestProjectBuilder

    @BeforeEach
    fun init() {
        testProjectBuilder = rootProject(projectDir = testProjectDir) {
            versionCatalogSpec.versions.apply {
                put("kgpKotlin", "\"$KOTLIN_VERSION\"")
                put("kgpJdk", "\"$JDK_VERSION\"")
            }
            addPlugin("com.kroger.gradle.root")
            addPlugin("com.kroger.gradle.published-kotlin-library-conventions", apply = false)
            addSubproject("kotlin-module") {
                addPlugin("com.kroger.gradle.published-kotlin-library-conventions")
            }
        }
    }

    @Test
    fun `WHEN published kotlin library plugin applied with repo url property set THEN expected default tasks exist`() {
        testProjectBuilder.configureSubproject("kotlin-module") {
            withProperties {
                put("kgp.repository.url", "https://artifactory")
            }
        }
        testProjectBuilder.build()

        val output = gradleRunner(testProjectDir, ":kotlin-module:tasks")
            .build()
            .output

        output.shouldContainAll(
            "assemble - ",
            "lintKotlin - ",
            "dokkaHtml - ",
            "koverHtmlReport",
            "publishMavenPublicationToArtifactoryRepository - ",
        )
    }

    @Test
    fun `WHEN published kotlin library plugin applied and kover disabled THEN no kover tasks exist`() {
        testProjectBuilder.configureSubproject("kotlin-module") {
            withProperties { put("kgp.plugins.autoapply.kover", "false") }
        }
        testProjectBuilder.build()

        val output = gradleRunner(testProjectDir, ":kotlin-module:tasks")
            .build()
            .output

        output.shouldNotContain("kover")
    }

    @Test
    fun `WHEN published kotlin library plugin applied and dokka disabled THEN no dokka tasks exist`() {
        testProjectBuilder.configureSubproject("kotlin-module") {
            withProperties { put("kgp.plugins.autoapply.dokka", "false") }
        }
        testProjectBuilder.build()

        val output = gradleRunner(testProjectDir, ":kotlin-module:tasks")
            .build()
            .output

        output.shouldNotContain("dokka")
    }
}
