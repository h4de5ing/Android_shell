package com.android.shell2.utils

import android.util.Log
import com.github.h4de5ing.base.COMMAND_EXIT
import com.github.h4de5ing.base.COMMAND_LINE_END
import com.github.h4de5ing.base.COMMAND_SH
import com.github.h4de5ing.base.CommandResult
import com.github.h4de5ing.base.TAG
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStreamReader


//耗时操作可能会没有结果返回；cat大文件没有结果返回；实时查看设备信息可能无结果返回，例如top。此方法待优化。
var currentDir = "/" // 初始化为根目录
fun execCommand(command: String): String {
    var returnResult = ""
    try {
        val commandList: List<String>
        var commandResult: CommandResult?
        if (command.contains("&&")) {
            commandList = command.split("&&")
            if (commandList.isNotEmpty()) commandList.forEach {
                if (it.trim().startsWith("cd ")) {
                    val cdResult = execFile("${it.trim()} && pwd", File(currentDir))
                    if (cdResult.result == 0) {
                        currentDir = cdResult.successMsg.toString()
                    } else returnResult += cdResult.errorMsg.toString() + "\n"
                } else {
                    commandResult = execFile(it.trim(), File(currentDir))
                    returnResult += (if (commandResult?.result == 0) commandResult?.successMsg else commandResult?.errorMsg) + "\n"
                }
            }
            returnResult =
                if (returnResult.last() != '\n') returnResult
                else returnResult.substring(0, returnResult.length - 1)
        } else {
            if (command.trim().startsWith("cd ")) {
                val cdResult = execFile("${command.trim()} && pwd", File(currentDir))
                if (cdResult.result == 0) {
                    currentDir = cdResult.successMsg.toString()
                } else returnResult = cdResult.errorMsg.toString()
            } else {
                commandResult = execFile(command.trim(), File(currentDir))
                returnResult = if (commandResult?.result == 0) commandResult?.successMsg
                    ?: "" else commandResult?.errorMsg ?: ""
            }
        }
    } catch (e: Exception) {
        returnResult = "error, ${e.message}"
        e.printStackTrace()
    }
    return returnResult
}

fun execFile(command: String, file: File): CommandResult {
    val commandResult = CommandResult()
    var process: Process? = null
    var os: DataOutputStream? = null
    var successResult: BufferedReader? = null
    var errorResult: BufferedReader? = null
    val successMsg: StringBuilder
    val errorMsg: StringBuilder
    try {
        process =
            if (command.contains("|")) Runtime.getRuntime()
                .exec(arrayOf(COMMAND_SH, "-c", command), null, file)
            else Runtime.getRuntime().exec(COMMAND_SH, null, file)
        os = DataOutputStream(process.outputStream)
        os.write(command.toByteArray())
        os.writeBytes(COMMAND_LINE_END)
        os.writeBytes(COMMAND_EXIT)
        os.flush()
        commandResult.result = process.waitFor()
        successMsg = StringBuilder()
        errorMsg = StringBuilder()
        successResult = BufferedReader(InputStreamReader(process.inputStream))
        errorResult = BufferedReader(InputStreamReader(process.errorStream))
        var s: String?
        while (successResult.readLine().also { s = it } != null) successMsg.append(s).append("\n")
        while (errorResult.readLine().also { s = it } != null) errorMsg.append(s).append("\n")
        commandResult.successMsg = if (successMsg.toString().last() == '\n') successMsg.toString()
            .substring(0, successMsg.toString().length - 1) else successMsg.toString()
        commandResult.errorMsg = if (errorMsg.toString().last() == '\n') errorMsg.toString()
            .substring(0, errorMsg.toString().length - 1) else errorMsg.toString()
    } catch (e: Exception) {
        val errMsg = e.message
        if (errMsg != null) {
            Log.e(TAG, errMsg)
        } else {
            e.printStackTrace()
        }
    } finally {
        try {
            os?.close()
            successResult?.close()
            errorResult?.close()
        } catch (e: IOException) {
            val errMsg = e.message
            if (errMsg != null) {
                Log.e(TAG, errMsg)
            } else {
                e.printStackTrace()
            }
        }
        process?.destroy()
    }
    return commandResult
}