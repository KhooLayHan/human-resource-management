package org.bhel.hrm.client.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import org.bhel.hrm.client.controllers.components.PageHeaderController;
import org.bhel.hrm.client.services.ServiceManager;
import org.bhel.hrm.common.dtos.EmployeeDTO;
import org.bhel.hrm.common.dtos.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.concurrent.ExecutorService;

public class ProfileController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    @FXML private PageHeaderController pageHeaderController;

    @FXML private TextField usernameField;
    @FXML private TextField roleField;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField icPassportField;

    @FXML private Button updateButton;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private UserDTO currentUser;
    private EmployeeDTO currentEmployeeProfile;

    private ServiceManager serviceManager;
    private ExecutorService executorService;

    @Override
    public void initialize() {
        pageHeaderController.setTitle("My Profile");
        pageHeaderController.setSubtitle("View and manage your personal information.");
    }

    public void setDependencies(ServiceManager serviceManager, ExecutorService executorService, UserDTO currentUser) {
        this.serviceManager = serviceManager;
        this.executorService = executorService;
        this.currentUser = currentUser;

        loadProfileData();
    }

    private void loadProfileData() {
        usernameField.setText(currentUser.username());
        usernameField.setText(currentUser.role().toString());


    }
}
