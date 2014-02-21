/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.plugins.cucumber.inspections;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.ex.ProblemDescriptorImpl;
import com.intellij.codeInspection.ex.UnfairLocalInspectionTool;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.cucumber.CucumberBundle;
import org.jetbrains.plugins.cucumber.psi.*;
import org.jetbrains.plugins.cucumber.psi.impl.GherkinScenarioOutlineImpl;
import org.jetbrains.plugins.cucumber.steps.AbstractStepDefinition;
import org.jetbrains.plugins.cucumber.steps.reference.CucumberStepReference;
import org.jetbrains.plugins.cucumber.steps.validvalues.ValidVariableValues;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author yole
 */
public class CucumberInvalidParamterValueInspection extends GherkinInspection implements UnfairLocalInspectionTool {

  @Override
  public boolean isEnabledByDefault() {
    return true;
  }

  @Nls
  @NotNull
  public String getDisplayName() {
    return CucumberBundle.message("cucumber.inspection.invalid.parameter.value.display");
  }

  @NotNull
  public String getShortName() {
    return "CucumberInvalidParamterValue";
  }

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, final boolean isOnTheFly) {
    return new StepDefinitionVisitor() {

      @Override
      protected void visitExistingDefinition(AbstractStepDefinition definition, GherkinStep step, CucumberStepReference reference) {

        if (!isOnTheFly) {
          return;
        }

        // highlighting for regexp params
        final List<TextRange> parameterRanges =
          GherkinPsiUtil.buildParameterRanges(step, definition, reference.getRangeInElement().getStartOffset());

        if (parameterRanges == null) return;

        for (int i = 0; i < parameterRanges.size(); i++) {
          TextRange range = parameterRanges.get(i);
          if (range.getLength() > 0) {
            if (definition.getVariableNames().size() <= i) {
              return;
            }
            String variableName = definition.getVariableNames().get(i);

            ValidVariableValues validVariableValues = definition.getPossibleVariableTypes(variableName);
            String currentValue = step.getText().substring(range.getStartOffset(), range.getEndOffset());
            if (!validVariableValues.isValidVariableValue(currentValue) && !isOutlineParameter(step, currentValue)) {
              registerWrongParameterValue(reference, range, validVariableValues);
            }
          }
        }
      }


      private void registerWrongParameterValue(CucumberStepReference reference, TextRange range, ValidVariableValues validVariableValues) {
        holder.registerProblem(reference.getElement(), range,
                               CucumberBundle.message("cucumber.inspection.invalid.parameter.value") + " #loc #ref",
                               new CucumberInvalidParameterValueFix(validVariableValues.getValidValues(), range));
      }

      private boolean isOutlineParameter(GherkinStep step, String currentValue) {
        return currentValue.startsWith("<") && currentValue.endsWith(">") && belongsToScenarioOutline(step);
      }
    };
  }


  private static boolean belongsToScenarioOutline(GherkinStep step) {
    return step.getStepHolder() instanceof GherkinScenarioOutlineImpl;
  }
}
