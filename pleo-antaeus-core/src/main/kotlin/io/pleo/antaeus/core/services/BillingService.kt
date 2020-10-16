package io.pleo.antaeus.core.services


import kotlin.concurrent.schedule
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import java.util.*

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.exceptions.*
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Customer
import mu.KotlinLogging

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val customerService: CustomerService,
    private val invoiceService: InvoiceService
) {
    private val logger = KotlinLogging.logger {}
    var timer: Timer = Timer(true)

    /* Billing Service Response */
    class BSResult(var nextBillDate: String = "", var cronEvtSet: Boolean = false)
    
    /* Is scheduler for monthly subscription payments set */
    private var isTimerSet: Boolean = false

    /* Are there missing payments */
    private var missingPayments: Boolean = false

    private fun getNextBillingDate() : Date {
        val cal = Calendar.getInstance().also {
            it.set(Calendar.MONTH, it.get(Calendar.MONTH)+1) // next Month
            it.set(Calendar.DAY_OF_MONTH, it.getActualMinimum(Calendar.DAY_OF_MONTH))
            it.set(Calendar.HOUR_OF_DAY, 12)
        }

        // Checking if on a weekend
        if (Calendar.SATURDAY == cal.get(Calendar.DAY_OF_WEEK)) {
            cal.add(Calendar.DAY_OF_WEEK, 2);
        } else if (Calendar.SUNDAY == cal.get(Calendar.DAY_OF_WEEK)) {
            cal.add(Calendar.DAY_OF_WEEK, 1);
        }
        return cal.getTime();
    }

    private fun setNextPaymentsTimer(date: Date) : Date {
        try {
            timer = Timer("PayTimerThread", true)
            timer.schedule(date) {processAllPayments()}
        }
        finally {
            logger.info("BS: Next payments are scheduled for ${date.toString()}")
            isTimerSet = true
        }
        
        return date
    }

    private fun processPayment(invoice: Invoice) {
        try {
            paymentProvider.charge(invoice)
        }
        catch (e: CustomerNotFoundException) {
            logger.error("Customer ${invoice.customerId} not found")
        }
        catch (e: CurrencyMismatchException) {
            logger.error("Currency ${invoice.amount.currency} is not valid")
        }
        catch (e: NetworkException) {
            logger.error("Network is down")
        }
        finally {
            if (!missingPayments && invoice.status != InvoiceStatus.PAID) {
                missingPayments = true
            }
        }
    }

    // Flow of Invoices to be consumed by the PaymentProvider
    private fun emitCustomerInvoices() = invoiceService.fetchAll().asFlow()
    private fun processAllPayments() {
        missingPayments = false
        try {
            runBlocking {
                logger.info("BS: Currently paying the monthly subscription payments")
                emitCustomerInvoices()
                    .collect {
                        processPayment(it)
                    }
            }
        } finally {
            logger.info("BS: Subscriptions paid")
            setNextPaymentsTimer(getNextBillingDate())
        }
    }

    // Flow of Unpaid Invoices to be consumed by the PaymentProvider
    private fun emitCustomerPendingInvoices() = invoiceService.fetchAllNotPaid().asFlow()
    private fun processAllPendingPayments() {
        missingPayments = false
        try {
            runBlocking {
                logger.info("BS: Re-trying this month's unpaid subscriptions")
                emitCustomerPendingInvoices()
                    .collect {
                        processPayment(it)
                    }
            }
        } finally {
            logger.info("BS: Pending subscriptions were paid")
        }
    }

    fun schedulePays(alive: Boolean) : BSResult {
        val bs: BSResult = BSResult()
        if (alive) {
            if (!isTimerSet) {
                bs.nextBillDate = setNextPaymentsTimer(getNextBillingDate()).toString()
                bs.cronEvtSet = isTimerSet
            }
        } else if (!alive) {
            timer.cancel()
            isTimerSet = false
        }
        return bs
    }

    fun forcePendingPays() : Boolean {
        if (missingPayments) (
            processAllPendingPayments()
        )
        return missingPayments
    }
}
