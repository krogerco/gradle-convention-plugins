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
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.androidLint)
}

version = System.getenv("BUILD_VERSION")

lint {
    baseline = file("lint-baseline.xml")
}

kotlin {
    explicitApi()
}

// configure kotlinter to include kts files when linting
val ktsFileMatcher = fileTree(rootDir).matching {
    include("**/*.kts")
}

tasks.withType<LintTask>().configureEach {
    this.source = this.source.plus(ktsFileMatcher)
}

tasks.withType<FormatTask>().configureEach {
    this.source = this.source.plus(ktsFileMatcher)
}

val testRuntimeDependencies by configurations.registering {
    extendsFrom(configurations.compileOnly.get())
}

tasks {
    withType<Test>().configureEach {
        useJUnitPlatform()
    }

    // By default GradleRunner.withPluginClasspath() only adds implementation dependencies to the
    // classpath. However, we also need compile dependencies so the Kotlin Plugin and Android Gradle
    // Plugin are available to be used when running plugin tests.
    withType<PluginUnderTestMetadata>().configureEach {
        pluginClasspath.from(testRuntimeDependencies)
    }
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "com.kroger.gradle.android-application-conventions"
            implementationClass = "com.kroger.gradle.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "com.kroger.gradle.android-library-conventions"
            implementationClass = "com.kroger.gradle.AndroidLibraryConventionPlugin"
        }
        register("kotlinLibrary") {
            id = "com.kroger.gradle.kotlin-library-conventions"
            implementationClass = "com.kroger.gradle.KotlinLibraryConventionPlugin"
        }
        register("dependencyManagement") {
            id = "com.kroger.gradle.dependency-conventions"
            implementationClass = "com.kroger.gradle.DependencyConventionPlugin"
        }
        register("release") {
            id = "com.kroger.gradle.release-conventions"
            implementationClass = "com.kroger.gradle.ReleaseConventionPlugin"
        }
        register("root") {
            id = "com.kroger.gradle.root"
            implementationClass = "com.kroger.gradle.KrogerRootPlugin"
        }
        register("publishedKotlinLibrary") {
            id = "com.kroger.gradle.published-kotlin-library-conventions"
            implementationClass = "com.kroger.gradle.PublishedKotlinLibraryConventionPlugin"
        }
        register("publishedAndroidLibrary") {
            id = "com.kroger.gradle.published-android-library-conventions"
            implementationClass = "com.kroger.gradle.PublishedAndroidLibraryConventionPlugin"
        }
    }
}

dependencies {
    compileOnly(libs.gradlePlugins.android)
    compileOnly(libs.gradlePlugins.androidJunit5)
    compileOnly(libs.gradlePlugins.compose)
    compileOnly(libs.gradlePlugins.dependencyAnalysis)
    compileOnly(libs.gradlePlugins.dokka)
    compileOnly(libs.gradlePlugins.gradleMavenPublishPlugin)
    compileOnly(libs.gradlePlugins.gradleVersions)
    compileOnly(libs.gradlePlugins.hilt)
    compileOnly(libs.gradlePlugins.kotlin)
    compileOnly(libs.gradlePlugins.kotlinter)
    compileOnly(libs.gradlePlugins.kover)
    compileOnly(libs.gradlePlugins.ksp)
    compileOnly(libs.gradlePlugins.room)

    lintChecks(libs.androidx.lint.gradle)

    testImplementation(platform(libs.junit.bom))
    testRuntimeOnly(libs.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.jupiter.api)
    testImplementation(libs.koTest)

    testRuntimeDependencies(libs.gradlePlugins.foojayResolver)
    testRuntimeDependencies(libs.gradlePlugins.kotlin.serialization)
}
