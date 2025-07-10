package com.manuelmaly.hn.server

/**
 * Interface for HTTP request commands (command pattern).
 *
 * @author manuelmaly
 *
 * @param <T>
 * Return type of request.
</T> */
interface IAPICommand<T> : Runnable {
    /**
     * Returns the result of this command AFTER it has been run.
     *
     * @return T return type of request.
     */
    val responseContent: T

    /**
     * Returns the error code which occurred. If a high-level error occurred
     * which has nothing to do with technical issues (e.g. validation error),
     * ServerErrorCodes.UNDEFINED is returned here.
     *
     * @see ServerErrorCodes
     */
    val errorCode: Int

    /**
     * Returns the HTTP status code from the response.
     * @return int http status code.
     */
    val actualStatusCode: Int

    fun responseHandlingFinished(parsedResponse: T, responseHttpStatus: Int)

    enum class RequestType {
        GET, PUT, POST, DELETE
    }

    companion object {
        const val JSON_MIME: String = "application/json"
        const val HTML_MIME: String = "text/html"
        const val MULTIPART_MIME: String = "multipart/form-data"
        const val ACCEPT_HEADER: String = "Accept"
        const val RESPONSE_SC_ERROR_MSG: String =
            "Response status code does not match the expected: "

        const val ERROR_NONE: Int = 0
        const val ERROR_UNKNOWN: Int = 1
        const val ERROR_GENERIC_COMMUNICATION_ERROR: Int = 10
        const val ERROR_DEVICE_OFFLINE: Int = 20
        const val ERROR_RESPONSE_PARSE_ERROR: Int = 30
        const val ERROR_SERVER_RETURNED_ERROR: Int = 40
        const val ERROR_CANCELLED_BY_USER: Int = 1000

        const val BROADCAST_INTENT_EXTRA_ERROR: String = "error"
        const val BROADCAST_INTENT_EXTRA_RESPONSE: String = "response"

        const val DEFAULT_BROADCAST_INTENT_ID: String = "APICommandFinished"
    }
}
