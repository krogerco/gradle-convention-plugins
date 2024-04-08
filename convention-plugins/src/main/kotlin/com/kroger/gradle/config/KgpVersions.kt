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

import org.gradle.api.artifacts.ExternalModuleDependencyBundle
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.provider.Provider
import java.util.Optional

internal class KgpVersions(private val catalog: VersionCatalog) {
    val kgpAndroidDesugarJdkLibs: String
        get() = getVersion("kgpAndroidDesugarJdkLibs")

    val kgpAndroidxHiltCompiler: String
        get() = getVersion("kgpAndroidxHiltCompiler")

    val kgpJdk: Int
        get() = getVersion("kgpJdk").toInt()

    val kgpCompileSdk: Int
        get() = getVersion("kgpCompileSdk").toInt()

    val kgpComposeBom: String
        get() = getVersion("kgpAndroidxComposeBom")

    val kgpComposeBundle: Provider<ExternalModuleDependencyBundle>
        get() = catalog.findBundle("kgpCompose").orElseThrow {
            IllegalStateException("Missing \"kgpCompose\" bundle in Version Catalog")
        }

    val kgpComposeCompiler: String
        get() = getVersion("kgpAndroidxComposeCompiler")

    val kgpDagger: String
        get() = getVersion("kgpDagger")

    val kgpDeepLink: String
        get() = getVersion("kgpDeepLink")

    val kgpDokka: String
        get() = getVersion("kgpDokka")

    val kgpJunit4: String
        get() = getVersion("kgpJunit4")

    val kgpJunit5: String
        get() = getVersion("kgpJunit5")

    val kgpKotlinxSerialization: String
        get() = getVersion("kgpKotlinxSerialization")

    val kgpMinSdk: Int
        get() = getVersion("kgpMinSdk").toInt()

    val kgpMoshi: String
        get() = getVersion("kgpMoshi")

    val kgpRoom: String
        get() = getVersion("kgpAndroidxRoom")

    val kgpTargetSdk: Int
        get() = getVersion("kgpTargetSdk").toInt()

    private fun getVersion(alias: String): String =
        getOptionalVersion(alias) ?: error("No version found in version catalog with alias: $alias")

    private fun getOptionalVersion(alias: String): String? =
        catalog.findVersion(alias).map { it.toString() }.orElseNull()
}

internal fun <T> Optional<T>.orElseNull(): T? = orElse(null)
