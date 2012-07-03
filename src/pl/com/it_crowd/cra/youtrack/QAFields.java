package pl.com.it_crowd.cra.youtrack;

import pl.com.it_crowd.youtrack.api.Commander;

public enum QAFields implements Commander {
    rule,
    revision,
    qaNoteType("qa note type"),
    sourceFile("source file");

// ------------------------------ FIELDS ------------------------------

    private String command;

// --------------------------- CONSTRUCTORS ---------------------------

    private QAFields()
    {
        command = name();
    }

    private QAFields(String command)
    {
        this.command = command;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public String getCommand()
    {
        if (command == null) {
            throw new UnsupportedOperationException("There is no command for field: " + name());
        }
        return command;
    }
}
