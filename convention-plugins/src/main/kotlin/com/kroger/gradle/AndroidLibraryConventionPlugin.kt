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

import com.android.build.api.AndroidPluginVersion
import com.android.build.api.dsl.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.kroger.gradle.config.KgpProperties
import com.kroger.gradle.config.MIN_SUPPORTED_AGP_VERSION
import com.kroger.gradle.config.configureDokka
import com.kroger.gradle.config.configureHilt
import com.kroger.gradle.config.configureKotlinAndroid
import com.kroger.gradle.config.configureKotlinter
import com.kroger.gradle.config.configureKover
import de.mannodermaus.gradle.plugins.junit5.AndroidJUnitPlatformPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPluginWrapper

/**
 * Apply conventions common to Android libraries.
 */
public class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        require(AndroidPluginVersion.getCurrent() >= MIN_SUPPORTED_AGP_VERSION) {
            "KGP plugins require AGP ${MIN_SUPPORTED_AGP_VERSION.version} or later. Found ${AndroidPluginVersion.getCurrent()}"
        }

        val kgpProperties = KgpProperties(target)
        with(target) {
            with(pluginManager) {
                apply(LibraryPlugin::class.java)
                apply(AndroidJUnitPlatformPlugin::class.java)
                apply(KotlinAndroidPluginWrapper::class.java)
                configureDokka(kgpProperties.autoApplyDokka, true)
                configureHilt(kgpProperties.autoConfigureHiltLibrary)
                configureKover(kgpProperties.autoApplyKover)
                configureKotlinter(kgpProperties.autoApplyKotlinter)
            }

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this, kgpProperties, ExplicitApiMode.Strict)
            }
        }
    }
}
