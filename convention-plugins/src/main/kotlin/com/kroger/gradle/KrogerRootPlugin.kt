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
import com.kroger.gradle.config.MIN_SUPPORTED_GRADLE_VERSION
import com.kroger.gradle.config.applyAndConfigureKotlinter
import com.kroger.gradle.config.buildVersion
import com.kroger.gradle.config.configureDokka
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.kotlin.dsl.register
import org.gradle.util.GradleVersion

/**
 * Apply common checks and plugins. This should only be applied on the root project.
 */
public class KrogerRootPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        require(target == target.rootProject) {
            "The KrogerRootPlugin should only be applied once and on the root project."
        }

        require(GradleVersion.current() >= MIN_SUPPORTED_GRADLE_VERSION) {
            "KGP plugins require Gradle ${MIN_SUPPORTED_GRADLE_VERSION.version} or later. Found ${GradleVersion.current()}"
        }

        with(target) {
            val kgpProperties = KgpProperties(this)
            if (kgpProperties.autoApplyDependencyManagement) {
                pluginManager.apply(DependencyConventionPlugin::class.java)
            }

            if (kgpProperties.autoApplyKotlinter) {
                applyAndConfigureKotlinter(this)
            }
            configureDokka(kgpProperties.autoApplyDokka)

            if (kgpProperties.autoConfigurePublishingProperties) {
                subprojects {
                    group = "com.kroger.${rootProject.name}"
                    version = buildVersion.getOrElse("0.0.1")
                }
            }

            tasks.register<Delete>("clean") {
                group = "build"
                delete(rootProject.layout.buildDirectory)
            }
        }
    }
}
