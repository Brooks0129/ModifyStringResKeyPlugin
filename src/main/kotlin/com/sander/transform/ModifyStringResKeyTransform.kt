package com.sander.transform

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FileUtils.listFiles
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.File


class ModifyStringResKeyTransform(private val project: Project, private val transformMap: MutableMap<String, String>) :
    Transform() {

    override fun getName(): String {
        return "ModifyStringResKey"
    }

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_CLASS
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return TransformManager.PROJECT_ONLY
    }

    override fun isIncremental(): Boolean {
        return true
    }

    override fun transform(transformInvocation: TransformInvocation?) {
        super.transform(transformInvocation)
        if (transformInvocation?.isIncremental != true) {
            transformInvocation?.outputProvider?.deleteAll();
        }
        transformInvocation?.inputs?.forEach { input ->
            input.jarInputs.forEach { jarInput ->
                val dest = transformInvocation.outputProvider.getContentLocation(
                    jarInput.name,
                    jarInput.contentTypes,
                    jarInput.scopes,
                    Format.JAR
                )
                if (transformInvocation.isIncremental) {
                    when (jarInput.status) {
                        Status.ADDED -> FileUtils.copyFile(jarInput.file, dest)
                        Status.CHANGED -> FileUtils.copyFile(jarInput.file, dest)
                        Status.REMOVED -> if (dest.exists()) {
                            dest.delete()
                        }
                        Status.NOTCHANGED -> {}
                        else -> {}
                    }
                } else {
                    FileUtils.copyFile(jarInput.file, dest)
                }
            }
            input.directoryInputs.forEach { directoryInput ->
                val dest = transformInvocation.outputProvider.getContentLocation(
                    directoryInput.name,
                    directoryInput.contentTypes,
                    directoryInput.scopes,
                    Format.DIRECTORY
                )
                if (transformInvocation.isIncremental) {
                    val changedFiles = directoryInput.changedFiles
                    changedFiles.forEach { (changedFile, status) ->
                        val destFilePath: String =
                            changedFile.absolutePath.replace(directoryInput.file.absolutePath, dest.absolutePath)
                        val destFile = File(destFilePath)
                        when (status) {
                            Status.NOTCHANGED -> {}
                            Status.ADDED -> {
                                FileUtils.touch(destFile)
                                processTransformClass(changedFile, destFile)
                                project.logger.info("ModifyStringResKeyTransform ADDED copyFile:${changedFile.absolutePath} to ${destFile.absolutePath}")
                            }
                            Status.CHANGED -> {
                                FileUtils.touch(destFile)
                                processTransformClass(changedFile, destFile)
                                project.logger.info("ModifyStringResKeyTransform CHANGED copyFile:${changedFile.absolutePath} to ${destFile.absolutePath}")
                            }
                            Status.REMOVED -> {
                                FileUtils.forceDelete(destFile)
                            }
                            else -> {}
                        }


                    }
                } else {
                    val extensions: Array<String> = arrayOf("class")
                    listFiles(directoryInput.file, extensions, true).forEach { file ->
                        val destFilePath: String =
                            file.absolutePath.replace(directoryInput.file.absolutePath, dest.absolutePath)
                        val destFile = File(destFilePath)
                        FileUtils.touch(destFile)
                        project.logger.info("ModifyStringResKeyTransform not Incremental copyFile:${file.absolutePath} to ${destFile.absolutePath}")
                        processTransformClass(file, destFile)
                    }

                }
            }

        }
    }

    private fun processTransformClass(file: File, des: File) {
        val classReader = ClassReader(file.readBytes())
        val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
        classReader.accept(
            ModifyStringResKeyClassVisitor(classWriter, transformMap),
            0
        )
        FileUtils.writeByteArrayToFile(des, classWriter.toByteArray())
    }

}
