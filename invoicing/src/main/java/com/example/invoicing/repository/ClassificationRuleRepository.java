package com.example.invoicing.repository;
import com.example.invoicing.entity.classification.ClassificationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClassificationRuleRepository extends JpaRepository<ClassificationRule, Long> {
    List<ClassificationRule> findByCompanyIdAndActiveTrueOrderByPriorityAscIdAsc(Long companyId);
    List<ClassificationRule> findByCompanyIdOrderByPriorityAscIdAsc(Long companyId);
    boolean existsByCompanyIdAndPriorityAndIdNot(Long companyId, int priority, Long excludeId);
}
