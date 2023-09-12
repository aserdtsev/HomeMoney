package ru.serdtsev.homemoney.infra.utils

import com.google.gson.Gson
import org.postgresql.util.PGobject
import java.time.LocalDate

fun Gson.toJsonb(src: Any): PGobject {
    val json = this.toJson(src)
    return PGobject().apply { type = "jsonb"; this.value = json }
}

operator fun ClosedRange<LocalDate>.iterator() : Iterator<LocalDate>{
    return object: Iterator<LocalDate> {
        private var next = this@iterator.start
        private val finalElement = this@iterator.endInclusive
        private var hasNext = !next.isAfter(this@iterator.endInclusive)

        override fun hasNext(): Boolean = hasNext

        override fun next(): LocalDate {
            val value = next
            if (value == finalElement) {
                hasNext = false
            } else {
                next = next.plusDays(1)
            }
            return value
        }
    }
}

