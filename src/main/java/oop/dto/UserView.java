package oop.dto;

import oop.User;

public record UserView(String username, String role, String roleText,
                       String displayName, String customerId) {

    public static UserView from(User user) {
        return new UserView(user.getUsername(),
                user.getRole() == null ? "" : user.getRole().name(),
                user.getRoleText(),
                user.getDisplayName(),
                user.getCustomerId());
    }
}
