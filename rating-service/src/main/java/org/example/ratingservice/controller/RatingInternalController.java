
package org.example.ratingservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.example.ratingservice.repository.RatingRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal")
public class RatingInternalController {

    private final RatingRepository repository;

    public RatingInternalController(RatingRepository repository) {
        this.repository = repository;
    }

    public record RatingBody(
            @NotNull @Min(1) @Max(10) Integer rating
    ) {}

    @GetMapping("/ratings/users/{userId}")
    public Map<Long, Integer> ratings(@PathVariable long userId) {
        return repository.ratings(userId);
    }

    @PutMapping("/ratings/users/{userId}/media/{mediaId}")
    public void rate(
            @PathVariable long userId,
            @PathVariable long mediaId,
            @Valid @RequestBody RatingBody body
    ) {
        repository.rate(userId, mediaId, body.rating());
    }

    @DeleteMapping("/ratings/users/{userId}/media/{mediaId}")
    public void deleteRating(
            @PathVariable long userId,
            @PathVariable long mediaId
    ) {
        repository.removeRating(userId, mediaId);
    }

    @GetMapping("/watchlist/users/{userId}")
    public List<Long> watchlist(@PathVariable long userId) {
        return repository.watchlist(userId);
    }

    @PostMapping("/watchlist/users/{userId}/media/{mediaId}")
    public void addWatchlist(
            @PathVariable long userId,
            @PathVariable long mediaId
    ) {
        repository.addWatchlist(userId, mediaId);
    }

    @DeleteMapping("/watchlist/users/{userId}/media/{mediaId}")
    public void removeWatchlist(
            @PathVariable long userId,
            @PathVariable long mediaId
    ) {
        repository.removeWatchlist(userId, mediaId);
    }
}
