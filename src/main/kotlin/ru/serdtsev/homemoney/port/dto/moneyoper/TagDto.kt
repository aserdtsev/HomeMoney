package ru.serdtsev.homemoney.port.dto.moneyoper

import ru.serdtsev.homemoney.domain.model.moneyoper.CategoryType
import java.util.*

class TagDto(val id: UUID, val name: String, val rootId: UUID?, val isCategory: Boolean?, val categoryType: CategoryType?,
             val isArc: Boolean?)