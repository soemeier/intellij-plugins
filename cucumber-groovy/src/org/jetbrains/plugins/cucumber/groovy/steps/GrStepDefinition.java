package org.jetbrains.plugins.cucumber.groovy.steps;

import com.intellij.ide.util.EditSourceUtil;
import com.intellij.openapi.editor.Document;
import com.intellij.pom.Navigatable;
import com.intellij.pom.PomNamedTarget;
import com.intellij.psi.*;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.cucumber.groovy.GrCucumberUtil;
import org.jetbrains.plugins.cucumber.steps.AbstractStepDefinition;
import org.jetbrains.plugins.cucumber.steps.validvalues.ValidVariableValuesAllValid;
import org.jetbrains.plugins.cucumber.steps.validvalues.ValidVariableValues;
import org.jetbrains.plugins.cucumber.steps.validvalues.ValidVariableValuesAsProvided;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.params.GrParameter;
import org.jetbrains.plugins.groovy.lang.psi.impl.GrClassReferenceType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Max Medvedev
 */
public class GrStepDefinition extends AbstractStepDefinition implements PomNamedTarget {
  public GrStepDefinition(GrMethodCall stepDefinition) {
    super(stepDefinition);
  }

  public static GrStepDefinition getStepDefinition(final GrMethodCall statement) {
    return CachedValuesManager.getCachedValue(statement, new CachedValueProvider<GrStepDefinition>() {
      @Nullable
      @Override
      public Result<GrStepDefinition> compute() {
        final Document document = PsiDocumentManager.getInstance(statement.getProject()).getDocument(statement.getContainingFile());
        return Result.create(new GrStepDefinition(statement), document);
      }
    });
  }

  @Override
  public List<String> getVariableNames() {
    PsiElement element = getElement();
    if (element instanceof GrMethodCall) {
      GrParameter[] parameters = getParameters((GrMethodCall)element);
      ArrayList<String> result = new ArrayList<String>();
      for (GrParameter parameter : parameters) {
        result.add(parameter.getName());
      }

      return result;
    }
    return Collections.emptyList();
  }

  private GrParameter[] getParameters(GrMethodCall element) {
    GrClosableBlock[] closures = element.getClosureArguments();
    assert closures.length == 1;
    return closures[0].getParameterList().getParameters();
  }

  @Override
  public ValidVariableValues getPossibleVariableTypes(String variableName) {

    PsiElement element = getElement();
    ValidVariableValues result = new ValidVariableValuesAllValid();
    if (element instanceof GrMethodCallExpression) {
      GrParameter[] parameters = getParameters((GrMethodCall)element);
      for (GrParameter parameter : parameters) {
        if (variableName.equalsIgnoreCase(parameter.getName()) && parameter.getType() instanceof GrClassReferenceType) {
          GrClassReferenceType grClassReferenceType = (GrClassReferenceType)parameter.getType();
          PsiClass psiClass = grClassReferenceType.resolve();
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
    if (!(element instanceof GrMethodCall)) {
      return null;
    }
    return GrCucumberUtil.getStepDefinitionPatternText((GrMethodCall)element);
  }

  @Override
  public String getName() {
    return getCucumberRegex();
  }

  @Override
  public boolean isValid() {
    final PsiElement element = getElement();
    return element != null && element.isValid();
  }

  @Override
  public void navigate(boolean requestFocus) {
    Navigatable descr = EditSourceUtil.getDescriptor(getElement());
    if (descr != null) descr.navigate(requestFocus);
  }

  @Override
  public boolean canNavigate() {
    return EditSourceUtil.canNavigate(getElement());
  }

  @Override
  public boolean canNavigateToSource() {
    return canNavigate();
  }
}
