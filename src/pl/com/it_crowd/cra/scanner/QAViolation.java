package pl.com.it_crowd.cra.scanner;

public class QAViolation extends QANote {
// ------------------------------ FIELDS ------------------------------

    public static final String TAG = "QA-VIOLATION";

    private Long ruleId;

// --------------------- GETTER / SETTER METHODS ---------------------

    public Long getRuleId()
    {
        return ruleId;
    }

    public void setRuleId(Long ruleId)
    {
        this.ruleId = ruleId;
    }

    @Override
    protected String getTagName()
    {
        return TAG;
    }
}
