package demo_tt.demo_tt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DemoTtApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoTtApplication.class, args);
	}

}
