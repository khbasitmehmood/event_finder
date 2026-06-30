package com.eventfinder.app.data.payment

import android.util.Log
import com.eventfinder.app.BuildConfig
import com.eventfinder.app.domain.model.PaymentCheckout
import com.eventfinder.app.domain.model.PaymentReceipt
import com.eventfinder.app.domain.model.PaymentRequest
import com.eventfinder.app.domain.model.PaymentStatus
import com.eventfinder.app.domain.repository.PaymentGateway
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudflareWorkerPaymentGateway @Inject constructor() : PaymentGateway {

    private val gson = Gson()
    private val tag = "PaymentGateway"

    override suspend fun createCheckout(request: PaymentRequest): Result<PaymentCheckout> {
        return post("/create-ticket-checkout", request, PaymentCheckout::class.java)
    }

    override suspend fun getCheckoutReceipt(checkoutId: String): Result<PaymentReceipt> {
        return post(
            path = "/confirm-ticket-checkout",
            body = mapOf("checkoutId" to checkoutId),
            responseClass = WorkerPaymentReceipt::class.java
        ).map { receipt ->
            PaymentReceipt(
                provider = receipt.provider,
                transactionId = receipt.transactionId,
                checkoutId = receipt.checkoutId,
                ticketId = receipt.ticketId,
                amount = receipt.amount,
                currency = receipt.currency,
                paidAt = receipt.paidAt,
                status = PaymentStatus.valueOf(receipt.status)
            )
        }
    }

    private suspend fun <T> post(path: String, body: Any, responseClass: Class<T>): Result<T> {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = BuildConfig.PAYMENT_API_BASE_URL.trimEnd('/')
                if (baseUrl.contains("YOUR_SUBDOMAIN")) {
                    Log.e(tag, "Payment backend URL is not configured")
                    return@withContext Result.failure(
                        IllegalStateException("Payment backend URL is not configured")
                    )
                }

                Log.d(tag, "POST $path -> $baseUrl$path")

                val connection = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 20_000
                    readTimeout = 30_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(gson.toJson(body))
                }

                val responseBody = if (connection.responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                }

                Log.d(
                    tag,
                    "Response $path status=${connection.responseCode} body=${responseBody.take(500)}"
                )

                if (connection.responseCode !in 200..299) {
                    Log.e(tag, "Payment backend failed path=$path status=${connection.responseCode}")
                    return@withContext Result.failure(
                        IllegalStateException(parseError(responseBody, connection.responseCode))
                    )
                }

                Result.success(gson.fromJson(responseBody, responseClass))
            } catch (e: Exception) {
                Log.e(tag, "Payment request failed path=$path", e)
                Result.failure(e)
            }
        }
    }

    private fun parseError(body: String, code: Int): String {
        return runCatching {
            gson.fromJson(body, WorkerError::class.java).error
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Payment backend failed with HTTP $code"
    }

    private data class WorkerPaymentReceipt(
        val provider: String,
        val transactionId: String,
        val checkoutId: String?,
        val ticketId: String?,
        val amount: Double,
        val currency: String,
        val paidAt: Long,
        val status: String
    )

    private data class WorkerError(val error: String = "")
}
