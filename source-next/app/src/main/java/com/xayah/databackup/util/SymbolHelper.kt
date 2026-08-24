package com.xayah.databackup.util

object SymbolHelper {
    const val USD = '$'
    const val BACKSLASH = '\\'
    const val QUOTE = '"'
    const val LF = '\n'
    const val DOT = '•'
    const val PERCENT = '%'

    /**
     * POSIX sh single-quote escaping.
     * Example: a'b -> 'a'\''b'
     */
    fun shellQuote(value: String): String = "'${value.replace("'", "'\\''")}'"
}
