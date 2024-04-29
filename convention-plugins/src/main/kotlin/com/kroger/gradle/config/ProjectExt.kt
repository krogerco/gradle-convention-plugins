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
package com.kroger.gradle.config

import dagger.hilt.android.plugin.HiltGradlePlugin
import kotlinx.kover.gradle.plugin.KoverGradlePlugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.DokkaTask
import org.jmailen.gradle.kotlinter.KotlinterPlugin

// region plugin configuration
/**
 * When [isDokkaEnabled] is true, adds the Dokka plugin.
 * @param isAndroidProject when true adds the android documentation plugin dependency
 */
internal fun Project.configureDokka(isDokkaEnabled: Boolean, isAndroidProject: Boolean = false) {
    if (isDokkaEnabled) {
        pluginManager.apply(DokkaPlugin::class.java)
        tasks.withType<DokkaTask>().configureEach {
            dokkaSourceSets.configureEach {
                offlineMode.set(true)
            }
        }

        if (isAndroidProject) {
            val dokkaVersion = KgpProperties(this).kgpVersions.kgpDokka
            dependencies.add(Configurations.DOKKA_PLUGIN, "org.jetbrains.dokka:android-documentation-plugin:$dokkaVersion")
        }
    }
}

/**
 * When [isHiltEnabled] is true, adds the plugins, dependencies, and settings required to use Hilt.
 */
internal fun Project.configureHilt(isHiltEnabled: Boolean) {
    if (isHiltEnabled) {
        pluginManager.apply("org.jetbrains.kotlin.kapt")
        pluginManager.apply(HiltGradlePlugin::class.java)

        hilt()
    }
}

/**
 * When [isKotlinterEnabled] is true, adds the Kotlinter plugin.
 */
internal fun Project.configureKotlinter(isKotlinterEnabled: Boolean) {
    if (isKotlinterEnabled) {
        pluginManager.apply(KotlinterPlugin::class.java)
    }
}

/**
 * When [isKoverEnabled] is true, adds the Kover plugin.
 */
internal fun Project.configureKover(isKoverEnabled: Boolean) {
    if (isKoverEnabled) {
        pluginManager.apply(KoverGradlePlugin::class.java)
    }
}
// endregion

/**
 * Returns the value of the BUILD_VERSION environment variable
 */
public val Project.buildVersion: Provider<String>
    get() = providers.environmentVariable("BUILD_VERSION")

// TODO Using providers.gradleProperty would be preferred for these properties but there is a long
// standing defect where the function does not work correctly when getting properties from subprojects
// https://github.com/gradle/gradle/issues/23572
internal fun Project.findOptionalStringProperty(propertyName: String): String? =
    findProperty(propertyName)?.toString()

internal fun Project.findOptionalBooleanProperty(propertyName: String): Boolean? =
    findOptionalStringProperty(propertyName)?.toBoolean()

internal fun Project.getVersionCatalog(name: String): VersionCatalog =
    getVersionCatalogOrNull(name) ?: error("Missing version catalog with name: $name")

internal fun Project.getVersionCatalogOrNull(name: String): VersionCatalog? =
    project.extensions.getByType<VersionCatalogsExtension>().find(name).orElseNull()
