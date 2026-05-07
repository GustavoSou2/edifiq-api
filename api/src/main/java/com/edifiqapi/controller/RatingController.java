package com.edifiqapi.controller;

import com.edifiqapi.domain.delivery.Rating;
import com.edifiqapi.repository.delivery.RatingRepository;
import com.edifiqapi.security.JwtClaims;
import com.edifiqapi.web.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/v1/ratings")
public class RatingController {
    private final RatingRepository ratingRepository;

    public RatingController(RatingRepository ratingRepository) {
        this.ratingRepository = ratingRepository;
    }

    @GetMapping
    public ApiResponse<List<RatingResponse>> list(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = JwtClaims.tenantId(jwt);
        return ApiResponse.of(ratingRepository.findAllByOrderSelection_Order_Tenant_Id(tenantId).stream()
                .map(RatingResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<RatingResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        String tenantId = JwtClaims.tenantId(jwt);
        Rating rating = ratingRepository.findByIdAndOrderSelection_Order_Tenant_Id(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "rating not found"));
        return ApiResponse.of(RatingResponse.from(rating));
    }

    @PatchMapping("/{id}/response")
    public ApiResponse<RatingResponse> respond(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @Valid @RequestBody RespondRatingRequest request
    ) {
        String tenantId = JwtClaims.tenantId(jwt);
        Rating rating = ratingRepository.findByIdAndOrderSelection_Order_Tenant_Id(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "rating not found"));
        rating.setResponse(request.response());
        return ApiResponse.of(RatingResponse.from(ratingRepository.save(rating)));
    }

    public record RespondRatingRequest(@NotBlank String response) {}

    public record RatingResponse(String id, int score, String comment, String response) {
        static RatingResponse from(Rating rating) {
            return new RatingResponse(rating.getId(), rating.getScore(), rating.getComment(), rating.getResponse());
        }
    }
}
