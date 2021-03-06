/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.jetbrains.flask.codeInsight.references;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.util.ArrayUtil;
import com.jetbrains.flask.codeInsight.FlaskNames;
import com.jetbrains.python.psi.*;
import com.intellij.psi.util.QualifiedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Reference that is injected into the first argument of the 'url_for' method and resolves to the corresponding
 * view function.
 *
 * @author yole
 */
public class FlaskViewMethodReference extends PsiReferenceBase<StringLiteralExpression> {
  public FlaskViewMethodReference(@NotNull StringLiteralExpression element) {
    super(element, element.getStringValueTextRange(), true);
  }

  @Nullable
  @Override
  public PsiElement resolve() {
    String routeName = getElement().getStringValue();
    PsiFile containingFile = getViewFunctionsFile();
    if (containingFile instanceof PyFile) {
      return ((PyFile) containingFile).findTopLevelFunction(routeName);
    }
    return null;
  }

  protected PsiFile getViewFunctionsFile() {
    return getElement().getContainingFile();
  }

  @Override
  public boolean isReferenceTo(PsiElement element) {
    if (element instanceof PyFunction && getElement().getStringValue().equals(((PyFunction)element).getName())) {
      return super.isReferenceTo(element);
    }
    return false;
  }

  @NotNull
  @Override
  public Object[] getVariants() {
    PsiFile containingFile = getViewFunctionsFile();
    if (containingFile instanceof PyFile) {
      List<Object> result = new ArrayList<Object>();
      for (PyFunction function : getRouteFunctions((PyFile)containingFile)) {
        result.add(LookupElementBuilder.createWithIcon(function));
      }
      return result.toArray();
    }
    return ArrayUtil.EMPTY_OBJECT_ARRAY;
  }

  private static List<PyFunction> getRouteFunctions(PyFile containingFile) {
    List<PyFunction> result = new ArrayList<PyFunction>();
    for (PyFunction function : containingFile.getTopLevelFunctions()) {
      PyDecoratorList decoratorList = function.getDecoratorList();
      if (decoratorList != null) {
        PyDecorator[] decorators = decoratorList.getDecorators();
        for (PyDecorator decorator : decorators) {
          if (isRouteDecorator(decorator)) {
            result.add(function);
            break;
          }
        }
      }
    }
    return result;
  }

  public static boolean isRouteDecorator(PyDecorator decorator) {
    QualifiedName qualifiedName = decorator.getQualifiedName();
    if (qualifiedName != null && qualifiedName.endsWith(FlaskNames.ROUTE)) {
      return true;
    }
    return false;
  }
}
