package org.bhel.hrm.client.constants;

public class FXMLPaths {
    private FXMLPaths() {
        throw new UnsupportedOperationException("FXMLPaths is a utility class and should not be instantiated.");
    }

    public static final String BASE = "/org/bhel/hrm/client/view/";

    private static final String EXTENSION = ".fxml";

    public static final String LOGIN = BASE + "LoginView" + EXTENSION;

    public static final String MAIN = BASE + "MainView" + EXTENSION;

    public static final String EMPLOYEE_MANAGEMENT = BASE + "EmployeeManagementView" + EXTENSION;

    public static final String LEAVE = BASE + "LeaveView" + EXTENSION;

    public static final String BENEFITS = BASE + "BenefitsView" + EXTENSION;

    public static final String RECRUITMENT = BASE + "RecruitmentView" + EXTENSION;

    public static final String TRAINING_ADMIN = BASE + "TrainingAdminView" + EXTENSION;

    public static final String TRAINING_CATALOG = BASE + "TrainingCatalogView" + EXTENSION;

    public static final String PROFILE = BASE + "ProfileView" + EXTENSION;

    public static class Dialogs {
        private Dialogs() {
            throw new UnsupportedOperationException("FXMLPaths.Dialogs is a utility class and should not be instantiated.");
        }

        public static final String DIALOG = BASE + "dialogs/";

        public static final String EMPLOYEE_FORM = DIALOG + "EmployeeFormView" + EXTENSION;

        public static final String JOB_OPENING_FORM = DIALOG + "JobOpeningForm" + EXTENSION;

        public static final String APPLICANT_STATUS_VIEW = DIALOG + "ApplicantStatusView" + EXTENSION;

        public static final String TRAINING_COURSE_FORM = DIALOG + "TrainingCourseFormView" + EXTENSION;

        public static final String CHANGE_PASSWORD = DIALOG + "ChangePasswordView" + EXTENSION;

        public static final String REPORT = DIALOG + "ReportDialogView" + EXTENSION;
    }
}
