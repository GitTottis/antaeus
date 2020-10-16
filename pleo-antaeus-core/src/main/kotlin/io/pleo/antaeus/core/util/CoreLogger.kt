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
    * Notice: Debug mode has to be activated in KotlinLogging
    *
    * @param msg This is the custom `log` message
    * @returns String The log output
    */
    fun log(msg: String) {
        logger.debug( "$intro Log: $msg" )
    }

    /**
    * Returns info message
    *
    * @param msg This is the custom `info` message
    * @returns String The info output
    */    
    fun info(msg: String) {
        logger.info(intro + "Info : " + msg)
    }

    /**
    * Returns error message
    *
    * @param msg This is the custom `error` message
    * @returns String The error output
    */
    fun error(e : Exception, msg: String) {
        logger.info(intro + "Error : " + e + " : " + msg)
    }
}
