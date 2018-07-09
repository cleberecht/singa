package de.bioforscher.singa.simulation.features;

import de.bioforscher.singa.features.model.FeatureOrigin;

/**
 * @author cl
 */
public class DefautFeatureSources {

    public static final FeatureOrigin EHRLICH2004 = new FeatureOrigin(FeatureOrigin.OriginType.LITERATURE, "Ehrlich 2004", "Ehrlich, Marcelo, et al. \"Endocytosis by random initiation and stabilization of clathrin-coated pits.\" Cell 118.5 (2004): 591-605.");
    public static final FeatureOrigin MERRIFIELD2005 = new FeatureOrigin(FeatureOrigin.OriginType.LITERATURE, "Merrifield 2005", "Merrifield, Christien J., David Perrais, and David Zenisek. \"Coupling between clathrin-coated-pit invagination, cortactin recruitment, and membrane scission observed in live cells.\" Cell 121.4 (2005): 593-606.");

    private DefautFeatureSources() {

    }

}
