package pl.com.it_crowd.cra.youtrack;

import pl.com.it_crowd.cra.scanner.QANote;
import pl.com.it_crowd.cra.scanner.QAViolation;

public enum QANoteTypeValues {
    Suggestion, Violation;

// -------------------------- STATIC METHODS --------------------------

    public static QANoteTypeValues valueOf(QANote note)
    {
        return note instanceof QAViolation ? Violation : Suggestion;
    }

// -------------------------- OTHER METHODS --------------------------

    public String getCommandValue()
    {
        return name();
    }
}

