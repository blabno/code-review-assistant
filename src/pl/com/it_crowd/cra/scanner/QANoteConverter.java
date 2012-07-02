package pl.com.it_crowd.cra.scanner;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.StringTokenizer;
import java.util.regex.Matcher;

public class QANoteConverter {
// ------------------------------ FIELDS ------------------------------

    private ExceptionHandler exceptionHandler;

// --------------------- GETTER / SETTER METHODS ---------------------

    public void setExceptionHandler(ExceptionHandler exceptionHandler)
    {
        this.exceptionHandler = exceptionHandler;
    }

// -------------------------- OTHER METHODS --------------------------

    @NotNull
    public QANote getAsObject(String comment)
    {
        QANote note;
        if (comment.contains(QAViolation.TAG)) {
            final QAViolation violation = new QAViolation();
            note = violation;
            violation.setRuleId(extractRuleId(comment));
        } else {
            note = new QASuggestion();
        }
        note.setId(extractNoteId(comment));
        note.setTicket(extractTicket(comment));
        note.setReporter(extractAuthor(comment));
        note.setRecipient(extractRecipient(comment));
        note.setDescription(extractDescription(comment));
        note.setRevision(extractRevision(comment));
        return note;
    }

    public String getAsString(QANote note)
    {
        final StringBuilder builder = new StringBuilder("/**\n");
        builder.append(" * ").append(note.getTagName()).append("\n").append(" * ").append(note.getDescription()).append("\n");
        if (note.getId() != null) {
            builder.append(" * @noteId: ").append(note.getId()).append("\n");
        }
        if (note instanceof QAViolation) {
            final Long ruleId = ((QAViolation) note).getRuleId();
            if (ruleId != null) {
                builder.append(" * @rule: ").append(ruleId).append("\n");
            }
        }
        if (!StringUtils.isBlank(note.getReporter())) {
            builder.append(" * @reporter: ").append(note.getReporter()).append("\n");
        }
        if (!StringUtils.isBlank(note.getTicket())) {
            builder.append(" * @ticket: ").append(note.getTicket()).append("\n");
        }
        if (!StringUtils.isBlank(note.getRecipient())) {
            builder.append(" * @recipient: ").append(note.getRecipient()).append("\n");
        }
        if (!StringUtils.isBlank(note.getRevision())) {
            builder.append(" * @revision: ").append(note.getRevision()).append("\n");
        }
        return builder.append(" */").toString();
    }

    private String extractAuthor(String comment)
    {
        final Matcher matcher = QANoteScanner.AUTHOR_TAG_PATTERN.matcher(comment);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    private String extractDescription(String comment)

    {
        final StringTokenizer tokenizer = new StringTokenizer(comment, "\n");
        String line;
        final StringBuilder stringBuilder = new StringBuilder();
        while (tokenizer.hasMoreTokens()) {
            line = tokenizer.nextToken();
            String descriptionPart = line;
            do {
                line = descriptionPart.trim();
                descriptionPart = line.replaceAll("(\\*/)|(^([/][*])*\\s*\\*+)|" + QANoteScanner.QA_TAGS + "|" + QANoteScanner.AUTHOR_TAG_PATTERN_STRING + "|"
                    + QANoteScanner.RECIPIENT_TAG_PATTERN_STRING + "|" + QANoteScanner.RULE_ID_TAG_PATTERN_STRING + "|"
                    + QANoteScanner.TICKET_TAG_PATTERN_STRING + "|" + QANoteScanner.NOTE_ID_TAG_PATTERN_STRING + "|" + QANoteScanner.REVISION_TAG_PATTERN_STRING
                    + "|//", "");
            } while (!line.equals(descriptionPart));
            stringBuilder.append(descriptionPart);
            stringBuilder.append("\n");
        }
        return stringBuilder.toString().trim();
    }

    private Long extractNoteId(String comment)
    {
        final Matcher matcher = QANoteScanner.NOTE_ID_TAG_PATTERN.matcher(comment);
        if (matcher.find()) {
            try {
                return new Long(matcher.group(1));
            } catch (NumberFormatException e) {
                handleException(e);
            }
        }
        return null;
    }

    private String extractRecipient(String comment)

    {
        final Matcher matcher = QANoteScanner.RECIPIENT_TAG_PATTERN.matcher(comment);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    private String extractRevision(String comment)
    {
        final Matcher matcher = QANoteScanner.REVISION_TAG_PATTERN.matcher(comment);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    private Long extractRuleId(String comment)
    {
        final Matcher matcher = QANoteScanner.RULE_ID_TAG_PATTERN.matcher(comment);
        if (matcher.find()) {
            try {
                return new Long(matcher.group(1));
            } catch (NumberFormatException e) {
                handleException(e);
            }
        }
        return null;
    }

    private String extractTicket(String comment)
    {
        final Matcher matcher = QANoteScanner.TICKET_TAG_PATTERN.matcher(comment);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    private void handleException(Exception e)
    {
        if (exceptionHandler != null) {
            exceptionHandler.handle(e);
        } else {
            throw new RuntimeException(e);
        }
    }
}
