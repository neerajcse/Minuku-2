package edu.umich.si.inteco.minuku_2;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import com.github.dkharrat.nexusdialog.FormController;
import com.github.dkharrat.nexusdialog.FormElementController;
import com.github.dkharrat.nexusdialog.controllers.FormSectionController;

import java.util.*;

import edu.umich.si.inteco.minuku.config.Constants;
import edu.umich.si.inteco.minuku.manager.MinukuStreamManager;
import edu.umich.si.inteco.minuku.manager.QuestionManager;
import edu.umich.si.inteco.minuku_2.question.QuestionConfig;
import edu.umich.si.inteco.minukucore.dao.DAOException;
import edu.umich.si.inteco.minukucore.exception.QuestionNotFoundException;
import edu.umich.si.inteco.minukucore.exception.StreamNotFoundException;
import edu.umich.si.inteco.minukucore.model.question.FreeResponse;
import edu.umich.si.inteco.minukucore.model.question.MultipleChoice;
import edu.umich.si.inteco.minukucore.model.question.Question;

/**
 * Created by shriti on 7/28/16.
 */
public class QuestionnaireActivity<T extends Question> extends BaseActivity {

    private FormController formController;

    private ImageView acceptButton;
    private ImageView rejectButton;
    
    private Map<T, FormElementController> questionControllerMap;
    private String TAG = "QuestionnaireActivity";

    //FreeResponse q1= new FreeResponse("First Name");
    //FreeResponse q2= new FreeResponse("Last Name");

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "creating activity");

        setContentView(R.layout.custom_form);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);


        setupForm(getIntent().getExtras());

        acceptButton = (ImageView) findViewById(R.id.acceptButton);
        rejectButton = (ImageView) findViewById(R.id.rejectButton);

        acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    acceptResults();
                } catch (StreamNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
        rejectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rejectResults();
            }
        });
        questionControllerMap = QuestionManager.getInstance().getQuestionFormControllerMap();
    }

    protected void setupForm(Bundle savedInstanceState) {
        formController = new FormController(this);

        int questionnaireId = Integer.valueOf(savedInstanceState.getString(Constants.BUNDLE_KEY_FOR_QUESTIONNAIRE_ID));

        Log.d(TAG, "creating form for questionnaire ID " + questionnaireId);
        //get questionnaire ID from bundle
        //QuestionManager.getInstance().getQuestionnaireForID(ID)
        //
        FormSectionController section = new FormSectionController(this, "Please answer the following questions");

        for(Question q : QuestionManager.getInstance().getQuestionnaireForID(questionnaireId).getQuestionnaire()) {
            try {
                section.addElement(QuestionConfig.getControllerFor(q, getApplicationContext()));
            } catch (QuestionNotFoundException e) {
                Log.d(TAG, "A question passed to question config was not found", e);

            }
        }

        formController.addSection(section);

        ViewGroup containerView = (ViewGroup)findViewById(R.id.form_elements_container);
        formController.recreateViews(containerView);
        Log.d(TAG, "creating activity");

    }

    public void acceptResults() throws StreamNotFoundException {

        //Object firstName = formController.getModel().getValue();
        //Object lastName = formController.getModel().getValue("lastName");



        for (Map.Entry<T, FormElementController> entry:questionControllerMap.entrySet()) {
            T question = entry.getKey();
            Object answer = formController.getModel().getValue(String.valueOf(question.getID()));
            if(question instanceof FreeResponse) {
                if(answer!=null)
                    ((FreeResponse) question).setAnswer(answer.toString());
                else
                    ((FreeResponse) question).setAnswer("");
                MinukuStreamManager.getInstance()
                            .getStreamGeneratorFor(FreeResponse.class)
                            .offer((FreeResponse) question);
            }
            if(question instanceof MultipleChoice) {
                Set<String> answerSet = new HashSet<>();
                if(answer != null) {
                    answerSet = (HashSet<String>) answer;
                    Log.d(TAG, answer.toString());
                }

                List<Integer> answers = new ArrayList<Integer>();

                MultipleChoice mcq = (MultipleChoice) question;
                List<String> answerChoices = Arrays.asList(mcq.getLabels());

                for(String someAnswer: answerSet) {
                    answers.add(answerChoices.indexOf(someAnswer));
                }
                ((MultipleChoice) question).setSelectedAnswerValues(answers.toArray(new Integer[0]));
                MinukuStreamManager.getInstance()
                        .getStreamGeneratorFor(MultipleChoice.class)
                        .offer((MultipleChoice) question);
            }
        }
        Log.d(TAG, "Increasing question count");
        mUserSubmissionStats.incrementQuestionCount();
        try {
            Log.d(TAG, "Uploading user submission stats");
            uploadUserSubmissionStats(mUserSubmissionStats);
        } catch (DAOException e) {
            Log.d(TAG, "Failed to upload user submission stats");
            e.printStackTrace();
        }
        showToast("Your answer has been recorded");
        finish();
    }

    /**
     * This is called when the user presses the "X" button the screen.
     */
    public void rejectResults() {
        showToast("Going back to home screen");
        finish();
    }
}
