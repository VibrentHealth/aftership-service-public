package com.vibrent.aftership.domain;

import lombok.Data;
import org.hibernate.envers.Audited;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.Instant;

@Data
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Audited
public abstract class Auditable {

    @Column(name = "created_on")
    private Long createdOn;

    @Column(name = "updated_on")
    private Long updatedOn;

    @PrePersist
    public void prePersist() {
        long currentTimestamp = Instant.now().toEpochMilli();
        this.createdOn = this.createdOn == null ? currentTimestamp : this.createdOn;
        this.updatedOn = currentTimestamp;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedOn = Instant.now().toEpochMilli();
    }
}
