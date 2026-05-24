package net.alkeari.geotectonic.cave;

import java.util.List;

public record KarstLayout(
    List<PathPoint> crackPoints,
    PathPoint mainChamber,
    List<PathPoint> satellites,
    List<PathPoint> passages
) {}
