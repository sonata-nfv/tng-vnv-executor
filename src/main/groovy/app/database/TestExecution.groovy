package app.database

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate

import javax.persistence.*

@Entity
class TestExecution implements Serializable {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    String id

    @Column
    @Enumerated(EnumType.STRING)
    TestState state

    @Lob
    @Column
    String dockerCompose

    @CreatedDate
    @Column(name = "created", insertable = true, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    Date created

    @LastModifiedDate
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "lastModified", insertable = false, updatable = true)
    Date lasModifiedDate


    TestExecution() {}

    TestExecution(String id, String dockerCompose) {
        this.id = id
        this.dockerCompose = dockerCompose
        this.state = TestState.STARTING
    }

    enum TestState {
        RUNNING, COMPLETED, CANCELLED, STARTING
    }
}
