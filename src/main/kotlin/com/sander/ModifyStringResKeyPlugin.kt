package com.sander

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.cxx.configure.trySymlinkNdk
import com.android.build.gradle.tasks.MergeResources
import com.android.utils.forEach
import com.sander.extension.ModifyStringResKeyExtension
import com.sander.transform.ModifyStringResKeyTransform
import com.sander.utils.ValuesFileFilter
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.TrueFileFilter
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import java.nio.charset.Charset
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class ModifyStringResKeyPlugin : Plugin<Project> {
    private val domFactory: DocumentBuilderFactory = DocumentBuilderFactory.newInstance()
    private val domBuilder: DocumentBuilder = domFactory.newDocumentBuilder()
    private val transformMap = mutableMapOf<String, String>()


    override fun apply(project: Project) {
        val extension = project.extensions.getByName("android")
        val modifyStringResKeyExtension =
            project.extensions.create("modify_string_key", ModifyStringResKeyExtension::class.java)
        val modifyStringResKeyTransform = ModifyStringResKeyTransform(project, transformMap)
        if (extension is AppExtension) {
            error("Cannot apply plugin in application module. You can apply this plugin in android library module.")
        } else if (extension is LibraryExtension) {
            extension.registerTransform(modifyStringResKeyTransform)
            extension.libraryVariants.all { libraryVariant ->
                project.tasks.findByName("package${libraryVariant.name.capitalize()}Resources")?.doLast {
                    project.logger.info("[ModifyStringResKeyPlugin]:allowlist:" + modifyStringResKeyExtension.allowlist)
                    // process string file
                    val outputDir = (it as MergeResources).outputDir.get().asFile
                    if (!outputDir.exists()) {
                        return@doLast
                    }
                    FileUtils.listFiles(outputDir, ValuesFileFilter(), TrueFileFilter.INSTANCE).forEach { file ->
                        processStringFile(file, project, modifyStringResKeyExtension)
                    }
                    project.logger.info("[ModifyStringResKeyPlugin] transformMap:$transformMap")

                    // process layout file
                    val layoutDir = File(outputDir, "layout")
                    if (layoutDir.exists()) {
                        FileUtils.listFiles(layoutDir, arrayOf("xml"), true).forEach { layoutFile ->
                            processXmlFile(layoutFile, transformMap)
                        }
                    }

                    // process menu file
                    val menuDir = File(outputDir, "menu")
                    if (menuDir.exists()) {
                        FileUtils.listFiles(menuDir, arrayOf("xml"), true).forEach { menuFile ->
                            processXmlFile(menuFile, transformMap)
                        }
                    }

                }
                project.tasks.findByName("process${libraryVariant.name.capitalize()}Manifest")?.doLast {
                    // process AndroidManifest file
                    val manifestFile =
                        File("${project.buildDir}/intermediates/library_manifest/${libraryVariant.name}/AndroidManifest.xml")
                    processXmlFile(manifestFile, transformMap)
                }
                project.tasks.findByName("generate${libraryVariant.name.capitalize()}RFile")?.doFirst {
                    val file =
                        File("${project.buildDir}/intermediates/local_only_symbol_list/${libraryVariant.name}/R-def.txt")
                    transformMap.forEach {
                        file.appendText("string ${it.key}")
                        file.appendText(System.lineSeparator())
                    }

                }

            }
        }

    }

    private fun processStringFile(
        file: File,
        project: Project,
        modifyStringResKeyExtension: ModifyStringResKeyExtension
    ) {
        val document = domBuilder.parse(file)
        val documentElement = document.documentElement
        val stringNodes = documentElement.getElementsByTagName("string")
        project.logger.info("[ModifyStringResKeyPlugin]stringNodes:${stringNodes}")
        stringNodes.forEach { node ->
            node.attributes.getNamedItem("name")?.let { nameItem ->
                if (modifyStringResKeyExtension.allowlist?.contains(nameItem.nodeValue) == true) {
                    project.logger.info("[ModifyStringResKeyPlugin]:${nameItem.nodeValue} skipped because in allowlist")
                    return@let
                } else {
                    project.logger.info("[ModifyStringResKeyPlugin]:${nameItem.nodeValue} not skipped because not in allowlist")
                }
                checkNotNull(modifyStringResKeyExtension.transform) {
                    "You have to specify a transform rule in the modify_string_key block."
                }
                if (modifyStringResKeyExtension.match == null) {
                    processNode(project, file, nameItem, modifyStringResKeyExtension.transform!!)
                } else {
                    val regex = Regex(modifyStringResKeyExtension.match!!)
                    if (regex.matches(nameItem.nodeValue)) {
                        processNode(project, file, nameItem, modifyStringResKeyExtension.transform!!)
                    }
                }
            }
        }
        val transformer: Transformer = TransformerFactory.newInstance().newTransformer()
        val output = StreamResult(file)
        val input = DOMSource(document)
        transformer.transform(input, output)
    }

    private fun processXmlFile(file: File?, transformMap: Map<String, String>) {
        if (file == null) {
            return
        }
        val document = domBuilder.parse(file)
        getMatchedNodes(document.documentElement, transformMap)
        val transformer: Transformer = TransformerFactory.newInstance().newTransformer()
        val output = StreamResult(file)
        val input = DOMSource(document)
        transformer.transform(input, output)
    }

    private fun getMatchedNodes(node: Node, transformMap: Map<String, String>) {
        node.attributes?.forEach {
            if (!it.nodeValue.startsWith("@string/")) {
                return@forEach
            }
            val value = it.nodeValue.substring(8)
            if (transformMap.containsKey(value)) {
                it.nodeValue = "@string/${transformMap[value]}"
            }
        }
        if (node.hasChildNodes()) {
            node.childNodes.forEach {
                getMatchedNodes(it, transformMap)
            }
        }

    }

    private fun processNode(project: Project, file: File, nameItem: Node, transform: String) {
        assert(transform.contains("{original}", true)) {
            "transform have to contain a {original} part."
        }
        val originalText: String = nameItem.nodeValue
        nameItem.nodeValue = transform.replace("{original}", originalText, true)
        transformMap[originalText] = nameItem.nodeValue
        project.logger.info("[ModifyStringResKeyPlugin]:" + file.name + ", transform $originalText to ${nameItem.nodeValue}")
    }
}