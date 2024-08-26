package TurnosOnline.ScapeRoomOnline.Config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Extrae el token JWT del encabezado de autorización
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);  // Elimina el prefijo "Bearer "

            try {
                // Valida el token y obtiene el nombre de usuario
                String username = jwtService.extractUsername(token);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // Verifica la validez del token
                    if (jwtService.validateToken(token, username)) {
                        // Crea la autenticación y configúrala en el contexto de seguridad
                        Authentication authentication = jwtService.getAuthentication(token);
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } else {
                        // Token inválido
                        httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido");
                        return;
                    }
                }
            } catch (Exception e) {
                // Maneja excepciones relacionadas con el token
                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido");
                return;
            }
        }

        chain.doFilter(request, response);
    }
}

