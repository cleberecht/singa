package bio.singa.mathematics.vectors;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BitVectorTest {

    private RegularBitVector bitVector;

    @BeforeEach
    void initialize() {
        bitVector = new RegularBitVector(true, false, true, true, false, true);
    }

    @Test
    void getCopy() {
        BitVector copyOfBitVector = bitVector.getCopy();
        copyOfBitVector.getElements()[0] = false;
        assertTrue(bitVector.getElements()[0] != copyOfBitVector.getElements()[0]);
    }

    @Test
    void equals() {
        BitVector copyOfBitVector = bitVector.getCopy();
        assertEquals(bitVector, copyOfBitVector);
    }

    @Test
    void fromBitString() {
        BitVector bitVector = BitVector.fromBitString("101101");
        assertEquals(this.bitVector, bitVector);
    }
}