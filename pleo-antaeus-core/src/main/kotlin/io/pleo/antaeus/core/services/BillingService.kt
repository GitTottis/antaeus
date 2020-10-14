package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import java.util.*

class BillingService(
    private val paymentProvider: PaymentProvider
) {
    // TODO - Add code e.g. here
    fun schedule() {
        return println("Payments Scheduled")
    }

    fun getStatus() {
        return println("Payments Status")
    }
}
