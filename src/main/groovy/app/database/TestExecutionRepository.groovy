package app.database

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.Temporal
import org.springframework.data.repository.query.Param

import javax.persistence.TemporalType

interface TestExecutionRepository extends JpaRepository<TestExecution, String> {

    @Query("Select t from TestExecution t where t.created <= :limitDate")
    Collection<TestExecution> findOldTestExecutions(@Param("limitDate") @Temporal(value = TemporalType.TIMESTAMP) Date limitDate);
}
