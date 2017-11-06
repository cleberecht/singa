package de.bioforscher.singa.simulation.features.permeability;

import de.bioforscher.singa.features.model.AbstractFeature;
import de.bioforscher.singa.features.model.FeatureOrigin;
import de.bioforscher.singa.features.model.ScalableFeature;
import tec.units.ri.quantity.Quantities;
import tec.units.ri.unit.ProductUnit;

import javax.measure.Quantity;
import javax.measure.quantity.Frequency;
import javax.measure.quantity.Length;
import javax.measure.quantity.Time;

import static tec.units.ri.AbstractUnit.ONE;
import static tec.units.ri.unit.Units.HERTZ;

/**
 * @author cl
 */
public class MembraneEntry extends AbstractFeature<Quantity<Frequency>> implements ScalableFeature<Quantity<Frequency>> {

    private Quantity<Frequency> scaledQuantity;
    private Quantity<Frequency> halfScaledQuantity;

    public MembraneEntry(Quantity<Frequency> frequencyQuantity, FeatureOrigin featureOrigin) {
        super(frequencyQuantity, featureOrigin);
    }

    public MembraneEntry(double frequency, FeatureOrigin featureOrigin) {
        super(Quantities.getQuantity(frequency, HERTZ), featureOrigin);
    }

    @Override
    public void scale(Quantity<Time> time, Quantity<Length> space) {
        // transform to specified unit
        Quantity<Frequency> scaledQuantity = getFeatureContent()
                .to(new ProductUnit<>(ONE.divide(time.getUnit())));
        // transform to specified amount
        this.scaledQuantity = scaledQuantity.multiply(time.getValue().doubleValue());
        // and half
        this.halfScaledQuantity = scaledQuantity.multiply(time.multiply(0.5).getValue().doubleValue());
    }

    @Override
    public Quantity<Frequency> getScaledQuantity() {
        return this.scaledQuantity;
    }

    @Override
    public Quantity<Frequency> getHalfScaledQuantity() {
        return this.halfScaledQuantity;
    }

}
