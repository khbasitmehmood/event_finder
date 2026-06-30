package com.eventfinder.app.data.mapper

import com.eventfinder.app.data.model.TicketDto
import com.eventfinder.app.domain.model.PaymentStatus
import com.eventfinder.app.domain.model.Ticket
import com.eventfinder.app.domain.model.TicketStatus
import com.eventfinder.app.domain.model.TicketType
import com.google.firebase.Timestamp

/**
 * Maps between Firestore Ticket DTOs and Domain Ticket models
 */
object TicketMapper {

    /**
     * Maps TicketDto (from Firestore) to Domain Ticket model
     */
    fun toDomain(dto: TicketDto): Ticket {
        return Ticket(
            id = dto.id,
            ticketId = dto.ticketId,
            eventId = dto.eventId,
            eventTitle = dto.eventTitle,
            eventStartTime = dto.eventStartTime?.toDate()?.time ?: 0L,
            userId = dto.userId,
            userName = dto.userName,
            userEmail = dto.userEmail,
            ticketType = safeValueOfTicketType(dto.ticketType),
            status = safeValueOfTicketStatus(dto.status),
            qrCodeData = dto.qrCodeData,
            purchasePrice = dto.purchasePrice.toDouble(),
            currency = dto.currency,
            paymentStatus = safeValueOfPaymentStatus(dto.paymentStatus),
            paymentProvider = dto.paymentProvider,
            paymentTransactionId = dto.paymentTransactionId,
            paidAt = dto.paidAt?.toDate()?.time,
            purchasedAt = dto.purchasedAt?.toDate()?.time ?: System.currentTimeMillis(),
            checkedInAt = dto.checkedInAt?.toDate()?.time,
            checkedInBy = dto.checkedInBy,
            eventLocation = dto.eventLocation,
            organizerId = dto.organizerId,
            organizerName = dto.organizerName
        )
    }

    /**
     * Maps Domain Ticket model to TicketDto (for Firestore)
     */
    fun toDto(ticket: Ticket): TicketDto {
        return TicketDto(
            id = ticket.id,
            ticketId = ticket.ticketId,
            eventId = ticket.eventId,
            eventTitle = ticket.eventTitle,
            eventStartTime = Timestamp(
                ticket.eventStartTime / 1000,
                ((ticket.eventStartTime % 1000) * 1000000).toInt()
            ),
            userId = ticket.userId,
            userName = ticket.userName,
            userEmail = ticket.userEmail,
            ticketType = ticket.ticketType.name,
            status = ticket.status.name,
            qrCodeData = ticket.qrCodeData,
            purchasePrice = ticket.purchasePrice,
            currency = ticket.currency,
            paymentStatus = ticket.paymentStatus.name,
            paymentProvider = ticket.paymentProvider,
            paymentTransactionId = ticket.paymentTransactionId,
            paidAt = ticket.paidAt?.let {
                Timestamp(it / 1000, ((it % 1000) * 1000000).toInt())
            },
            purchasedAt = Timestamp(
                ticket.purchasedAt / 1000,
                ((ticket.purchasedAt % 1000) * 1000000).toInt()
            ),
            checkedInAt = ticket.checkedInAt?.let {
                Timestamp(it / 1000, ((it % 1000) * 1000000).toInt())
            },
            checkedInBy = ticket.checkedInBy,
            eventLocation = ticket.eventLocation,
            organizerId = ticket.organizerId,
            organizerName = ticket.organizerName
        )
    }

    /**
     * Safe valueOf for TicketType with fallback to PUBLIC_RESERVATION
     */
    private fun safeValueOfTicketType(value: String?): TicketType {
        return try {
            value?.let { TicketType.valueOf(it) } ?: TicketType.PUBLIC_RESERVATION
        } catch (e: IllegalArgumentException) {
            TicketType.PUBLIC_RESERVATION
        }
    }

    /**
     * Safe valueOf for TicketStatus with fallback to RESERVED
     */
    private fun safeValueOfTicketStatus(value: String?): TicketStatus {
        return try {
            value?.let { TicketStatus.valueOf(it) } ?: TicketStatus.RESERVED
        } catch (e: IllegalArgumentException) {
            TicketStatus.RESERVED
        }
    }

    private fun safeValueOfPaymentStatus(value: String?): PaymentStatus {
        return try {
            value?.let { PaymentStatus.valueOf(it) } ?: PaymentStatus.NOT_REQUIRED
        } catch (e: IllegalArgumentException) {
            PaymentStatus.NOT_REQUIRED
        }
    }
}
