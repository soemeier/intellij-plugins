package org.jetbrains.plugins.cucumber.steps.validvalues;


import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ValidVariableValuesAllValid implements ValidVariableValues {


    public boolean isValidVariableValue(String valueToCompare) {
        return true;
    }


    @Override
    public List<String> getValidValues() {
        return null;
    }

}
