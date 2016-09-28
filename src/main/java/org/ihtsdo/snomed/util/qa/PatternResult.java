package org.ihtsdo.snomed.util.qa;

public class PatternResult {
	private String patternId;

    private String form;

    private String term;

    private String isNew;

    private String preexisting;

    private String conceptId;

    private String current;

    private String matchDescription;

    private String semtag;

    private String previousEffectiveTime;

    private String currentEffectiveTime;

    private String resultId;

    private String changed;

    public String getPatternId ()
    {
        return patternId;
    }

    public void setPatternId (String patternId)
    {
        this.patternId = patternId;
    }

    public String getForm ()
    {
        return form;
    }

    public void setForm (String form)
    {
        this.form = form;
    }

    public String getTerm ()
    {
        return term;
    }

    public void setTerm (String term)
    {
        this.term = term;
    }

    public String getIsNew ()
    {
        return isNew;
    }

    public void setIsNew (String isNew)
    {
        this.isNew = isNew;
    }

    public String getPreexisting ()
    {
        return preexisting;
    }

    public void setPreexisting (String preexisting)
    {
        this.preexisting = preexisting;
    }

    public String getConceptId ()
    {
        return conceptId;
    }

    public void setConceptId (String conceptId)
    {
        this.conceptId = conceptId;
    }

    public String getCurrent ()
    {
        return current;
    }

    public void setCurrent (String current)
    {
        this.current = current;
    }

    public String getMatchDescription ()
    {
        return matchDescription;
    }

    public void setMatchDescription (String matchDescription)
    {
        this.matchDescription = matchDescription;
    }

    public String getSemtag ()
    {
        return semtag;
    }

    public void setSemtag (String semtag)
    {
        this.semtag = semtag;
    }

    public String getPreviousEffectiveTime ()
    {
        return previousEffectiveTime;
    }

    public void setPreviousEffectiveTime (String previousEffectiveTime)
    {
        this.previousEffectiveTime = previousEffectiveTime;
    }

    public String getCurrentEffectiveTime ()
    {
        return currentEffectiveTime;
    }

    public void setCurrentEffectiveTime (String currentEffectiveTime)
    {
        this.currentEffectiveTime = currentEffectiveTime;
    }

    public String getResultId ()
    {
        return resultId;
    }

    public void setResultId (String resultId)
    {
        this.resultId = resultId;
    }

    public String getChanged ()
    {
        return changed;
    }

    public void setChanged (String changed)
    {
        this.changed = changed;
    }

    @Override
    public String toString()
    {
        return "ClassPojo [patternId = "+patternId+", form = "+form+", term = "+term+", isNew = "+isNew+", preexisting = "+preexisting+", conceptId = "+conceptId+", current = "+current+", matchDescription = "+matchDescription+", semtag = "+semtag+", previousEffectiveTime = "+previousEffectiveTime+", currentEffectiveTime = "+currentEffectiveTime+", resultId = "+resultId+", changed = "+changed+"]";
    }
}
