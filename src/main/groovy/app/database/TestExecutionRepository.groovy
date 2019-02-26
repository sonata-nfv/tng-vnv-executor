package app.database

import org.springframework.data.jpa.repository.JpaRepository

interface TestExecutionRepository extends JpaRepository<TestExecution, String> {
}
