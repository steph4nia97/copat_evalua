package cl.copat.evalua.shared.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RelojConfig {

  @Bean
  Clock reloj(@Value("${copat.zona}") String zona) {
    return Clock.system(ZoneId.of(zona));
  }
}
