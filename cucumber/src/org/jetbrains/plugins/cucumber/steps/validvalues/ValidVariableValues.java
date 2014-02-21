package org.jetbrains.plugins.cucumber.steps.validvalues;


import java.util.List;

public interface ValidVariableValues {

    boolean isValidVariableValue(String valueToCompare);

    List<String> getValidValues();

}
