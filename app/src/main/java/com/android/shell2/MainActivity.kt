package com.android.shell2

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.TextUtils
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.android.shell2.services.ShellService
import com.android.shell2.utils.CommandExecution
import java.util.Locale

class MainActivity : Activity() {
    private var tv: TextView? = null
    private var input: EditText? = null
    private var isRoot: CheckBox? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tv = findViewById(R.id.tv)
        input = findViewById(R.id.etInput)
        isRoot = findViewById(R.id.is_root)
        tv?.movementMethod = ScrollingMovementMethod()
        input?.setOnEditorActionListener { _, actionId, _ ->
            if (EditorInfo.IME_ACTION_DONE == actionId) done()
            false
        }
        findViewById<Button>(R.id.clean).setOnClickListener { input?.setText("") }
        if (BuildConfig.DEBUG) {
            try {
                startService(Intent(this, ShellService::class.java))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val permissionRead: Int = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (permissionRead != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "正在请求权限", Toast.LENGTH_SHORT).show()
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        }
    }

    private fun updateTv(message: String) {
        try {
            runOnUiThread {
                tv?.apply {
                    append("\n${message}")
                    val offset: Int = lineCount * lineHeight - height
                    scrollTo(0, offset.coerceAtLeast(0))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.run -> done()
            R.id.clean -> tv?.text = "\$_"
        }
        return super.onOptionsItemSelected(item)
    }

    private fun done() {
        val inputStr = input?.text.toString()
        if (!TextUtils.isEmpty(inputStr)) {
            val result = CommandExecution.execCommand(
                inputStr.lowercase(Locale.getDefault()),
                isRoot?.isChecked ?: false
            )
            println(result)
            updateTv("${result.successMsg}\n${result.errorMsg}")
        } else {
            input?.requestFocus()
            input?.error = "input command"
        }
    }
}