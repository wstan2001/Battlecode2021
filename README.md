# Battlecode2021
Codebase for Battlecode 2021 competition.

See strategy overview below.

## Pathing

To unambiguously refer to locations on the map, we use their mod 128 coordinates. This works because the map size is guaranteed to be at most 64x64, so given our current location and the mod 128 coordinates of our target location, we can uniquely determine where our target location is (think about why we need 7 bits instead of 6). Originally, our scheme was to discover the mod 64 coordinates of the left and bottom boundaries, then shift everything so that the bottom left square was (0, 0), but this requires many scouts (1 infl politicians) to discover the boundaries, and the opportunity cost of not spawning slanderers early game is immense...

Once we know this, we use a greedy pathing strategy that looks at three possible 1-step movements on the current turn and picks the best one. For example, if our target direction is North, we may try moving NW, N, or NE.

Slanderer movement code is a bit relaxed and allows for a wider range of movements (5 possible moves) to better escape enemy muckrakers.

## Flags and opcodes

Flags are 24 bit integers used to convey information. The vast majority of flags take the form

 0000 | 0000000 | 0000000 | 000000
(opc)  (x-coord) (y-coord)  (data)

but every flag will have the leftmost 4 bits be the opcode signifier.

MOVE: Gives a target location for a unit to move towards.
SCOUT: Gives a general direction for a unit to explore.
TROOOP: Initialize a politician to patrol base, gives it a general direction.
BOMB: Initialize a politician to move to a target location and explode.
SLAND: Sent by slanderers to inform other troops it is a slanderer and its turn of creation
ENEMYEC: Report the coordinates and an influence estimate of an enemy EC.
NEUTRALEC: ^ for neutral EC.
ALLYEC: ^ for ally EC.

## Units and roles

### Enlightenment Center: 

### Politician:

There are two main types of politicians: troops and bombs.

Troops have relatively low influence counts (~1/10 of EC influence and at least 17). Their primary role is to kill 1 influence muckrakers
and allow ally ECs to spawn slanderers. Upon creation, troops will spread out around 10 units from the EC and start to wander. When they
detect an enemy muckraker, they will stalk the muckraker until it gets too close to an EC or a slanderer, which will prompt the politician
to explode, hopefully killing the muckraker.

Bombs have very high influence count and are meant to attack ECs (prioritize neutral, then enemy ECs). In the event that the ally multiplier
exceeds 4.2, bombs will attempt to bomb their own ECs.

### Slanderer:

Nothing special here, just runs away from enemy muckrakers when it sees one.

Flag is set to SLAND opcode and data field displays the turn of creation to signal to EC how important it is to protect it.

### Muckraker:

Also nothing special here. Muckrakers are primarily scouts in the early game, trying to expose slanderers and discover EC coordinates.
Muckraker IDs are saved by EC and muckrakers will report EC locations back to the home EC using flags.

Some muckrakers are sent to explore a random direction, but once an enemy EC is discovered muckrakers are sent to invade the EC and apply pressure,
preventing the spawn of more slanderers.

When a muckraker discovers a slanderer, it will chase and expose it.
