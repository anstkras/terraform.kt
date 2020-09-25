package io.terraformkt.wrapper

import io.terraformkt.terraform.TFFile
import io.terraformkt.wrapper.utils.Archive
import io.terraformkt.wrapper.utils.CommandLine
import io.terraformkt.wrapper.utils.CommandLine.os
import io.terraformkt.wrapper.utils.Downloads
import org.codehaus.plexus.util.Os
import java.io.File
import java.net.URL

object TerraformWrapper {
    fun downloadTerraform(downloadPath: File, version: String) {
        val terraformFile = downloadPath.resolve("terraform")
        if (terraformFile.exists()) {
            return
        }

        Downloads.download(URL("https://releases.hashicorp.com/terraform/$version/terraform_${version}_$os.zip"), downloadPath, Archive.ZIP)

        if (Os.isFamily(Os.FAMILY_MAC) || Os.isFamily(Os.FAMILY_UNIX)) {
            CommandLine.execute("chmod", listOf("+x", terraformFile.absolutePath), downloadPath, false)
        }
    }

    fun downloadSchema(downloadPath: File, terraformProvider: String, schemaVersion: String) {
        val terraformPath = downloadPath.resolve("terraform").absolutePath
        createConfigFile(downloadPath, terraformProvider, schemaVersion)
        CommandLine.executeOrFail(terraformPath, listOf("init"), downloadPath, redirectStdout = true, redirectErr = true)
        CommandLine.executeOrFailToFile(
            terraformPath, listOf("providers", "schema", "-json"), downloadPath,
            downloadPath.resolve("schema.json"), redirectErr = true
        )
    }

    fun terraformApply(tfFiles: List<TFFile>, terraformExecutable: File, directoryToWriteFiles: File) {
        terraformGenerate(tfFiles, directoryToWriteFiles)
        CommandLine.executeOrFail(
            terraformExecutable.absolutePath,
            listOf("init", directoryToWriteFiles.absolutePath),
            terraformExecutable.parentFile,
            redirectStdout = true,
            redirectErr = true
        )
        CommandLine.executeOrFail(
            terraformExecutable.absolutePath,
            listOf("apply", "-auto-approve", directoryToWriteFiles.absolutePath),
            terraformExecutable.parentFile,
            redirectStdout = true
        )
    }

    fun terraformPlan(tfFiles: List<TFFile>, terraformExecutable: File, directoryToWriteFiles: File) {
        terraformGenerate(tfFiles, directoryToWriteFiles)
        CommandLine.executeOrFail(
            terraformExecutable.absolutePath,
            listOf("init", directoryToWriteFiles.absolutePath),
            terraformExecutable.parentFile,
            redirectStdout = true,
            redirectErr = true
        )
        CommandLine.executeOrFail(
            terraformExecutable.absolutePath,
            listOf("plan", directoryToWriteFiles.absolutePath),
            terraformExecutable.parentFile,
            redirectStdout = true
        )
    }

    fun terraformGenerate(tfFiles: List<TFFile>, directoryToWriteFiles: File) {
        directoryToWriteFiles.mkdirs()
        tfFiles.forEach { file -> file.writeToDirectory(directoryToWriteFiles) }
    }

    fun createConfigFile(downloadPath: File, terraformProvider: String, schemaVersion: String) {
        val configFile = downloadPath.resolve("config.tf")
        configFile.createNewFile()
        configFile.writeText(
            """
        |provider "$terraformProvider" {
        |    version = "$schemaVersion"
        |}
        """.trimMargin()
        )
    }
}