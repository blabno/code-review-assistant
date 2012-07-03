package pl.com.it_crowd.cra.youtrack;

public class QACommand extends pl.com.it_crowd.youtrack.api.Command {
// -------------------------- STATIC METHODS --------------------------

    public static QACommand qaNoteTypeCommand(QANoteTypeValues qaNoteType)
    {
        return new QACommand().qaNoteType(qaNoteType);
    }

    public static QACommand revisionCommand(Long revision)
    {
        return new QACommand().revision(revision);
    }

    public static QACommand ruleCommand(String rule)
    {
        return new QACommand().rule(rule);
    }

    public static QACommand sourceFileCommand(String sourceFile)
    {
        return new QACommand().sourceFile(sourceFile);
    }

// --------------------------- CONSTRUCTORS ---------------------------

    private QACommand()
    {
    }

// -------------------------- OTHER METHODS --------------------------

    public QACommand qaNoteType(QANoteTypeValues qaNoteType)
    {
        return (QACommand) command(QAFields.qaNoteType, qaNoteType.getCommandValue());
    }

    public QACommand revision(Long revision)
    {
        return (QACommand) command(QAFields.revision, revision.toString());
    }

    public QACommand rule(String rule)
    {
        return (QACommand) command(QAFields.rule, rule);
    }

    public QACommand sourceFile(String sourceFile)
    {
        return (QACommand) command(QAFields.sourceFile, sourceFile);
    }
}
