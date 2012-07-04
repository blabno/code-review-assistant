package pl.com.it_crowd.cra;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.com.it_crowd.cra.model.QANoteManager;
import pl.com.it_crowd.cra.scanner.QANote;

import javax.swing.event.HyperlinkEvent;
import java.io.PrintWriter;
import java.io.StringWriter;

public final class QANotifications {
// ------------------------------ FIELDS ------------------------------

    private static final String NOTE_RELATED_EXCEPTIONS = "QANote exceptions";

    private static final String NOTIFICATIONS = "Code review";

// -------------------------- STATIC METHODS --------------------------

    public static void handle(Throwable throwable, String title, final QANote note, final Project project)
    {
        final StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        String qaNotePresentable = String.format("%s[id=%d]", note.getClass().getSimpleName(), note.getId());
        final String notificationTitle = String.format("<a href='xxx'>%s</a> %s\n%s: %s", qaNotePresentable, title, throwable.getClass().getSimpleName(),
            throwable.getMessage());
        Notification notification = new Notification(NOTE_RELATED_EXCEPTIONS, notificationTitle, writer.toString(), NotificationType.ERROR,
            new NotificationListener() {
                public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event)
                {
                    QANoteManager.getInstance(project).selectNote(note);
                    QANoteManager.getInstance(project).highlightSelectedNote();
                }
            });
        Notifications.Bus.notify(notification, project);
    }

    public static void inform(@NotNull String title, @NotNull String content, @Nullable Project project)
    {
        Notifications.Bus.notify(new Notification(NOTIFICATIONS, title, content, NotificationType.INFORMATION), project);
    }

// --------------------------- CONSTRUCTORS ---------------------------

    private QANotifications()
    {
    }
}
