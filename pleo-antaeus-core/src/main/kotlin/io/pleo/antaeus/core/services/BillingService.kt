package io.pleo.antaeus.core.services

import kotlin.concurrent.schedule
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.util.*
import io.pleo.antaeus.core.exceptions.*
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Customer

/**
    Implements the BillingService Class
 */
class BillingService(
    private val paymentProvider: PaymentProvider,
    private val customerService: CustomerService,
    private val invoiceService: InvoiceService
) {
    /* Use internal Logging Mechanism */
    private val logger = CoreLogger()

    /* Use util Scheduler Class */
    private val scheduler = Scheduler()

    /* Billing Service Response */
    class BSResult(var nextBillDate: String = "", var cronEvtSet: Boolean = false)

    /* Are there missing payments */
    private var missingPayments: Boolean = false

    /* Flow of Invoices to be consumed by the PaymentProvider */
    private fun emitCustomerInvoices() = invoiceService.fetchAll().asFlow()

    /* Flow of Unpaid Invoices to be consumed by the PaymentProvider */
    private fun emitCustomerPendingInvoices() = invoiceService.fetchAllNotPaid().asFlow()

    /**
    * This function charges all the invoices 
    * 
    * @throws PaymentsProcessException when process stops unexpectedly
    */
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
        }
        catch (e: PaymentsProcessException) {
            logger.error("BS: Payments process not finished")
        }
        finally {
            logger.info("BS: Subscriptions paid")
            scheduler.start().schedule( scheduler.getNextBillingDate() ) {processAllPayments()}
        }
    }

    /**
    * This function tries to force the payment of any PENDING transactions remaining
    * among the invoices 
    * 
    * @throws PaymentsProcessException when process stops unexpectedly
    */
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
        }
        catch (e: PaymentsProcessException) {
            logger.error("BS: Payments process not finished")
        }
        finally {
            logger.info("BS: Pending subscriptions were paid")
        }
    }


    /**
    * This function tries to process a single invoice payment
    *
    * @param  Invoice to be processed
    * @return Boolean true  : payment was successfully processed
    *                 false : payment was not processed
    * @throws CustomerNotFoundException when invalid customer is set in the Invoice
    * @throws CurrencyMismatchException when invalid currency is set in the Invoice
    * @throws NetworkException when Network error occured when charging the client
    */
    fun processPayment(invoice: Invoice) : Boolean {
        var processed: Boolean = false
        try {
            paymentProvider.charge(invoice)
            processed = true
        }
        catch (e: CustomerNotFoundException) {
            logger.error("BS: Customer ${invoice.customerId} not found")
        }
        catch (e: CurrencyMismatchException) {
            logger.error("BS: Currency ${invoice.amount.currency} is not valid")
        }
        catch (e: NetworkException) {
            logger.error("BS: Network is down")
        }
        finally {
            if (!missingPayments && invoice.status != InvoiceStatus.PAID) {
                missingPayments = true
            }
        }
        return processed
    }


    /**
    * (Overloaded function) 
    *
    * @see BillingService#processPayment
    * @param  Integer Invoice Id to be processed
    */
    fun processPayment(invoiceId: Int) : Boolean {
        return processPayment(invoiceService.fetch(invoiceId))
    }


    /**
    * This is the entry point of the service instance to serve payments. It takes
    * a parameter that enables or disables the scheduling of the payments.
    *
    * @param  Boolean Enables or Disables the billing scheduler
    * @return Object : {
    *                   nextBillDate : Date     Date of the next billings
    *                   cronEvtSet   : Boolean  Flag that shows if the Scheduler is set  
    *                  }
    */
    fun schedulePays(alive: Boolean) : BSResult {
        val bs: BSResult = BSResult()
        if (alive) {
            if (!scheduler.getIsSchedulerSet()) {
                scheduler.start().schedule( scheduler.getNextBillingDate() ) {processAllPayments()}
                
                bs.nextBillDate = scheduler.getNextBillingDate().toString()
                bs.cronEvtSet   = scheduler.getIsSchedulerSet()
            }
        } else if (!alive) {
            scheduler.stop()
        }
        return bs
    }


    /**
    * This function is the entry point of the class to process all pending transactions
    *
    * @return Boolean true  If there are still pending payments
    *                 false If there are no pending payments left
    */
    fun forcePendingPays() : Boolean {
        if (missingPayments) (
            processAllPendingPayments()
        )
        return missingPayments
    }
}
