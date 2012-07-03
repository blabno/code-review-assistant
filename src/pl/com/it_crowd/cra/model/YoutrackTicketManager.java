package pl.com.it_crowd.cra.model;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.apache.http.auth.AuthenticationException;
import org.jetbrains.annotations.NotNull;
import pl.com.it_crowd.youtrack.api.IssueWrapper;
import pl.com.it_crowd.youtrack.api.YoutrackAPI;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@com.intellij.openapi.components.State(
    name = YoutrackTicketManager.COMPONENT_NAME,
    storages = {@Storage(
        file = "$PROJECT_FILE$")})
public class YoutrackTicketManager implements ProjectComponent, PersistentStateComponent<YoutrackTicketManager.State> {
// ------------------------------ FIELDS ------------------------------

    public static final String COMPONENT_NAME = "YoutrackTicketManager";

    private String password;

    private Map<String, IssueWrapper> tickets = new HashMap<String, IssueWrapper>();

    private String username;

    private YoutrackAPI youtrackAPI;

    private String youtrackProjectID;

    private String youtrackServiceLocation;

// -------------------------- STATIC METHODS --------------------------

    public static YoutrackTicketManager getInstance(Project project)
    {
        return project.getComponent(YoutrackTicketManager.class);
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        youtrackAPI = null;
        this.password = password;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        youtrackAPI = null;
        this.username = username;
    }

    private YoutrackAPI getYoutrackAPI()
    {
        if (youtrackAPI == null) {
            try {
                youtrackAPI = new YoutrackAPI(youtrackServiceLocation, username, password);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return youtrackAPI;
    }

    public String getYoutrackProjectID()
    {
        return youtrackProjectID;
    }

    public void setYoutrackProjectID(String youtrackProjectID)
    {
        this.youtrackProjectID = youtrackProjectID;
    }

    public String getYoutrackServiceLocation()
    {
        return youtrackServiceLocation;
    }

    public void setYoutrackServiceLocation(String youtrackServiceLocation)
    {
        youtrackAPI = null;
        this.youtrackServiceLocation = youtrackServiceLocation;
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface BaseComponent ---------------------

    public void initComponent()
    {
    }

    public void disposeComponent()
    {
    }

// --------------------- Interface NamedComponent ---------------------

    @NotNull
    public String getComponentName()
    {
        return COMPONENT_NAME;
    }

// --------------------- Interface PersistentStateComponent ---------------------

    public State getState()
    {
        return new State(username, password, youtrackProjectID, youtrackServiceLocation);
    }

    public void loadState(State state)
    {
        username = state.username;
        password = state.password;
        youtrackProjectID = state.projectID;
        youtrackServiceLocation = state.serviceLocation;
    }

// --------------------- Interface ProjectComponent ---------------------

    public void projectOpened()
    {
        youtrackAPI = null;
        tickets.clear();
    }

    public void projectClosed()
    {
        youtrackAPI = null;
        tickets.clear();
    }

// -------------------------- OTHER METHODS --------------------------

    public IssueWrapper createTicket(String summary, String description)
    {
        try {
            final YoutrackAPI api = getYoutrackAPI();
            final String issueId = api.createIssue(youtrackProjectID, summary, description);
            final IssueWrapper issue = api.getIssue(issueId);
            tickets.put(issue.getId(), issue);
            return issue;
        } catch (IOException e) {
            throw new RuntimeException("Cannot create ticket", e);
        } catch (JAXBException e) {
            throw new RuntimeException("Cannot create ticket", e);
        }
    }

    public IssueWrapper getTicket(String ticketId) throws IOException, AuthenticationException, JAXBException
    {
        final YoutrackAPI api = getYoutrackAPI();
        return api.getIssue(ticketId);
    }

    public void updateTicket(String ticketId, String command) throws IOException
    {
        final YoutrackAPI api = getYoutrackAPI();
        api.command(ticketId, command);
    }

// -------------------------- INNER CLASSES --------------------------

    public static class State {
// ------------------------------ FIELDS ------------------------------

        //TODO encrypt this field
        public String password;

        public String projectID;

        public String serviceLocation;

        public String username;

// --------------------------- CONSTRUCTORS ---------------------------

        public State()
        {
        }

        public State(String username, String password, String projetID, String serviceLocation)
        {
            this();
            this.username = username;
            this.password = password;
            this.serviceLocation = serviceLocation;
            this.projectID = projetID;
        }
    }
}
