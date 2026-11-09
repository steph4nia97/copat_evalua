package cl.copat.evalua;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class CopatEvaluaApplication {

  public static void main(String[] args) {
    SpringApplication.run(CopatEvaluaApplication.class, args);
  }
}
