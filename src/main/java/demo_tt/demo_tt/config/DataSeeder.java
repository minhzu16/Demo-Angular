package demo_tt.demo_tt.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import demo_tt.demo_tt.entity.UserEntity;
import demo_tt.demo_tt.repository.UserRepository;

@Configuration
public class DataSeeder {

	@Bean
	CommandLineRunner seedUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			// Clear existing users to ensure fresh data
			userRepository.deleteAll();
			System.out.println("Cleared existing users");
			
			// Create test user
			UserEntity user = new UserEntity();
			user.setUsername("test");
			user.setPassword(passwordEncoder.encode("123456"));
			user.setFullName("Nguyễn Văn Test");
			user.setAge(25);
			user.setEmail("test@example.com");
			user.setPhoneNumber("0123456789");
			user.setAddress("123 Đường ABC, Quận 1, TP.HCM");
			user.setGender(UserEntity.Gender.MALE);
			user.setWorkplace("Công ty ABC");
			userRepository.save(user);
			System.out.println("Created user: test");
			
			// Create admin user
			UserEntity admin = new UserEntity();
			admin.setUsername("admin");
			admin.setPassword(passwordEncoder.encode("admin123"));
			admin.setFullName("Trần Thị Admin");
			admin.setAge(30);
			admin.setEmail("admin@example.com");
			admin.setPhoneNumber("0987654321");
			admin.setAddress("456 Đường XYZ, Quận 2, TP.HCM");
			admin.setGender(UserEntity.Gender.FEMALE);
			admin.setWorkplace("Công ty XYZ");
			userRepository.save(admin);
			System.out.println("Created user: admin");
		};
	}
}


