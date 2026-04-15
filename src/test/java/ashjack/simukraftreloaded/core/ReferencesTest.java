package ashjack.simukraftreloaded.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Pins the GUI id constants in {@link References}. GUI ids are referenced
 * through IGuiHandler at runtime by raw byte value, so changing the constant
 * without updating every caller would silently misroute a GUI open request.
 */
public class ReferencesTest
{
    @Test
    public void folkInventoryGuiId_isZero()
    {
        assertEquals(References.GUI_FOLKINVENTORY, (byte) 0);
    }
}
