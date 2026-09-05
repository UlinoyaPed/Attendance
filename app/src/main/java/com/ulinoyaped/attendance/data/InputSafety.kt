package com.ulinoyaped.attendance.data

import java.io.InputStream
import java.io.ByteArrayOutputStream

const val MAX_ROSTER_BYTES = 1024 * 1024
const val MAX_BACKUP_BYTES = 8 * 1024 * 1024
const val MAX_STUDENTS = 5000

fun requireSafeField(value: String, label: String, max: Int = 120, required: Boolean = true) {
    require(!required || value.isNotBlank()) { "$label 不能为空" }
    require(value.length <= max && value.none { it.isISOControl() || it == '\u2028' || it == '\u2029' }) {
        "$label 过长或包含控制字符"
    }
}

fun InputStream.readBoundedText(limit: Int): String {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    var total = 0
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        total += count
        require(total <= limit) { "文件超过大小限制（${limit / 1024} KB）" }
        output.write(buffer, 0, count)
    }
    return output.toByteArray().toString(Charsets.UTF_8)
}

// Parse complete CSV records before validating name/number fields. Quoted line
// breaks remain part of one field and are rejected, never turned into students.
fun parseRosterRecords(text: String, delimiter: Char): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    val row = mutableListOf<String>()
    val field = StringBuilder()
    var quoted = false
    var closedQuote = false
    var i = 0
    fun endField() {
        require(row.size < 16) { "名单列数过多" }
        requireSafeField(field.toString(), "名单字段", 512, false)
        row += field.toString().trim()
        field.clear()
        closedQuote = false
    }
    fun endRow() {
        endField()
        if (row.any { it.isNotEmpty() }) rows += row.toList()
        row.clear()
        require(rows.size <= MAX_STUDENTS + 1) { "名单最多 $MAX_STUDENTS 人" }
    }
    while (i < text.length) {
        val c = text[i]
        if (quoted) {
            if (c == '"') {
                if (text.getOrNull(i + 1) == '"') { field.append('"'); i++ }
                else { quoted = false; closedQuote = true }
            } else field.append(c)
        } else when {
            c == delimiter -> endField()
            c == '\n' || c == '\r' -> {
                endRow()
                if (c == '\r' && text.getOrNull(i + 1) == '\n') i++
            }
            c == '"' && field.isEmpty() && !closedQuote -> quoted = true
            else -> {
                require(!closedQuote && c != '"') { "CSV 引号格式无效" }
                field.append(c)
            }
        }
        require(field.length <= 512) { "名单字段过长" }
        i++
    }
    require(!quoted) { "CSV 引号未闭合" }
    if (field.isNotEmpty() || row.isNotEmpty() || closedQuote) endRow()
    return rows
}

fun safeCsvCell(value: String): String {
    requireSafeField(value, "导出字段", required = false)
    val text = if (value.trimStart().firstOrNull() in listOf('=', '+', '-', '@')) "'$value" else value
    return "\"${text.replace("\"", "\"\"")}\""
}

fun requireShallowJson(text: String) {
    var depth = 0
    var quoted = false
    var escaped = false
    for (c in text) {
        if (quoted) {
            if (escaped) escaped = false
            else if (c == '\\') escaped = true
            else if (c == '"') quoted = false
        } else when (c) {
            '"' -> quoted = true
            '{', '[' -> { depth++; require(depth <= 32) { "备份嵌套层数过多" } }
            '}', ']' -> { depth--; require(depth >= 0) { "备份格式无效" } }
        }
    }
    require(depth == 0 && !quoted) { "备份格式不完整" }
}
