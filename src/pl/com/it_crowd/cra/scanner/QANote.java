package pl.com.it_crowd.cra.scanner;

import org.jetbrains.annotations.Nullable;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public abstract class QANote {
// ------------------------------ FIELDS ------------------------------

    public static final String DESCRIPTION_PROPERTY = "description";

    public static final String FILE_PROPERTY = "file";

    public static final String RECIPIENT_PROPERTY = "recipient";

    public static final String REPORTER_PROPERTY = "reporter";

    public static final String REVISION_PROPERTY = "revision";

    public static final String TICKET_PROPERTY = "ticket";

    protected PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    private String description;

    private String fileName;

    private Long id;

    private String recipient;

    private String reporter;

    private Long revision;

    private String ticket;

// --------------------- GETTER / SETTER METHODS ---------------------

    public String getDescription()
    {
        return description;
    }

    public String getFileName()
    {
        return fileName;
    }

    public Long getId()
    {
        return id;
    }

    public void setId(@Nullable Long id)
    {
        this.id = id;
    }

    public String getRecipient()
    {
        return recipient;
    }

    public String getReporter()
    {
        return reporter;
    }

    public Long getRevision()
    {
        return revision;
    }

    public String getTicket()

    {
        return ticket;
    }

// ------------------------ CANONICAL METHODS ------------------------

    @Override
    public String toString()
    {
        return "QANote{" +
            "reporter='" + reporter + '\'' +
            ", description='" + description + '\'' +
            ", fileName='" + fileName + '\'' +
            ", id=" + id +
            ", recipient='" + recipient + '\'' +
            ", revision='" + revision + '\'' +
            '}';
    }

// -------------------------- OTHER METHODS --------------------------

    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public void setDescription(String description)
    {
        final Object oldValue = this.description;
        this.description = description;
        propertyChangeSupport.firePropertyChange(DESCRIPTION_PROPERTY, oldValue, description);
    }

    public void setFileName(String fileName)
    {
        final Object oldValue = this.fileName;
        this.fileName = fileName;
        propertyChangeSupport.firePropertyChange(FILE_PROPERTY, oldValue, fileName);
    }

    public void setRecipient(String recipient)
    {
        final Object oldValue = this.recipient;
        this.recipient = recipient;
        propertyChangeSupport.firePropertyChange(RECIPIENT_PROPERTY, oldValue, recipient);
    }

    public void setReporter(String reporter)
    {
        final Object oldValue = this.reporter;
        this.reporter = reporter;
        propertyChangeSupport.firePropertyChange(REPORTER_PROPERTY, oldValue, reporter);
    }

    public void setRevision(Long revision)
    {
        final Object oldValue = this.revision;
        this.revision = revision;
        propertyChangeSupport.firePropertyChange(REVISION_PROPERTY, oldValue, revision);
    }

    public void setTicket(String ticket)
    {
        final Object oldValue = this.ticket;
        this.ticket = ticket;
        propertyChangeSupport.firePropertyChange(TICKET_PROPERTY, oldValue, ticket);
    }

    protected abstract String getTagName();
}
