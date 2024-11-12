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

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
    kgpProperties: KgpProperties,
    explicitApiMode: ExplicitApiMode,
) {
    val kgpVersions = kgpProperties.kgpVersions
    configureKotlin(kgpVersions, explicitApiMode)
    with(commonExtension) {
        compileSdk = kgpVersions.kgpCompileSdk

        defaultConfig {
            minSdk = kgpVersions.kgpMinSdk
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        if (kgpProperties.autoConfigureCompose) {
            configureCompose(commonExtension)
        }

        compileOptions {
            isCoreLibraryDesugaringEnabled = kgpProperties.autoConfigureCoreLibraryDesugaring
        }

        if (kgpProperties.autoConfigureCoreLibraryDesugaring) {
            coreLibraryDesugaring()
        }

        packaging {
            resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
}

internal fun Project.configureKotlin(kgpVersions: KgpVersions, explicitApiMode: ExplicitApiMode) {
    extensions.configure<KotlinProjectExtension> {
        jvmToolchain(kgpVersions.kgpJdk)
        explicitApi = explicitApiMode
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

/**
 * Enables the compose build feature and sets the compose compiler version.
 * If auto-configure compose dependencies is enabled, the dependencies are added.
 */
public fun Project.configureCompose(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    val kgpProperties = KgpProperties(this)

    with(commonExtension) {
        val kgpVersions = kgpProperties.kgpVersions
        buildFeatures.compose = true
        composeOptions {
            kotlinCompilerExtensionVersion = kgpVersions.kgpComposeCompiler
        }

        dependencies.apply {
            composeBasic()
            logger.info("autoconfigureComposeSetting = ${kgpProperties.autoConfigureComposeDependencies}")
            when (kgpProperties.autoConfigureComposeDependencies) {
                ComposeDependencies.BUNDLE -> {
                    val composeBundle = kgpVersions.kgpComposeBundle
                    add(Configurations.IMPLEMENTATION, composeBundle)
                }

                ComposeDependencies.MATERIAL -> {
                    composeMaterial()
                }

                ComposeDependencies.MATERIAL3 -> {
                    composeMaterial3()
                }

                ComposeDependencies.NONE -> Unit
            }
        }
    }
}
