package com.zorvyn.finance.repository;

import com.zorvyn.finance.domain.entity.FinancialRecord;
import com.zorvyn.finance.domain.enums.RecordType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialRecordRepository extends JpaRepository<FinancialRecord, Long> {

    Optional<FinancialRecord> findByIdAndDeletedFalse(Long id);

    Page<FinancialRecord> findByDeletedFalse(Pageable pageable);

    Page<FinancialRecord> findByTypeAndDeletedFalse(RecordType type, Pageable pageable);

    Page<FinancialRecord> findByCategoryAndDeletedFalse(String category, Pageable pageable);

    Page<FinancialRecord> findByDateBetweenAndDeletedFalse(LocalDate start, LocalDate end, Pageable pageable);

    // dynamic filter query — null params are ignored so the caller can pass any combination
    @Query("SELECT r FROM FinancialRecord r WHERE r.deleted = false " +
           "AND (:type IS NULL OR r.type = :type) " +
           "AND (:category IS NULL OR r.category = :category) " +
           "AND (:startDate IS NULL OR r.date >= :startDate) " +
           "AND (:endDate IS NULL OR r.date <= :endDate)")
    Page<FinancialRecord> findWithFilters(
            @Param("type") RecordType type,
            @Param("category") String category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    // aggregation pushed to DB — much faster than loading all records into java
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM FinancialRecord r " +
           "WHERE r.type = :type AND r.deleted = false")
    BigDecimal sumByType(@Param("type") RecordType type);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM FinancialRecord r " +
           "WHERE r.type = :type AND r.deleted = false " +
           "AND r.date BETWEEN :startDate AND :endDate")
    BigDecimal sumByTypeAndDateBetween(
            @Param("type") RecordType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT r.category, SUM(r.amount) FROM FinancialRecord r " +
           "WHERE r.deleted = false AND r.type = :type " +
           "GROUP BY r.category ORDER BY SUM(r.amount) DESC")
    List<Object[]> sumByCategoryAndType(@Param("type") RecordType type);

    @Query("SELECT FUNCTION('YEAR', r.date), FUNCTION('MONTH', r.date), r.type, SUM(r.amount) " +
           "FROM FinancialRecord r WHERE r.deleted = false " +
           "AND r.date BETWEEN :startDate AND :endDate " +
           "GROUP BY FUNCTION('YEAR', r.date), FUNCTION('MONTH', r.date), r.type " +
           "ORDER BY FUNCTION('YEAR', r.date), FUNCTION('MONTH', r.date)")
    List<Object[]> monthlyTrends(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT r FROM FinancialRecord r JOIN FETCH r.createdBy WHERE r.deleted = false ORDER BY r.createdAt DESC")
    List<FinancialRecord> findRecentRecords(Pageable pageable);

    @Query("SELECT DISTINCT r.category FROM FinancialRecord r WHERE r.deleted = false ORDER BY r.category")
    List<String> findDistinctCategories();
}
