
package io.pleo.antaeus.core.util

import java.util.*

/**
    Implements the payments scheduler
 */
class Scheduler() {
    
    private var timer: Timer = Timer(true)

    /* Is scheduler for monthly subscription payments set */
    private var isSchedulerSet: Boolean = false

    /* Is scheduler for monthly subscription payments set */
    private var nextBillingDate: Date = Date()

    /**
    * Getter of isSchedulerSet flag
    *
    * @return Boolean The value of isSchedulerSet.
    */
    fun getIsSchedulerSet() : Boolean {
        return isSchedulerSet
    }

    /**
    * Setter of isSchedulerSet flag
    */
    private fun setIsSchedulerSet( value: Boolean ) {
        isSchedulerSet = value
    }

    /**
    * Calculates the next billing date
    *
    * @return Date The next billing date.
    */
    fun getNextBillingDate() : Date {
        if (!getIsSchedulerSet()) {
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
            nextBillingDate = cal.getTime();
        }
        return nextBillingDate
    }

    /**
    * Calculates the next billing date
    *
    * @return Date The next billing date.
    */
    private fun setNextPaymentsTimer() : Boolean {
        try {
            timer = Timer("PayTimerThread", true)
        }
        finally {
            setIsSchedulerSet(true)
        }
        return getIsSchedulerSet()
    }

    /**
    * Starts a new Timer deamon thread
    *
    * @return Timer The scheduler.
    */
    fun start() : Timer {
        if (!getIsSchedulerSet()) {
            setNextPaymentsTimer()
        }
        return timer
    }

    /**
    * Stops the Timer deamon thread
    *
    * @return Boolean true  : Timer deamon is still alive.
    *                 false : Timer deamon is not alive.
    */
    fun stop() : Boolean{
        if (getIsSchedulerSet()) {
            timer.cancel()
            setIsSchedulerSet(false)
        }
        return getIsSchedulerSet()
    }
}
