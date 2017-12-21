package de.bioforscher.singa.core.parameters;

public final class ParameterValue<Type extends Comparable<Type>> {

    private final Parameter<Type> parameter;
    private final Type value;

    public ParameterValue(Parameter<Type> parameter, Type value) {
        this.parameter = parameter;
        if (parameter.isInRange(value)) {
            this.value = value;
        } else {
            throw new IllegalArgumentException("Unable to assign " + value.toString() + " to the parameter " + parameter);
        }
    }

    public Parameter<Type> getParameter() {
        return parameter;
    }

    public Type getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString(); // parameter + " = " +
    }

}
