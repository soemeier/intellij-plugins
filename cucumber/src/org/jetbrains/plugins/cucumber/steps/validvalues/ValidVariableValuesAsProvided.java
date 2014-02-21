package org.jetbrains.plugins.cucumber.steps.validvalues;

import java.util.ArrayList;
import java.util.List;


public class ValidVariableValuesAsProvided implements ValidVariableValues {

    public List<String> validVariableValues = new ArrayList<String>();

    public boolean isValidVariableValue(String valueToCompare) {

        for (String validVariableValue : validVariableValues) {
            if (validVariableValue.equalsIgnoreCase(valueToCompare)) {
                return true;
            }
        }
        return false;

    }

    @Override
    public List<String> getValidValues() {
        return validVariableValues;
    }


    public void addValidVariableValue(String validValue) {
        validVariableValues.add(validValue);
    }

}
