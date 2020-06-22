package bio.singa.simulation.model.sections;

import bio.singa.features.parameters.Environment;
import bio.singa.simulation.entities.ChemicalEntity;
import bio.singa.simulation.entities.SimpleEntity;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author cl
 */
class ConcentrationPoolTest {

    private static final ChemicalEntity entityA = SimpleEntity.create("A").build();
    private static final ChemicalEntity entityB = SimpleEntity.create("B").build();

    @Test
    void getReferencedEntities() {
        ConcentrationPool pool = new ConcentrationPool();
        pool.set(entityA, 1.0);
        Set<ChemicalEntity> entities = pool.getReferencedEntities();
        assertEquals(entities.size(), 1);
        assertTrue(entities.contains(entityA));
    }

    @Test
    void testGetAndSet() {
        Environment.reset();
        ConcentrationPool pool = new ConcentrationPool();
        pool.set(entityA, 1.0);
        assertEquals(1.0, pool.get(entityA));
        assertEquals(0.0, pool.get(entityB));
    }

}