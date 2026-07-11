package org.labellum.mc.dttfc.util;

import com.dtteam.dynamictrees.api.season.SeasonProvider;
import com.dtteam.dynamictrees.systems.season.ActiveSeasonGrowthCalculator;
import com.dtteam.dynamictrees.systems.season.NormalSeasonManager;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.ICalendar;
import net.dries007.tfc.util.climate.Climate;

public class TFCSeasonManager extends NormalSeasonManager
{
    private static final float SPRING_START = 2.0f / 12.0f;

    public TFCSeasonManager()
    {
        super(level -> new Tuple<>(new TFCSeasonProvider(), new ActiveSeasonGrowthCalculator()));
        this.setTropicalPredicate((level, pos) -> TFCSeasonManager.isTropicalClimate(level, pos));
    }

    private static boolean isTropicalClimate(LevelAccessor level, BlockPos pos)
    {
        if (!(level instanceof Level actualLevel) || !canQueryClimate(actualLevel, pos))
        {
            return false;
        }
        return Climate.getInstantTemperature(actualLevel, pos) > 19f && Climate.getAverageRainfall(actualLevel, pos) > 330f;
    }

    private static boolean canQueryClimate(Level level, BlockPos pos)
    {
        final var server = level.getServer();
        return !level.isClientSide()
            && level.dimension().equals(Level.OVERWORLD)
            && server != null
            && Thread.currentThread() == server.getRunningThread()
            && level.isLoaded(pos);
    }

    private static class TFCSeasonProvider implements SeasonProvider
    {
        @Override
        public Float getSeasonValue(Level level, BlockPos pos)
        {
            return getPercentPassedSinceSpring(level) * 4.0f;
        }

        @Override
        public void updateTick(Level level, long dayTime)
        {
        }

        @Override
        public boolean shouldSnowMelt(Level level, BlockPos pos)
        {
            if (!canQueryClimate(level, pos))
            {
                return false;
            }
            return Climate.getInstantTemperature(level, pos) > 0f;
        }

        private float getPercentPassedSinceSpring(Level level)
        {
            float pct = getYearPercentPassed(level) - SPRING_START;
            if (pct < 0)
            {
                pct += 1;
            }
            return pct;
        }

        /**
         * [0, 1], 0 == january.
         */
        private float getYearPercentPassed(Level level)
        {
            final ICalendar calendar = Calendars.get(level);
            return (float) (calendar.getCalendarTicks() % calendar.getCalendarTicksInYear()) / calendar.getCalendarTicksInYear();
        }
    }
}
