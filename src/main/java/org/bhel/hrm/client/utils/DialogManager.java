package org.bhel.hrm.client.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.bhel.hrm.common.error.ErrorCode;
import org.bhel.hrm.common.error.ErrorMessageProvider;

import java.util.Optional;

/**
 * A utility class for displaying standardized dialog boxes (Alerts) to the user.
 * It integrates with the ErrorMessageProvider to show consistent, user-friendly error messages.
 */
public final class DialogManager {
    // Create a single, shared instance of the message provider for the client.
    private static final ErrorMessageProvider messageProvider = new ErrorMessageProvider();

    private DialogManager() {
        throw new UnsupportedOperationException("DialogManager is a utility class and should not be instantiated.");
    }

    // --- Information and Success Dialogs ---

    /**
     * Shows a standard information dialog.
     *
     * @param title The title of the dialog window
     * @param message The message to display to the user
     */
    public static void showInfoDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // --- Warning and Confirmation Dialogs ---

    /**
     * Shows a standard warning dialog.
     *
     * @param title The title of the dialog window
     * @param message The warning message to display to the user
     */
    public static void showWarningDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows a confirmation dialog with "OK" and "Cancel" buttons.
     *
     * @param title The title of the dialog window
     * @param question The question to ask the user (e.g., "Are you sure you want to delete this?")
     * @return {@code true} if the user clicks OK, false otherwise
     */
    public static boolean showConfirmationDialog(String title, String question) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(question);

        Optional<ButtonType> result = alert.showAndWait();
        return
            result.isPresent() &&
            result.get() == ButtonType.OK
        ;
    }

    // --- Error Dialogs ---

    /**
     * Shows a standardized error dialog using an ErrorCode.
     * This is the preferred method for displaying errors to the user, as it uses
     * the centralized error messages from the properties file.
     *
     * @param title The title of the error dialog
     * @param errorCode The ErrorCode that corresponds to this error
     */
    public static void showErrorDialog(String title, ErrorCode errorCode) {
        String message = messageProvider.getMessage(errorCode);

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Overloaded method to show an error dialog with a custom message.
     * Should be used for errors that don't have a specific ErrorCode.
     *
     * @param title The title of the error dialog
     * @param message The custom error message to display
     */
    public static void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
