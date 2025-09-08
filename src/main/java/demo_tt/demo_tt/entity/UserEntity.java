package demo_tt.demo_tt.entity;

import jakarta.persistence.*;
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

    @Column(unique = true)
    private String email;

    @Size(max = 255)
    @Column(name = "phone_number")
    private String phoneNumber;
}
