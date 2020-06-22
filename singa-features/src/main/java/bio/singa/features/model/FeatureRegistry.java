package bio.singa.features.model;

import bio.singa.features.quantities.MembraneDiffusivity;
import tech.units.indriya.ComparableQuantity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author cl
 */
public class FeatureRegistry {

    private static FeatureRegistry instance = getInstance();

    private AtomicInteger identifierGenerator;
    private List<QualitativeFeature<?>> qualitativeFeatures;
    private List<AbstractQuantitativeFeature<?>> quantitativeFeatures;
    private List<AbstractScalableQuantitativeFeature<?>> scalableQuantitativeFeatures;

    private FeatureRegistry() {
        identifierGenerator = new AtomicInteger();
        qualitativeFeatures = new ArrayList<>();
        quantitativeFeatures = new ArrayList<>();
        scalableQuantitativeFeatures = new ArrayList<>();
    }

    private static FeatureRegistry getInstance() {
        if (instance == null) {
            reinitialize();
        }
        return instance;
    }

    public static void reinitialize() {
        synchronized (FeatureRegistry.class) {
            instance = new FeatureRegistry();
        }
    }

    public static void addQuantitativeFeature(AbstractQuantitativeFeature<?> quantitativeFeature) {
        quantitativeFeature.setIdentifier(getInstance().identifierGenerator.getAndIncrement());
        getInstance().quantitativeFeatures.add(quantitativeFeature);
    }

    public static void addQualitativeFeature(QualitativeFeature<?> qualitativeFeature) {
        qualitativeFeature.setIdentifier(getInstance().identifierGenerator.getAndIncrement());
        getInstance().qualitativeFeatures.add(qualitativeFeature);
    }

    public static void addScalableQuantitativeFeatures(AbstractScalableQuantitativeFeature<?> scalableQuantitativeFeature) {
        // crosscheck
        scalableQuantitativeFeature.setIdentifier(getInstance().identifierGenerator.getAndIncrement());
        if (scalableQuantitativeFeature instanceof MembraneDiffusivity) {
            for (AbstractScalableQuantitativeFeature<?> registryFeature : getInstance().scalableQuantitativeFeatures) {
                if (registryFeature.getClass().equals(scalableQuantitativeFeature.getClass())) {
                    ComparableQuantity registryContent = (ComparableQuantity) registryFeature.getContent();
                    ComparableQuantity newContent = (ComparableQuantity) scalableQuantitativeFeature.getContent();

                    if (registryContent.isEquivalentOf(newContent)) {
                        scalableQuantitativeFeature.setIdentifier(registryFeature.getIdentifier());
                    }
                }
            }
        }
        scalableQuantitativeFeature.scale();
        getInstance().scalableQuantitativeFeatures.add(scalableQuantitativeFeature);
    }

    public static Feature<?> get(int identifier) {
        for (AbstractScalableQuantitativeFeature<?> scalableQuantitativeFeature : getScalableQuantitativeFeatures()) {
            if (scalableQuantitativeFeature.getIdentifier() == identifier) {
                return scalableQuantitativeFeature;
            }
        }
        for (AbstractQuantitativeFeature<?> quantitativeFeature : getQuantitativeFeatures()) {
            if (quantitativeFeature.getIdentifier() == identifier) {
                return quantitativeFeature;
            }
        }
        for (QualitativeFeature<?> qualitativeFeature : getQualitativeFeatures()) {
            if (qualitativeFeature.getIdentifier() == identifier) {
                return qualitativeFeature;
            }
        }
        return null;
    }

    public static void scale() {
        for (AbstractScalableQuantitativeFeature<?> feature : getInstance().scalableQuantitativeFeatures) {
            feature.scale();
        }
    }

    public static void scale(double factor) {
        for (AbstractScalableQuantitativeFeature<?> feature : getInstance().scalableQuantitativeFeatures) {
            feature.scale(factor);
        }
    }

    public static List<AbstractQuantitativeFeature<?>> getQuantitativeFeatures() {
        return getInstance().quantitativeFeatures;
    }

    public static List<QualitativeFeature<?>> getQualitativeFeatures() {
        return getInstance().qualitativeFeatures;
    }

    public static List<AbstractScalableQuantitativeFeature<?>> getScalableQuantitativeFeatures() {
        return getInstance().scalableQuantitativeFeatures;
    }

    public static List<Feature<?>> getAllFeatures() {
        List<Feature<?>> features = new ArrayList<>();
        features.addAll(getQualitativeFeatures());
        features.addAll(getQuantitativeFeatures());
        features.addAll(getScalableQuantitativeFeatures());
        return features;
    }

}
