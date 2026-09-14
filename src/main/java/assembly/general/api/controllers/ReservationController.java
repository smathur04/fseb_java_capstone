package assembly.general.api.controllers;

import assembly.general.api.dto.*;
import assembly.general.api.security.AuthenticatedUser;
import assembly.general.api.service.ReservationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(
            ReservationService reservationService
    ) {
        this.reservationService =
                reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationCreateResponse> createReservation(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody ReservationCreateRequest request
    ) {
        ReservationCreateResponse response =
                reservationService
                        .createReservation(
                                principal.userId(),
                                request
                        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<ActiveReservationsResponse> getActiveReservations(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        ActiveReservationsResponse response =
                reservationService
                        .getActiveReservations(
                                principal.userId()
                        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{reservationId}/checkout")
    public ResponseEntity<CheckoutResponse> checkout(
            @PathVariable UUID reservationId,
            @RequestBody CheckoutRequest request
    ) {
        CheckoutResponse response =
                reservationService.checkout(
                        reservationId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{reservationId}/return")
    public ResponseEntity<ReturnResponse> returnBook(
            @PathVariable UUID reservationId,
            @Valid @RequestBody ReturnRequest request
    ) {
        ReturnResponse response =
                reservationService.returnBook(
                        reservationId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<PageResponse<ReservationHistoryResponse>> getHistory(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<ReservationHistoryResponse> response =
                reservationService
                        .getHistory(
                                principal.userId(),
                                page,
                                size
                        );

        return ResponseEntity.ok(response);
    }
}