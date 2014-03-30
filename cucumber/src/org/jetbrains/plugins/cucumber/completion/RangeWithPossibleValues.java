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
package org.jetbrains.plugins.cucumber.completion;


import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RangeWithPossibleValues {

  private TextRange range;

  private final List<String> possibleValueForRange;

  public RangeWithPossibleValues(TextRange range, @Nullable List<String> possibleValueForRange) {
    this.range = range;
    this.possibleValueForRange = possibleValueForRange;
  }

  public TextRange getRange() {
    return range;
  }

  public void setRange(TextRange range) {
    this.range = range;
  }

  @Nullable
  public List<String> getPossibleValueForRange() {
    return possibleValueForRange;
  }
}
