/*
 * Copyright (c) 2023 Mark Dubkov. All rights reserved.
 */

/** you can edit [NEEDED KOTLIN DEPENDENCIES]*/

// nothing

/** CUSTOM TUPES (cannot be edited) [PROJECT DEPENDENCIES] */
import kotlinx.serialization.json.*
import dev.inmo.tgbotapi.types.message.textsources.*
import dev.inmo.tgbotapi.utils.buildEntities
import ru.kramlex.akcept.bot.data.ApplicationData
import ru.kramlex.akcept.bot.utils.StringListScriptWrapper

/** EDIT ZONE */

private enum class LoanProgram(private val value: String) {
    Primary("Primary"),
    StateMortage("StateMortage"),
    FamilyMortage("FamilyMortage"),
    FamilyRefinancing("FamilyRefinancing"),
    IT("IT"),
    Refinancing("Refinancing"),
    SecuredByRealEstate("SecuredByRealEstate"),
    Comerce("Comerce"),
    Autoparking("Autoparking"),
    SecondaryRoom("SecondaryRoom"),
    SecondaryFlat("SecondaryFlat"),
    SecondaryApartments("SecondaryApartments"),
    SecondaryHouses("SecondaryHouses");

    companion object {
        fun fromString(string: String): LoanProgram? =
            LoanProgram.values().firstOrNull { it.value == string }
    }
}

private enum class InitialPayment(private val value: String) {
    FromTen("FromTen"),
    FromFifteen("FromFifteen"),
    FromTwenty("FromTwenty"),
    FromThirty("FromThirty"),
    FromForty("FromForty"),
    FromFifty("FromFifty");

    companion object {
        fun fromString(string: String): InitialPayment? =
            InitialPayment.values().firstOrNull { it.value == string }
    }
}

private enum class KZ(private val value: String) {
    FromTenToEighty("FromTenToEighty"),
    FromEightyToNinety("FromEightyToNinety"),
    FromTenToFifty("FromTenToFifty"),
    FromFiftyToSeventy("FromFiftyToSeventy");
    companion object {
        fun fromString(string: String): KZ? =
            KZ.values().firstOrNull { it.value == string }
    }
}

private data class ValueAndVarningKey(
    val valueKey: String,
    val warningKey: String? = null
)

fun calculatePaymentsFromJson(
    jsonObject: JsonObject,
): List<TextSourcesList> = try {
    val isBankClient: Boolean = jsonObject["isBankClient"]
        ?.jsonPrimitive?.booleanOrNull
        ?: throw IllegalStateException("correct isBankClient not exist in jsonObject")

    val loanProgram: LoanProgram = jsonObject["loanProgram"]
        ?.jsonPrimitive?.contentOrNull?.let { stringValue ->
            LoanProgram.fromString(stringValue)
        } ?: throw IllegalStateException("correct loadProgram not exist in jsonObject")

    val needKz: Boolean = when (loanProgram) {
        LoanProgram.FamilyRefinancing, LoanProgram.Refinancing, LoanProgram.SecuredByRealEstate -> true
        else -> false
    }

    val valueAndWarning: ValueAndVarningKey = if (needKz) {

        val kz: KZ = jsonObject["KZ"]
            ?.jsonPrimitive?.contentOrNull?.let { stringValue ->
                KZ.fromString(stringValue)
            } ?: throw IllegalStateException("correct KZ not exist in jsonObject")

        when (loanProgram) {
            LoanProgram.FamilyRefinancing -> ValueAndVarningKey(
                valueKey = "Rate.FamilyMortageRefinancing",
                warningKey = null
            )


            LoanProgram.Refinancing -> when (kz) {


                KZ.FromTenToEighty -> if (isBankClient) {
                    ValueAndVarningKey(
                        valueKey = "Rate.Refinancing.IsBankClient.FromTenToEighty",
                        warningKey = null
                    )
                } else {
                    ValueAndVarningKey(
                        valueKey = "Rate.Refinancing.IsNotBankClient.FromTenToEighty",
                        warningKey = null
                    )
                }


                KZ.FromEightyToNinety -> if (isBankClient) {
                    ValueAndVarningKey(
                        valueKey = "Rate.Refinancing.IsBankClient.FromEightyToNinety",
                        warningKey = null
                    )
                } else {
                    ValueAndVarningKey(
                        valueKey = "Rate.Refinancing.IsNotBankClient.FromEightyToNinety",
                        warningKey = null
                    )
                }
                else -> throw IllegalStateException("invalid kz for loanProgram")
            }


            LoanProgram.SecuredByRealEstate -> when (kz) {
                KZ.FromTenToFifty -> if (isBankClient) {
                    ValueAndVarningKey(
                        valueKey = "Rate.SecuredByRealEstate.IsBankClient.FromTenToFifty",
                        warningKey = null
                    )
                } else {
                    ValueAndVarningKey(
                        valueKey = "Rate.SecuredByRealEstate.IsNotBankClient.FromTenToFifty",
                        warningKey = null
                    )
                }
                KZ.FromFiftyToSeventy -> if (isBankClient) {
                    ValueAndVarningKey(
                        valueKey = "Rate.SecuredByRealEstate.IsBankClient.FromFiftyToSeventy",
                        warningKey = null
                    )
                } else {
                    ValueAndVarningKey(
                        valueKey = "Rate.SecuredByRealEstate.IsNotBankClient.FromFiftyToSeventy",
                        warningKey = null
                    )
                }
                else -> throw IllegalStateException("invalid kz for loanProgram")
            }


            else -> throw IllegalStateException("invalid need kz state")
        }
    } else {

        val initialPayment: InitialPayment = jsonObject["initialPayment"]
            ?.jsonPrimitive?.contentOrNull?.let { stringValue ->
                InitialPayment.fromString(stringValue)
            } ?: throw IllegalStateException("correct initialPayment not exist in jsonObject")

        when (loanProgram) {

            LoanProgram.Primary -> when (initialPayment) {

                InitialPayment.FromTen -> if (isBankClient) {
                    ValueAndVarningKey(
                        valueKey = "Rate.Primary.isBankClient.FromTen",
                        warningKey = null
                    )
                } else {
                    ValueAndVarningKey(
                        valueKey = "Rate.Primary.isNotBankClient.FromTen",
                        warningKey = null
                    )
                }

                InitialPayment.FromTwenty -> if (isBankClient) {
                    ValueAndVarningKey(
                        valueKey = "Rate.Primary.isBankClient.FromTwenty",
                        warningKey = null
                    )
                } else {
                    ValueAndVarningKey(
                        valueKey = "Rate.Primary.isNotBankClient.FromTwenty",
                        warningKey = null
                    )
                }


                else -> throw IllegalStateException("invalid initialPayment for loanProgram")
            }


            LoanProgram.StateMortage -> when (initialPayment) {

                InitialPayment.FromFifteen -> ValueAndVarningKey(
                    valueKey = "Rate.MortageWithStateSupport.FromFifteen",
                    warningKey = null
                )


                else -> throw IllegalStateException("invalid initialPayment for loanProgram")
            }


            LoanProgram.FamilyMortage -> when (initialPayment) {
                InitialPayment.FromFifteen -> ValueAndVarningKey(
                    valueKey = "Rate.FamilyMortage.FromFifteen",
                    warningKey = null
                )


                InitialPayment.FromTwenty -> ValueAndVarningKey(
                    valueKey = "Rate.FamilyMortage.FromTwenty",
                    warningKey = null
                )


                else -> throw IllegalStateException("invalid initialPayment for loanProgram")
            }


            LoanProgram.IT -> when (initialPayment) {

                InitialPayment.FromFifteen -> ValueAndVarningKey(
                    valueKey = "Rate.ITMortage",
                    warningKey = null
                )

                else -> throw IllegalStateException("invalid initialPayment for loanProgram")
            }


            LoanProgram.Comerce -> when (initialPayment) {

                InitialPayment.FromThirty -> if (isBankClient) {
                    ValueAndVarningKey(
                        valueKey = "Rate.Commercial.FromTwenty",
                        warningKey = null
                    )
                } else {
                    ValueAndVarningKey(
                        valueKey = "Rate.Commercial.FromTwenty",
                        warningKey = null
                    )
                }

                else -> throw IllegalStateException("invalid initialPayment for loanProgram")
            }


            LoanProgram.Autoparking -> when (initialPayment) {

                InitialPayment.FromForty -> ValueAndVarningKey(
                    valueKey = "Rate.Parking",
                    warningKey = null
                )

                else -> throw IllegalStateException("invalid initialPayment for loanProgram")
            }


            LoanProgram.SecondaryRoom -> when (initialPayment) {
                InitialPayment.FromTen -> if (isBankClient) {
                    ValueAndVarningKey(
                        valueKey = "Rate.SecondaryRoom.IsBankClient.FromTen",
                        warningKey = null
                    )
                } else {
                    ValueAndVarningKey(
                        valueKey = "Rate.SecondaryRoom.IsNotBankClient.FromTen",
                        warningKey = null
                    )
                }


                InitialPayment.FromTwenty -> if (isBankClient) {
                    ValueAndVarningKey(
                        valueKey = "Rate.SecondaryRoom.IsBankClient.FromTwenty",
                        warningKey = null
                    )
                } else {
                    ValueAndVarningKey(
                        valueKey = "Rate.SecondaryRoom.IsNotBankClient.FromTwenty",
                        warningKey = null
                    )
                }


                else -> throw IllegalStateException("invalid initialPayment for loanProgram")
            }


            LoanProgram.SecondaryFlat -> when (initialPayment) {
                InitialPayment.FromTen -> if (isBankClient) {
                    ValueAndVarningKey(
                        valueKey = "Rate.SecondaryFlat.IsBankClient.FromTen",
                        warningKey = null
                    )
                } else {
                    ValueAndVarningKey(
                        valueKey = "Rate.SecondaryFlat.IsNotBankClient.FromTen",
                        warningKey = null
                    )
                }


                InitialPayment.FromTwenty -> if (isBankClient) {
                    ValueAndVarningKey(
                        valueKey = "Rate.SecondaryFlat.IsBankClient.FromTwenty",
                        warningKey = null
                    )
                } else {
                    ValueAndVarningKey(
                        valueKey = "Rate.SecondaryFlat.IsNotBankClient.FromTwenty",
                        warningKey = null
                    )
                }


                else -> throw IllegalStateException("invalid initialPayment for loanProgram")
            }


            LoanProgram.SecondaryApartments -> when (initialPayment) {

                InitialPayment.FromThirty -> if (isBankClient) {
                    ValueAndVarningKey(
                        valueKey = "Rate.SecondaryApartments.IsBankClient",
                        warningKey = null
                    )
                } else {
                    ValueAndVarningKey(
                        valueKey = "Rate.SecondaryApartments.IsNotBankClient",
                        warningKey = null
                    )
                }


                else -> throw IllegalStateException("invalid initialPayment for loanProgram")
            }


            LoanProgram.SecondaryHouses -> when (initialPayment) {

                InitialPayment.FromFifty -> if (isBankClient) {
                    ValueAndVarningKey(
                        valueKey = "Rate.SecondaryHouses.IsBankClient",
                        warningKey = null
                    )
                } else {
                    ValueAndVarningKey(
                        valueKey = "Rate.SecondaryHouses.IsNotBankClient",
                        warningKey = null
                    )
                }


                else -> throw IllegalStateException("invalid initialPayment for loanProgram")
            }

            else -> throw IllegalStateException("invalid need initialPayment state")
        }
    }

    buildList {
        val warningMessage: String? = valueAndWarning.warningKey?.let { ApplicationData.getNullableValue(it) }
        val rateValue: String? = valueAndWarning.valueKey?.let { ApplicationData.getNullableValue(it) }

        if (warningMessage != null) {
            buildEntities {
                + bold("[ПРЕДУПРЕЖДЕНИЕ]") + " " + warningMessage
            }.also { add(it) }
        }

        if (rateValue != null) {
            buildEntities {
                + "Ставка по кредиту составит: " + bold("$rateValue%")
            }.also { add(it) }
        }
    }
} catch (error: Exception) {
    buildList {
        buildEntities {
            +"Произошла ошибка при вычислении, попробуйте еще раз!"
        }.also { add(it) }
    }
}


/** END EDIT ZONE */

StringListScriptWrapper(
    calculation = ::calculatePaymentsFromJson
)
