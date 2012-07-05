package pl.com.it_crowd.cra.scanner;

import org.apache.commons.lang.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QANoteScanner {
// ------------------------------ FIELDS ------------------------------

    static final Pattern ASSIGNEE_TAG_PATTERN;

    static String ASSIGNEE_TAG_PATTERN_STRING = "@assignee\\s*:\\s*((\\w|\\.)+)\\s*";

    static final Pattern AUTHOR_TAG_PATTERN;

    static final String AUTHOR_TAG_PATTERN_STRING = "@reporter\\s*:\\s*((\\w|\\.)+)\\s*";

    static final Pattern NOTE_ID_TAG_PATTERN;

    static final String NOTE_ID_TAG_PATTERN_STRING = "@noteId\\s*:\\s*(\\d+)";

    static final Pattern QA_COMMENT_PATTERN;

    private static final String QA_COMMENT_PATTERN_STRING;

    static final String QA_TAGS = "QA-VIOLATION|QA-SUGGESTION|QA-REVIEW";

    static final Pattern REVISION_TAG_PATTERN;

    static final String REVISION_TAG_PATTERN_STRING = "@revision\\s*:\\s*(\\d+)";

    static final Pattern RULE_ID_TAG_PATTERN;

    static final String RULE_ID_TAG_PATTERN_STRING = "@ruleId\\s*:\\s*(\\d+)";

    static final Pattern TICKET_TAG_PATTERN;

    static final String TICKET_TAG_PATTERN_STRING = "@ticket\\s*:\\s*((\\w|-)+)\\s*";

    private File directoryToScan;

    private final FileFilter filter = new FileFilter() {
        public boolean accept(File file)
        {
            final String name = file.getName();
            return (file.isDirectory() && !name.endsWith(".svn")) || name.endsWith(".java");
        }
    };

    private QANoteConverter noteConverter = new QANoteConverter();

    private Long revision;

    private String rootPath;

    private long temporaryIdSequence = 0;

// -------------------------- STATIC METHODS --------------------------

    static {
        /**
         * Old too heavy pattern that sometimes hangs the JVM
         */
//        QA_COMMENT_PATTERN_STRING =
//            "(/\\*+([^*]|[\\r\\n]|(\\*+([^*/]|[\\r\\n])))*(" + QA_TAGS + ")([^*]|[\\r\\n]|(\\*+([^*/]|[\\r\\n])))*\\*+/)|(//.*(" + QA_TAGS + ").*)";
        QA_COMMENT_PATTERN_STRING = "/\\*+([^*]|[\\r\\n]|(\\*+([^*/]|[\\r\\n])))*\\*+/|(//.*)";
        AUTHOR_TAG_PATTERN = Pattern.compile(AUTHOR_TAG_PATTERN_STRING);
        NOTE_ID_TAG_PATTERN = Pattern.compile(NOTE_ID_TAG_PATTERN_STRING);
        ASSIGNEE_TAG_PATTERN = Pattern.compile(ASSIGNEE_TAG_PATTERN_STRING);
        REVISION_TAG_PATTERN = Pattern.compile(REVISION_TAG_PATTERN_STRING);
        RULE_ID_TAG_PATTERN = Pattern.compile(RULE_ID_TAG_PATTERN_STRING);
        QA_COMMENT_PATTERN = Pattern.compile(QA_COMMENT_PATTERN_STRING);
        TICKET_TAG_PATTERN = Pattern.compile(TICKET_TAG_PATTERN_STRING);
    }

// --------------------------- CONSTRUCTORS ---------------------------

    public QANoteScanner()
    {
        noteConverter.setExceptionHandler(new ExceptionHandler() {
            public void handle(Exception e)
            {
                System.err.println(e);
            }
        });
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public File getDirectoryToScan()
    {
        return directoryToScan;
    }

    public void setDirectoryToScan(File directoryToScan)
    {
        this.directoryToScan = directoryToScan;
    }

    public String getRootPath()
    {
        return rootPath;
    }

    public void setRootPath(String rootPath)
    {
        this.rootPath = rootPath;
    }

    public void setRevision(Long revision)
    {
        this.revision = revision;
    }

// -------------------------- OTHER METHODS --------------------------

    public void adjustQANotes()
    {
        for (File file : getFiles()) {
            adjustQANotes(file);
        }
    }

    public void adjustQANotes(File file)
    {
        final String fileName = file.getAbsolutePath().substring(rootPath.length() - 1);
        try {
            final String str = IOUtils.toString(new FileInputStream(file));
            final StringBuilder code = new StringBuilder(str);
            if (adjustQANotes(code, fileName)) {
                IOUtils.write(code.toString(), new FileOutputStream(file));
            }
        } catch (IOException e) {
            handleFileProcessingException(e);
        }
    }

    public boolean adjustQANotes(StringBuilder code, String fileName)
    {
        boolean changesFound = false;
        final CommentIterator iterator = new CommentIterator(code);
        while (iterator.hasNext()) {
            final QANote qaNote = toQANote(iterator.next(), true, fileName);
            final String newComment = noteConverter.getAsString(qaNote);
            iterator.replace(newComment);
            changesFound = true;
        }
        return changesFound;
    }

    public File getFile(QANote note) throws FileNotFoundException
    {
        final File file = new File(getRootPath() + File.separator + note.getFileName());
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + file);
        }
        return file;
    }

    public List<QANote> getQANotes()
    {
        final List<QANote> notes = new ArrayList<QANote>();
        for (File file : getFiles()) {
            notes.addAll(getQANotes(file));
        }
        return notes;
    }

    public List<QANote> getQANotes(File file)
    {
        final List<QANote> notes = new ArrayList<QANote>();
        final String fileName = file.getAbsolutePath().substring(rootPath.length() + 1);
        try {
            final String code = IOUtils.toString(new FileInputStream(file));
            notes.addAll(getQANotes(code, fileName));
        } catch (IOException e) {
            handleFileProcessingException(e);
        }
        return notes;
    }

    public List<QANote> getQANotes(String code, String fileName)
    {
        final List<QANote> notes = new ArrayList<QANote>();
        final CommentIterator iterator = new CommentIterator(new StringBuilder(code));
        while (iterator.hasNext()) {
            notes.add(toQANote(iterator.next(), false, fileName));
        }
        return notes;
    }

    public synchronized boolean syncToFile(QANote note, boolean keepNoteId) throws IOException
    {
        final File file = getFile(note);
        final String str = IOUtils.toString(new FileInputStream(file));
        final StringBuilder code = new StringBuilder(str);
        final QANoteScanner.CommentIterator iterator = new QANoteScanner.CommentIterator(code);
        boolean changeFound = false;
        while (iterator.hasNext()) {
            final QANote qaNote = toQANote(iterator.next(), false);
            if (qaNote.getTicket() != null && ObjectUtils.equals(qaNote.getTicket(), note.getTicket()) || qaNote.getId() != null && ObjectUtils.equals(
                qaNote.getId(), note.getId())) {
                if (!keepNoteId) {
                    note.setId(null);
                }
                iterator.replace(noteConverter.getAsString(note));
                changeFound = true;
                break;
            }
        }
        if (changeFound) {
            IOUtils.write(code.toString(), new FileOutputStream(file));
        }
        return changeFound;
    }

    @NotNull
    public QANote toQANote(String comment, boolean generateId)
    {
        final QANote qaNote = noteConverter.getAsObject(comment);
        if (generateId) {
            qaNote.setId(generateQANoteTemporaryId());
        }
        if (qaNote.getRevision() == null) {
            qaNote.setRevision(revision);
        }
        return qaNote;
    }

    private long generateQANoteTemporaryId()
    {
        return ++temporaryIdSequence;
    }

    private List<File> getFiles()
    {
        if (directoryToScan == null) {
            throw new IllegalStateException("Set directoryToScan first!");
        }
        final List<File> fileList = new ArrayList<File>();
        scan(directoryToScan, fileList);
        return fileList;
    }

    private void handleFileProcessingException(IOException e)
    {
        System.err.println(e);
    }

    private void scan(File file, Collection<File> files)
    {
        if (!file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + file.getAbsolutePath());
        }
        if (file.isDirectory()) {
            for (File child : file.listFiles(filter)) {
                scan(child, files);
            }
        } else {
            files.add(file);
        }
    }

    private QANote toQANote(String comment, boolean generateId, String fileName)
    {
        QANote qaNote = toQANote(comment, generateId);
        qaNote.setFileName(fileName);
        return qaNote;
    }

// -------------------------- INNER CLASSES --------------------------

    public static class CommentIterator implements Iterator<String> {
// ------------------------------ FIELDS ------------------------------

        private StringBuilder code;

        private Fragment current;

        private Boolean hasNext;

        private Matcher matcher;

        private int nextPosition;

// --------------------------- CONSTRUCTORS ---------------------------

        public CommentIterator(StringBuilder code)
        {
            setCode(code);
            current = null;
            hasNext = null;
            nextPosition = 0;
        }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface Iterator ---------------------

        public boolean hasNext()
        {
            if (hasNext == null) {
                boolean commentFound;
                do {
                    commentFound = false;
                    if (matcher.find(nextPosition)) {
                        nextPosition = matcher.end();
                        final String comment = matcher.group();
                        commentFound = true;
                        hasNext = comment.matches("(.|[\r\n])*(" + QA_TAGS + ")(.|[\r\n])*");
                    } else {
                        hasNext = false;
                    }
                } while (commentFound && !hasNext);
            }
            return hasNext;
        }

        public String next()
        {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            current = new Fragment();
            current.text = matcher.group();
            current.start = matcher.start();
            current.end = matcher.end();
            hasNext = null;
            return current.text;
        }

        public void remove()
        {
            if (current == null) {
                throw new NoSuchElementException();
            }
            code.delete(current.start, current.end);
            matcher.reset(code);
            hasNext = null;
            nextPosition = current.start;
            current = null;
        }

// -------------------------- OTHER METHODS --------------------------

        public void replace(String comment)
        {
            final Fragment fragment = current;
            remove();
            current = fragment;
            code.insert(fragment.start, comment);
            matcher.reset(code);
            current.end = current.start + comment.length();
            nextPosition = current.end;
        }

        public void setCode(StringBuilder code)
        {
            this.code = code;
            matcher = QA_COMMENT_PATTERN.matcher(code);
        }

// -------------------------- INNER CLASSES --------------------------

        private static class Fragment implements Serializable {
// ------------------------------ FIELDS ------------------------------

            public String text;

            private int end;

            private int start;
        }
    }
}
