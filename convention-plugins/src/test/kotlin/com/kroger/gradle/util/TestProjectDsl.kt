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
package com.kroger.gradle.util

import org.gradle.testkit.runner.GradleRunner
import java.io.File

@DslMarker
annotation class TestProjectMarker

/**
 * Helper used to configure a gradle project.
 */
@TestProjectMarker
open class TestProjectBuilder(val projectPath: File) {
    init {
        projectPath.mkdirs()
    }

    protected val plugins: MutableSet<PluginSpec> = LinkedHashSet()
    private var buildFileContents: String = ""
    private val gradleProperties: MutableMap<String, String> = mutableMapOf()
    private val importStatements: MutableSet<String> = mutableSetOf()

    private val buildFile: File = projectPath.resolve("build.gradle.kts")
    private val gradlePropertiesFile: File = projectPath.resolve("gradle.properties")

    private fun withBuildFile(plugins: MutableSet<PluginSpec>, buildFileContents: String) {
        val imports = importStatements.sorted().joinToString("\n")
        val pluginsString = plugins.joinToString(
            separator = "\n    ",
            prefix = "plugins {\n    ",
            postfix = "\n}\n",
        ) { plugin ->
            val version = if (plugin.version.isEmpty()) {
                ""
            } else {
                " version \"${plugin.version}\""
            }

            if (plugin.apply) {
                "id(\"${plugin.id}\")$version"
            } else {
                "id(\"${plugin.id}\")$version apply false"
            }
        }

        buildFile.appendText(imports)
        buildFile.appendText(pluginsString)
        buildFile.appendText(buildFileContents)
    }

    /**
     * @param contents adds the provided String to what will be written to the project build file.
     */
    fun appendBuildFile(contents: String) {
        buildFileContents += "\n$contents"
    }

    /**
     * Creates a directory in the current project.
     * @param relativePath relative path of the directory to create from the current project root.
     */
    fun createProjectDir(relativePath: String) {
        projectPath.resolve(relativePath).mkdirs()
    }

    /**
     * Modifies the current properties by applying [applyProperties].
     */
    fun withProperties(applyProperties: MutableMap<String, String>.() -> Unit) {
        gradleProperties.applyProperties()
    }

    /**
     * Modifies the current import statements by applying [applyImportStatements].
     */
    fun withImportStatements(applyImportStatements: MutableSet<String>.() -> Unit) {
        this.importStatements.applyImportStatements()
    }

    private fun writePropertiesToFile() {
        val propertiesString = gradleProperties.entries.joinToString(
            separator = "\n",
        ) { (property, value) ->
            "$property=$value"
        }
        gradlePropertiesFile.writeText(propertiesString)
    }

    fun addPlugin(id: String) {
        plugins.add(PluginSpec(id))
    }

    open fun build() {
        withBuildFile(plugins, buildFileContents)
        writePropertiesToFile()
    }
}

/**
 * Helper to configure the root project.
 */
class RootTestProjectBuilder(private val name: String, projectPath: File) : TestProjectBuilder(projectPath) {
    private val settingsFile: File = projectPath.resolve("settings.gradle.kts")

    private val gradleDir: File = projectPath.resolve("gradle")
        .apply { mkdir() }

    private val versionCatalogFile = gradleDir.resolve("libs.versions.toml")

    private fun writeVersionCatalog() {
        versionCatalogFile.writeText(
            buildString {
                printVersionCatalogSection(this, "versions", versionCatalogSpec.versions)
                printVersionCatalogSection(this, "libraries", versionCatalogSpec.libraries)
                printVersionCatalogSection(this, "bundles", versionCatalogSpec.bundles)
                printVersionCatalogSection(this, "plugins", versionCatalogSpec.plugins)
            },
        )
    }

    private fun printVersionCatalogSection(
        builder: StringBuilder,
        section: String,
        map: Map<String, String>,
    ) = builder.apply {
        if (map.any()) {
            appendLine("[$section]")
            map.forEach { (name, value) ->
                appendLine("$name = $value")
            }
            appendLine()
        }
    }

    val versionCatalogSpec = VersionCatalogSpec()

    private val subprojects: MutableList<TestProjectBuilder> = mutableListOf()

    fun addPlugin(pluginId: String, version: String = "", apply: Boolean = false) {
        plugins.add(PluginSpec(pluginId, version, apply))
    }

    /**
     * Finds a subproject and brings it into scope for further configuration.
     */
    fun configureSubproject(name: String, configure: TestProjectBuilder.() -> Unit) {
        val subproject = subprojects.firstOrNull { it.projectPath.name == name }
            ?: throw IllegalStateException("No subproject found with name: $name")

        subproject.configure()
    }

    /**
     * Add a new subproject to this root project with the supplied [name].
     * @param name used for the subproject and its directory
     */
    fun addSubproject(name: String, init: TestProjectBuilder.() -> Unit) {
        val testProjectBuilder = TestProjectBuilder(projectPath.resolve(name))
        testProjectBuilder.init()
        subprojects.add(testProjectBuilder)
    }

    override fun build() {
        super.build()
        writeSettingsFile()
        writeVersionCatalog()
        subprojects.forEach { it.build() }
    }

    private fun writeSettingsFile() {
        val subprojectIncludes = subprojects.joinToString(
            separator = "\n",
        ) {
            "include(\":${it.projectPath.name}\")"
        }

        settingsFile.writeText(
            buildString {
                appendLine("rootProject.name = \"$name\"\n")
                appendLine(
                    """
                    plugins {
                        // required to download toolchain
                        id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
                    }
                    """.trimIndent(),
                )
                appendLine(subprojectIncludes)
            },
        )
    }
}

fun rootProject(
    projectName: String = "test-project",
    projectDir: File,
    init: RootTestProjectBuilder.() -> Unit,
): RootTestProjectBuilder {
    val rootProject = RootTestProjectBuilder(projectName, projectDir)
    rootProject.init()
    return rootProject
}

/**
 * @param id id of the plugin
 * @param version optional version of the plugin
 * @param apply if true the plugin will be applied in the gradle file
 */
data class PluginSpec(val id: String, val version: String = "", val apply: Boolean = true)

/**
 * Versions, libraries, and plugins will be written to the default version catalog of the root project.
 * @param versions each entry in the map will be written under `[versions]` in the toml file
 * @param libraries each entry in the map will be written under `[libraries]` in the toml file
 * @param plugins each entry in the map will be written under `[plugins]` in the toml file
 * @param bundles each entry in the map will be written under `[bundles]` in the toml file
 */
data class VersionCatalogSpec(
    val versions: MutableMap<String, String> = mutableMapOf(),
    val libraries: MutableMap<String, String> = mutableMapOf(),
    val plugins: MutableMap<String, String> = mutableMapOf(),
    val bundles: MutableMap<String, String> = mutableMapOf(),
)

fun gradleRunner(projectDir: File, vararg arguments: String): GradleRunner =
    GradleRunner.create()
        .forwardOutput()
        .withArguments(*arguments)
        .withProjectDir(projectDir)
        .withPluginClasspath()

const val JDK_VERSION: String = "17"
const val KOTLIN_VERSION: String = "2.1.20"
