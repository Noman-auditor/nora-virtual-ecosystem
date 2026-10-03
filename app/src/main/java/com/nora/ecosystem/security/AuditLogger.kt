package com.nora.ecosystem.security

import com.nora.ecosystem.data.AuditDao
import com.nora.ecosystem.data.AuditEventEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuditLogger @Inject constructor(private val dao: AuditDao) {
    private val secretPattern = Regex("(?i)(password|token|secret|key)\\s*[=:]\\s*\\S+")
    suspend fun log(action: String, details: String = "", actor: String = "local") {
        dao.insert(AuditEventEntity(ts = System.currentTimeMillis(), actor = actor,
            action = action, details = details.replace(secretPattern, "$1=[REDACTED]")))
    }
}
