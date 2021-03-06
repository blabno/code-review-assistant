package pl.com.it_crowd.cra.ui;

import com.intellij.ui.DocumentAdapter;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import pl.com.it_crowd.cra.model.YoutrackTicketManager;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.Arrays;

public class SettingsForm {
// ------------------------------ FIELDS ------------------------------

    private boolean modified;

    private JPanel rootComponent;

    private YoutrackTicketManager ticketManager;

    private JTextField youtrackBaseURL;

    private JPasswordField youtrackPassword;

    private JTextField youtrackProjectID;

    private JTextField youtrackUsername;

// --------------------------- CONSTRUCTORS ---------------------------

    public SettingsForm(YoutrackTicketManager ticketManager)
    {
        this.ticketManager = ticketManager;
        reset();
        final DocumentAdapter documentAdapter = new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent documentEvent)
            {
                modified = true;
            }
        };
        for (JTextComponent component : Arrays.asList(youtrackBaseURL, youtrackUsername, youtrackPassword, youtrackProjectID)) {
            component.getDocument().addDocumentListener(documentAdapter);
        }
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public boolean isModified()
    {
        return modified;
    }

// -------------------------- OTHER METHODS --------------------------

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$()
    {
        return rootComponent;
    }

    public void apply()
    {
        ticketManager.setPassword(new String(youtrackPassword.getPassword()));
        ticketManager.setUsername(youtrackUsername.getText());
        ticketManager.setYoutrackProjectID(youtrackProjectID.getText());
        ticketManager.setYoutrackServiceLocation(youtrackBaseURL.getText());
        modified = false;
    }

    public void reset()
    {
        youtrackBaseURL.setText(ticketManager.getYoutrackServiceLocation());
        youtrackUsername.setText(ticketManager.getUsername());
        youtrackPassword.setText(ticketManager.getPassword());
        youtrackProjectID.setText(ticketManager.getYoutrackProjectID());
        modified = false;
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$()
    {
        rootComponent = new JPanel();
        rootComponent.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
        rootComponent.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Youtrack Rest API"));
        final JLabel label1 = new JLabel();
        label1.setText("Base URL ");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        youtrackBaseURL = new JTextField();
        youtrackBaseURL.setText("");
        panel1.add(youtrackBaseURL,
            new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Username");
        panel1.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Password");
        panel1.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        youtrackUsername = new JTextField();
        panel1.add(youtrackUsername,
            new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        youtrackPassword = new JPasswordField();
        panel1.add(youtrackPassword,
            new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Project ID");
        panel1.add(label4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        youtrackProjectID = new JTextField();
        panel1.add(youtrackProjectID,
            new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final Spacer spacer1 = new Spacer();
        rootComponent.add(spacer1,
            new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null,
                null, 0, false));
    }
}
