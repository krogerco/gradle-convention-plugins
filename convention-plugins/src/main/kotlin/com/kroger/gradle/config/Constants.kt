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

import com.android.build.api.AndroidPluginVersion
import org.gradle.util.GradleVersion

public object Configurations {
    public const val ANDROID_TEST_IMPLEMENTATION: String = "androidTestImplementation"
    public const val ANDROID_TEST_RUNTIME_ONLY: String = "androidTestRuntimeOnly"
    public const val API: String = "api"
    public const val CORE_LIBRARY_DESUGARING: String = "coreLibraryDesugaring"
    public const val DEBUG_IMPLEMENTATION: String = "debugImplementation"
    public const val DOKKA_PLUGIN: String = "dokkaPlugin"
    public const val IMPLEMENTATION: String = "implementation"
    public const val KAPT: String = "kapt"
    public const val KAPT_ANDROID_TEST: String = "kaptAndroidTest"
    public const val KAPT_TEST: String = "kaptTest"
    public const val KSP: String = "ksp"
    public const val KSP_ANDROID_TEST: String = "kspAndroidTest"
    public const val KSP_TEST: String = "kspTest"
    public const val TEST_API: String = "testApi"
    public const val TEST_IMPLEMENTATION: String = "testImplementation"
    public const val TEST_RUNTIME_ONLY: String = "testRuntimeOnly"
}

internal object ExtraKeys {
    internal const val KGP_VERSIONS: String = "kgp.versions"
    internal const val KGP_PROPERTIES: String = "kgp.properties"
}

internal val MIN_SUPPORTED_AGP_VERSION by lazy { AndroidPluginVersion(8, 3, 0) }
internal val MIN_SUPPORTED_GRADLE_VERSION = GradleVersion.version("8.4")

internal val AndroidPluginVersion.version: String
    get() = "$major.$minor.$micro"
