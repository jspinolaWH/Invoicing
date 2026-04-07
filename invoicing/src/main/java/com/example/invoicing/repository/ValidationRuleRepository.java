package com.example.invoicing.repository;
import com.example.invoicing.entity.validation.ValidationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ValidationRuleRepository extends JpaRepository<ValidationRule, Long> {
    @Query("""
        SELECT v FROM ValidationRule v
        WHERE v.companyId = :companyId AND v.active = true
        ORDER BY v.blocking DESC, v.ruleCode ASC
        """)
    List<ValidationRule> findAllActiveByCompanyId(@Param("companyId") Long companyId);
    List<ValidationRule> findByCompanyIdOrderByBlockingDescRuleCodeAsc(Long companyId);

    @Query("""
        SELECT v FROM ValidationRule v
        WHERE v.active = true
        ORDER BY v.blocking DESC, v.ruleCode ASC
        """)
    List<ValidationRule> findAllActive();
}
