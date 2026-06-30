package com.eventfinder.app.data.payment

import com.eventfinder.app.domain.model.PaymentReceipt
import com.eventfinder.app.domain.model.PaymentCheckout
import com.eventfinder.app.domain.model.PaymentRequest
import com.eventfinder.app.domain.model.PaymentStatus
import com.eventfinder.app.domain.repository.PaymentGateway
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Test-only gateway for the Pakistan payment flow.
 *
 * This keeps paid tickets behind a payment step without putting real merchant
 * secrets in the Android app. Replace this class with a backend-backed provider
 * for JazzCash, Easypaisa, PayFast, Paymob, or another live gateway.
 */
@Singleton
class SandboxPakistanPaymentGateway @Inject constructor() : PaymentGateway {

    override suspend fun createCheckout(request: PaymentRequest): Result<PaymentCheckout> {
        if (request.amount <= 0.0) {
            return Result.failure(IllegalArgumentException("Payment amount must be greater than zero"))
        }

        if (request.currency.uppercase(Locale.US) != "PKR") {
            return Result.failure(IllegalArgumentException("Only PKR payments are supported in sandbox"))
        }

        delay(SANDBOX_PROCESSING_DELAY_MS)
        val checkoutId = "sandbox-checkout-${UUID.randomUUID()}"
        val transactionId = "sandbox-pk-${UUID.randomUUID()}"

        return Result.success(
            PaymentCheckout(
                checkoutId = checkoutId,
                checkoutUrl = "https://sandbox.api.getsafepay.com/embedded/external/complete?checkout_id=$checkoutId",
                provider = PROVIDER,
                transactionId = transactionId,
                amount = request.amount,
                currency = request.currency.uppercase(Locale.US)
            )
        )
    }

    override suspend fun getCheckoutReceipt(checkoutId: String): Result<PaymentReceipt> {
        delay(SANDBOX_PROCESSING_DELAY_MS)
        return Result.success(
            PaymentReceipt(
                provider = PROVIDER,
                transactionId = checkoutId,
                checkoutId = checkoutId,
                ticketId = null,
                amount = 0.0,
                currency = "PKR",
                paidAt = System.currentTimeMillis(),
                status = PaymentStatus.PAID
            )
        )
    }

    private companion object {
        const val PROVIDER = "PAKISTAN_SANDBOX"
        const val SANDBOX_PROCESSING_DELAY_MS = 900L
    }
}
