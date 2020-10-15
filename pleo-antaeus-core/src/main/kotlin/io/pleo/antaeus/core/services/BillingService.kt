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
    class BSResult(var nextBillDate: String = "", var cronEvtSet: Boolean = false)
    val invoices: Queue<Invoice> = LinkedList<Invoice>()
    var isTimerSet: Boolean = false
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

    internal fun setNextPaymentsTimer() : Date {
        val date: Date = getNextBillingDate()
        // Create new Timer
        timer = Timer("PayTimerThread", true)
        // timer.schedule(3000) {processPayments()}
        timer.schedule(date) {processPayments()}
        isTimerSet = true
        return date
    }

    fun schedulePays() : BSResult {
        val bs: BSResult = BSResult()
        // Load Invoices
        if (invoices.isEmpty()) {
            val invoiceIterator = invoiceService.fetchAll().iterator()
            while(invoiceIterator.hasNext()) {
                invoices.add(invoiceIterator.next())
            }
        }
        if (!isTimerSet) {
            bs.nextBillDate = setNextPaymentsTimer().toString()
            bs.cronEvtSet = isTimerSet
        }
        return bs
    }

    fun unSchedulePays() : BSResult  {
        val bs: BSResult = BSResult()
        timer.cancel()
        isTimerSet = false
        return bs
    }
}
