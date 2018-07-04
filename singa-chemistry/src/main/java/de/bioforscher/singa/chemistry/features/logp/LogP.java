package de.bioforscher.singa.chemistry.features.logp;

import de.bioforscher.singa.chemistry.features.FeatureRegistry;
import de.bioforscher.singa.features.model.AbstractFeature;
import de.bioforscher.singa.features.model.FeatureOrigin;

/**
 * @author cl
 */
public class LogP extends AbstractFeature<Double> {

    public static String SYMBOL = "Log P_okt/wat";

    public LogP(Double value, FeatureOrigin featureOrigin) {
        super(value, featureOrigin);
    }

    public static void register() {
        FeatureRegistry.addProviderForFeature(LogP.class, LogPProvider.class);
    }

    @Override
    public String getSymbol() {
        return SYMBOL;
    }

}
