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
import com.kroger.gradle.config.configureDokka
import com.kroger.gradle.config.configureKotlin
import com.kroger.gradle.config.configureKover
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jmailen.gradle.kotlinter.KotlinterPlugin

/**
 * Apply conventions common to Kotlin libraries.
 */
public class KotlinLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val kgpProperties = KgpProperties(target)
        val kgpVersions = kgpProperties.kgpVersions
        with(target) {
            with(pluginManager) {
                apply(JavaLibraryPlugin::class.java)
                apply(KotlinPluginWrapper::class.java)
                configureDokka(kgpProperties.autoApplyDokka)
                configureKover(kgpProperties.autoApplyKover)
                if (kgpProperties.autoApplyKotlinter) {
                    apply(KotlinterPlugin::class.java)
                }
            }

            configureKotlin(kgpVersions, ExplicitApiMode.Strict)
            target.configure<JavaPluginExtension> {
                val javaVersion = JavaVersion.toVersion(kgpVersions.kgpJvmTarget)
                sourceCompatibility = javaVersion
                targetCompatibility = javaVersion
            }
        }
    }
}
