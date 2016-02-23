package ru.serdtsev.homemoney.dto

import java.util.*

data class Authentication(var userId: UUID, var bsId: UUID, var token: UUID)
