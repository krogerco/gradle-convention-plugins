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

import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.extra

internal class KgpProperties private constructor(private val project: Project) {
    /**
     * Whether to auto-apply the Dependency Maintenance plugin to the root project. Default is true.
     */
    val autoApplyDependencyManagement: Boolean
        get() = project.findOptionalBooleanProperty("kgp.plugins.autoapply.dependencymanagement") != false

    /**
     * Whether to auto-apply the Dokka plugin to supported projects. Default is true.
     */
    val autoApplyDokka: Boolean
        get() = project.findOptionalBooleanProperty("kgp.plugins.autoapply.dokka") != false

    /**
     * Whether to auto-apply the Kotlinter plugin to supported projects. Default is true.
     */
    val autoApplyKotlinter: Boolean
        get() = project.findOptionalBooleanProperty("kgp.plugins.autoapply.kotlinter") != false

    /**
     * Whether to auto-apply the Kover plugin to supported projects. Default is true.
     */
    val autoApplyKover: Boolean
        get() = project.findOptionalBooleanProperty("kgp.plugins.autoapply.kover") != false

    /**
     * Whether to auto-configure compose in Android related projects
     */
    val autoConfigureCompose: Boolean
        get() = project.findOptionalBooleanProperty("kgp.android.autoconfigure.compose") != false

    /**
     * Whether to auto-configure compose dependencies. Default is none.
     */
    val autoConfigureComposeDependencies: ComposeDependencies
        get() = project.findOptionalStringProperty("kgp.android.autoconfigure.compose.dependencies")?.let { propertyValue ->
            try {
                ComposeDependencies.valueOf(propertyValue.uppercase())
            } catch (e: IllegalArgumentException) {
                val composeDependencies = ComposeDependencies.values().joinToString { it.name.lowercase() }
                throw IllegalArgumentException("Invalid compose dependencies value: $propertyValue. Valid values are: $composeDependencies", e)
            }
        } ?: ComposeDependencies.NONE

    /**
     * Whether to auto-configure core library desugaring to use newer Java APIs on older versions of Android. Default is false.
     */
    val autoConfigureCoreLibraryDesugaring: Boolean
        get() = project.findOptionalBooleanProperty("kgp.android.autoconfigure.corelibrarydesugaring") == true

    /**
     * Whether to auto-configure each subproject's group and version. Default is true.
     */
    val autoConfigurePublishingProperties: Boolean
        get() = project.findOptionalBooleanProperty("kgp.autoconfigure.publishingproperties") != false

    /**
     * Whether to auto-configure Hilt when the Android Application Convention plugin is applied. Default is true.
     */
    val autoConfigureHiltApplication: Boolean
        get() = project.findOptionalBooleanProperty("kgp.android.autoconfigure.hilt.application") != false

    /**
     * Whether to auto-configure Hilt when the Android Library Convention plugin is applied. Default is false.
     */
    val autoConfigureHiltLibrary: Boolean
        get() = project.findOptionalBooleanProperty("kgp.android.autoconfigure.hilt.library") == true

    /**
     * Versions loaded from the version catalog that are used in the various plugins.
     */
    val kgpVersions: KgpVersions by lazy {
        val rootProject = project.rootProject
        rootProject.getOrCreateExtra(ExtraKeys.KGP_VERSIONS) {
            val versionCatalog = rootProject.getVersionCatalog(versionCatalogName)
            KgpVersions(versionCatalog)
        }
    }

    /**
     * The name of the environment variable containing the repository password. Default is ARTIFACTORY_PASSWORD.
     */
    val repositoryPasswordEnvironmentVariable: String
        get() = project.findOptionalStringProperty("kgp.repository.credentials.env.password") ?: "ARTIFACTORY_PASSWORD"

    /**
     * The name of the environment variable containing the repository username. Default is ARTIFACTORY_USERNAME.
     */
    val repositoryUsernameEnvironmentVariable: String
        get() = project.findOptionalStringProperty("kgp.repository.credentials.env.username") ?: "ARTIFACTORY_USERNAME"

    /**
     * The name of the repository used by default for publishing tasks. Default is Artifactory.
     */
    val repositoryName: String
        get() = project.findOptionalStringProperty("kgp.repository.name") ?: "Artifactory"

    /**
     * The url of the repository used by default for publishing tasks.
     */
    val repositoryUrl: String?
        get() = project.findOptionalStringProperty("kgp.repository.url")

    /**
     * The name of the version catalog that should be used. Default is libs.
     */
    private val versionCatalogName: String
        get() = project.findOptionalStringProperty("kgp.versioncatalog.name") ?: "libs"

    companion object {
        operator fun invoke(project: Project): KgpProperties =
            project.getOrCreateExtra(ExtraKeys.KGP_PROPERTIES) {
                KgpProperties(project)
            }
    }
}

private fun <T> ExtensionAware.getOrCreateExtra(key: String, body: () -> T): T =
    if (extra.has(key)) {
        @Suppress("UNCHECKED_CAST")
        extra.get(key) as T
    } else {
        body().also { extra.set(key, it) }
    }
