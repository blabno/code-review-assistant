package pl.com.it_crowd.cra.model;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import pl.com.it_crowd.cra.ui.CodeReviewAssistantPanel;
import pl.com.it_crowd.cra.ui.SettingsForm;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;

public class CodeReviewProjectConfigurable implements Configurable {
// ------------------------------ FIELDS ------------------------------

    private Project project;

    private SettingsForm settingsForm;

// --------------------------- CONSTRUCTORS ---------------------------

    public CodeReviewProjectConfigurable(Project project)
    {
        this.project = project;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public boolean isModified()
    {
        boolean modified = false;
        if (settingsForm != null) {
            modified = settingsForm.isModified();
        }
        return modified;
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface Configurable ---------------------

    @Nls
    public String getDisplayName()
    {
        return "Code review assistant";  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Icon getIcon()
    {
        return new ImageIcon(CodeReviewAssistantPanel.class.getResource("/icons/code-review-small.png"));
    }

    public String getHelpTopic()
    {
        return null;
    }

// --------------------- Interface UnnamedConfigurable ---------------------

    public JComponent createComponent()
    {
        settingsForm = new SettingsForm(project.getComponent(YoutrackTicketManager.class));
        return settingsForm.$$$getRootComponent$$$();
    }

    public void apply() throws ConfigurationException
    {
        if (settingsForm != null) {
            settingsForm.apply();
        }
    }

    public void reset()
    {
        if (settingsForm != null) {
            settingsForm.reset();
        }
    }

    public void disposeUIResources()
    {
    }
}
