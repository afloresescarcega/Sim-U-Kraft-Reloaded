package ashjack.simukraftreloaded.core.game;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for {@link GameMode}'s int-to-enum-to-int round trip.
 * GameMode is one of the few classes in the mod with no Minecraft/Forge
 * dependencies, so it is exercised in isolation without the full game runtime.
 */
public class GameModeTest
{
    @After
    public void resetStaticState()
    {
        GameMode.gameMode = null;
    }

    @Test
    public void setGameModeFromNumber_minusOne_mapsToDoNotRun()
    {
        GameMode.setGameModeFromNumber(-1);
        assertEquals(GameMode.GAMEMODES.DONOTRUN, GameMode.gameMode);
    }

    @Test
    public void setGameModeFromNumber_zero_mapsToNormal()
    {
        GameMode.setGameModeFromNumber(0);
        assertEquals(GameMode.GAMEMODES.NORMAL, GameMode.gameMode);
    }

    @Test
    public void setGameModeFromNumber_one_mapsToCreative()
    {
        GameMode.setGameModeFromNumber(1);
        assertEquals(GameMode.GAMEMODES.CREATIVE, GameMode.gameMode);
    }

    @Test
    public void setGameModeFromNumber_two_mapsToHardcore()
    {
        GameMode.setGameModeFromNumber(2);
        assertEquals(GameMode.GAMEMODES.HARDCORE, GameMode.gameMode);
    }

    @Test
    public void setGameModeFromNumber_unknown_leavesGameModeUnchanged()
    {
        GameMode.setGameModeFromNumber(0);
        GameMode.setGameModeFromNumber(99);
        assertEquals(GameMode.GAMEMODES.NORMAL, GameMode.gameMode);
    }

    @Test
    public void getGameModeNumber_roundTripsAllKnownValues()
    {
        for (int i = -1; i <= 2; i++)
        {
            GameMode.setGameModeFromNumber(i);
            assertEquals(i, GameMode.getGameModeNumber());
        }
    }

    @Test
    public void getGameModeNumber_returnsZeroWhenGameModeIsNull()
    {
        GameMode.gameMode = null;
        assertEquals(0, GameMode.getGameModeNumber());
    }

    @Test
    public void setGameModeFromNumber_unknown_whenGameModeIsNull_leavesItNull()
    {
        GameMode.gameMode = null;
        GameMode.setGameModeFromNumber(42);
        assertNull(GameMode.gameMode);
    }
}
