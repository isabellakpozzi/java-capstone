package assembly.general.api.controllers;

import assembly.general.api.dto.*;
import assembly.general.api.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> reserveBook(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ReservationRequest request
    ) {
        ReservationResponse response = reservationService.reserveBook(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ActiveReservationsResponse getActiveReservations(@AuthenticationPrincipal UUID userId) {
        return reservationService.getActiveReservations(userId);
    }

    @PostMapping("/{reservationId}/checkout")
    public CheckoutResponse checkout(
            @PathVariable UUID reservationId,
            @RequestBody(required = false) CheckoutRequest request
    ) {
        return reservationService.checkout(reservationId, request != null ? request : new CheckoutRequest());
    }

    @PostMapping("/{reservationId}/return")
    public ReturnResponse returnBook(
            @PathVariable UUID reservationId,
            @Valid @RequestBody ReturnRequest request
    ) {
        return reservationService.returnBook(reservationId, request);
    }

    @GetMapping("/history")
    public PagedResponse<BorrowingHistoryItem> getHistory(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reservationService.getHistory(userId, page, size);
    }
}