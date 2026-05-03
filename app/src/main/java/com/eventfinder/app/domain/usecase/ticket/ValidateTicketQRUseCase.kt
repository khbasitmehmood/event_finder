package com.eventfinder.app.domain.usecase.ticket

import com.eventfinder.app.domain.model.Ticket
import com.eventfinder.app.domain.repository.TicketRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for validating a ticket using QR code data
 */
@Singleton
class ValidateTicketQRUseCase @Inject constructor(
    private val ticketRepository: TicketRepository
) {

    suspend operator fun invoke(qrCodeData: String): Result<Ticket?> {
        return ticketRepository.validateTicketByQR(qrCodeData)
    }
}
