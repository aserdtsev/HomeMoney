package ru.serdtsev.homemoney.moneyoper.dto

import ru.serdtsev.homemoney.moneyoper.model.CategoryType
import java.util.*

class TagDto(val id: UUID, val name: String, val rootId: UUID?, val isCategory: Boolean?, val categoryType: CategoryType?,
             val isArc: Boolean?)