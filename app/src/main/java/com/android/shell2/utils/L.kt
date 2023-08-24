package com.android.shell2.utils

import android.os.Environment
import android.text.TextUtils
import com.github.h4de5ing.base.CommandResult
import com.github.h4de5ing.base.exec
import com.github.h4de5ing.base.now
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream

fun commandPath(): String =
    Environment.getExternalStorageDirectory().absolutePath + File.separator + "init.txt"

fun runCommand(isLog: Boolean = true, change: ((String) -> Unit) = {}) {
    try {
        val path = commandPath()
        if (File(path).exists()) {
            BufferedInputStream(FileInputStream(path)).buffered().reader().readLines().forEach {
                println("执行命令:${it}")
                val result = exec(it)
                if (isLog) result.log(it)
                change("${result.successMsg}\n${result.errorMsg}")
            }
        } else println("init.txt 文件不存在")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun CommandResult.log(command: String): String {
    val log = StringBuilder()
    log.append("-------------${now()}\n")
    log.append("${command}->${this.result}\n")
    if (this.result == 0) {
        if (!TextUtils.isEmpty(this.successMsg))
            log.append("success:${this.successMsg}\n")
    } else {
        if (!TextUtils.isEmpty(this.errorMsg))
            log.append("error:${this.errorMsg}\n")
    }
    log.append("-------------\n")
    println("命令执行结果:${log}")
    File(Environment.getExternalStorageDirectory().absolutePath + File.separator + "log.txt")
        .writeText(log.toString())
    return log.toString()
}