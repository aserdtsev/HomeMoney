package ru.serdtsev.homemoney.utils

import com.opentable.db.postgres.embedded.FlywayPreparer
import com.opentable.db.postgres.junit5.EmbeddedPostgresExtension
import com.opentable.db.postgres.junit5.PreparedDbExtension
import org.junit.jupiter.api.extension.RegisterExtension

class TestHelper {
    companion object {
        val db: PreparedDbExtension = EmbeddedPostgresExtension.preparedDatabase(
            FlywayPreparer.forClasspathLocation("db/migration"))
    }

}