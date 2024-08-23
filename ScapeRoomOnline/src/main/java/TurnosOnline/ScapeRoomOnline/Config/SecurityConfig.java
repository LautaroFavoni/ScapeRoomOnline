package TurnosOnline.ScapeRoomOnline.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Desactiva CSRF solo si estás seguro de que no es necesario
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/public/**").permitAll() // Asegura que todos los endpoints con /public son accesibles
                        .anyRequest().authenticated() // Requiere autenticación para todos los demás
                )
                .formLogin(login -> login // Configura la página de inicio de sesión
                        .loginPage("/login")
                        .permitAll()
                )
                .logout(logout -> logout.permitAll()); // Permite el acceso al logout

        return http.build();
    }


    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.withDefaultPasswordEncoder()
                .username("USER")
                .password("PASSWORD")
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(user);
    }
}
