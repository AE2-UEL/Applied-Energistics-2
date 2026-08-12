# Loot Tables

***
AE2 adds a few loot tables for generating its chest loot. Modpack developers may want to configure these loot
tables. The format of the loot tables are explained below.

## Mineshaft loot

In loot table: `minecraft:chests/abandoned_mineshaft`

Names of loot pools added to the table by this mod:

- `AE2 Crystals`
- `AE2 DUSTS`

The loot pools added for certus quartz crystals and dust that can spawn in mineshaft chests. These are added
programmatically, so they are not overridable by JSON.

If you are a modpack author that needs to override these, consider using something like LootTweaker.

## Meteorite loot

In custom loot table: `appliedenergistics2:meteor_loot`

Names of loot pools added to the table by this mod:

- `presses`
- `junk`

This determines what loot will be spawned in the center of meteorites. This loot table
**can** be overrided by JSON. The default loot table defined
[here](/src/main/resources/assets/appliedenergistics2/loot_tables/meteor_loot.json) (`appliedenergistics2:meteor_loot`)
uses some custom conditions and functions.

### Custom conditions

(1) `appliedenergistics2:check_tally` - Applicable to entries. This condition only allows the entry to be rolled `max`
number of times, regardless of the number of `rolls` specified by the pool. To be used together with the custom `tally`
function.   
Example:

```yaml
{
  "condition": "appliedenergistics2:check_tally",    # The name of this condition
  "id": "appliedenergistics2:material:1",            # The id of the item to check
  "max": 1,                                          # The max number of times this entry can be rolled
  "context_id": 0                                    # Optional. A unique integer if you want to tally the same id in a different entry.
}
```

Notes:

- `id` can be any string, but should match the id of a corresponding `tally` function.
- `context_id` defaults to 0 and is optional. The only use case for it is if you have a separate entry that has the same
  `id` as this entry, and you want to track a separate count of that entry.

(2) `appliedenergistics2:feature_enabled` - Applicable to pools or entries. This condition checks if all specified AE
`features` are enabled in the config.  
Example:

```yaml
{
  "condition": "appliedenergistics2:feature_enabled",    # The name of this condition
  "features": [ # The names of the features to check. This can be a string, or an array of strings.
    "PRESSES",                                           # Checks this feature is enabled...
    "SPAWN_PRESSES_IN_METEORITES"                        # and this feature.
  ]
}
```

Notes:

- `features` can be a single string, or an array of strings.
- See `appeng.core.features.AEFeature` for a list of valid feature names. The name to specify is the enum constant's
  name.

### Custom functions

(1) `appliedenergistics2:to_random_ore` - This function transforms the input item into a random `OreDictionary` item
specified.  
Example:

```yaml
{
  "function": "appliedenergistics2:to_random_ore",    # The name of this function
  "ores": [ # The list of ore dictionary values to choose from randomly.
    "nuggetIron",                                     # Transform into this oredict item...
    "nuggetGold"                                      # or this oredict item...
    # or any other ones you specify here.
  ]
}
```

(2) `appliedenergistics2:tally` - This function increments the number of times this entry has been rolled. To
be used for tracking with the custom `check_tally` condition.  
Example:

```yaml
{
  "function": "appliedenergistics2:tally",    # The name of this function
  "id": "minecraft:dirt",                     # Optional. The id of the item to check. This must match the id of a corresponding check_tally condition.               
  "context_id": 0                             # Optional. A unique integer if you want to tally the same id in a different entry.
}
```

Notes:

- `id` is optional and defaults to the item's registry name, appended by its metadata. For example, certus quartz
  crystal would default to an id of `appliedenergistics2:material:0`.
- `context_id` is optional and defaults to 0. The only use case for it is if you have a separate entry that has the same
  `id` as this entry, and you want to track a separate count of that entry.