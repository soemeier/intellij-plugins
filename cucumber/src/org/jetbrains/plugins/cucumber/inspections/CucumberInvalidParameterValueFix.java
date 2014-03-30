package org.jetbrains.plugins.cucumber.inspections;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.cucumber.CucumberBundle;
import org.jetbrains.plugins.cucumber.psi.GherkinElementFactory;
import org.jetbrains.plugins.cucumber.psi.GherkinFile;
import org.jetbrains.plugins.cucumber.psi.GherkinStep;
import org.jetbrains.plugins.cucumber.steps.CucumberStepsIndex;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Soeren Meier
 */
public class CucumberInvalidParameterValueFix implements LocalQuickFix {

    List<String> validValues = new ArrayList<String>();
    private TextRange range;

    public CucumberInvalidParameterValueFix(List<String> validValues, TextRange range) {
        this.validValues = validValues;
        this.range = range;
    }

    @NotNull
    public String getName() {
        return "Select Valid Value";
    }

    @NotNull
    public String getFamilyName() {
        return getName();
    }

    public void applyFix(@NotNull final Project project, final @NotNull ProblemDescriptor descriptor) {


        final JBPopupFactory popupFactory = JBPopupFactory.getInstance();

        final GherkinStep step = (GherkinStep) descriptor.getPsiElement();


        final ListPopup popupStep =
                popupFactory.createListPopup(new BaseListPopupStep<String>(
                        CucumberBundle.message("choose.step.definition.file"), validValues) {

                    @Override
                    public boolean isSpeedSearchEnabled() {
                        return true;
                    }

                    @Override
                    public PopupStep onChosen(final String selectedValue, boolean finalChoice) {

                        return doFinalStep(new Runnable() {
                            @Override
                            public void run() {
                                addStep(step, selectedValue, project);
                            }
                        });


                    }
                });


        popupStep.showCenteredInCurrentWindow(step.getProject());
    }

    private void addStep(final GherkinStep step, final String selectedValue, final Project project) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                new WriteCommandAction.Simple(step.getProject()) {
                    @Override
                    protected void run() throws Throwable {
                        String text = step.getText();

                        String finalText = text.substring(0, range.getStartOffset()) + selectedValue + text.substring(range.getEndOffset());
                        
                        final PsiElement[] elements = GherkinElementFactory.getTopLevelElements(project, finalText);
                        step.deleteChildRange(step.getFirstChild(), step.getLastChild());
                        for (PsiElement element : elements) {
                            step.add(element);
                        }

                    }
                }.execute();
            }
        });

    }


}
