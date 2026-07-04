# Haven-OG

Haven-OG is a WorldGuard add-on for safer safe zones.

## Features

- Adds the `haven` WorldGuard flag.
- Despawns hostile mobs inside flagged regions.
- Cancels hostile mob targeting against players, player-owned pets, and ridden mounts inside flagged regions.
- Clears nearby hostile mobs that already targeted protected entities entering a flagged region.

## Usage

Set the custom flag on each protected region:

```mcfunction
/rg flag spawn haven allow
/rg flag market haven allow
```

Unset or deny the flag to disable Haven-OG behavior:

```mcfunction
/rg flag spawn haven none
/rg flag market haven deny
```

## License

Haven-OG is public domain software.
