package org.jetbrains.plugins.cucumber.steps.validvalues;


import java.util.List;

public class ValidVariableValuesAllValid implements ValidVariableValues {


    public boolean isValidVariableValue(String valueToCompare) {
        return true;
    }

    @Override
    public List<String> getValidValues() {
        throw new UnsupportedOperationException("All variable values are valid. So please do no ask me for valid values.");
    }

}
