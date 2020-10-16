package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.util.Scheduler

import java.util.*

import org.junit.jupiter.api.*

class SchedulerTest {
    private val scheduler = Scheduler()

    @Test
    fun `Payments are not planned for a weekend`() {
        assert(scheduler.getNextBillingDate().getDay() != Calendar.getInstance().get(Calendar.SATURDAY))
        assert(scheduler.getNextBillingDate().getDay() != Calendar.getInstance().get(Calendar.SUNDAY))
    }

    @Test
    fun `Starting the Scheduler starts a Timer Deamon`() {
        scheduler.start()
        assert(scheduler.getIsSchedulerSet() == true)
    }

    @Test
    fun `Stopping the Scheduler stops any Timer Deamons`() {
        scheduler.stop()
        assert(scheduler.getIsSchedulerSet() == false)
    }
}
