package com.admin.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.admin.entity.SubscriptionPlanDetails;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SubscriptionPlanRepository {

    private final JdbcTemplate jdbcTemplate;

    public SubscriptionPlanRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean planCodeExists(String planCode) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM subscription_plans WHERE plan_code = ?",
                Integer.class,
                planCode);
        return count != null && count > 0;
    }

    public boolean planCodeExistsForAnotherPlan(String planCode, String planId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM subscription_plans WHERE plan_code = ? AND id <> ?",
                Integer.class,
                planCode,
                planId);
        return count != null && count > 0;
    }

    public void insert(
            String id,
            String planCode,
            String planName,
            String billingPeriod,
            BigDecimal priceAmount,
            String currency,
            String limitsJson,
            String featuresJson,
            String status
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO subscription_plans (
                    id, plan_code, plan_name, billing_period, price_amount,
                    currency, limits_json, features_json, status
                ) VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), ?)
                """,
                id,
                planCode,
                planName,
                billingPeriod,
                priceAmount,
                currency,
                limitsJson,
                featuresJson,
                status);
    }

    public boolean update(
            String id,
            String planCode,
            String planName,
            String billingPeriod,
            BigDecimal priceAmount,
            String currency,
            String limitsJson,
            String featuresJson,
            String status
    ) {
        return jdbcTemplate.update(
                """
                UPDATE subscription_plans
                SET plan_code = ?, plan_name = ?, billing_period = ?,
                    price_amount = ?, currency = ?, limits_json = CAST(? AS jsonb),
                    features_json = CAST(? AS jsonb), status = ?, updated_at = CURRENT_TIMESTAMP(3)
                WHERE id = ?
                """,
                planCode,
                planName,
                billingPeriod,
                priceAmount,
                currency,
                limitsJson,
                featuresJson,
                status,
                id) > 0;
    }

    public boolean updateStatus(String id, String status) {
        return jdbcTemplate.update(
                """
                UPDATE subscription_plans
                SET status = ?, updated_at = CURRENT_TIMESTAMP(3)
                WHERE id = ?
                """,
                status,
                id) > 0;
    }

    public Optional<SubscriptionPlanDetails> findById(String id) {
        return jdbcTemplate.query(
                        """
                        SELECT id, plan_code, plan_name, billing_period, price_amount,
                               currency, limits_json, features_json, status,
                               created_at, updated_at
                        FROM subscription_plans
                        WHERE id = ?
                        LIMIT 1
                        """,
                        (resultSet, rowNumber) -> mapDetails(resultSet),
                        id)
                .stream()
                .findFirst();
    }

    public List<SubscriptionPlanDetails> findAll(
            String search,
            String status,
            int limit,
            int offset
    ) {
        String searchPattern = "%" + search + "%";
        return jdbcTemplate.query(
                """
                SELECT id, plan_code, plan_name, billing_period, price_amount,
                       currency, limits_json, features_json, status,
                       created_at, updated_at
                FROM subscription_plans
                WHERE (? = '' OR UPPER(plan_code) LIKE UPPER(?) OR UPPER(plan_name) LIKE UPPER(?))
                  AND (? = '' OR status = ?)
                ORDER BY created_at DESC, plan_code ASC
                LIMIT ? OFFSET ?
                """,
                (resultSet, rowNumber) -> mapDetails(resultSet),
                search,
                searchPattern,
                searchPattern,
                status,
                status,
                limit,
                offset);
    }

    public long countAll(String search, String status) {
        String searchPattern = "%" + search + "%";
        Long count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM subscription_plans
                WHERE (? = '' OR UPPER(plan_code) LIKE UPPER(?) OR UPPER(plan_name) LIKE UPPER(?))
                  AND (? = '' OR status = ?)
                """,
                Long.class,
                search,
                searchPattern,
                searchPattern,
                status,
                status);
        return count == null ? 0L : count;
    }

    private SubscriptionPlanDetails mapDetails(java.sql.ResultSet resultSet)
            throws java.sql.SQLException {
        return new SubscriptionPlanDetails(
                resultSet.getString("id"),
                resultSet.getString("plan_code"),
                resultSet.getString("plan_name"),
                resultSet.getString("billing_period"),
                resultSet.getBigDecimal("price_amount"),
                resultSet.getString("currency"),
                resultSet.getString("limits_json"),
                resultSet.getString("features_json"),
                resultSet.getString("status"),
                toInstant(resultSet.getTimestamp("created_at")),
                toInstant(resultSet.getTimestamp("updated_at")));
    }

    private Instant toInstant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }
}
