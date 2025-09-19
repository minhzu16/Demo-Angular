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
			
			// Create additional 19 users
			createUser(userRepository, passwordEncoder, "user1", "	", "Lê Văn An", 28, "user1@example.com", "0123456780", "789 Đường DEF, Quận 3, TP.HCM", UserEntity.Gender.MALE, "Công ty DEF");
			createUser(userRepository, passwordEncoder, "user2", "password123", "Phạm Thị Bình", 26, "user2@example.com", "0123456781", "321 Đường GHI, Quận 4, TP.HCM", UserEntity.Gender.FEMALE, "Công ty GHI");
			createUser(userRepository, passwordEncoder, "user3", "password123", "Hoàng Văn Cường", 32, "user3@example.com", "0123456782", "654 Đường JKL, Quận 5, TP.HCM", UserEntity.Gender.MALE, "Công ty JKL");
			createUser(userRepository, passwordEncoder, "user4", "password123", "Vũ Thị Dung", 24, "user4@example.com", "0123456783", "987 Đường MNO, Quận 6, TP.HCM", UserEntity.Gender.FEMALE, "Công ty MNO");
			createUser(userRepository, passwordEncoder, "user5", "password123", "Đặng Văn Em", 29, "user5@example.com", "0123456784", "147 Đường PQR, Quận 7, TP.HCM", UserEntity.Gender.MALE, "Công ty PQR");
			createUser(userRepository, passwordEncoder, "user6", "password123", "Bùi Thị Phương", 27, "user6@example.com", "0123456785", "258 Đường STU, Quận 8, TP.HCM", UserEntity.Gender.FEMALE, "Công ty STU");
			createUser(userRepository, passwordEncoder, "user7", "password123", "Ngô Văn Giang", 31, "user7@example.com", "0123456786", "369 Đường VWX, Quận 9, TP.HCM", UserEntity.Gender.MALE, "Công ty VWX");
			createUser(userRepository, passwordEncoder, "user8", "password123", "Lý Thị Hoa", 25, "user8@example.com", "0123456787", "741 Đường YZA, Quận 10, TP.HCM", UserEntity.Gender.FEMALE, "Công ty YZA");
			createUser(userRepository, passwordEncoder, "user9", "password123", "Trịnh Văn Ích", 33, "user9@example.com", "0123456788", "852 Đường BCD, Quận 11, TP.HCM", UserEntity.Gender.MALE, "Công ty BCD");
			createUser(userRepository, passwordEncoder, "user10", "password123", "Đinh Thị Kim", 23, "user10@example.com", "0123456789", "963 Đường EFG, Quận 12, TP.HCM", UserEntity.Gender.FEMALE, "Công ty EFG");
			createUser(userRepository, passwordEncoder, "user11", "password123", "Phan Văn Long", 30, "user11@example.com", "0123456790", "159 Đường HIJ, Quận Bình Thạnh, TP.HCM", UserEntity.Gender.MALE, "Công ty HIJ");
			createUser(userRepository, passwordEncoder, "user12", "password123", "Võ Thị Mai", 26, "user12@example.com", "0123456791", "357 Đường KLM, Quận Tân Bình, TP.HCM", UserEntity.Gender.FEMALE, "Công ty KLM");
			createUser(userRepository, passwordEncoder, "user13", "password123", "Hồ Văn Nam", 28, "user13@example.com", "0123456792", "468 Đường NOP, Quận Tân Phú, TP.HCM", UserEntity.Gender.MALE, "Công ty NOP");
			createUser(userRepository, passwordEncoder, "user14", "password123", "Lương Thị Oanh", 24, "user14@example.com", "0123456793", "579 Đường QRS, Quận Phú Nhuận, TP.HCM", UserEntity.Gender.FEMALE, "Công ty QRS");
			createUser(userRepository, passwordEncoder, "user15", "password123", "Tôn Văn Phúc", 32, "user15@example.com", "0123456794", "680 Đường TUV, Quận Gò Vấp, TP.HCM", UserEntity.Gender.MALE, "Công ty TUV");
			createUser(userRepository, passwordEncoder, "user16", "password123", "Chu Thị Quỳnh", 27, "user16@example.com", "0123456795", "791 Đường WXY, Quận Bình Tân, TP.HCM", UserEntity.Gender.FEMALE, "Công ty WXY");
			createUser(userRepository, passwordEncoder, "user17", "password123", "Đỗ Văn Rồng", 29, "user17@example.com", "0123456796", "802 Đường ZAB, Quận Thủ Đức, TP.HCM", UserEntity.Gender.MALE, "Công ty ZAB");
			createUser(userRepository, passwordEncoder, "user18", "password123", "Lê Thị Sương", 25, "user18@example.com", "0123456797", "913 Đường CDE, Quận Hóc Môn, TP.HCM", UserEntity.Gender.FEMALE, "Công ty CDE");
			createUser(userRepository, passwordEncoder, "user19", "password123", "Nguyễn Văn Tùng", 31, "user19@example.com", "0123456798", "024 Đường FGH, Quận Củ Chi, TP.HCM", UserEntity.Gender.MALE, "Công ty FGH");
			createUser(userRepository, passwordEncoder, "user20", "password123", "Trần Thị Uyên", 26, "user20@example.com", "0123456799", "135 Đường IJK, Quận Nhà Bè, TP.HCM", UserEntity.Gender.FEMALE, "Công ty IJK");
			
			System.out.println("Created 20 additional users");
		};
	}
	
	private void createUser(UserRepository userRepository, PasswordEncoder passwordEncoder, 
			String username, String password, String fullName, int age, String email, 
			String phoneNumber, String address, UserEntity.Gender gender, String workplace) {
		UserEntity user = new UserEntity();
		user.setUsername(username);
		user.setPassword(passwordEncoder.encode(password));
		user.setFullName(fullName);
		user.setAge(age);
		user.setEmail(email);
		user.setPhoneNumber(phoneNumber);
		user.setAddress(address);
		user.setGender(gender);
		user.setWorkplace(workplace);
		userRepository.save(user);
		System.out.println("Created user: " + username);
	}
}


