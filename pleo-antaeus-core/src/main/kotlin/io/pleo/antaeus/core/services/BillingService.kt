package io.pleo.antaeus.core.services

import java.util.*
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice

class BillingService(
    private val paymentProvider: PaymentProvider
) {

    internal fun getNextBillingDate() : Date {
        val cal = Calendar.getInstance().also {
            it.set(Calendar.MONTH, Calendar.MONTH+1)
            it.set(Calendar.HOUR_OF_DAY, 8)
            it.set(Calendar.DAY_OF_MONTH, it.getActualMinimum(Calendar.DAY_OF_MONTH))
        }
        return cal.getTime();
    }
    
    fun schedulePays() : String {
        return "Payments scheduled for: " + getNextBillingDate()
    }
}
