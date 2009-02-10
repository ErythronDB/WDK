package org.gusdb.wdk.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class Category extends WdkModelBase {

    private static final Logger logger = Logger.getLogger(Category.class);

    private String name;
    private String displayName;
    private List<WdkModelText> questionRefs;
    private List<Question> questions;

    private Categories categories;

    public Category() {
        questionRefs = new ArrayList<WdkModelText>();
        questions = new ArrayList<Question>();
    }

    void setCategories(Categories categories) {
        this.categories = categories;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void addQuestionRef(WdkModelText questionRef) {
        this.questionRefs.add(questionRef);
    }

    public Question[] getQuestions() {
        Question[] array = new Question[questions.size()];
        questions.toArray(array);
        return array;
    }

    @Override
    public void excludeResources(String projectId) throws WdkModelException {
        // exclude questionRefs
        List<WdkModelText> temp = new ArrayList<WdkModelText>();
        for (WdkModelText ref : questionRefs) {
            if (ref.include(projectId)) {
                ref.excludeResources(projectId);
                temp.add(ref);
            }
        }
        questionRefs.clear();
        questionRefs = temp;
    }

    public void resolveReferences(WdkModel model) throws WdkModelException {
        // get the base recordClass
        String recordClassRef = categories.getRecordClassRef();
        for (WdkModelText ref : questionRefs) {
            Question question = null;
            try {
                question = (Question) model.resolveReference(ref.getText().trim());
            } catch (WdkModelException ex) {
                // relax a bit, just ignore the missing questions
                logger.debug("The question [" + ref + "] is defined in "
                        + "category [" + name + "], but doesn't exist in the "
                        + "model.");
                continue;
            }

            // make sure the recordClass matches
            if (!question.getRecordClass().getFullName().equals(recordClassRef))
                logger.debug("Question " + question.getFullName()
                        + " should not be included in categories of "
                        + recordClassRef);

            questions.add(question);
        }
        questionRefs = null;
    }
}
