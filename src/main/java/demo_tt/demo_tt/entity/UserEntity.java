package demo_tt.demo_tt.entity;

import demo_tt.demo_tt.converter.EncryptedStringConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "user_name", unique = true, nullable = false)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false, length = 500)
    @Size(max = 500)
    @Convert(converter = EncryptedStringConverter.class)
    private String fullName;

    @Column(name = "age")
    private Integer age;

    @Size(max = 255)
    @Column(name = "phone_number", length = 255)
    @Convert(converter = EncryptedStringConverter.class)
    private String phoneNumber;

    @Column(name = "address", length = 1000)
    @Size(max = 1000)
    @Convert(converter = EncryptedStringConverter.class)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Column(name = "workplace", length = 500)
    @Size(max = 500)
    @Convert(converter = EncryptedStringConverter.class)
    private String workplace;

    @Email
    @Column(unique = true, nullable = false, length = 500)
    @Size(max = 500)
    @Convert(converter = EncryptedStringConverter.class)
    private String email;

    public enum Gender {
        MALE, FEMALE, OTHER
    }
}
