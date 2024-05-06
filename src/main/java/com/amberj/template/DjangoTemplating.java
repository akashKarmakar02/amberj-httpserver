package com.amberj.template;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DjangoTemplating {

    public String parse(String template, Object data) {

        List<String> variables = extractVariables(template);

        template = renderForLoop(template, data);
        template = renderConditionalBlocks(template, data);


        for (String variable : variables) {
            Object value = getValueFromObject(data, variable);
            template = template.replace("{{ " + variable + " }}", value.toString());
        }

        return template;
    }

    private Object getValueFromObject(Object data, String variable) {
        try {
            var field = data.getClass().getDeclaredField(variable);
            field.setAccessible(true);
            return field.get(data);
        } catch (Exception e) {
            return "null";
        }
    }

    private List<String> extractVariables(String template) {
        List<String> variables = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\{\\{\\s*(.*?)\\s*}}");
        Matcher matcher = pattern.matcher(template);
        while (matcher.find()) {
            String variable = matcher.group(1);
            variables.add(variable.trim());
        }

        return variables;
    }

    private String renderConditionalBlocks(String htmlContent, Object data) {
        Pattern pattern = Pattern.compile("\\{%\\s*if\\s+(?:\"(\\w+)\"|([^\"\\s]+))\\s*(>=|<=|>|<|==)\\s*(?:\"(\\w+)\"|([^\"\\s]+))\\s*%}(.*?)(?:\\{%\\s*else\\s*%}(.*?))?\\{%\\s*endif\\s*%}", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(htmlContent);
        StringBuilder result = new StringBuilder();

        // Iterate through matches and evaluate conditions
        while (matcher.find()) {
            Optional<String> value = Optional.empty();
            String variable = matcher.group(2);
            String operator = matcher.group(3);
            String valueString = matcher.group(4);
            Optional<Integer> valueInt = Optional.empty();
            Optional<Integer> variableInt = Optional.empty();
            if (valueString == null) {
                valueInt = Optional.of(Integer.parseInt(matcher.group(5)));
            }
            if (variable == null) {
                value = Optional.of(matcher.group(1));
            } else {
                try {
                    variableInt = Optional.of(Integer.parseInt(variable));
                } catch (NumberFormatException ignored) {

                }
            }



            String ifContent = matcher.group(6);
            String elseContent = matcher.group(7);

            Object variableValue;

            if (value.isEmpty() && variableInt.isEmpty()) {
                variableValue = getValueFromObject(data, variable);
            } else if (variableInt.isPresent()) {
                variableValue = variableInt.get();
            } else {
                variableValue = value.get();
            }

            boolean toRender;

            toRender = valueInt.map(
                            integer -> evaluateCondition(operator, integer, variableValue))
                    .orElseGet(() -> evaluateCondition(operator, valueString, variableValue)
                    );

            if (elseContent == null) {
                elseContent = "";
            }
            if (toRender) {
                matcher.appendReplacement(result, ifContent);
            }
            else {
                matcher.appendReplacement(result, elseContent);
            }
        }

        matcher.appendTail(result);

        return result.toString();
    }

    private String renderForLoop(String htmlContent, Object data) {
        Pattern pattern = Pattern.compile("\\{%\\s*for\\s+(\\w+)\\s+in\\s+(.*?)\\s*%}(.*?)\\{%\\s*endfor\\s*%}", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(htmlContent);
        StringBuilder result = new StringBuilder();
        String loopVariable;

        while (matcher.find()) {
            loopVariable = matcher.group(1);
            String iterableName = matcher.group(2);
            String forContent = matcher.group(3);


            Object iterableObject = getValueFromObject(data, iterableName);
            StringBuilder loopResult;
            switch (detectIterableType(iterableObject)) {
                case ARRAY:
                    loopResult = new StringBuilder();
                    for (Object item : (Object[]) iterableObject) {
                        loopResult.append(renderForLoopContent(forContent, loopVariable, item));
                    }
                    matcher.appendReplacement(result, loopResult.toString());
                    break;
                case ITERABLE:
                    loopResult = new StringBuilder();
                    for (Object item : (Iterable<?>) iterableObject) {
                        loopResult.append(renderForLoopContent(forContent, loopVariable, item));
                    }
                    matcher.appendReplacement(result, loopResult.toString());
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported iterable type");
            }
        }

        matcher.appendTail(result);

        return result.toString();
    }

    private String renderForLoopContent(String forContent, String loopVariable, Object item) {
        var variables = extractVariables(forContent);
        Pattern pattern = Pattern.compile("\\{%\\s*if\\s+(?:\"(\\w+)\"|([^\"\\s]+))\\s*(>=|<=|>|<|==)\\s*(?:\"(\\w+)\"|([^\"\\s]+))\\s*%}(.*?)(?:\\{%\\s*else\\s*%}(.*?))?\\{%\\s*endif\\s*%}", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(forContent);
        StringBuilder result = new StringBuilder();

        while(matcher.find()) {
            String variable = matcher.group(2);
            if (variable != null) {
                var variable2 = getValueFromObject(item, variable.split("\\.")[1]);
                String variable3 = matcher.group(3);
                String variable4 = matcher.group(4);
                String variable5 = matcher.group(5);
                String variable6 = matcher.group(6);
                String variable7 = matcher.group(7);

                String modifiedMatch = String.format("{%% if %s %s %s %%}%s{%% else %%}%s{%% endif %%}",
                        variable2 instanceof Integer ? variable2 : "\"" + variable2 + "\"", variable3, variable4 == null ? variable5 : "\"" + variable4 + "\"", variable6, variable7);

                matcher.appendReplacement(result, modifiedMatch);
            }
        }

        forContent = String.valueOf(result);
        for (var variable: variables) {
            if (variable.contains(".")) {
                var property = variable.split("\\.")[1];
                Object value = getValueFromObject(item, property);
                forContent = forContent.replaceAll("\\{\\{\\s*" + variable + "\\s*}}", value.toString());
                forContent = forContent.replaceAll("\\{%\\s*if\\s+" + variable + "\\\\s*(==|>=|<=|>|<)\\\\s*\"[^\"]*\"\\\\s*%}","\"" + value + "\"");
            } else {
                forContent = forContent.replaceAll("\\{\\{\\s*" + variable + "\\s*}}", item.toString());
            }
        }


        return forContent;
    }

    private static IterableType detectIterableType(Object iterableObject) {
        if (iterableObject instanceof Object[]) {
            return IterableType.ARRAY;
        } else if (iterableObject instanceof Iterable) {
            return IterableType.ITERABLE;
        } else {
            throw new IllegalArgumentException("Unsupported iterable type");
        }
    }

    public static boolean evaluateCondition(String operator, Object value1, Object value2) {
        if (value1.getClass() != value2.getClass()) {
            return false;
        }
        return switch (operator) {
            case ">" -> compareValues(value1, value2) < 0;
            case ">=" -> compareValues(value1, value2) >= 0;
            case "<" -> compareValues(value1, value2) > 0;
            case "<=" -> compareValues(value1, value2) <= 0;
            case "==" -> compareValues(value1, value2) == 0;
            default -> false;
        };
    }

    public static int compareValues(Object value1, Object value2) {
        if (value1 instanceof Integer && value2 instanceof Integer) {
            return Integer.compare((Integer) value1, (Integer) value2);
        } else if (value1 instanceof String && value2 instanceof String) {
            return ((String) value1).compareTo((String) value2);
        } else {
            throw new IllegalArgumentException("Unsupported data types for comparison");
        }
    }
}

