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

import com.kroger.gradle.config.KgpProperties
import com.vanniktech.maven.publish.MavenPublishPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.configure

/**
 * Applies conventions common to published artifacts.
 */
public class ReleaseConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(MavenPublishPlugin::class.java)

            // evaluate project before applying defaults so the project has the opportunity to provide its own settings
            afterEvaluate {
                extensions.configure<PublishingExtension> {
                    val kgpProperties = KgpProperties(project)
                    val repositoryName = kgpProperties.repositoryName ?: "Artifactory"
                    val repositoryUrl = kgpProperties.repositoryUrl
                    if (repositories.isEmpty() && !repositoryUrl.isNullOrBlank()) {
                        logger.info("No repository configuration found so using kgp.repository.url setting: $repositoryUrl")
                        repositories {
                            maven {
                                name = repositoryName
                                url = project.uri(repositoryUrl)
                                credentials {
                                    username = System.getenv("ARTIFACTORY_USERNAME")
                                    password = System.getenv("ARTIFACTORY_PASSWORD")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
