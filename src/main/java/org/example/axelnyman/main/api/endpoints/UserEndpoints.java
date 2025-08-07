package org.example.axelnyman.main.api.endpoints;

import org.example.axelnyman.main.domain.abstracts.IDomainService;
import org.example.axelnyman.main.domain.dtos.UserDtos.UserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
public class UserEndpoints {

    private final IDomainService domainService;

    public UserEndpoints(IDomainService domainService) {
        this.domainService = domainService;
    }


    @GetMapping("/users/{id}")
    public CompletableFuture<ResponseEntity<UserResponse>> getUserById(@PathVariable Long id) {
        return domainService.getUserById(id)
                .thenApply(optionalUser -> optionalUser
                        .map(user -> ResponseEntity.ok(user))
                        .orElse(ResponseEntity.notFound().build()));
    }

    @GetMapping("/users")
    public CompletableFuture<ResponseEntity<List<UserResponse>>> getAllUsers() {
        return domainService.getAllUsers()
                .thenApply(users -> ResponseEntity.ok(users));
    }

    @DeleteMapping("/users/{id}")
    public CompletableFuture<ResponseEntity<Void>> deleteUser(@PathVariable Long id) {
        return domainService.deleteUser(id)
                .thenApply(deleted -> deleted
                        ? ResponseEntity.noContent().<Void>build()
                        : ResponseEntity.notFound().<Void>build());
    }
}
