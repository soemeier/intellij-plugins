package org.jetbrains.plugins.cucumber.steps.validvalues;


import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ValidVariableValues {

    boolean isValidVariableValue(String valueToCompare);

  @Nullable
    List<String> getValidValues();

}
