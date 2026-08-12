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

class ReleaseConventionPluginTest {
    @TempDir
    private lateinit var testProjectDir: File
    private lateinit var testProjectBuilder: RootTestProjectBuilder

    @BeforeEach
    fun setup() {
        testProjectBuilder = rootProject(projectDir = testProjectDir) {
            versionCatalogSpec.versions.apply {
                put("kgpKotlin", "\"$KOTLIN_VERSION\"")
                put("kgpJdk", "\"$JDK_VERSION\"")
            }
            addPlugin("com.kroger.gradle.root")
            addPlugin("com.kroger.gradle.published-kotlin-library-conventions", apply = false)
            addSubproject("kotlin-module") {
                addPlugin("com.kroger.gradle.published-kotlin-library-conventions")
                appendBuildFile(
                    """
                    afterEvaluate {
                        publishing {
                            publications.configureEach {
                                this as MavenPublication
                                (repositories.firstOrNull() as? MavenArtifactRepository)?.let { repository ->
                                    println("REPOSITORY COUNT: ${"$"}{repositories.count()}")
                                    println("REPOSITORY NAME: ${"$"}{repository.name}")
                                    println("REPOSITORY URL: ${"$"}{repository.url}")
                                    println("REPOSITORY USERNAME: ${"$"}{repository.credentials.username}")
                                    println("REPOSITORY PASSWORD: ${"$"}{repository.credentials.password}")
    
                                    println("PUBLICATION ARTIFACT ID: ${"$"}artifactId")
                                    println("PUBLICATION GROUP ID: ${"$"}groupId")
                                    println("PUBLICATION VERSION: ${"$"}version")
                                }
                            }
                        }
                    }
                    """.trimIndent(),
                )
            }
        }
    }

    @Test
    fun `GIVEN release plugin WHEN no repo settings supplied THEN no artifactory publish task exists`() {
        testProjectBuilder.build()
        val output = gradleRunner(testProjectDir, ":kotlin-module:tasks")
            .build()
            .output

        output.shouldNotContainAny(
            "publishMavenPublicationToArtifactoryRepository",
            "REPOSITORY COUNT:",
            "REPOSITORY NAME:",
            "REPOSITORY URL:",
            "PUBLICATION ARTIFACT ID:",
            "PUBLICATION GROUP ID:",
            "PUBLICATION VERSION:",
        )
    }

    @Test
    fun `GIVEN release plugin WHEN default credential properties used THEN property values applied correctly`() {
        testProjectBuilder.configureSubproject("kotlin-module") {
            withProperties {
                put("kgp.repository.url", "https://fakeurl")
            }
        }

        testProjectBuilder.build()
        val output = gradleRunner(testProjectDir, ":kotlin-module:tasks")
            .withEnvironment(
                mapOf(
                    "ARTIFACTORY_USERNAME" to "fakeusername",
                    "ARTIFACTORY_PASSWORD" to "fakepassword",
                ),
            )
            .build()
            .output

        output.shouldContainAll(
            "publishMavenPublicationToArtifactoryRepository",
            "REPOSITORY COUNT: 1",
            "REPOSITORY NAME: Artifactory",
            "REPOSITORY URL: https://fakeurl",
            "REPOSITORY USERNAME: fakeusername",
            "REPOSITORY PASSWORD: fakepassword",
        )
    }

    @Test
    fun `GIVEN release plugin WHEN properties set THEN property values applied correctly`() {
        testProjectBuilder.configureSubproject("kotlin-module") {
            withProperties {
                put("kgp.repository.name", "fakename")
                put("kgp.repository.credentials.env.password", "CUSTOM_PASSWORD")
                put("kgp.repository.credentials.env.username", "CUSTOM_USERNAME")
                put("kgp.repository.url", "https://fakeurl")
                put("GROUP", "com.kroger.override")
                put("POM_ARTIFACT_ID", "override-module")
                put("VERSION_NAME", "1.0.0")
            }
        }

        testProjectBuilder.build()
        val output = gradleRunner(testProjectDir, ":kotlin-module:tasks")
            .withEnvironment(
                mapOf(
                    "CUSTOM_USERNAME" to "customfakeusername",
                    "CUSTOM_PASSWORD" to "customfakepassword",
                ),
            )
            .build()
            .output

        output.shouldContainAll(
            "publishMavenPublicationToFakenameRepository",
            "REPOSITORY COUNT: 1",
            "REPOSITORY NAME: fakename",
            "REPOSITORY URL: https://fakeurl",
            "REPOSITORY USERNAME: customfakeusername",
            "REPOSITORY PASSWORD: customfakepassword",
            "PUBLICATION ARTIFACT ID: override-module",
            "PUBLICATION GROUP ID: com.kroger.override",
            "PUBLICATION VERSION: 1.0.0",
        )
    }

    @Test
    fun `GIVEN release plugin WHEN credentials missing value THEN no value available error occurs`() {
        testProjectBuilder.configureSubproject("kotlin-module") {
            withProperties {
                put("kgp.repository.name", "fakename")
                put("kgp.repository.credentials.env.password", "CUSTOM_PASSWORD")
                put("kgp.repository.credentials.env.username", "CUSTOM_USERNAME")
                put("kgp.repository.url", "https://fakeurl")
                put("GROUP", "com.kroger.override")
                put("POM_ARTIFACT_ID", "override-module")
                put("VERSION_NAME", "1.0.0")
            }
        }

        testProjectBuilder.build()
        val output = gradleRunner(testProjectDir, ":kotlin-module:tasks")
            .buildAndFail()
            .output

        output.shouldContainAll(
            """
                Cannot query the value of this provider because it has no value available.
                  The value of this provider is derived from:
                    - environment variable 'CUSTOM_USERNAME'
            """.trimIndent(),
        )
    }

    @Test
    fun `GIVEN release plugin WHEN subproject contains repository definition THEN properties not used`() {
        testProjectBuilder.configureSubproject("kotlin-module") {
            appendBuildFile(
                """
                extensions.configure<PublishingExtension> {
                    repositories {
                        maven {
                            name = "NewRepo"
                            url = uri("https://newrepo")
                            credentials {
                                username = "user1"
                                password = "password1"
                            }
                        }
                    }
                }
                """.trimIndent(),
            )
            withProperties {
                put("kgp.repository.name", "notused")
                put("kgp.repository.url", "https://notused")
            }
        }

        testProjectBuilder.build()
        val output = gradleRunner(testProjectDir, ":kotlin-module:tasks")
            .build()
            .output

        output.shouldContainAll(
            "publishMavenPublicationToNewRepoRepository",
            "REPOSITORY COUNT: 1",
            "REPOSITORY NAME: NewRepo",
            "REPOSITORY URL: https://newrepo",
            "REPOSITORY USERNAME: user1",
            "REPOSITORY PASSWORD: password1",
            "PUBLICATION ARTIFACT ID: kotlin-module",
            "PUBLICATION GROUP ID: com.kroger.test-project",
            "PUBLICATION VERSION: 0.0.1",
        )
    }
}
