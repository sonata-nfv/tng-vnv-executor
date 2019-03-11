package app.database

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

import javax.persistence.*

@Entity
@EntityListeners(AuditingEntityListener.class)
class TestExecution implements Serializable {

    @Id
    @Column(name = "uuid", updatable = false, nullable = false)
    String uuid

    @Column
    @Enumerated(EnumType.STRING)
    TestState state

    @Lob
    @Column
    String dockerCompose

    @CreatedDate
    @Column(name = "created", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    Date created

    @LastModifiedDate
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "lastModified")
    Date lasModifiedDate


    TestExecution() {}

    TestExecution(String uuid, String dockerCompose) {
        this.uuid = uuid
        this.dockerCompose = dockerCompose
        this.state = TestState.STARTING
    }

    enum TestState {
        RUNNING, COMPLETED, CANCELLING, CANCELLED, STARTING, ERROR
    }
}
