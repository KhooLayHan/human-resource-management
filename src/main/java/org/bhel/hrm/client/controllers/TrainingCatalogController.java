package org.bhel.hrm.client.controllers;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
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

public class TrainingCatalogController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(TrainingCatalogController.class);

    @FXML private PageHeaderController pageHeaderController;
    @FXML private ScrollPane scrollPane;
    @FXML private FlowPane coursesContainer;

    private HRMService hrmService;
    private ExecutorService executorService;
    private UserDTO currentUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        pageHeaderController.setTitle("Training Catalog");
        pageHeaderController.setSubtitle("Browse available courses and check your enrollment status.");

        // Bind width to make the flow pane responsive
        coursesContainer.prefWidthProperty().bind(scrollPane.widthProperty().subtract(20));
    }

    public void setDependencies(HRMService service, ExecutorService executor, UserDTO user) {
        this.hrmService = service;
        this.executorService = executor;
        this.currentUser = user;
        loadCatalogData();
    }

    /**
     * Data carrier for our background task.
     */
    private record CatalogData(
            int employeeId,
            List<TrainingCourseDTO> courses,
            Map<Integer, TrainingEnrollmentDTO> enrollmentMap // Map<CourseID, Enrollment>
    ) {}

    private void loadCatalogData() {
        coursesContainer.getChildren().clear();
        coursesContainer.getChildren().add(new Label("Loading catalog..."));

        Task<CatalogData> task = new Task<>() {
            @Override
            protected CatalogData call() throws Exception {
                // 1. Get Employee ID
                int empId = hrmService.getEmployeeByUserId(currentUser.id()).id();

                // 2. Get All Courses
                List<TrainingCourseDTO> courses = hrmService.getAllTrainingCourses();

                // 3. Get My Enrollments
                List<TrainingEnrollmentDTO> enrollments = hrmService.getEmployeeTrainingEnrollments(empId);

                // 4. Map enrollments by CourseID for O(1) lookup
                Map<Integer, TrainingEnrollmentDTO> map = enrollments.stream()
                        .collect(Collectors.toMap(TrainingEnrollmentDTO::courseId, e -> e));

                return new CatalogData(empId, courses, map);
            }
        };

        task.setOnSucceeded(e -> renderCards(task.getValue()));

        task.setOnFailed(e -> {
            logger.error("Failed to load catalog", task.getException());
            DialogManager.showErrorDialog("Load Error", "Could not load training catalog.");
        });

        executorService.submit(task);
    }

    private void renderCards(CatalogData data) {
        coursesContainer.getChildren().clear();

        if (data.courses().isEmpty()) {
            coursesContainer.getChildren().add(new Label("No training courses available at the moment."));
            return;
        }

        for (TrainingCourseDTO course : data.courses()) {
            // Check if enrolled
            TrainingEnrollmentDTO existingEnrollment = data.enrollmentMap().get(course.id());

            VBox card = createCourseCard(course, existingEnrollment, data.employeeId());
            coursesContainer.getChildren().add(card);
        }
    }

    private VBox createCourseCard(TrainingCourseDTO course, TrainingEnrollmentDTO enrollment, int employeeId) {
        VBox card = new VBox(10); // 10px spacing vertical
        card.setPadding(new Insets(15));
        card.setPrefWidth(280);
        card.setMinHeight(220);

        // Base Card Style: White background, subtle shadow, rounded corners
        String baseStyle = "-fx-background-color: white; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0); " +
                "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-width: 1; ";

        // --- 1. Header (Title + Department Badge) ---
        String titleText = (course.title() != null && !course.title().isEmpty()) ? course.title() : "Untitled Course";

        // FIX 1: Convert Enum to String using .toString() (or .name())
        String deptText = (course.department() != null) ? course.department().toString() : "GEN";

        Label title = new Label(titleText);
        title.setWrapText(true);
        // Force text fill to almost black so it's visible
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #222222;");
        title.setMaxWidth(180);

        Label dept = new Label(deptText);
        // Badge Style: Light Blue bg, Dark Blue text
        dept.setStyle("-fx-background-color: #E3F2FD; -fx-text-fill: #1565C0; " +
                "-fx-padding: 4 8 4 8; -fx-background-radius: 12; " +
                "-fx-font-size: 10px; -fx-font-weight: bold;");

        // FIX 2: Use Region.USE_PREF_SIZE
        dept.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);

        // Spacer to push department to the right
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(5, title, spacer, dept);
        header.setAlignment(Pos.TOP_LEFT);

        // --- 2. Meta Data (ID + Duration) ---
        Label idLabel = new Label("ID: #" + course.id());
        idLabel.setStyle("-fx-text-fill: #888888; -fx-font-family: 'Monospaced'; -fx-font-size: 11px;");

        Label durLabel = new Label("• " + course.durationInHours() + " Hours");
        durLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px; -fx-font-weight: bold;");

        HBox meta = new HBox(8, idLabel, durLabel);

        // --- 3. Description ---
        String descText = (course.description() != null) ? course.description() : "No description available.";
        Label desc = new Label(descText);
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #444444; -fx-font-size: 12px;");
        desc.setMaxHeight(60);

        VBox.setVgrow(desc, Priority.ALWAYS);

        // --- 4. Footer (Dynamic Action) ---
        VBox footer = new VBox(5);
        footer.setAlignment(Pos.CENTER);

        if (enrollment != null) {
            // STATE: Already Enrolled
            card.setStyle(baseStyle + "-fx-border-color: #4CAF50;");

            Label statusIcon = new Label("✔ Enrolled");
            statusIcon.setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold; -fx-font-size: 14px;");

            String dateStr = enrollment.enrollmentDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Label dateLabel = new Label("On: " + dateStr);
            dateLabel.setStyle("-fx-text-fill: #555555; -fx-font-size: 11px;");

            footer.getChildren().addAll(statusIcon, dateLabel);
        } else {
            // STATE: Not Enrolled
            card.setStyle(baseStyle + "-fx-border-color: #E0E0E0;");

            Button enrollBtn = new Button("Enroll Now");
            enrollBtn.setMaxWidth(Double.MAX_VALUE);
            enrollBtn.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white; " +
                    "-fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 4;");

            enrollBtn.setOnMouseEntered(e -> enrollBtn.setStyle("-fx-background-color: #1565C0; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 4;"));
            enrollBtn.setOnMouseExited(e -> enrollBtn.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 4;"));

            enrollBtn.setOnAction(e -> handleEnroll(course, employeeId));
            footer.getChildren().add(enrollBtn);
        }

        javafx.scene.control.Separator line = new javafx.scene.control.Separator();

        card.getChildren().addAll(header, meta, desc, line, footer);
        return card;
    }

    private void handleEnroll(TrainingCourseDTO course, int employeeId) {
        if (!DialogManager.showConfirmationDialog("Confirm Enrollment", "Enroll in '" + course.title() + "'?")) {
            return;
        }

        Task<Void> enrollTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                hrmService.enrollInTraining(employeeId, course.id());
                return null;
            }
        };

        enrollTask.setOnSucceeded(e -> {
            DialogManager.showInfoDialog("Success", "You have successfully enrolled!");
            // Reload to update the card state to "Enrolled"
            loadCatalogData();
        });

        enrollTask.setOnFailed(e -> {
            logger.error("Enrollment failed", enrollTask.getException());
            DialogManager.showErrorDialog("Enrollment Failed", enrollTask.getException().getMessage());
        });

        executorService.submit(enrollTask);
    }
}