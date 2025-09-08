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
			if (userRepository.findByUsername("test").isEmpty()) {
				UserEntity user = new UserEntity();
				user.setUsername("test");
				user.setPassword(passwordEncoder.encode("123456"));
				user.setEmail("test@example.com");
				userRepository.save(user);
			}
		};
	}
}


