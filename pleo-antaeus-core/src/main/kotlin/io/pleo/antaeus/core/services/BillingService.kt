package io.pleo.antaeus.core.services

import java.util.*
import java.util.Queue
import java.util.LinkedList

import kotlin.concurrent.schedule

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.models.Invoice

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val customerService: CustomerService,
    private val invoiceService: InvoiceService
) {
    val invoices: Queue<Invoice> = LinkedList<Invoice>()
    var timer: Timer = Timer(true)

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
    
    internal fun processPayments() {
            try {
                println("Next Billing Round will run on: " + getNextBillingDate())
            } finally {
                setNextPaymentsTimer()
            }
    }

    internal fun setNextPaymentsTimer() {
        // Create new Timer
        timer = Timer("PayTimerThread", true)
        // timer.schedule(3000) {processPayments()}
        timer.schedule(getNextBillingDate()) {processPayments()}
    }

    fun schedulePays() : Queue<Invoice>  {
        // Load Invoices
        if (invoices.isEmpty()) {
            val invoiceIterator = invoiceService.fetchAll().iterator()
            while(invoiceIterator.hasNext()) {
                invoices.add(invoiceIterator.next())
            }
        }
        setNextPaymentsTimer()
        return invoices
    }

}
