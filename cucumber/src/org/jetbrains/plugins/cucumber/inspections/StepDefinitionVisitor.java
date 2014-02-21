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


import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.plugins.cucumber.CucumberBundle;
import org.jetbrains.plugins.cucumber.psi.*;
import org.jetbrains.plugins.cucumber.steps.AbstractStepDefinition;
import org.jetbrains.plugins.cucumber.steps.CucumberStepsIndex;
import org.jetbrains.plugins.cucumber.steps.reference.CucumberStepReference;
import org.jetbrains.plugins.cucumber.steps.validvalues.ValidVariableValues;

import java.util.List;

public abstract class StepDefinitionVisitor extends GherkinElementVisitor {

  @Override
  public void visitStep(GherkinStep step) {
    final PsiElement parent = step.getParent();
    if (parent instanceof GherkinStepsHolder) {
      final PsiReference[] references = step.getReferences();
      if (references.length != 1 || !(references[0] instanceof CucumberStepReference)) return;

      CucumberStepReference reference = (CucumberStepReference)references[0];
      final AbstractStepDefinition definition = reference.resolveToDefinition();

      if (definition != null) {
        visitExistingDefinition(definition, step, reference);
      } else {
        visitNonExistingDefinition(step, reference);
      }

    }
  }

  protected void visitExistingDefinition(AbstractStepDefinition definition, GherkinStep step, CucumberStepReference reference) {

  }

  protected void visitNonExistingDefinition(GherkinStep step, CucumberStepReference reference) {

  }

}
