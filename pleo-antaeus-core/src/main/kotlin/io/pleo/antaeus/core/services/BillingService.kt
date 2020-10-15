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
import io.pleo.antaeus.models.Customer
import mu.KotlinLogging

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val customerService: CustomerService,
    private val invoiceService: InvoiceService
) {
    private val logger = KotlinLogging.logger {}
    class BSResult(var nextBillDate: String = "", var cronEvtSet: Boolean = false)
    var isTimerSet: Boolean = false
    var timer: Timer = Timer(true)

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
    }


    // Flow of Invoices to be consumed by the PaymentProvider
    private fun emitCustomerInvoices() = invoiceService.fetchAll().asFlow()
    private fun processAllPayments() {
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
            setNextPaymentsTimer()
        }
    }

    private fun setNextPaymentsTimer() : Date {
        val date: Date = getNextBillingDate()
        
        // Creates new Timer Thread
        timer = Timer("PayTimerThread", true)
    
        logger.info("BS: Next payments are scheduled for ${date.toString()}")
        timer.schedule(10000) {processAllPayments()} // @TODO: DELETE this line and uncomment the next
        // timer.schedule(date) {processAllPayments()}
        isTimerSet = true
        return date
    }

    fun schedulePays( alive: Boolean) : BSResult {
        val bs: BSResult = BSResult()
        if (alive) {
            if (!isTimerSet) {
                bs.nextBillDate = setNextPaymentsTimer().toString()
                bs.cronEvtSet = isTimerSet
            }
        } else if (!alive) {
            timer.cancel()
            isTimerSet = false
        }
        return bs
    }
}
