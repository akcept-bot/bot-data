/*
 * Copyright (c) 2023 Mark Dubkov. All rights reserved.
 */

/** you can edit [NEEDED KOTLIN DEPENDENCIES]*/
import kotlin.math.absoluteValue
import kotlin.math.pow

/** CUSTOM TUPES (cannot be edited) [PROJECT DEPENDENCIES] */
import kotlinx.datetime.*
import kotlinx.serialization.json.*
import dev.inmo.tgbotapi.extensions.utils.formatting.*
import dev.inmo.tgbotapi.types.message.textsources.*
import ru.kramlex.akcept.bot.utils.StringListScriptWrapper

/** EDIT ZONE */

val MONTH_COUNT = 12
val PERCENT_MAX = 100

data class DifferencePayment(
    val date: LocalDate,
    val amount: Double,
    val daysCount: Int,
    val accumulated: Double,
) {
    val stringAmount: String
        get() = amount.roundToStringRub()

    val stringAccumulated: String
        get() = accumulated.roundToStringRub()
}

fun Double.roundToRub(): Double {
    return this.roundToStringRub().toDouble()
}

fun Double.roundToStringRub(): String {
    val primaryPart = this.toInt()
    val additionalPart = (this - primaryPart)

    val additionalRounded =
        if ((additionalPart * 1000).toInt() % 10 >= 5) (additionalPart * 100).toInt() + 1
        else (additionalPart * 100).toInt()
    val additionalRoundedString = additionalRounded
        .toString()
        .padStart(2, '0')

    return "$primaryPart.$additionalRoundedString"
}

fun LocalDate.toHumanReadable(): String {
    return listOf(
        dayOfMonth.toString().padStart(2, '0'),
        monthNumber.toString().padStart(2, '0'),
        year.toString()
    ).joinToString(".")
}

fun calculateAnnuitentlyPayments(
    loanAmount: Double, // rub
    loanPeriod: Int, // months
    percent: Double, // in percent
): Double {
    val monthlyPercent: Double = percent / PERCENT_MAX / MONTH_COUNT
    val k: Double =
        monthlyPercent * (1 + monthlyPercent).pow(loanPeriod) /
                ((1 + monthlyPercent).pow(loanPeriod) - 1)
    val monthlyPayment = loanAmount * k
    return monthlyPayment.roundToRub()
}

fun calculateDifferencePayments(
    loanAmount: Double, // rub
    loanPeriod: Int, // months
    percent: Double, // in percent,
    startDate: LocalDate,
): List<DifferencePayment> {

    val mutableList: MutableList<DifferencePayment> = mutableListOf()
    val primaryMonthlyPayment: Double = loanAmount / loanPeriod
    repeat(loanPeriod) { monthNumber ->
        val paymentDate = startDate.plus(DateTimeUnit.MONTH * (monthNumber + 1))
        val startMonthDate =
            if (monthNumber == 0) startDate
            else startDate.plus(DateTimeUnit.MONTH * monthNumber)
        val dayCount = startMonthDate
            .daysUntil(paymentDate).absoluteValue
        val remains = loanAmount - monthNumber * primaryMonthlyPayment
        val accumulated = remains * percent * dayCount / 365 / PERCENT_MAX
        val payment = (accumulated + primaryMonthlyPayment)

        val differencePayment = DifferencePayment(
            date = paymentDate,
            amount = payment,
            daysCount = dayCount,
            accumulated = accumulated
        )
        mutableList.add(differencePayment)
    }
    return mutableList.toList()
}

// =======


fun calculatePaymentsFromJson(
    jsonObject: JsonObject,
): List<TextSourcesList> {

    val loanType: String = jsonObject["loanType"]
        ?.jsonPrimitive?.contentOrNull
        ?: throw IllegalStateException("loanType not exist in jsonObject")

    val loanAmount: Double = jsonObject["loanAmount"]
        ?.jsonPrimitive?.double
        ?: throw IllegalStateException("loanAmount not exist in jsonObject")

    val month: Int = jsonObject["months"]
        ?.jsonPrimitive?.int
        ?: throw IllegalStateException("loanPeriod not exist in jsonObject")

    val percent: Double = jsonObject["bid"]
        ?.jsonPrimitive?.double
        ?: throw IllegalStateException("percent not exist in jsonObject")

    when (loanType) {
        "Annuitenly" -> {
            val value = calculateAnnuitentlyPayments(
                loanAmount = loanAmount, loanPeriod = month, percent = percent
            )

            return listOf(
                buildEntities {
                    +"Ежемесячный платеж составит " + bold("$value ₽")
                }
            )
        }

        "Difference" -> {
            val date: LocalDate = jsonObject["date"]
                ?.jsonPrimitive?.contentOrNull
                ?.toLocalDate()
                ?: throw IllegalStateException("percent not exist in jsonObject")

            val results = calculateDifferencePayments(
                loanAmount = loanAmount, loanPeriod = month, percent = percent, startDate = date
            )

            val stringsSources: MutableList<TextSourcesList> = mutableListOf()

            buildEntities {
                +"Платежи рассчитаны от ${date.toHumanReadable()}"
                +newLine
                +newLine
            }.also { stringsSources.add(it) }

            results.chunked(20).forEachIndexed { chunkIndex, payments ->
                buildEntities {
                    payments.forEachIndexed { index, payment ->
                        +"Платеж в ${chunkIndex * 20 + index + 1} месяц (${payment.date.toHumanReadable()}) составит " + bold(
                            "${payment.stringAmount} ₽"
                        ) + newLine
                        +"Процент составит " + bold("${payment.stringAccumulated} ₽") + newLine
                        if (payments.lastIndex != index) +newLine
                    }

                }.also { stringsSources.add(it) }
            }

            buildEntities {
                val fullAmount: String = results.sumOf { it.amount }.roundToStringRub()
                val fullAccumulated: String = results.sumOf { it.accumulated }.roundToStringRub()
                +newLine + "Общая сумма к выплате " + bold("$fullAmount ₽") + newLine
                +"Общая сумма накопившихся процентов равна " + bold("$fullAccumulated ₽") + newLine
            }.also { stringsSources.add(it) }

            return stringsSources.toList()
        }

        else -> throw IllegalStateException("loanType is illegal")
    }
}

/** END EDIT ZONE */

StringListScriptWrapper(
    calculation = ::calculatePaymentsFromJson
)
