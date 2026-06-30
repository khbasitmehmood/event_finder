package com.eventfinder.app.domain.repository

import com.eventfinder.app.domain.model.PaymentReceipt
import com.eventfinder.app.domain.model.PaymentCheckout
import com.eventfinder.app.domain.model.PaymentRequest

interface PaymentGateway {
    suspend fun createCheckout(request: PaymentRequest): Result<PaymentCheckout>

    suspend fun getCheckoutReceipt(checkoutId: String): Result<PaymentReceipt>
}
