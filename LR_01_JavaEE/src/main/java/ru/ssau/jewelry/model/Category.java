package ru.ssau.jewelry.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

@Entity
@Table(name = "categories")
public class Category implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private long version;

    @NotBlank(message = "Введите название категории")
    @Size(max = 100, message = "Название категории: не более 100 символов")
    @Column(nullable = false, length = 100)
    private String name;

    @Size(max = 500, message = "Описание: не более 500 символов")
    @Column(length = 500)
    private String description;

    public Category() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
