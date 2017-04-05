package de.bioforscher.singa.mathematics.algorithms.optimization;

import de.bioforscher.singa.core.parameters.MixedParameterList;
import de.bioforscher.singa.core.parameters.UniqueParameterList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractGridSearch {

    private List<UniqueParameterList<?>> inputParameterList;
    private Map<MixedParameterList, Double> resultingValues;

    public AbstractGridSearch(List<UniqueParameterList<?>> inputParameterList) {
        this.inputParameterList = inputParameterList;
        this.resultingValues = new HashMap<>();
    }

    public List<UniqueParameterList<?>> getInputParameterList() {
        return inputParameterList;
    }

    public void setInputParameterList(List<UniqueParameterList<?>> inputParameterList) {
        this.inputParameterList = inputParameterList;
    }

    public Map<MixedParameterList, Double> getResultingValues() {
        return resultingValues;
    }

    abstract public void search();

}