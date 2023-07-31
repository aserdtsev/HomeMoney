package ru.serdtsev.homemoney.domain.repository

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service

@Service
class RepositoryRegistry(
    val moneyOperRepository: MoneyOperRepository
) {
    @PostConstruct
    fun init() {
        instance = this
    }

    companion object {
        lateinit var instance: RepositoryRegistry
    }
}