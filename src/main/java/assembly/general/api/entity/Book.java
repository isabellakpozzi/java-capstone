package assembly.general.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * represents a catalog item. `availableCopies` is the source of truth for
 * inventory. status (AVAILABLE / CHECKED_OUT) is derived at the DTO/service
 * layer and never persisted here.
 */
@Entity
@Table(name = "books")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String isbn;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false)
    private String genre;

    @Column(nullable = false)
    private Integer publicationYear;

    @Column(length = 2000)
    private String description;

    private String publisher;

    private Integer pageCount;

    private String language;

    @Column(nullable = false)
    private Integer totalCopies;

    @Column(nullable = false)
    private Integer availableCopies;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /** derived, not persisted. matches "status calculated dynamically" spec */
    @Transient
    public String getStatus() {
        return (availableCopies != null && availableCopies > 0) ? "AVAILABLE" : "CHECKED_OUT";
    }
}