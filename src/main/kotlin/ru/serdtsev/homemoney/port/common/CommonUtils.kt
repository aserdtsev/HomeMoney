package ru.serdtsev.homemoney.port.common

import com.google.gson.Gson
import org.postgresql.util.PGobject

fun Gson.toJsonb(src: Any): PGobject {
    val json = this.toJson(src)
    return PGobject().apply { type = "jsonb"; this.value = json }
}