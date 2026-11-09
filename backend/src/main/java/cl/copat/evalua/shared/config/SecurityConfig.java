package cl.copat.evalua.shared.config;

import cl.copat.evalua.departamento1.dominio.Mensajes;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.function.Supplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfig {

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  SecurityFilterChain filtro(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
    CookieCsrfTokenRepository csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
    csrf.setCookiePath("/");

    http
        .csrf(config -> config
            .csrfTokenRepository(csrf)
            .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
        .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            .sessionFixation().changeSessionId())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(new AntPathRequestMatcher("/api/auth/login", HttpMethod.POST.name())).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/api/auth/csrf", HttpMethod.GET.name())).permitAll()
            .requestMatchers("/api/**").authenticated()
            .anyRequest().denyAll())
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, error) ->
                escribir(objectMapper, response, 401, Mensajes.SESION))
            .accessDeniedHandler((request, response, error) ->
                escribir(objectMapper, response, 403, Mensajes.PROHIBIDO)))
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable);
    return http.build();
  }

  private static void escribir(ObjectMapper objectMapper, HttpServletResponse response, int status, String mensaje)
      throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    objectMapper.writeValue(response.getWriter(), new cl.copat.evalua.shared.web.ErrorRespuesta(mensaje));
  }

  static final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
    private final CsrfTokenRequestHandler plano = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
      this.xor.handle(request, response, csrfToken);
      csrfToken.get();
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
      String encabezado = request.getHeader(csrfToken.getHeaderName());
      return (StringUtils.hasText(encabezado) ? this.plano : this.xor).resolveCsrfTokenValue(request, csrfToken);
    }
  }

  static final class CsrfCookieFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        jakarta.servlet.FilterChain filterChain) throws jakarta.servlet.ServletException, IOException {
      Object atributo = request.getAttribute(CsrfToken.class.getName());
      if (atributo instanceof CsrfToken token) {
        token.getToken();
      } else if (atributo instanceof Supplier<?> supplier && supplier.get() instanceof CsrfToken token) {
        token.getToken();
      }
      filterChain.doFilter(request, response);
    }
  }
}
