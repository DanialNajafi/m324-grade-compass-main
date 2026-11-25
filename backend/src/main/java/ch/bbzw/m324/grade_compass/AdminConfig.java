package ch.bbzw.m324.grade_compass;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminConfig {

    private final String adminPassword;

    public AdminConfig(@Value("${admin.password:}") String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public boolean isAdmin(String user, String password) {
        return "admin".equals(user) && !adminPassword.isEmpty() && adminPassword.equals(password);
    }
}
