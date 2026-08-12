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

class ConfigureComposeTest {
    @TempDir
    private lateinit var testProjectDir: File
    private lateinit var testProjectBuilder: RootTestProjectBuilder

    @BeforeEach
    fun init() {
        testProjectBuilder = rootProject(projectDir = testProjectDir) {
            versionCatalogSpec.apply {
                versions.apply {
                    put("kgpAndroidxComposeBom", "\"2022.12.00\"")
                    put("kgpCompileSdk", "\"32\"")
                    put("kgpDokka", "\"1.8.20\"")
                    put("kgpKotlin", "\"$KOTLIN_VERSION\"")
                    put("kgpJdk", "\"$JDK_VERSION\"")
                    put("kgpMinSdk", "\"26\"")
                    put("kgpTargetSdk", "\"26\"")
                }
            }

            addPlugin("com.kroger.gradle.root")

            addSubproject("android-library-module") {
                addPlugin("com.kroger.gradle.android-library-conventions")
                appendBuildFile(
                    """
                    android {
                        namespace = "com.kroger.kgp.testmodule"
                    }
                    
                    afterEvaluate {
                        listOf("implementation", "debugImplementation", "androidTestImplementation").forEach { configurationName ->
                            configurations.named(configurationName).configure {
                                println("CONFIGURATION NAME: ${"$"}name")
                                dependencies.forEach { println("${"$"}name(${"$"}{it.group}:${"$"}{it.name}:${"$"}{it.version})") }
                                println()
                            }
                        }
                    }
                    """.trimIndent(),
                )
            }
        }
    }

    @Test
    fun `GIVEN compose autoconfigure disabled WHEN gradle configuration runs THEN compose related versions are not needed`() {
        testProjectBuilder.withProperties {
            put("kgp.android.autoconfigure.compose", "false")
        }
        testProjectBuilder.build()
        val output = gradleRunner(testProjectDir, ":android-library-module:tasks")
            .build()
            .output

        output.shouldNotContain("implementation(androidx.compose:compose-bom:2022.12.00)")
    }

    @Test
    fun `GIVEN compose autoconfigure disabled WHEN configureCompose manually called THEN compose related versions are needed`() {
        testProjectBuilder.withProperties {
            put("kgp.android.autoconfigure.compose", "false")
        }
        testProjectBuilder.configureSubproject("android-library-module") {
            withImportStatements {
                add("import com.kroger.gradle.config.*")
                add("import com.android.build.gradle.LibraryExtension")
            }
            appendBuildFile("configureCompose(android)")
        }
        testProjectBuilder.build()
        val output = gradleRunner(testProjectDir, ":android-library-module:tasks")
            .build()
            .output

        output.shouldContain("implementation(androidx.compose:compose-bom:2022.12.00)")
    }

    @Test
    fun `GIVEN compose autoconfigure enabled WHEN compose bom version missing THEN exception thrown`() {
        testProjectBuilder.versionCatalogSpec.versions.remove("kgpAndroidxComposeBom")
        testProjectBuilder.build()
        val output = gradleRunner(testProjectDir, ":android-library-module:tasks")
            .buildAndFail()
            .output

        output.shouldContain("No version found in version catalog with alias: kgpAndroidxComposeBom")
    }

    @Test
    fun `GIVEN compose autoconfigure enabled and compose dependencies autoconfigured WHEN dependency property value is invalid THEN exception thrown`() {
        testProjectBuilder.withProperties {
            put("kgp.android.autoconfigure.compose.dependencies", "invalid")
        }

        testProjectBuilder.build()
        val output = gradleRunner(testProjectDir, ":android-library-module:tasks")
            .buildAndFail()
            .output

        output.shouldContain("Invalid compose dependencies value: invalid.")
    }

    @Test
    fun `GIVEN compose autoconfigure enabled and compose dependencies autoconfigured WHEN autoconfigure is bundle and bundle is missing THEN exception thrown`() {
        testProjectBuilder.withProperties {
            put("kgp.android.autoconfigure.compose.dependencies", "bundle")
        }

        testProjectBuilder.build()
        val output = gradleRunner(testProjectDir, ":android-library-module:tasks")
            .buildAndFail()
            .output

        output.shouldContain("Missing \"kgpCompose\" bundle in Version Catalog")
    }

    @Test
    fun `GIVEN compose autoconfigure enabled THEN expected default dependencies added`() {
        testProjectBuilder.build()
        val output = gradleRunner(testProjectDir, ":android-library-module:tasks")
            .build()
            .output

        output.shouldContainAll(
            "implementation(androidx.compose:compose-bom:2022.12.00)",
            "androidTestImplementation(androidx.compose:compose-bom:2022.12.00)",
            "implementation(androidx.compose.ui:ui-tooling-preview:null)",
            "debugImplementation(androidx.compose.ui:ui-tooling:null)",
            "debugImplementation(androidx.compose.ui:ui-test-manifest:null)",
            "androidTestImplementation(androidx.compose.ui:ui-test-junit4:null)",
        )
    }

    @Test
    fun `GIVEN compose autoconfigure enabled and compose dependencies autoconfigured WHEN autoconfigure is material THEN expected dependencies added`() {
        testProjectBuilder.withProperties {
            put("kgp.android.autoconfigure.compose.dependencies", "material")
        }

        testProjectBuilder.build()
        val output = gradleRunner(testProjectDir, ":android-library-module:tasks")
            .build()
            .output

        output.shouldContain("implementation(androidx.compose.material:material:null)")
    }

    @Test
    fun `GIVEN compose autoconfigure enabled and compose dependencies autoconfigured WHEN autoconfigure is material3 THEN expected dependencies added`() {
        testProjectBuilder.withProperties {
            put("kgp.android.autoconfigure.compose.dependencies", "material3")
        }

        testProjectBuilder.build()
        val output = gradleRunner(testProjectDir, ":android-library-module:tasks")
            .build()
            .output

        output.shouldContainAll(
            "implementation(androidx.compose.material:material-icons-core:null)",
            "implementation(androidx.compose.material3:material3:null)",
            "implementation(androidx.compose.material3:material3-window-size-class:null)",
        )
    }

    @Test
    fun `GIVEN compose autoconfigure enabled and compose dependencies autoconfigured WHEN autoconfigure is bundle THEN expected dependencies added`() {
        testProjectBuilder.versionCatalogSpec.apply {
            libraries.apply {
                put("composeActivity", "{ module = \"androidx.activity:activity-compose\", version = \"1.5.1\" }")
                put("composeViewModel", "{ module = \"androidx.lifecycle:lifecycle-viewmodel-compose\", version = \"2.5.1\" }")
            }
            bundles["kgpCompose"] = "[\"composeActivity\", \"composeViewModel\"]"
        }

        testProjectBuilder.withProperties {
            put("kgp.android.autoconfigure.compose.dependencies", "bundle")
        }

        testProjectBuilder.build()
        val output = gradleRunner(testProjectDir, ":android-library-module:tasks")
            .build()
            .output

        output.shouldContain("implementation(androidx.activity:activity-compose:1.5.1)")
        output.shouldContain("implementation(androidx.lifecycle:lifecycle-viewmodel-compose:2.5.1)")
    }
}
