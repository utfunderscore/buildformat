package org.readutf.buildformat.common.format.requirements;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;


public record RequirementData(
        @Nullable String exact,
        @Nullable String startsWith,
        @Nullable String endsWith,
        int minimumAmount
) {

    @JsonIgnore
    public String getRegex() {
        if (exact != null) {
            return "^" + Pattern.quote(exact) + "$";
        } else if (startsWith != null) {
            return "^" + Pattern.quote(startsWith) + ".*";
        } else if (endsWith != null) {
            return ".*" + Pattern.quote(endsWith) + "$";
        }
        return "";
    }

    @JsonIgnore
    public String getExplanation() {

        if (exact != null) {
            return String.format("%s markers with name '%s'", minimumAmount, exact);
        } else if (startsWith != null) {
            return String.format("%s markers starting with '%s'", minimumAmount, startsWith);
        } else if (endsWith != null) {
            return String.format("%s markers ending with '%s'", minimumAmount, endsWith);
        }


        return "No specific requirements";
    }

}
