package org.jetbrains.plugins.cucumber.java.steps;

import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.cucumber.java.CucumberJavaUtil;
import org.jetbrains.plugins.cucumber.steps.AbstractStepDefinition;
import org.jetbrains.plugins.cucumber.steps.validvalues.ValidVariableValuesAllValid;
import org.jetbrains.plugins.cucumber.steps.validvalues.ValidVariableValues;
import org.jetbrains.plugins.cucumber.steps.validvalues.ValidVariableValuesAsProvided;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: Andrey.Vokin
 * Date: 7/16/12
 */
public class JavaStepDefinition extends AbstractStepDefinition {
  public JavaStepDefinition(PsiMethod method) {
    super(method);
  }

  @Override
  public List<String> getVariableNames() {
    PsiElement element = getElement();
    if (element instanceof PsiMethod) {
      PsiParameter[] parameters = ((PsiMethod)element).getParameterList().getParameters();
      ArrayList<String> result = new ArrayList<String>();
      for (PsiParameter parameter : parameters) {
        result.add(parameter.getName());
      }
      return result;
    }
    return Collections.emptyList();
  }

    @Override
    public ValidVariableValues getPossibleVariableTypes(String variableName) {

        PsiElement element = getElement();
        ValidVariableValues result = new ValidVariableValuesAllValid();
        if (element instanceof PsiMethod) {
            PsiParameter[] parameters = ((PsiMethod)element).getParameterList().getParameters();
            for (PsiParameter parameter : parameters) {
                if (variableName.equalsIgnoreCase(parameter.getName())) {
                    PsiClassReferenceType psiClassReferenceType = (PsiClassReferenceType) parameter.getType();
                    PsiClass psiClass = psiClassReferenceType.resolve();
                    if (psiClass != null && psiClass.isEnum()) {
                        ValidVariableValuesAsProvided validVariableValuesAsProvided = new ValidVariableValuesAsProvided();
                        PsiField[] psiEnumConstant = psiClass.getFields();
                        for (PsiField enumConstant : psiEnumConstant) {
                            validVariableValuesAsProvided.addValidVariableValue(enumConstant.getName());
                        }
                        result = validVariableValuesAsProvided;
                    }
                }
            }

        }
        return result;
    }

  @Nullable
  @Override
  protected String getCucumberRegexFromElement(PsiElement element) {
    if (!(element instanceof PsiMethod)) {
      return null;
    }

    String result = null;
    PsiAnnotation stepAnnotation = CucumberJavaUtil.getCucumberStepAnnotation((PsiMethod)element);
    assert stepAnnotation != null;
    final PsiElement annotationValue = CucumberJavaUtil.getAnnotationValue(stepAnnotation);
    if (annotationValue != null) {
      final PsiConstantEvaluationHelper evaluationHelper = JavaPsiFacade.getInstance(element.getProject()).getConstantEvaluationHelper();
      final Object constantValue = evaluationHelper.computeConstantExpression(annotationValue, false);
      if (constantValue != null) {
        String patternText = constantValue.toString();
        if (patternText.length() > 1) {
          result = patternText.replace("\\\\", "\\").replace("\\\"", "\"");
        }
      }
    }

    return result;
  }
}
