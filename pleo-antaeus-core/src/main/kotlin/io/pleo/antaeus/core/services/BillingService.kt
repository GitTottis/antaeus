package io.pleo.antaeus.core.services

import java.util.*
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice

class BillingService(
    private val paymentProvider: PaymentProvider
) {
    internal fun getNextBillingDate() : Date {
        val cal = Calendar.getInstance().also {
            it.set(Calendar.MONTH, it.get(Calendar.MONTH)+1) // next Month
            it.set(Calendar.DAY_OF_MONTH, it.getActualMinimum(Calendar.DAY_OF_MONTH))
            it.set(Calendar.HOUR_OF_DAY, 12)
        }

        // Check if its on a weekend
        if (Calendar.SATURDAY == cal.get(Calendar.DAY_OF_WEEK)) {
            cal.add(Calendar.DAY_OF_WEEK, 2);
        } else if (Calendar.SUNDAY == cal.get(Calendar.DAY_OF_WEEK)) {
            cal.add(Calendar.DAY_OF_WEEK, 1);
        }
        return cal.getTime();
    }

    fun schedulePays() : String {
        return "Payments scheduled for: " + getNextBillingDate()
    }
}
