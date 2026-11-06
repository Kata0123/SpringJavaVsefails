
package org.example.ratingservice.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class RatingRepository {

    private final JdbcTemplate jdbc;

    public RatingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<Long, Integer> ratings(long userId) {
        Map<Long, Integer> result = new LinkedHashMap<>();

        jdbc.query(
                """
                SELECT media_id, rating
                FROM user_ratings
                WHERE user_id = ?
                """,
                (RowCallbackHandler) rs ->
                        result.put(
                                rs.getLong("media_id"),
                                rs.getInt("rating")
                        ),
                userId
        );

        return result;
    }

    public void rate(long userId, long mediaId, int rating) {
        jdbc.update(
                """
                INSERT INTO user_ratings(user_id, media_id, rating)
                VALUES (?, ?, ?)
                ON CONFLICT(user_id, media_id)
                DO UPDATE SET rating = EXCLUDED.rating
                """,
                userId,
                mediaId,
                rating
        );
    }

    public void removeRating(long userId, long mediaId) {
        jdbc.update(
                """
                DELETE FROM user_ratings
                WHERE user_id = ? AND media_id = ?
                """,
                userId,
                mediaId
        );
    }

    public List<Long> watchlist(long userId) {
        return jdbc.query(
                """
                SELECT media_id
                FROM watchlist
                WHERE user_id = ?
                ORDER BY added_at DESC
                """,
                (rs, row) -> rs.getLong("media_id"),
                userId
        );
    }

    public void addWatchlist(long userId, long mediaId) {
        jdbc.update(
                """
                INSERT INTO watchlist(user_id, media_id)
                VALUES (?, ?)
                ON CONFLICT(user_id, media_id) DO NOTHING
                """,
                userId,
                mediaId
        );
    }

    public void removeWatchlist(long userId, long mediaId) {
        jdbc.update(
                """
                DELETE FROM watchlist
                WHERE user_id = ? AND media_id = ?
                """,
                userId,
                mediaId
        );
    }
}
