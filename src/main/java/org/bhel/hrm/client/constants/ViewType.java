package org.bhel.hrm.client.constants;

import org.bhel.hrm.common.dtos.UserDTO;

import java.util.EnumSet;
import java.util.Set;

/**
 * Enumeration of all available views in the application.
 * <p>
 * Each enum constant represents a distinct UI view with an associated display name,
 * FXML file path, and a set of user roles that are permitted to access it.
 * This design enables centralized, type-safe view management and role-based access control.
 */
public enum ViewType {
    // Common
    DASHBOARD("Dashboard", FXMLPaths.DASHBOARD, EnumSet.allOf(UserDTO.Role.class)),
    PROFILE("Profile", FXMLPaths.PROFILE, EnumSet.allOf(UserDTO.Role.class)),

    // Employee
    LEAVE("Leave", FXMLPaths.LEAVE, EnumSet.allOf(UserDTO.Role.class)),
    BENEFITS("Benefits", FXMLPaths.BENEFITS, EnumSet.allOf(UserDTO.Role.class)),
    TRAINING_CATALOG("Training Catalog", FXMLPaths.TRAINING_CATALOG, EnumSet.allOf(UserDTO.Role.class)),

    // HR Staff
    EMPLOYEE_MANAGEMENT("Employee Management", FXMLPaths.EMPLOYEE_MANAGEMENT, EnumSet.of(UserDTO.Role.HR_STAFF)),
    RECRUITMENT("Recruitment", FXMLPaths.RECRUITMENT, EnumSet.of(UserDTO.Role.HR_STAFF)),
    TRAINING_ADMIN("Training Admin", FXMLPaths.TRAINING_ADMIN, EnumSet.of(UserDTO.Role.HR_STAFF));

    private final String displayName;
    private final String fxmlPath;
    private final Set<UserDTO.Role> allowedRoles;

    ViewType(
        String displayName,
        String fxmlPath,
        Set<UserDTO.Role> allowedRoles
    ) {
        this.displayName = displayName;
        this.fxmlPath = fxmlPath;
        this.allowedRoles = allowedRoles;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFxmlPath() {
        return fxmlPath;
    }

    public Set<UserDTO.Role> getAllowedRoles() {
        return allowedRoles;
    }

    /**
     * Checks if this view is accessible to the specified user role.
     *
     * @param role The user role to check
     * @return {@code true} if the role is allowed to access this view, false otherwise
     */
    public boolean isAllowedForRole(UserDTO.Role role) {
        return allowedRoles.contains(role);
    }

    /**
     * Retrieves all views that are accessible to the specified user role.
     *
     * @param role The user role for which to find accessible views
     * @return An EnumSet containing all {@link ViewType} constants accessible to the role
     */
    public static Set<ViewType> getViewsForRole(UserDTO.Role role) {
        Set<ViewType> views = EnumSet.noneOf(ViewType.class);

        for (ViewType view : values()) {
            if (view.isAllowedForRole(role))
                views.add(view);
        }

        return views;
    }
}
