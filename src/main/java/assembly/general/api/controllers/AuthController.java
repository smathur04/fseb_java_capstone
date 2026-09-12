package assembly.general.api.controllers;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    @PostMapping("/auth/register")
    public String authRegister() {
        return "register";
    }

    @PostMapping("/auth/login")
    public String authLogin() {
        return "login";
    }
}