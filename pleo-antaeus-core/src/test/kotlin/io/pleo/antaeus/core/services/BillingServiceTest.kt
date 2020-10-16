package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.*
import io.pleo.antaeus.core.exceptions.*
import io.pleo.antaeus.core.external.PaymentProvider

import java.math.BigDecimal
import org.junit.jupiter.api.*

class BillingServiceTest {
    private val invoice = Invoice(80, 8,Money(BigDecimal(362.19),Currency.DKK), InvoiceStatus.PAID)
    private val customer = Customer(93,Currency.DKK)

    private val paymentProvider = mockk<PaymentProvider>()
    private val invoiceService = mockk<InvoiceService>()
    private val customerService = mockk<CustomerService>()
    
    private val billingService = BillingService(
        paymentProvider = paymentProvider,
        invoiceService = invoiceService,
        customerService = customerService
    )

    @Test
    fun `Payment process returns false when network is down`() {
        every { paymentProvider.charge(any()) } throws NetworkException()
        assert(billingService.processPayment(invoice) == false)
    }
    
    @Test
    fun `Payment process returns false when customer not found`() {
        every { paymentProvider.charge(any()) } throws CustomerNotFoundException(customer.id)
        assert(billingService.processPayment(invoice) == false)
    }

    @Test
    fun `Payment process returns false when ther is a currency missmatch`() {
        every { paymentProvider.charge(any()) } throws CurrencyMismatchException(invoice.id, customer.id)
        assert(billingService.processPayment(invoice) == false)
    }

    @Test
    fun `Payment process returns true when no problem is found`() {
        every { paymentProvider.charge(any()) } returns true
        assert(billingService.processPayment(invoice) == true)
    }

    @Test
    fun `Payments scheduler starts successfully on a date`() {
        var r = billingService.schedulePays(true)
        assert(r.nextBillDate != "")
        assert(r.cronEvtSet == true)
    }

    @Test
    fun `Payments scheduler stops successfully`() {
        var r = billingService.schedulePays(false)
        assert(r.nextBillDate == "")
        assert(r.cronEvtSet == false)
    }
}
