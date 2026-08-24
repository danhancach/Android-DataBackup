package com.xayah.databackup.util

import com.topjohnwu.superuser.Shell
import com.xayah.databackup.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RootShellResult(
    val code: Int,
    val out: List<String>,
)

object RootShellHelper {
    private const val TAG = "RootShellHelper"

    suspend fun execute(vararg args: String): RootShellResult = withContext(Dispatchers.IO) {
        val shell = runCatching { Shell.getShell() }.getOrNull()
        if (shell == null) {
            LogHelper.e(TAG, "execute", "Shell is not available.")
            return@withContext RootShellResult(code = -1, out = listOf("Shell is not available."))
        }
        val result = shell.newJob()
            .to(ArrayList<String>(), null)
            .add(args.joinToString(" "))
            .exec()
        RootShellResult(code = result.code, out = result.out)
    }

    suspend fun executeQuoted(vararg args: String): RootShellResult {
        val quoted = args.map { arg ->
            if (arg == "|" || arg == ">" || arg == "&&" || arg == "||") arg else SymbolHelper.shellQuote(arg)
        }
        return execute(*quoted.toTypedArray())
    }
}
