package org.bhel.hrm.client.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.bhel.hrm.client.controllers.components.PageHeaderController;
import org.bhel.hrm.client.utils.DialogManager;
import org.bhel.hrm.common.dtos.TrainingCourseDTO;
import org.bhel.hrm.common.dtos.TrainingEnrollmentDTO;
import org.bhel.hrm.common.dtos.UserDTO;
import org.bhel.hrm.common.services.HRMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public class MyTrainingController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(MyTrainingController.class);

    @FXML private PageHeaderController pageHeaderController;
    @FXML private TableView<EnrollmentViewModel> enrollmentTable;
    @FXML private TableColumn<EnrollmentViewModel, String> courseTitleColumn;
    @FXML private TableColumn<EnrollmentViewModel, String> departmentColumn;
    @FXML private TableColumn<EnrollmentViewModel, String> dateColumn;
    @FXML private TableColumn<EnrollmentViewModel, String> statusColumn;

    private HRMService hrmService;
    private ExecutorService executorService;
    private UserDTO currentUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        pageHeaderController.setTitle("My Training");
        pageHeaderController.setSubtitle("View the status of your training enrollments.");

        setupColumns();
    }

    public void setDependencies(HRMService service, ExecutorService executor, UserDTO user) {
        this.hrmService = service;
        this.executorService = executor;
        this.currentUser = user;
        loadData();
    }

    private void setupColumns() {
        courseTitleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().courseTitle));
        departmentColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().department));
        dateColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().date));
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status));
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    private void loadData() {
        enrollmentTable.setPlaceholder(new Label("Loading records..."));

        Task<List<EnrollmentViewModel>> task = new Task<>() {
            @Override
            protected List<EnrollmentViewModel> call() throws Exception {
                // 1. Get Employee ID from User ID
                // Note: Ensure UserDTO has userId() accessor. Using .userId() assuming record.
                int employeeId = hrmService.getEmployeeByUserId(currentUser.id()).id();

                // 2. Fetch Enrollments
//                List<TrainingEnrollmentDTO> enrollments = hrmService.getEmployeeTrainingEnrollments(employeeId);

                // 3. Fetch All Courses (To resolve Course ID -> Title)
                // In a real app, you might optimize this, but for this scale, it's fine.
//                List<TrainingCourseDTO> courses = hrmService.getAllTrainingCourses();
//                Map<Integer, TrainingCourseDTO> courseMap = courses.stream()
//                        .collect(Collectors.toMap(TrainingCourseDTO::id, c -> c));

                // 4. Combine into ViewModel
//                return enrollments.stream().map(e -> {
//                    TrainingCourseDTO course = courseMap.get(e.courseId());
//                    String title = (course != null) ? course.title() : "Unknown Course (" + e.courseId() + ")";
//                    String dept = (course != null) ? course.department() : "-";
//
//                    return new EnrollmentViewModel(
//                            title,
//                            dept,
//                            e.enrollmentDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
//                            e.status().name()
//                    );
//                }).collect(Collectors.toList());
                return null;

            };
        };

        task.setOnSucceeded(e -> {
            enrollmentTable.setItems(FXCollections.observableArrayList(task.getValue()));
            if (task.getValue().isEmpty()) {
                enrollmentTable.setPlaceholder(new Label("You have not enrolled in any training courses yet."));
            }
        });

        task.setOnFailed(e -> {
            logger.error("Failed to load training history", task.getException());
            DialogManager.showErrorDialog("Load Error", "Failed to retrieve training history.");
        });

        executorService.submit(task);
    }

    // Inner class to represent the row data
    private record EnrollmentViewModel(String courseTitle, String department, String date, String status) {}
}