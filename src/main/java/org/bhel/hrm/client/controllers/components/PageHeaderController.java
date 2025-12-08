package org.bhel.hrm.client.controllers.components;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Controller for the reusable PageHeader component.
 * Provides methods to dynamically set the title and subtitle.
 */
public class PageHeaderController {
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;

    public void setTitle(String title) {
        if (titleLabel != null)
            titleLabel.setText(title);
    }

    public void setSubtitle(String subtitle) {
        if (subtitleLabel != null)
            subtitleLabel.setText(subtitle);
    }

    public void hideSubtitle() {
        if (subtitleLabel != null) {
            subtitleLabel.setVisible(false);
            subtitleLabel.setManaged(false);
        }
    }
}
