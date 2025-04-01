package org.app.autfmi.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletRequest;
import org.app.autfmi.model.dto.LinkTokenDTO;
import org.app.autfmi.model.dto.UserDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtHelper {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    public UserDTO decodeToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();

        UserDTO user = new UserDTO();
        user.setIdUsuario((Integer) claims.get("id_usuario"));
        user.setIdEmpresa((Integer) claims.get("id_empresa"));
        user.setUsuario((String) claims.get("username"));
        user.setIdRoles((List<Integer>) claims.get("id_roles"));

        return user;
    }

    public String generateToken(UserDTO user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", user.getUsuario());
        claims.put("fullname", user.getNombres() + " " + user.getApellidos());
        claims.put("roles", user.getRoles());
        claims.put("id_roles", user.getIdRoles());
        claims.put("id_usuario", user.getIdUsuario());
        claims.put("id_empresa", user.getIdEmpresa());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getUsuario())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    public static String extractToken(HttpServletRequest httpServletRequest) throws Exception {
        String authorizationHeader = httpServletRequest.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new Exception("Authorization header is missing or invalid");
        }
        return authorizationHeader.split(" ")[1];
    }


    public LinkTokenDTO decodeLinkToken(String token) {
        LinkTokenDTO dataToken = new LinkTokenDTO();
        Claims claims = Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();

        String authToken = (String) claims.get("authToken");
        UserDTO userData = decodeToken(authToken);

        dataToken.setUserData(userData);
        dataToken.setLstrRequerimientos((List<Integer>) claims.get("lst_rq"));

        return dataToken;
    }

}
