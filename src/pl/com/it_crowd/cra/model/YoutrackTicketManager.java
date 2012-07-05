package pl.com.it_crowd.cra.model;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import pl.com.it_crowd.youtrack.api.Filter;
import pl.com.it_crowd.youtrack.api.IssueWrapper;
import pl.com.it_crowd.youtrack.api.YoutrackAPI;
import pl.com.it_crowd.youtrack.api.rest.AssigneeList;
import pl.com.it_crowd.youtrack.api.rest.AssigneeType;

import javax.xml.bind.JAXBException;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@com.intellij.openapi.components.State(
    name = YoutrackTicketManager.COMPONENT_NAME,
    storages = {@Storage(
        file = "$PROJECT_FILE$")})
public class YoutrackTicketManager implements ProjectComponent, PersistentStateComponent<YoutrackTicketManager.State> {
// ------------------------------ FIELDS ------------------------------

    public static final String COMPONENT_NAME = "YoutrackTicketManager";

    public static final String TICKETS_PROPERTY = "tickets";

    private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    private String password;

    private final Project project;

    private final Map<String, IssueWrapper> tickets = new HashMap<String, IssueWrapper>();

    private String username;

    private YoutrackAPI youtrackAPI;

    private String youtrackProjectID;

    private String youtrackServiceLocation;

// -------------------------- STATIC METHODS --------------------------

    public static YoutrackTicketManager getInstance(Project project)
    {
        return project.getComponent(YoutrackTicketManager.class);
    }

// --------------------------- CONSTRUCTORS ---------------------------

    public YoutrackTicketManager(Project project)
    {
        this.project = project;
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
            if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
                throw new IllegalStateException("Configure Youtrack username and password");
            }
            try {
                //Result is ignored, it's just for validating the youtrackServiceLocation
                new URL(youtrackServiceLocation);
            } catch (MalformedURLException e) {
                throw new RuntimeException("Invalid Youtrack service location: " + youtrackServiceLocation, e);
            }
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

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener)
    {
        changeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public IssueWrapper createTicket(String summary, String description)
    {
        try {
            final YoutrackAPI api = getYoutrackAPI();
            final String issueId = api.createIssue(youtrackProjectID, summary, description);
            final IssueWrapper issue = api.getIssue(issueId);
            tickets.put(issue.getId(), issue);
            changeSupport.firePropertyChange(TICKETS_PROPERTY, null, tickets.values());
            return issue;
        } catch (IOException e) {
            throw new RuntimeException("Cannot create ticket", e);
        } catch (JAXBException e) {
            throw new RuntimeException("Cannot create ticket", e);
        }
    }

    public void fetchTickets(final Filter filter)
    {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Fetching tickets from Youtrack") {
            public void run(@NotNull ProgressIndicator progressIndicator)
            {
                final YoutrackAPI api = getYoutrackAPI();
                final List<IssueWrapper> list;
                try {
                    list = api.searchIssuesByProject(youtrackProjectID, filter);
                } catch (Exception e) {
                    throw new RuntimeException("Problems fetching tickets from Youtrack", e);
                }
                if (progressIndicator.isCanceled()) {
                    return;
                }
                synchronized (tickets) {
                    tickets.clear();
                    for (IssueWrapper ticket : list) {
                        tickets.put(ticket.getId(), ticket);
                    }
                    changeSupport.firePropertyChange(TICKETS_PROPERTY, null, tickets.values());
                }
            }
        });
    }

    public List<AssigneeType> getAssignees() throws JAXBException, IOException
    {
        final YoutrackAPI api = getYoutrackAPI();
        final AssigneeList assigneeList = api.getAssignees(youtrackProjectID);
        return assigneeList.getAssignees().getAssignees();
    }

    public IssueWrapper getTicket(String ticketId)
    {
        return tickets.get(ticketId);
    }

    public String getTicketURL(String ticketId)
    {
        return String.format("%s/issue/%s", youtrackServiceLocation, ticketId);
    }

    public Collection<IssueWrapper> getTickets()
    {
        return tickets.values();
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
