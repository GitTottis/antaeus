package io.pleo.antaeus.core.util

import mu.KotlinLogging

 /**
 * Implements the core packages logging service.
 */
class CoreLogger() {
    private val logger = KotlinLogging.logger {}
    private val intro = "ANTAEUS | "

    /**
    * Returns log message
    *
    * @param msg This is the custom `log` message
    * @returns String The log output
    */
    fun log(msg: String): String {
        return intro + "Log :" + msg
    }

    /**
    * Returns info message
    *
    * @param msg This is the custom `info` message
    * @returns String The info output
    */    
    fun info(msg: String): String {
        return intro + "Info :" + msg
    }

    /**
    * Returns debug message
    *
    * @param msg This is the custom `debug` message
    * @returns String The debug output
    */
    fun debug(msg: String): String {
        return intro + "Debug :" + msg
    }

    /**
    * Returns error message
    *
    * @param msg This is the custom `error` message
    * @returns String The error output
    */
    fun error(msg: String): String {
        return intro + "Error :" + msg
    }
}
