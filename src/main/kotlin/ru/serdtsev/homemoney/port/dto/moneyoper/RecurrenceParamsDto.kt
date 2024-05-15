package ru.serdtsev.homemoney.port.dto.moneyoper

interface IRecurrenceParamsDto

class RecurrenceParamsDto(val type: String, val data: IRecurrenceParamsDto)

class DayRecurrenceParamsDto(var n: Int = 1): IRecurrenceParamsDto